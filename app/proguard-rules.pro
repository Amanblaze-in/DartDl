# Add project specific ProGuard rules here.

#noinspection ShrinkerUnresolvedReference

-dontobfuscate

# ─── yt-dlp Android (native lib JNI bridges) ───────────────────────────────────
-keep class com.yausername.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }

# Keep native method declarations so JNI resolution doesn't break in release
-keepclasseswithmembernames class * {
    native <methods>;
}

# ─── Prevent R8 from removing Python/ffmpeg dynamic library loaders ─────────────
-keep class com.chaquo.** { *; }

# ─── Kotlinx Serialization ──────────────────────────────────────────────────────
# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# ─── Kotlin Reflection (needed by Koin and serialization) ───────────────────────
-keep class kotlin.reflect.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ─── OkHttp ─────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ─── Media3 / ExoPlayer ─────────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── Koin Dependency Injection ──────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.* <methods>;
}

# ─── Debugging: preserve line numbers for crash reports ─────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile