# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data classes used with DataStore and serialization
-keepclassmembers class com.betteruniverse.mementolauncher.data.** { *; }

# Keep WorkManager worker classes
-keep class com.betteruniverse.mementolauncher.worker.** { *; }

# Keep Hilt-generated components
-keep class com.betteruniverse.mementolauncher.di.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.betteruniverse.mementolauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializer companions
-keepclassmembers class com.betteruniverse.mementolauncher.data.** {
    *** Companion;
}
-keep class com.betteruniverse.mementolauncher.data.**$serializer { *; }
-keep class com.betteruniverse.mementolauncher.data.**$$serializer { *; }
