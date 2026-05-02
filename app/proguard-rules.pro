# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Сохраняем информацию о строках в стек-трейсах (для отладки крэшей).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------
# Retrofit
# https://square.github.io/retrofit/#download
# ---------------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit does reflection on generic parameters and @Metadata.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# ---------------------------------------------------------------
# OkHttp
# ---------------------------------------------------------------
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------------------------------------------------------------
# Gson
# https://github.com/google/gson/blob/main/examples/android-proguard-example/proguard.cfg
# ---------------------------------------------------------------
-keepattributes *Annotation*

# Gson generic type information — нужна для корректной десериализации.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Сохраняем все DTO-классы — Gson обращается к их полям рефлексией.
-keep class com.shestikpetr.meteoapp.data.remote.dto.** { *; }

# Retrofit API-интерфейс.
-keep interface com.shestikpetr.meteoapp.data.remote.api.** { *; }

# ---------------------------------------------------------------
# OpenStreetMap (osmdroid)
# ---------------------------------------------------------------
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ---------------------------------------------------------------
# Kotlin coroutines / serialization metadata
# ---------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembernames class kotlinx.** { volatile <fields>; }
