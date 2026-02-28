package com.optimistswe.mementolauncher

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Memento.
 *
 * Initializes app-wide dependencies and configures WorkManager for background updates.
 * This class is specified in AndroidManifest.xml as the application entry point.
 *
 * @see WorkManager for background wallpaper update scheduling
 */
@HiltAndroidApp
class MementoApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        // WorkManager is initialized automatically via Configuration.Provider
    }

    /**
     * Provides custom WorkManager configuration.
     *
     * Uses the default configuration with minimum logging in release builds.
     * This approach is recommended over manual initialization for better lifecycle handling.
     *
     * @return WorkManager configuration with default settings
     */
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
