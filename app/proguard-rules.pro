# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data classes used with DataStore and serialization
-keepclassmembers class com.optimistswe.mementolauncher.data.** { *; }

# Keep WorkManager worker classes
-keep class com.optimistswe.mementolauncher.worker.** { *; }

# Keep Hilt-generated components
-keep class com.optimistswe.mementolauncher.di.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.optimistswe.mementolauncher.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializer companions
-keepclassmembers class com.optimistswe.mementolauncher.data.** {
    *** Companion;
}
-keep class com.optimistswe.mementolauncher.data.**$serializer { *; }
-keep class com.optimistswe.mementolauncher.data.**$$serializer { *; }
