package com.optimistswe.mementolauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build

/**
 * Handles setting the device wallpaper.
 *
 * This class wraps Android's [WallpaperManager] to provide a simple API
 * for setting home screen, lock screen, or both wallpapers.
 *
 * ## Permissions
 * Requires `android.permission.SET_WALLPAPER` which is automatically
 * granted without user prompt.
 *
 * ## Usage Example
 * ```kotlin
 * val updater = WallpaperUpdater(context)
 * updater.setWallpaper(bitmap, WallpaperTarget.BOTH)
 * ```
 *
 * @param context Application or Activity context
 * @see WallpaperTarget for available wallpaper destinations
 */
class WallpaperUpdater(private val context: Context) {

    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(context)
    }

    /**
     * Sets the wallpaper to the provided bitmap.
     *
     * @param bitmap The image to set as wallpaper
     * @param target Where to set the wallpaper (home, lock, or both)
     * @return [WallpaperResult] indicating success or failure with details
     */
    fun setWallpaper(bitmap: Bitmap, target: WallpaperTarget): WallpaperResult {
        return try {
            when (target) {
                WallpaperTarget.HOME -> setHomeWallpaper(bitmap)
                WallpaperTarget.LOCK -> setLockWallpaper(bitmap)
                WallpaperTarget.BOTH -> setBothWallpapers(bitmap)
            }
            WallpaperResult.Success
        } catch (e: Exception) {
            WallpaperResult.Error(e.message ?: "Unknown error setting wallpaper")
        }
    }

    /**
     * Sets the home screen wallpaper only.
     *
     * @param bitmap The image to set as wallpaper
     * @throws Exception if wallpaper cannot be set
     */
    private fun setHomeWallpaper(bitmap: Bitmap) {
        wallpaperManager.setBitmap(
            bitmap,
            null, // Full screen, no crop
            true, // Allow backup
            WallpaperManager.FLAG_SYSTEM
        )
    }

    /**
     * Sets the lock screen wallpaper only.
     *
     * Note: Lock screen wallpaper support was added in Android N (API 24).
     * On older devices, this will set the home screen wallpaper instead.
     *
     * @param bitmap The image to set as wallpaper
     * @throws Exception if wallpaper cannot be set
     */
    private fun setLockWallpaper(bitmap: Bitmap) {
        wallpaperManager.setBitmap(
            bitmap,
            null,
            true,
            WallpaperManager.FLAG_LOCK
        )
    }

    /**
     * Sets both home and lock screen wallpapers.
     *
     * @param bitmap The image to set as wallpaper
     * @throws Exception if wallpaper cannot be set
     */
    private fun setBothWallpapers(bitmap: Bitmap) {
        wallpaperManager.setBitmap(
            bitmap,
            null,
            true,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        )
    }

    /**
     * Checks if the device supports setting wallpaper.
     *
     * Some devices (like Android TV or Wear OS) may not support wallpapers.
     *
     * @return true if wallpaper can be changed, false otherwise
     */
    fun isWallpaperSupported(): Boolean {
        return wallpaperManager.isWallpaperSupported &&
                wallpaperManager.isSetWallpaperAllowed
    }
}

/**
 * Target destination for wallpaper.
 *
 * @property HOME Only the home screen wallpaper
 * @property LOCK Only the lock screen wallpaper
 * @property BOTH Both home and lock screen wallpapers
 */
enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

/**
 * Result of a wallpaper update operation.
 *
 * Sealed class to represent either success or failure with error details.
 */
sealed class WallpaperResult {
    /**
     * Wallpaper was set successfully.
     */
    data object Success : WallpaperResult()

    /**
     * Wallpaper could not be set.
     *
     * @property message Description of what went wrong
     */
    data class Error(val message: String) : WallpaperResult()
}
