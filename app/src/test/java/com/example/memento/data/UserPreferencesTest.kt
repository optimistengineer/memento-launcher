package com.optimistswe.mementolauncher.data

import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [UserPreferences] data class and its associated enums.
 */
class UserPreferencesTest {

    private fun createDefault() = UserPreferences(
        birthDate = null,
        lifeExpectancy = 80,
        wallpaperTarget = WallpaperTarget.BOTH,
        theme = CalendarTheme.DARK,
        dotStyle = DotStyle.FILLED_CIRCLE,
        backgroundStyle = BackgroundStyle.MATRIX_GRID,
        fontSize = FontSize.MEDIUM,
        isSetupComplete = false,
        autoOpenKeyboard = true,
        clockStyle = ClockStyle.H24,
        searchBarPosition = SearchBarPosition.TOP,
        hiddenPackages = emptySet(),
        distractingPackages = emptySet(),
        mindfulMessage = "Breathe.",
        blockShortFormContent = false,
        usageNudgeEnabled = false,
        usageNudgeMinutes = 15
    )

    @Test
    fun `default preferences are valid`() {
        val prefs = createDefault()
        assertNull(prefs.birthDate)
        assertEquals(80, prefs.lifeExpectancy)
        assertFalse(prefs.isSetupComplete)
        assertTrue(prefs.autoOpenKeyboard)
    }

    @Test
    fun `copy with birthDate preserves other fields`() {
        val original = createDefault()
        val date = LocalDate.of(1995, 5, 15)
        val modified = original.copy(birthDate = date)
        assertEquals(date, modified.birthDate)
        assertEquals(original.lifeExpectancy, modified.lifeExpectancy)
        assertEquals(original.theme, modified.theme)
    }

    @Test
    fun `equality works for identical preferences`() {
        val a = createDefault()
        val b = createDefault()
        assertEquals(a, b)
    }

    // ═══════════════════════════════════════════
    // Enum Tests
    // ═══════════════════════════════════════════

    @Test
    fun `DotStyle has 4 values`() {
        assertEquals(4, DotStyle.entries.size)
    }

    @Test
    fun `DotStyle valueOf works for all entries`() {
        DotStyle.entries.forEach { style ->
            assertEquals(style, DotStyle.valueOf(style.name))
        }
    }

    @Test
    fun `BackgroundStyle has 2 values`() {
        assertEquals(2, BackgroundStyle.entries.size)
    }

    @Test
    fun `FontSize scale values`() {
        assertEquals(0.8f, FontSize.SMALL.scale, 0.001f)
        assertEquals(1.0f, FontSize.MEDIUM.scale, 0.001f)
        assertEquals(1.2f, FontSize.LARGE.scale, 0.001f)
    }

    @Test
    fun `FontSize has 3 values`() {
        assertEquals(3, FontSize.entries.size)
    }

    @Test
    fun `CalendarTheme has 2 values`() {
        assertEquals(2, CalendarTheme.entries.size)
    }

    @Test
    fun `ClockStyle has 3 values`() {
        assertEquals(3, ClockStyle.entries.size)
    }

    @Test
    fun `SearchBarPosition has 2 values`() {
        assertEquals(2, SearchBarPosition.entries.size)
    }

    @Test
    fun `SearchBarPosition valueOf works`() {
        assertEquals(SearchBarPosition.TOP, SearchBarPosition.valueOf("TOP"))
        assertEquals(SearchBarPosition.BOTTOM, SearchBarPosition.valueOf("BOTTOM"))
    }

    @Test
    fun `WallpaperTarget has 3 values`() {
        assertEquals(3, WallpaperTarget.entries.size)
    }
}
