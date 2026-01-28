# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Google Play Billing
-keep class com.android.vending.billing.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Kotlin Serialization (if used)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep generic signature of Flow (for StateFlow, etc.)
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.StateFlow
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.SharedFlow

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep data classes
-keep class com.example.englishword.data.local.entity.** { *; }
-keep class com.example.englishword.domain.model.** { *; }
-keep class com.example.englishword.ui.**.UiState { *; }

# Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
