package com.optimistswe.mementolauncher.worker

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.optimistswe.mementolauncher.data.CalendarTheme
import com.optimistswe.mementolauncher.data.DotStyle
import com.optimistswe.mementolauncher.data.PreferencesRepository
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.generator.CalendarConfig
import com.optimistswe.mementolauncher.generator.CalendarImageGenerator
import com.optimistswe.mementolauncher.wallpaper.WallpaperResult
import com.optimistswe.mementolauncher.wallpaper.WallpaperUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that generates and sets the life calendar wallpaper.
 *
 * This worker runs periodically (configured for weekly updates) to:
 * 1. Read user preferences from DataStore
 * 2. Calculate the current weeks lived
 * 3. Generate a new calendar image
 * 4. Set it as the device wallpaper
 *
 * ## Scheduling
 * The work is scheduled using [PeriodicWorkRequest] with a 7-day interval.
 * WorkManager handles:
 * - Surviving device reboots
 * - Respecting battery optimization (Doze mode)
 * - Retry on failure
 *
 * ## Usage
 * ```kotlin
 * // Schedule the worker (typically called once during setup)
 * WallpaperUpdateWorker.scheduleWeeklyUpdate(context)
 *
 * // Cancel scheduled updates
 * WallpaperUpdateWorker.cancelScheduledUpdates(context)
 * ```
 *
 * @param context Application context
 * @param params Worker parameters from WorkManager
 * @see WorkManager for scheduling details
 */
@HiltWorker
class WallpaperUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {

    companion object {
        /**
         * Unique work name for identifying this periodic work.
         * Used to replace existing work when rescheduling.
         */
        private const val WORK_NAME = "life_calendar_wallpaper_update"

        /**
         * Interval between wallpaper updates in days.
         * Set to 7 for weekly updates matching the week-based calendar.
         */
        private const val UPDATE_INTERVAL_DAYS = 7L

        /**
         * Schedules the wallpaper update to run weekly.
         *
         * If a scheduled update already exists, it will be replaced with
         * the new schedule. This ensures only one instance is active.
         *
         * @param context Application context for accessing WorkManager
         */
        fun scheduleWeeklyUpdate(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WallpaperUpdateWorker>(
                UPDATE_INTERVAL_DAYS, TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        /**
         * Cancels any scheduled wallpaper updates.
         *
         * Call this if the user wants to disable automatic updates
         * or during app uninstall/cleanup.
         *
         * @param context Application context for accessing WorkManager
         */
        fun cancelScheduledUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Triggers an immediate wallpaper update.
         *
         * Useful for manual "refresh" button or after settings change.
         * This runs in addition to the scheduled updates.
         *
         * @param context Application context for accessing WorkManager
         */
        fun triggerImmediateUpdate(context: Context) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<WallpaperUpdateWorker>()
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private val calculator = LifeCalendarCalculator()
    private val generator = CalendarImageGenerator()
    private val wallpaperUpdater = WallpaperUpdater(applicationContext)

    /**
     * Performs the wallpaper update work.
     *
     * This method is called by WorkManager on a background thread.
     * It reads preferences, generates the calendar, and sets the wallpaper.
     *
     * @return [Result.success] if wallpaper was updated, [Result.retry] on recoverable
     *         errors, or [Result.failure] on permanent errors
     */
    override suspend fun doWork(): Result {
        return try {
            // Get user preferences
            val preferences = preferencesRepository.getUserPreferences().first()

            // Validate that setup is complete
            val birthDate = preferences.birthDate
            if (birthDate == null) {
                // User hasn't completed setup yet, skip silently
                return Result.success()
            }

            // Calculate life metrics
            val metrics = calculator.calculateMetrics(
                birthDate = birthDate,
                lifeExpectancy = preferences.lifeExpectancy
            )

            // Get screen dimensions for optimal wallpaper size
            val (width, height) = getScreenDimensions()

            // Create calendar configuration based on theme and dot style
            val config = createConfig(width, height, preferences.theme, preferences.dotStyle)

            // Generate the calendar image
            val bitmap = generator.generate(metrics, config)

            try {
                // Set as wallpaper
                val result = wallpaperUpdater.setWallpaper(bitmap, preferences.wallpaperTarget)

                when (result) {
                    is WallpaperResult.Success -> Result.success()
                    is WallpaperResult.Error -> Result.retry()
                }
            } finally {
                // Clean up bitmap to free memory even if setWallpaper throws
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Log error for debugging
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Gets the device's screen dimensions for optimal wallpaper sizing.
     *
     * @return Pair of (width, height) in pixels
     */
    private fun getScreenDimensions(): Pair<Int, Int> {
        val windowManager = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    /**
     * Creates a calendar configuration based on the user's theme preference.
     *
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     * @param theme User's selected theme
     * @return Configured [CalendarConfig] for image generation
     */
    private fun createConfig(width: Int, height: Int, theme: CalendarTheme, dotStyle: DotStyle): CalendarConfig {
        return when (theme) {
            CalendarTheme.DARK -> CalendarConfig(
                width = width,
                height = height,
                backgroundColor = 0xFF000000.toInt(),
                filledColor = 0xFFFFFFFF.toInt(),
                emptyColor = 0xFF4A4A4A.toInt(),
                dotStyle = dotStyle
            )
            CalendarTheme.LIGHT -> CalendarConfig(
                width = width,
                height = height,
                backgroundColor = 0xFFFFFFFF.toInt(),
                filledColor = 0xFF000000.toInt(),
                emptyColor = 0xFFCCCCCC.toInt(),
                dotStyle = dotStyle
            )
        }
    }
}
