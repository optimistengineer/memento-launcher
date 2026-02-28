package com.optimistswe.mementolauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for managing global user preferences and life calendar configuration.
 *
 * This is the primary repository for the launcher's configuration. it manages everything from
 * the user's birth date (used for the life calendar) to UI aesthetics like themes and font sizes.
 * Data is persisted using Jetpack DataStore.
 *
 * @property dataStore The [DataStore] instance used for persisting configuration.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object PreferencesKeys {
        val BIRTH_DATE_EPOCH_DAYS = longPreferencesKey("birth_date_epoch_days")
        val LIFE_EXPECTANCY = intPreferencesKey("life_expectancy")
        val WALLPAPER_TARGET = stringPreferencesKey("wallpaper_target")
        val THEME = stringPreferencesKey("theme")
        val DOT_STYLE = stringPreferencesKey("dot_style")
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val AUTO_OPEN_KEYBOARD = booleanPreferencesKey("auto_open_keyboard")
        val BACKGROUND_STYLE = stringPreferencesKey("background_style")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val SEARCH_BAR_POSITION = stringPreferencesKey("search_bar_position")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val DISTRACTING_PACKAGES = stringSetPreferencesKey("distracting_packages")
        val MINDFUL_MESSAGE = stringPreferencesKey("mindful_message")
        val BLOCK_SHORT_FORM_CONTENT = booleanPreferencesKey("block_short_form")
        val USAGE_NUDGE_ENABLED = booleanPreferencesKey("usage_nudge_enabled")
        val USAGE_NUDGE_MINUTES = intPreferencesKey("usage_nudge_minutes")
    }

    companion object {
        val DEFAULT_DISTRACTING_APPS = setOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android",
            "com.snapchat.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.google.android.youtube",
            "com.reddit.frontpage",
            "com.pinterest"
        )
    }

    /**
     * Retrieves all user preferences as a reactive [Flow].
     *
     * Handles [java.io.IOException] by emitting empty preferences to prevent the app from crashing
     * on DataStore corruption.
     *
     * @return A [Flow] of [UserPreferences] updated whenever any preference changes.
     */
    fun getUserPreferences(): Flow<UserPreferences> {
        return dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val epochDays = preferences[PreferencesKeys.BIRTH_DATE_EPOCH_DAYS]
                val birthDate = epochDays?.let { LocalDate.ofEpochDay(it) }

                UserPreferences(
                    birthDate = birthDate,
                    lifeExpectancy = preferences[PreferencesKeys.LIFE_EXPECTANCY]
                        ?: LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY,
                    wallpaperTarget = preferences[PreferencesKeys.WALLPAPER_TARGET]
                        ?.let { WallpaperTarget.valueOf(it) }
                        ?: WallpaperTarget.BOTH,
                    theme = preferences[PreferencesKeys.THEME]
                        ?.let { CalendarTheme.valueOf(it) }
                        ?: CalendarTheme.DARK,
                    dotStyle = preferences[PreferencesKeys.DOT_STYLE]
                        ?.let { DotStyle.valueOf(it) }
                        ?: DotStyle.FILLED_CIRCLE,
                    backgroundStyle = preferences[PreferencesKeys.BACKGROUND_STYLE]
                        ?.let { BackgroundStyle.valueOf(it) }
                        ?: BackgroundStyle.MATRIX_GRID,
                    fontSize = preferences[PreferencesKeys.FONT_SIZE]
                        ?.let { FontSize.valueOf(it) }
                        ?: FontSize.MEDIUM,
                    isSetupComplete = preferences[PreferencesKeys.IS_SETUP_COMPLETE] ?: false,
                    autoOpenKeyboard = preferences[PreferencesKeys.AUTO_OPEN_KEYBOARD] ?: true,
                    clockStyle = preferences[PreferencesKeys.CLOCK_STYLE]
                        ?.let { ClockStyle.valueOf(it) }
                        ?: ClockStyle.H24,
                    searchBarPosition = preferences[PreferencesKeys.SEARCH_BAR_POSITION]
                        ?.let { SearchBarPosition.valueOf(it) }
                        ?: SearchBarPosition.TOP,
                    hiddenPackages = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet(),
                    distractingPackages = preferences[PreferencesKeys.DISTRACTING_PACKAGES] ?: DEFAULT_DISTRACTING_APPS,
                    mindfulMessage = preferences[PreferencesKeys.MINDFUL_MESSAGE] ?: "IS THIS\nINTENTIONAL?",
                    blockShortFormContent = preferences[PreferencesKeys.BLOCK_SHORT_FORM_CONTENT] ?: false,
                    usageNudgeEnabled = preferences[PreferencesKeys.USAGE_NUDGE_ENABLED] ?: false,
                    usageNudgeMinutes = preferences[PreferencesKeys.USAGE_NUDGE_MINUTES] ?: 15
                )
            }
    }

    /**
     * Persists the user's birth date.
     *
     * @param birthDate The user's date of birth.
     */
    suspend fun saveBirthDate(birthDate: LocalDate) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIRTH_DATE_EPOCH_DAYS] = birthDate.toEpochDay()
        }
    }

    /**
     * Persists the user's expected life span in years.
     *
     * @param years The number of years the user expects or wants to live.
     */
    suspend fun saveLifeExpectancy(years: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIFE_EXPECTANCY] = years
        }
    }

    /**
     * Sets which display (Home, Lock, or Both) the calendar wallpaper should be applied to.
     *
     * @param target The [WallpaperTarget] defining the destination.
     */
    suspend fun saveWallpaperTarget(target: WallpaperTarget) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WALLPAPER_TARGET] = target.name
        }
    }

    /**
     * Sets the visual theme (Light or Dark) for the launcher and calendar.
     *
     * @param theme The [CalendarTheme] to apply.
     */
    suspend fun saveTheme(theme: CalendarTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    /**
     * Sets the aesthetic style for the dots in the calendar grid.
     *
     * @param style The [DotStyle] to use for rendering.
     */
    suspend fun saveDotStyle(style: DotStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOT_STYLE] = style.name
        }
    }

    /**
     * Marks that the user has completed the initial onboarding/setup flow.
     */
    suspend fun setSetupComplete() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = true
        }
    }

    /**
     * Sets whether the soft keyboard should automatically appear when entering the app drawer.
     *
     * @param enabled If `true`, the search field will request focus immediately.
     */
    suspend fun saveAutoOpenKeyboard(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_OPEN_KEYBOARD] = enabled
        }
    }

    /**
     * Sets the background visual style (e.g., Solid Black or Matrix Grid).
     *
     * @param style The [BackgroundStyle] to apply.
     */
    suspend fun saveBackgroundStyle(style: BackgroundStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BACKGROUND_STYLE] = style.name
        }
    }

    /**
     * Sets the global font and icon scaling size.
     *
     * @param size The [FontSize] (Small, Medium, or Large) to apply.
     */
    suspend fun saveFontSize(size: FontSize) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SIZE] = size.name
        }
    }

    /**
     * Sets the clock display format.
     *
     * @param style The [ClockStyle] (24h, 12h, or 24h with seconds).
     */
    suspend fun saveClockStyle(style: ClockStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOCK_STYLE] = style.name
        }
    }

    /**
     * Sets the vertical position of the search bar in the app drawer.
     *
     * @param position Top or Bottom.
     */
    suspend fun saveSearchBarPosition(position: SearchBarPosition) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_BAR_POSITION] = position.name
        }
    }

    /**
     * Adds or removes a package name from the hidden apps list.
     *
     * @param packageName The package name to toggle visibility for.
     */
    suspend fun togglePackageVisibility(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet()
            val updated = current.toMutableSet()
            if (updated.contains(packageName)) {
                updated.remove(packageName)
            } else {
                updated.add(packageName)
            }
            preferences[PreferencesKeys.HIDDEN_PACKAGES] = updated
        }
    }

    /**
     * Adds or removes a package name from the distracting apps list.
     *
     * @param packageName The package name to toggle.
     */
    suspend fun toggleDistractingPackage(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DISTRACTING_PACKAGES] ?: emptySet()
            val updated = current.toMutableSet()
            if (updated.contains(packageName)) {
                updated.remove(packageName)
            } else {
                updated.add(packageName)
            }
            preferences[PreferencesKeys.DISTRACTING_PACKAGES] = updated
        }
    }

    /**
     * Persists a custom mindful delay message.
     *
     * @param message Text up to 10 words.
     */
    suspend fun saveMindfulMessage(message: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.MINDFUL_MESSAGE] = message
        }
    }

    /**
     * Persists whether the generic shorts/reels blocker service is enabled in settings.
     */
    suspend fun saveBlockShortFormContent(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BLOCK_SHORT_FORM_CONTENT] = enabled
        }
    }

    /** Persists whether the usage nudge overlay is enabled for distracting apps. */
    suspend fun saveUsageNudgeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_NUDGE_ENABLED] = enabled
        }
    }

    /** Persists the number of continuous minutes before a usage nudge fires. */
    suspend fun saveUsageNudgeMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USAGE_NUDGE_MINUTES] = minutes
        }
    }

    suspend fun restoreAll(
        birthDateEpochDays: Long?,
        lifeExpectancy: Int,
        wallpaperTarget: String,
        theme: String,
        dotStyle: String,
        backgroundStyle: String,
        fontSize: String,
        isSetupComplete: Boolean,
        autoOpenKeyboard: Boolean,
        clockStyle: String,
        searchBarPosition: String,
        hiddenPackages: Set<String>,
        distractingPackages: Set<String>,
        mindfulMessage: String,
        blockShortFormContent: Boolean,
        usageNudgeEnabled: Boolean,
        usageNudgeMinutes: Int
    ) {
        dataStore.edit { preferences ->
            if (birthDateEpochDays != null) {
                preferences[PreferencesKeys.BIRTH_DATE_EPOCH_DAYS] = birthDateEpochDays
            } else {
                preferences.remove(PreferencesKeys.BIRTH_DATE_EPOCH_DAYS)
            }
            preferences[PreferencesKeys.LIFE_EXPECTANCY] = lifeExpectancy
            preferences[PreferencesKeys.WALLPAPER_TARGET] = wallpaperTarget
            preferences[PreferencesKeys.THEME] = theme
            preferences[PreferencesKeys.DOT_STYLE] = dotStyle
            preferences[PreferencesKeys.BACKGROUND_STYLE] = backgroundStyle
            preferences[PreferencesKeys.FONT_SIZE] = fontSize
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = isSetupComplete
            preferences[PreferencesKeys.AUTO_OPEN_KEYBOARD] = autoOpenKeyboard
            preferences[PreferencesKeys.CLOCK_STYLE] = clockStyle
            preferences[PreferencesKeys.SEARCH_BAR_POSITION] = searchBarPosition
            preferences[PreferencesKeys.HIDDEN_PACKAGES] = hiddenPackages
            preferences[PreferencesKeys.DISTRACTING_PACKAGES] = distractingPackages
            preferences[PreferencesKeys.MINDFUL_MESSAGE] = mindfulMessage
            preferences[PreferencesKeys.BLOCK_SHORT_FORM_CONTENT] = blockShortFormContent
            preferences[PreferencesKeys.USAGE_NUDGE_ENABLED] = usageNudgeEnabled
            preferences[PreferencesKeys.USAGE_NUDGE_MINUTES] = usageNudgeMinutes
        }
    }

    /**
     * Bulk saves core onboarding preferences in a single transaction.
     *
     * @param birthDate User's date of birth.
     * @param lifeExpectancy Target life span in years.
     * @param wallpaperTarget Destination for generating calendar wallpaper.
     * @param theme Light or Dark theme.
     * @param dotStyle Shape/Style of the calendar dots.
     */
    suspend fun saveAllPreferences(
        birthDate: LocalDate?,
        lifeExpectancy: Int,
        wallpaperTarget: WallpaperTarget,
        theme: CalendarTheme,
        dotStyle: DotStyle
    ) {
        dataStore.edit { preferences ->
            if (birthDate != null) {
                preferences[PreferencesKeys.BIRTH_DATE_EPOCH_DAYS] = birthDate.toEpochDay()
            }
            preferences[PreferencesKeys.LIFE_EXPECTANCY] = lifeExpectancy
            preferences[PreferencesKeys.WALLPAPER_TARGET] = wallpaperTarget.name
            preferences[PreferencesKeys.THEME] = theme.name
            preferences[PreferencesKeys.DOT_STYLE] = dotStyle.name
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = true
        }
    }
}

/**
 * Data class representing the full state of user preferences.
 *
 * @property birthDate User's birth date, used to calculate "weeks lived". Null if not set.
 * @property lifeExpectancy Target age in years.
 * @property wallpaperTarget Location where the generated calendar wallpaper is applied.
 * @property theme Light or Dark visual theme.
 * @property dotStyle Shape of dots used in the life calendar grid.
 * @property backgroundStyle Overall background aesthetic (Matrix vs Solid).
 * @property fontSize Global text and icon scaling factor.
 * @property isSetupComplete Whether the onboarding flow has been finished.
 * @property autoOpenKeyboard Whether search keyboard opens instantly in the drawer.
 * @property clockStyle 12h, 24h, or 24h with seconds clock format.
 * @property searchBarPosition Vertical position of the search bar (Top or Bottom).
 * @property hiddenPackages Set of package names that should be hidden from the app drawer.
 * @property distractingPackages Set of package names that trigger the mindful delay overlay.
 * @property mindfulMessage Custom message shown on the mindful delay overlay.
 * @property blockShortFormContent Whether the accessibility service is instructed to block shorts/reels.
 * @property usageNudgeEnabled Whether to re-trigger mindful overlay after extended distracting app use.
 * @property usageNudgeMinutes How many continuous minutes in a distracting app before a nudge fires.
 */
data class UserPreferences(
    val birthDate: LocalDate?,
    val lifeExpectancy: Int,
    val wallpaperTarget: WallpaperTarget,
    val theme: CalendarTheme,
    val dotStyle: DotStyle,
    val backgroundStyle: BackgroundStyle,
    val fontSize: FontSize,
    val isSetupComplete: Boolean,
    val autoOpenKeyboard: Boolean,
    val clockStyle: ClockStyle,
    val searchBarPosition: SearchBarPosition,
    val hiddenPackages: Set<String>,
    val distractingPackages: Set<String>,
    val mindfulMessage: String,
    val blockShortFormContent: Boolean,
    val usageNudgeEnabled: Boolean,
    val usageNudgeMinutes: Int
)

enum class SearchBarPosition {
    TOP,
    BOTTOM
}

/**
 * Shape and aesthetic of individual dots in the life calendar grid.
 */
enum class DotStyle {
    /** Classic filled circles. */
    FILLED_CIRCLE,
    /** Circular outlines. */
    RING,
    /** Sharp square shapes. */
    SQUARE,
    /** Rotated squares (diamonds). */
    DIAMOND
}

/**
 * Background rendering strategies for the launcher screens.
 */
enum class BackgroundStyle {
    /** Pure black background for maximum OLED battery saving. */
    SOLID_BLACK,
    /** Minimalist grid of subtle dots. */
    MATRIX_GRID
}

/**
 * Scaling options for the launcher's typography and icons.
 *
 * @property scale The multiplier applied to default sizes.
 */
enum class FontSize(val scale: Float) {
    SMALL(0.8f),
    /** Standard size (100%). */
    MEDIUM(1.0f),
    LARGE(1.2f)
}

/**
 * Visual polarity of the calendar and UI elements.
 */
enum class CalendarTheme {
    /** White dots on a black background. */
    DARK,
    /** Black dots on a white background. */
    LIGHT
}

/**
 * Time display formats for the home screen clock.
 */
enum class ClockStyle {
    /** 24-hour format (e.g., 14:30). */
    H24,
    /** 12-hour format with AM/PM (e.g., 2:30 PM). */
    H12,
    /** 24-hour format with seconds (e.g., 14:30:45). */
    H24_SEC
}
