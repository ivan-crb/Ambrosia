# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/IvanCM/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room
-keep class com.ivan_crb.ambrosia.data.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.Serializer *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.Polymorphic *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}
-keep class kotlinx.serialization.json.Json { *; }
-keep class * extends kotlinx.serialization.KSerializer { *; }
-keep class com.ivan_crb.ambrosia.data.Converters { *; }

# General Compose
-keep class androidx.compose.ui.platform.** { *; }
