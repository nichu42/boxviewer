# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers for Play Console deobfuscation (mapping.txt is uploaded automatically).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- BoxViewer keeps for R8 full mode ---

# Moshi + Kotlin codegen (reflection via KotlinJsonAdapterFactory)
-keep class de.nichu42.boxviewer.data.api.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# Room entities / DAOs (kept via @Entity already, but explicit for safety)
-keep class de.nichu42.boxviewer.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Retrofit service interface
-keep interface de.nichu42.boxviewer.data.api.OpenSenseMapApi { *; }
-keep class retrofit2.** { *; }

# ZXing core (no obfuscation needed, keep to avoid shrinking)
-keep class com.google.zxing.** { *; }

# Compose / Navigation / Material — handled by AGP built-ins; no manual keep needed.

# CrashHandler / Application — keep for diagnostics
-keep class de.nichu42.boxviewer.BoxViewerApplication { *; }
-keep class de.nichu42.boxviewer.util.CrashHandler { *; }
-keep class de.nichu42.boxviewer.util.ApiLogger { *; }

# OkHttp / Okio — keep enum and companion objects
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Preserve BuildConfig for User-Agent header
-keep class de.nichu42.boxviewer.BuildConfig { *; }
