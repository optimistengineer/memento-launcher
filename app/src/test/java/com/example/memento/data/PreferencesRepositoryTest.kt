package com.optimistswe.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        repository = PreferencesRepository(dataStore)
    }

    @Test
    fun `getUserPreferences returns defaults initially`() = runTest(testDispatcher) {
        val prefs = repository.getUserPreferences().first()
        
        assertNull(prefs.birthDate)
        assertEquals(LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY, prefs.lifeExpectancy)
        assertEquals(WallpaperTarget.BOTH, prefs.wallpaperTarget)
        assertEquals(CalendarTheme.DARK, prefs.theme)
        assertEquals(BackgroundStyle.MATRIX_GRID, prefs.backgroundStyle)
        assertFalse(prefs.isSetupComplete)
    }

    @Test
    fun `saveBirthDate persists value`() = runTest(testDispatcher) {
        val date = LocalDate.of(1995, 1, 1)
        repository.saveBirthDate(date)
        
        val prefs = repository.getUserPreferences().first()
        assertEquals(date, prefs.birthDate)
    }

    @Test
    fun `setSetupComplete sets boolean to true`() = runTest(testDispatcher) {
        repository.setSetupComplete()
        val prefs = repository.getUserPreferences().first()
        assertTrue(prefs.isSetupComplete)
    }

    @Test
    fun `saveAllPreferences persists multiple values`() = runTest(testDispatcher) {
        val date = LocalDate.of(1980, 5, 20)
        repository.saveAllPreferences(
            birthDate = date,
            lifeExpectancy = 90,
            wallpaperTarget = WallpaperTarget.HOME,
            theme = CalendarTheme.LIGHT,
            dotStyle = DotStyle.SQUARE
        )
        
        val prefs = repository.getUserPreferences().first()
        assertEquals(date, prefs.birthDate)
        assertEquals(90, prefs.lifeExpectancy)
        assertEquals(WallpaperTarget.HOME, prefs.wallpaperTarget)
        assertEquals(CalendarTheme.LIGHT, prefs.theme)
        assertEquals(DotStyle.SQUARE, prefs.dotStyle)
        assertTrue(prefs.isSetupComplete)
    }

    @Test
    fun `togglePackageVisibility adds and removes packages`() = runTest(testDispatcher) {
        val pkg = "com.test.hidden"
        
        repository.togglePackageVisibility(pkg)
        assertTrue(repository.getUserPreferences().first().hiddenPackages.contains(pkg))
        
        repository.togglePackageVisibility(pkg)
        assertFalse(repository.getUserPreferences().first().hiddenPackages.contains(pkg))
    }

    @Test
    fun `saveBackgroundStyle persists enum name`() = runTest(testDispatcher) {
        repository.saveBackgroundStyle(BackgroundStyle.SOLID_BLACK)
        assertEquals(BackgroundStyle.SOLID_BLACK, repository.getUserPreferences().first().backgroundStyle)
    }

    @Test
    fun `saveFontSize persists enum name`() = runTest(testDispatcher) {
        repository.saveFontSize(FontSize.LARGE)
        assertEquals(FontSize.LARGE, repository.getUserPreferences().first().fontSize)
    }

    // ═══════════════════════════════════════════
    // Additional preference saves
    // ═══════════════════════════════════════════

    @Test
    fun `saveClockStyle persists value`() = runTest(testDispatcher) {
        repository.saveClockStyle(ClockStyle.H12)
        assertEquals(ClockStyle.H12, repository.getUserPreferences().first().clockStyle)
    }

    @Test
    fun `saveClockStyle H24_SEC persists`() = runTest(testDispatcher) {
        repository.saveClockStyle(ClockStyle.H24_SEC)
        assertEquals(ClockStyle.H24_SEC, repository.getUserPreferences().first().clockStyle)
    }

    @Test
    fun `saveSearchBarPosition persists value`() = runTest(testDispatcher) {
        repository.saveSearchBarPosition(SearchBarPosition.BOTTOM)
        assertEquals(SearchBarPosition.BOTTOM, repository.getUserPreferences().first().searchBarPosition)
    }

    @Test
    fun `saveAutoOpenKeyboard persists false`() = runTest(testDispatcher) {
        repository.saveAutoOpenKeyboard(false)
        assertFalse(repository.getUserPreferences().first().autoOpenKeyboard)
    }

    @Test
    fun `saveDotStyle persists value`() = runTest(testDispatcher) {
        repository.saveDotStyle(DotStyle.RING)
        assertEquals(DotStyle.RING, repository.getUserPreferences().first().dotStyle)
    }

    @Test
    fun `saveDotStyle DIAMOND persists`() = runTest(testDispatcher) {
        repository.saveDotStyle(DotStyle.DIAMOND)
        assertEquals(DotStyle.DIAMOND, repository.getUserPreferences().first().dotStyle)
    }

    @Test
    fun `saveLifeExpectancy persists value`() = runTest(testDispatcher) {
        repository.saveLifeExpectancy(100)
        assertEquals(100, repository.getUserPreferences().first().lifeExpectancy)
    }

    @Test
    fun `saveTheme LIGHT persists`() = runTest(testDispatcher) {
        repository.saveTheme(CalendarTheme.LIGHT)
        assertEquals(CalendarTheme.LIGHT, repository.getUserPreferences().first().theme)
    }

    @Test
    fun `saveWallpaperTarget LOCK persists`() = runTest(testDispatcher) {
        repository.saveWallpaperTarget(WallpaperTarget.LOCK)
        assertEquals(WallpaperTarget.LOCK, repository.getUserPreferences().first().wallpaperTarget)
    }

    @Test
    fun `saveWallpaperTarget HOME persists`() = runTest(testDispatcher) {
        repository.saveWallpaperTarget(WallpaperTarget.HOME)
        assertEquals(WallpaperTarget.HOME, repository.getUserPreferences().first().wallpaperTarget)
    }

    // ═══════════════════════════════════════════
    // Distracting packages
    // ═══════════════════════════════════════════

    @Test
    fun `toggleDistractingPackage adds package`() = runTest(testDispatcher) {
        repository.toggleDistractingPackage("com.test.app")
        val prefs = repository.getUserPreferences().first()
        assertTrue(prefs.distractingPackages.contains("com.test.app"))
    }

    @Test
    fun `toggleDistractingPackage removes when toggled twice`() = runTest(testDispatcher) {
        repository.toggleDistractingPackage("com.test.app")
        repository.toggleDistractingPackage("com.test.app")
        val prefs = repository.getUserPreferences().first()
        assertFalse(prefs.distractingPackages.contains("com.test.app"))
    }

    // ═══════════════════════════════════════════
    // Mindful message
    // ═══════════════════════════════════════════

    @Test
    fun `saveMindfulMessage persists text`() = runTest(testDispatcher) {
        repository.saveMindfulMessage("Take a breath")
        assertEquals("Take a breath", repository.getUserPreferences().first().mindfulMessage)
    }

    // ═══════════════════════════════════════════
    // Block short form content
    // ═══════════════════════════════════════════

    @Test
    fun `saveBlockShortFormContent persists true`() = runTest(testDispatcher) {
        repository.saveBlockShortFormContent(true)
        assertTrue(repository.getUserPreferences().first().blockShortFormContent)
    }

    @Test
    fun `saveBlockShortFormContent persists false`() = runTest(testDispatcher) {
        repository.saveBlockShortFormContent(true)
        repository.saveBlockShortFormContent(false)
        assertFalse(repository.getUserPreferences().first().blockShortFormContent)
    }

    // ═══════════════════════════════════════════
    // Usage nudge
    // ═══════════════════════════════════════════

    @Test
    fun `saveUsageNudgeEnabled persists true`() = runTest(testDispatcher) {
        repository.saveUsageNudgeEnabled(true)
        assertTrue(repository.getUserPreferences().first().usageNudgeEnabled)
    }

    @Test
    fun `saveUsageNudgeMinutes persists value`() = runTest(testDispatcher) {
        repository.saveUsageNudgeMinutes(30)
        assertEquals(30, repository.getUserPreferences().first().usageNudgeMinutes)
    }

    // ═══════════════════════════════════════════
    // Hidden packages additional
    // ═══════════════════════════════════════════

    @Test
    fun `togglePackageVisibility multiple packages`() = runTest(testDispatcher) {
        repository.togglePackageVisibility("com.a")
        repository.togglePackageVisibility("com.b")
        val hidden = repository.getUserPreferences().first().hiddenPackages
        assertEquals(2, hidden.size)
        assertTrue(hidden.contains("com.a"))
        assertTrue(hidden.contains("com.b"))
    }

    @Test
    fun `togglePackageVisibility remove one of two`() = runTest(testDispatcher) {
        repository.togglePackageVisibility("com.a")
        repository.togglePackageVisibility("com.b")
        repository.togglePackageVisibility("com.a") // remove
        val hidden = repository.getUserPreferences().first().hiddenPackages
        assertEquals(1, hidden.size)
        assertFalse(hidden.contains("com.a"))
        assertTrue(hidden.contains("com.b"))
    }

    // ═══════════════════════════════════════════
    // saveAllPreferences with null birthDate
    // ═══════════════════════════════════════════

    @Test
    fun `saveAllPreferences with null birthDate does not crash`() = runTest(testDispatcher) {
        repository.saveAllPreferences(
            birthDate = null,
            lifeExpectancy = 75,
            wallpaperTarget = WallpaperTarget.BOTH,
            theme = CalendarTheme.DARK,
            dotStyle = DotStyle.FILLED_CIRCLE
        )
        val prefs = repository.getUserPreferences().first()
        assertNull(prefs.birthDate)
        assertEquals(75, prefs.lifeExpectancy)
        assertTrue(prefs.isSetupComplete) // setup is marked complete
    }
}
