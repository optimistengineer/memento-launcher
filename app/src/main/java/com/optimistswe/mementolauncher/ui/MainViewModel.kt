package com.optimistswe.mementolauncher.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.optimistswe.mementolauncher.data.CalendarTheme
import com.optimistswe.mementolauncher.data.PreferencesRepository
import com.optimistswe.mementolauncher.data.UserPreferences
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.generator.CalendarConfig
import com.optimistswe.mementolauncher.generator.CalendarImageGenerator
import com.optimistswe.mementolauncher.wallpaper.WallpaperResult
import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import com.optimistswe.mementolauncher.wallpaper.WallpaperUpdater
import com.optimistswe.mementolauncher.worker.WallpaperUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the Memento app.
 *
 * Manages the app state and coordinates between UI, data layer, and domain logic.
 * Handles user preferences, calendar generation, and wallpaper setting.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val wallpaperUpdater: WallpaperUpdater,
    @ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val calculator = LifeCalendarCalculator()
    private val generator = CalendarImageGenerator()
    
    // Helper to schedule worker
    private fun scheduleWorker() {
        WallpaperUpdateWorker.scheduleWeeklyUpdate(context)
    }

    private val _preferences = MutableStateFlow<UserPreferences?>(null)
    val preferences: StateFlow<UserPreferences?> = _preferences.asStateFlow()

    private val _metrics = MutableStateFlow<CalendarMetrics?>(null)
    val metrics: StateFlow<CalendarMetrics?> = _metrics.asStateFlow()

    var previewBitmap: Bitmap? by mutableStateOf(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var wallpaperSet by mutableStateOf(false)
        private set

    // Screen dimensions for preview generation
    private var screenWidth = 1080
    private var screenHeight = 2400

    init {
        observePreferences()
    }

    /**
     * Observes user preferences and regenerates calendar when they change.
     */
    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.getUserPreferences().collect { prefs ->
                _preferences.value = prefs
                if (prefs.birthDate != null) {
                    generateCalendar(prefs)
                }
            }
        }
    }

    /**
     * Sets the screen dimensions for optimal preview generation.
     *
     * @param width Screen width in pixels
     * @param height Screen height in pixels
     */
    fun setScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    /**
     * Completes the onboarding flow and saves user preferences.
     *
     * @param birthDate User's birth date
     * @param lifeExpectancy Expected lifespan in years
     */
    fun completeOnboarding(birthDate: LocalDate?, lifeExpectancy: Int) {
        viewModelScope.launch {
            preferencesRepository.saveAllPreferences(
                birthDate = birthDate,
                lifeExpectancy = lifeExpectancy,
                wallpaperTarget = WallpaperTarget.LOCK, // Default to Lock Screen
                theme = CalendarTheme.DARK,
                dotStyle = com.optimistswe.mementolauncher.data.DotStyle.FILLED_CIRCLE
            )
            scheduleWorker()
        }
    }

    /**
     * Updates the user's birth date.
     *
     * @param birthDate New birth date
     */
    fun updateBirthDate(birthDate: LocalDate) {
        viewModelScope.launch {
            preferencesRepository.saveBirthDate(birthDate)
            wallpaperSet = false
        }
    }

    /**
     * Updates the life expectancy setting.
     *
     * @param years New life expectancy in years
     */
    fun updateLifeExpectancy(years: Int) {
        viewModelScope.launch {
            preferencesRepository.saveLifeExpectancy(years)
            wallpaperSet = false
        }
    }

    /**
     * Updates the auto open keyboard preference.
     */
    fun updateAutoOpenKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveAutoOpenKeyboard(enabled)
        }
    }

    /**
     * Updates the background style preference.
     */
    fun updateBackgroundStyle(style: com.optimistswe.mementolauncher.data.BackgroundStyle) {
        viewModelScope.launch {
            preferencesRepository.saveBackgroundStyle(style)
        }
    }

    /**
     * Updates the font size preference.
     */
    fun updateFontSize(size: com.optimistswe.mementolauncher.data.FontSize) {
        viewModelScope.launch {
            preferencesRepository.saveFontSize(size)
        }
    }

    /**
     * Updates the wallpaper target setting.
     *
     * @param target Where to apply wallpaper (home, lock, or both)
     */
    fun updateWallpaperTarget(target: WallpaperTarget) {
        viewModelScope.launch {
            preferencesRepository.saveWallpaperTarget(target)
        }
    }

    /**
     * Updates the calendar theme setting.
     *
     * @param theme New theme (dark or light)
     */
    fun updateTheme(theme: CalendarTheme) {
        viewModelScope.launch {
            preferencesRepository.saveTheme(theme)
            wallpaperSet = false
        }
    }

    /**
     * Updates the dot style setting.
     *
     * @param style New dot style (Circle, Ring, etc.)
     */
    fun updateDotStyle(style: com.optimistswe.mementolauncher.data.DotStyle) {
        viewModelScope.launch {
            preferencesRepository.saveDotStyle(style)
            wallpaperSet = false
        }
    }

    /**
     * Generates the calendar preview bitmap.
     *
     * @param prefs User preferences for generation
     */
    private fun generateCalendar(prefs: UserPreferences) {
        isLoading = true
        viewModelScope.launch {
            try {
                val birthDate = prefs.birthDate ?: return@launch
                val metrics = calculator.calculateMetrics(birthDate, prefs.lifeExpectancy)
                _metrics.value = metrics

                val config = createConfig(prefs.theme, prefs.dotStyle)
                previewBitmap?.recycle()
                previewBitmap = generator.generate(metrics, config)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Refreshes the calendar preview.
     */
    fun refresh() {
        _preferences.value?.let { prefs ->
            generateCalendar(prefs)
            wallpaperSet = false
        }
    }

    /**
     * Sets the current preview as the device wallpaper.
     */
    fun setWallpaper() {
        val bitmap = previewBitmap ?: return
        val prefs = _preferences.value ?: return

        viewModelScope.launch {
            // Force Lock Screen as per user request
            val result = wallpaperUpdater.setWallpaper(bitmap, WallpaperTarget.LOCK)
            wallpaperSet = result is WallpaperResult.Success
        }
    }

    /**
     * Creates a calendar configuration based on theme, dot style and screen dimensions.
     *
     * @param theme The calendar theme
     * @param dotStyle The dot aesthetic preference
     * @return Configured CalendarConfig
     */
    private fun createConfig(theme: CalendarTheme, dotStyle: com.optimistswe.mementolauncher.data.DotStyle): CalendarConfig {
        return when (theme) {
            CalendarTheme.DARK -> CalendarConfig(
                width = screenWidth,
                height = screenHeight,
                backgroundColor = 0xFF000000.toInt(),
                filledColor = 0xFFFFFFFF.toInt(),
                emptyColor = 0xFF4A4A4A.toInt(),
                dotStyle = dotStyle
            )
            CalendarTheme.LIGHT -> CalendarConfig(
                width = screenWidth,
                height = screenHeight,
                backgroundColor = 0xFFFFFFFF.toInt(),
                filledColor = 0xFF000000.toInt(),
                emptyColor = 0xFFCCCCCC.toInt(),
                dotStyle = dotStyle
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewBitmap?.recycle()
    }
}
