# WhAlert ProGuard/R8 rules
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.whalert.app.**$$serializer { *; }
-keepclassmembers class com.whalert.app.** { *** Companion; }
-keepclasseswithmembers class com.whalert.app.** { kotlinx.serialization.KSerializer serializer(...); }

# libphonenumber
-keep class com.google.i18n.phonenumbers.** { *; }

# OkHttp / okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Tink (transitive of androidx.security-crypto): errorprone/JSR305 annotations are
# compile-time only and stripped at runtime — safe to ignore for R8.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn net.jodah.failsafe.**

# Biometric: keep fragment-based API entry points referenced reflectively by the library
-keep class androidx.biometric.** { *; }
