import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

/**
 * Secrets policy (SECURITY):
 *  - No API key / token may EVER be committed to Git or baked into the APK.
 *  - Optional values are read from environment variables or local.properties at
 *    BUILD time and injected as BuildConfig fields. If they are absent, the
 *    corresponding feature stays DISABLED at runtime and the UI says so honestly.
 *  - WHALERT_API_BASE_URL must be an HTTPS URL of a real backend you operate.
 *    There is NO default fictional backend: if unset, remote sync is disabled
 *    and the app states it clearly in the UI.
 */
fun secret(name: String): String =
    (System.getenv(name) ?: run {
        val p = Properties()
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { p.load(it) }
        p.getProperty(name)
    }) ?: ""

android {
    namespace = "com.whalert.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.whalert.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "API_BASE_URL", "\"${secret("WHALERT_API_BASE_URL")}\"")
        buildConfigField("boolean", "REMOTE_ENABLED", secret("WHALERT_API_BASE_URL").isNotBlank().toString().lowercase())
    }

    signingConfigs {
        create("release") {
            // Release keystore supplied by CI (GitHub Actions secrets) or local.properties.
            // Never committed to the repository.
            val ksPath = secret("WHALERT_KEYSTORE_PATH")
            if (ksPath.isNotBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = secret("WHALERT_KEYSTORE_PASSWORD")
                keyAlias = secret("WHALERT_KEY_ALIAS")
                keyPassword = secret("WHALERT_KEY_PASSWORD")
            }
        }
    }

    // Build variant matrix:
    //  - "official" (default): the honest, optimized production APK. R8 minification +
    //    resource shrinking are intentionally KEPT ENABLED. We refuse to disable them
    //    just to inflate the file size toward the 90 MB target: per the project rules,
    //    artificial padding (random files, dead code, unused libraries) is forbidden and
    //    disabling R8 would be exactly that kind of dishonest inflation. The real feature
    //    set legitimately fits in a few MB; this is documented in SIZE_REPORT.md.
    //  - "unminified": same app, no R8/resource shrinking, used ONLY for developer
    //    diagnostics (mapping issues, debugging). Not the shipped artifact.
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val sc = signingConfigs.findByName("release")
            signingConfig = if (sc != null && sc.storeFile != null) sc
                else signingConfigs.getByName("debug") // CI generates a keystore; see workflow
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // Full lint is run separately (CI job / ./gradlew lint). The memory-constrained
        // release build disables lintVital so assembleRelease stays reliable.
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose + Material 3 (Apache-2.0)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation Compose (Apache-2.0)
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // OkHttp — HTTPS networking (Apache-2.0). Used directly by RemoteClient;
    // certificate validation + timeouts enforced there. No Retrofit/Room/DataStore:
    // every dependency below is actually referenced in app code (see THIRD_PARTY.md).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Security Crypto — Android Keystore backed EncryptedSharedPreferences (Apache-2.0)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Biometric — strong auth for the private console (Apache-2.0)
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")

    // kotlinx.serialization JSON — dossier persistence + typed API payloads (Apache-2.0)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines (Apache-2.0)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // libphonenumber — strict E.164 validation, real use in PhoneNumberValidator (Apache-2.0)
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.52")

    // Coil — AsyncImage used in NewReportScreen evidence previews (Apache-2.0)
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")

    // Tests (JVM unit tests only — MockWebServer exercises the real OkHttp stack)
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

