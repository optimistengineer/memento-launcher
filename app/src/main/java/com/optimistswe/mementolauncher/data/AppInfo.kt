package com.optimistswe.mementolauncher.data

/**
 * Represents a launchable application on the device.
 *
 * @property label Display name of the app
 * @property packageName Unique package identifier (e.g., "com.google.android.youtube")
 * @property activityName Fully qualified activity class name for launching
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String
)
