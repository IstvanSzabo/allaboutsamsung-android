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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontobfuscate
-keepattributes *Annotation*,SourceFile,LineNumberTable

-keep class com.google.api.services.youtube.model.*
-keep class com.google.api.client.googleapis.json.* { *; }

# Needed due to https://github.com/Kotlin/kotlinx.coroutines/issues/858
-keep @interface kotlin.coroutines.jvm.internal.DebugMetadata { *; }