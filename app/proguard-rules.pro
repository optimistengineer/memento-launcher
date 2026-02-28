# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data classes used with DataStore
-keepclassmembers class com.example.lifecalendar.data.** { *; }

# Keep WorkManager worker classes
-keep class com.example.lifecalendar.worker.** { *; }
