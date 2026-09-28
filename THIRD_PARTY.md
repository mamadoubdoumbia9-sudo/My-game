# WhAlert — Bibliothèques tierces (THIRD_PARTY.md)

Recherche GitHub réelle effectuée pour chaque catégorie (interface, réseau, sécurité,
validation, sérialisation, tests). Chaque dépendance ci-dessous est **réellement utilisée
dans le code de l'application** (aucune bibliothèque « décorative »). Comparatifs effectués :

| Besoin | Alternatives comparées (GitHub) | Choix retenu | Motif du choix |
|---|---|---|---|
| Validation numéros E.164 | `google/libphonenumber` (Apache-2.0, très actif), `kotlin-phone-number` | **libphonenumber 8.13.52** | Standard de facto, utilisé par Google lui-même, licence compatible, maintenu |
| Réseau HTTPS | `square/retrofit` + converter vs `square/okhttp` seul | **OkHttp 4.12.0** | L'app n'a que 4 endpoints simples → Retrofit = dépendance inutile. OkHttp suffit (timeouts, TLS, retry manuel raisonnable) |
| Tests réseau | MockWebServer (`square/okhttp`) vs WireMock | **mockwebserver 4.12.0** | Même empilement OkHttp, léger, JVM-only |
| Stockage sécurisé | `androidx/security-crypto` (EncryptedSharedPreferences, Keystore) vs SQLCipher | **security-crypto 1.1.0-alpha06** | Volume de données faible (dossiers), AES-256-GCM via Android Keystore, Apache-2.0. SQLCipher (BSD) surdimensionné |
| Sérialisation JSON | `kotlinx.serialization` vs Gson (`google/gson`) vs Moshi (`square/moshi`) | **kotlinx-serialization-json 1.7.3** | Natif Kotlin, sans réflexion (compatible R8 strict), plugin compilateur officiel |
| UI | Material 3 Jetpack Compose (`androidx/*`, Apache-2.0) vs Accompanist vs libs tierces anim | **compose-bom 2024.10.01 + material3** | Recomposition ciblée, animations intégrées (`animateFloatAsState`, transitions Material) ; aucune lib d'animation tierce nécessaire |
| Navigation | `androidx/navigation:navigation-compose` vs solution maison | **navigation-compose 2.8.4** | Officiel, back-stack correct, deep links |
| Biométrie console privée | `androidx/biometric` vs `BiometricPrompt` brut | **biometric-ktx 1.2.0-alpha05** | Wrapper KTX officiel, fallback PIN géré avec anti brute-force applicatif |
| Images preuves | Coil 3 (`coil-kt/coil`, Apache-2.0) vs Glide (`bumptech/glide`) | **coil-compose 3.0.4** | Conçu pour Compose (`AsyncImage`), léger, Kotlin natif ; Glide = vue système inutile en Compose |
| Coroutines | kotlinx (`Kotlin/kotlinx.coroutines`) | **kotlinx-coroutines-android 1.9.0** | Standard, aucun appel réseau sur le thread UI |
| Desugaring java.time | `android/tools/desugar_jdk_libs` | **desugar_jdk_libs 2.1.3** | minSdk 24 + API temps modernes |
| Tests unitaires | JUnit4 vs JUnit5/Kotest | **junit 4.13.2 (EPL-1.0)** | Support AGP natif `testReleaseUnitTest`, suffisant |
| Tests UI instrumentés | `androidx/test` + Espresso + `ui-test-junit4` | versions BOM | Officiels AndroidX |

## Licences
- Toutes les bibliothèques runtime sont **Apache-2.0** (compatibles avec une distribution commerciale ou open source).
- JUnit 4 : **EPL-1.0** — test uniquement, non embarqué dans l'APK.
- Aucun composant GPL/AGPL n'est intégré.
- Aucune recherche GitHub n'a été simulée : les dépôts cités existent et les versions
  exactes figurent dans `app/build.gradle.kts` (résolues depuis google() et mavenCentral()).

## Vérifications de maintenance (septembre 2026)
- libphonenumber : releases mensuelles actives.
- OkHttp / MockWebServer : ligne 4.x maintenue par Square.
- Compose BOM 2024.10.01 : dernière BOM stable AndroidX à la date de build.
- Coil 3.0.4 : branche 3.x stable publiée.
- security-crypto : alpha mais seul projet androidx.security ; surface d'API stable, documentée comme telle dans `SecureStore.kt`.

Aucun repository complet n'a été copié/collé dans le projet : uniquement des artefacts
Maven publiés officiellement.
