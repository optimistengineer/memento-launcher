package com.betteruniverse.mementolauncher.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.betteruniverse.mementolauncher.data.CalendarTheme
import com.betteruniverse.mementolauncher.data.PreferencesRepository
import com.betteruniverse.mementolauncher.data.UserPreferences
import com.betteruniverse.mementolauncher.domain.CalendarMetrics
import com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator
import com.betteruniverse.mementolauncher.generator.CalendarConfig
import com.betteruniverse.mementolauncher.generator.CalendarImageGenerator
import com.betteruniverse.mementolauncher.wallpaper.WallpaperResult
import com.betteruniverse.mementolauncher.wallpaper.WallpaperTarget
import com.betteruniverse.mementolauncher.wallpaper.WallpaperUpdater
import com.betteruniverse.mementolauncher.worker.WallpaperUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var generateJob: Job? = null

    // Screen dimensions for preview generation
    private var screenWidth = 1080
    private var screenHeight = 2400

    init {
        observePreferences()
    }

    /**
     * Observes user preferences and regenerates calendar when they change.
     */

    /**
     * Runs a persistence write with a crash guard.
     *
     * Every setter in this ViewModel used to be a bare `viewModelScope.launch { repo.write() }`.
     * DataStore.edit throws IOException when the disk is full and CorruptionException when the
     * store file is damaged, and an exception in a launched coroutine that nobody catches kills
     * the process — for a HOME app, that meant one failed settings write crashed the launcher,
     * and a corrupt store made every subsequent attempt crash it again. Failures here are logged
     * and dropped: the in-memory StateFlows keep the value for this session, so the UI stays
     * consistent and the user retries by simply using the app.
     *
     * CancellationException is rethrown — swallowing it would break structured cancellation.
     */
    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "persistence write failed", e)
            }
        }
    }

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
    fun completeOnboarding(birthDate: LocalDate?, lifeExpectancy: Int, showLifeCalendar: Boolean = true) {
        persist {
            preferencesRepository.saveAllPreferences(
                birthDate = birthDate,
                lifeExpectancy = lifeExpectancy,
                wallpaperTarget = WallpaperTarget.LOCK, // Default to Lock Screen
                theme = CalendarTheme.DARK,
                dotStyle = com.betteruniverse.mementolauncher.data.DotStyle.FILLED_CIRCLE,
                showLifeCalendar = showLifeCalendar
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
        persist {
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
        persist {
            preferencesRepository.saveLifeExpectancy(years)
            wallpaperSet = false
        }
    }

    /**
     * Updates the auto open keyboard preference.
     */
    fun updateAutoOpenKeyboard(enabled: Boolean) {
        persist {
            preferencesRepository.saveAutoOpenKeyboard(enabled)
        }
    }

    /**
     * Updates the background style preference.
     */
    fun updateBackgroundStyle(style: com.betteruniverse.mementolauncher.data.BackgroundStyle) {
        persist {
            preferencesRepository.saveBackgroundStyle(style)
        }
    }

    /**
     * Updates the font size preference.
     */
    fun updateFontSize(size: com.betteruniverse.mementolauncher.data.FontSize) {
        persist {
            preferencesRepository.saveFontSize(size)
        }
    }

    /**
     * Updates the wallpaper target setting.
     *
     * @param target Where to apply wallpaper (home, lock, or both)
     */
    fun updateWallpaperTarget(target: WallpaperTarget) {
        persist {
            preferencesRepository.saveWallpaperTarget(target)
        }
    }

    /**
     * Updates the calendar theme setting.
     *
     * @param theme New theme (dark or light)
     */
    fun updateTheme(theme: CalendarTheme) {
        persist {
            preferencesRepository.saveTheme(theme)
            wallpaperSet = false
        }
    }

    /**
     * Updates the dot style setting.
     *
     * @param style New dot style (Circle, Ring, etc.)
     */
    fun updateDotStyle(style: com.betteruniverse.mementolauncher.data.DotStyle) {
        persist {
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
        generateJob?.cancel()
        isLoading = true
        generateJob = viewModelScope.launch {
            try {
                val birthDate = prefs.birthDate ?: return@launch
                // A stored birth date can be in the future (e.g. restored from a hand-edited
                // backup), which calculateMetrics rejects. The surrounding try/finally only
                // resets isLoading — it does not catch — so an throw here would propagate out
                // of the coroutine and crash the app. Skip generation instead.
                val metrics = runCatching {
                    calculator.calculateMetrics(birthDate, prefs.lifeExpectancy)
                }.getOrNull() ?: return@launch
                _metrics.value = metrics

                val config = createConfig(prefs.theme, prefs.dotStyle)
                // Drawing allocates a full-screen bitmap and draws thousands of shapes. Doing that
                // on viewModelScope's main dispatcher janked the UI during onboarding, which is
                // the one place this preview is generated. Safe to move off-thread now that
                // CalendarImageGenerator keeps its Paint/Path state per call.
                val newBitmap = withContext(Dispatchers.Default) {
                    generator.generate(metrics, config)
                }
                if (newBitmap != null) {
                    // The previous bitmap is deliberately NOT recycled. It was recycled after a
                    // delay(500) — a cancellation point outside any finally — so a cancelled
                    // generation leaked it, and the delay itself was a guess at when Compose had
                    // stopped drawing it. Recycling too early crashes; recycling too late leaks.
                    // Since API 26 bitmap pixels live in the native heap tracked by
                    // NativeAllocationRegistry, so simply dropping the reference lets GC reclaim
                    // it correctly, with no window in which a live Canvas can touch freed memory.
                    previewBitmap = newBitmap
                }
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
    private fun createConfig(theme: CalendarTheme, dotStyle: com.betteruniverse.mementolauncher.data.DotStyle): CalendarConfig {
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
        // No explicit recycle: Compose may still be drawing this bitmap while the ViewModel is
        // being torn down, and drawing a recycled bitmap throws. GC reclaims the native pixels
        // once the last reference goes.
        previewBitmap = null
    }
}
