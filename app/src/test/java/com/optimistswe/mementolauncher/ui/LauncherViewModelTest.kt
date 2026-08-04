package com.optimistswe.mementolauncher.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.optimistswe.mementolauncher.data.*
import com.optimistswe.mementolauncher.wallpaper.WallpaperTarget
import com.optimistswe.mementolauncher.ui.managers.TimeManager
import com.optimistswe.mementolauncher.ui.managers.WidgetManager
import com.optimistswe.mementolauncher.ui.screens.AppDrawerItem
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LauncherViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val appRepository = mockk<AppRepository>(relaxed = true)
    private val favoritesRepository = mockk<FavoritesRepository>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val appLabelRepository = mockk<AppLabelRepository>(relaxed = true)
    private val folderRepository = mockk<FolderRepository>(relaxed = true)
    private val backupManager = mockk<BackupManager>(relaxed = true)
    private val timeManager = mockk<TimeManager>(relaxed = true)
    private val widgetManager = mockk<WidgetManager>(relaxed = true)

    private lateinit var viewModel: LauncherViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { appRepository.observeApps() } returns flowOf(emptyList())
        every { favoritesRepository.getFavorites() } returns flowOf(emptyList())
        
        val defaultPrefs = UserPreferences(
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
            searchBarPosition = SearchBarPosition.BOTTOM,
            hiddenPackages = emptySet(),
            distractingPackages = emptySet(),
            mindfulMessage = "Breathe.",
            blockShortFormContent = false,
            usageNudgeEnabled = false,
            usageNudgeMinutes = 15
        )
        every { preferencesRepository.getUserPreferences() } returns flowOf(defaultPrefs)
        every { appLabelRepository.getCustomLabels() } returns flowOf(emptyMap())
        every { folderRepository.folders } returns flowOf(emptyList())
    }

    private fun createViewModel() {
        viewModel = LauncherViewModel(
            appRepository,
            favoritesRepository,
            preferencesRepository,
            appLabelRepository,
            folderRepository,
            backupManager,
            timeManager,
            widgetManager,
            testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filteredApps filters correctly based on search query`() = runTest {
        val apps = listOf(
            AppInfo("App A", "com.a", "a"),
            AppInfo("App B", "com.b", "b"),
            AppInfo("Game C", "com.c", "c")
        )
        every { appRepository.observeApps() } returns flowOf(apps)
        
        createViewModel()

        viewModel.updateSearchQuery("Game")
        
        viewModel.filteredApps.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Game C", result[0].label)
        }
    }

    @Test
    fun `toggleFavorite calls favoritesRepository`() = runTest {
        createViewModel()
        viewModel.toggleFavorite("com.test")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { favoritesRepository.addFavorite("com.test") }
    }

    @Test
    fun `filteredApps respects hidden packages`() = runTest {
        val apps = listOf(
            AppInfo("App A", "com.a", "a"),
            AppInfo("App B", "com.b", "b")
        )
        every { appRepository.observeApps() } returns flowOf(apps)
        
        val prefs = UserPreferences(
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
            searchBarPosition = SearchBarPosition.BOTTOM,
            hiddenPackages = setOf("com.a"),
            distractingPackages = emptySet(),
            mindfulMessage = "Breathe.",
            blockShortFormContent = false,
            usageNudgeEnabled = false,
            usageNudgeMinutes = 15
        )
        every { preferencesRepository.getUserPreferences() } returns flowOf(prefs)

        createViewModel()

        viewModel.filteredApps.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("com.b", result[0].packageName)
        }
    }

    @Test
    fun `filteredApps applies custom labels`() = runTest {
        val apps = listOf(AppInfo("Original", "com.test", "icon"))
        every { appRepository.observeApps() } returns flowOf(apps)
        every { appLabelRepository.getCustomLabels() } returns flowOf(mapOf("com.test" to "Renamed"))

        createViewModel()

        viewModel.filteredApps.test {
            val result = awaitItem()
            assertEquals("Renamed", result[0].label)
        }
    }

    @Test
    fun `groupedDrawerItems correctly groups apps and folders`() = runTest {
        val apps = listOf(
            AppInfo("Apple", "com.apple", "icon"),
            AppInfo("Banana", "com.banana", "icon"),
            AppInfo("Calculator", "com.calc", "icon")
        )
        val folders = listOf(
            AppFolder(id = "1", name = "Fruit", packages = listOf("com.apple", "com.banana"))
        )
        
        every { appRepository.observeApps() } returns flowOf(apps)
        every { folderRepository.folders } returns flowOf(folders)
        every { preferencesRepository.getUserPreferences() } returns flowOf(UserPreferences(
            null, 80, WallpaperTarget.BOTH, CalendarTheme.DARK, DotStyle.FILLED_CIRCLE,
            BackgroundStyle.MATRIX_GRID, FontSize.MEDIUM, true, true, ClockStyle.H24,
            SearchBarPosition.BOTTOM, emptySet(), emptySet(), "Breathe.", false,
            false, 15
        ))

        createViewModel()

        viewModel.groupedDrawerItems.test {
            val groups = awaitItem()
            
            // 'F' group for Folder "Fruit"
            // 'C' group for App "Calculator"
            assertEquals(2, groups.size)
            
            val fruitGroup = groups['F']!!
            assertEquals(1, fruitGroup.size)
            val folderItem = fruitGroup[0] as com.optimistswe.mementolauncher.ui.screens.AppDrawerItem.Folder
            assertEquals("Fruit", folderItem.folder.name)
            assertEquals(2, folderItem.resolvedApps.size)
            
            val calcGroup = groups['C']!!
            assertEquals(1, calcGroup.size)
            val appItem = calcGroup[0] as com.optimistswe.mementolauncher.ui.screens.AppDrawerItem.App
            assertEquals("Calculator", appItem.info.label)
        }
    }

    @Test
    fun `refreshClock delegates to timeManager`() {
        createViewModel()
        viewModel.refreshClock()
        verify { timeManager.refresh() }
    }

    @Test
    fun `refreshWidgets delegates to widgetManager`() {
        createViewModel()
        viewModel.refreshWidgets()
        verify { widgetManager.refresh() }
    }

    // ═══════════════════════════════════════════
    // Additional tests
    // ═══════════════════════════════════════════

    @Test
    fun `updateSearchQuery sets query value`() = runTest {
        createViewModel()
        viewModel.updateSearchQuery("calc")
        assertEquals("calc", viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch resets query to empty`() = runTest {
        createViewModel()
        viewModel.updateSearchQuery("test")
        viewModel.clearSearch()
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `isFavorite returns false when no favorites`() {
        createViewModel()
        assertFalse(viewModel.isFavorite("com.nonexistent"))
    }

    @Test
    fun `requestAppLaunch invokes direct launch for non-distracting app`() = runTest {
        createViewModel()
        var launchedPkg: String? = null
        viewModel.requestAppLaunch("com.test") { launchedPkg = it }
        assertEquals("com.test", launchedPkg)
    }

    @Test
    fun `requestAppLaunch intercepts distracting app`() = runTest {
        val prefs = UserPreferences(
            null, 80, WallpaperTarget.BOTH, CalendarTheme.DARK, DotStyle.FILLED_CIRCLE,
            BackgroundStyle.MATRIX_GRID, FontSize.MEDIUM, true, true, ClockStyle.H24,
            SearchBarPosition.BOTTOM, emptySet(), setOf("com.distracting"), "Breathe.",
            false, false, 15
        )
        every { preferencesRepository.getUserPreferences() } returns flowOf(prefs)

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        var launchedPkg: String? = null
        viewModel.requestAppLaunch("com.distracting") { launchedPkg = it }

        assertNull(launchedPkg) // Should not have been directly launched
        assertEquals("com.distracting", viewModel.interceptedLaunchPackage.value)
    }

    @Test
    fun `clearInterceptedLaunch resets to null`() = runTest {
        createViewModel()
        viewModel.triggerMindfulInterruption("com.test")
        assertEquals("com.test", viewModel.interceptedLaunchPackage.value)

        viewModel.clearInterceptedLaunch()
        assertNull(viewModel.interceptedLaunchPackage.value)
    }

    @Test
    fun `triggerMindfulInterruption sets package`() {
        createViewModel()
        viewModel.triggerMindfulInterruption("com.youtube")
        assertEquals("com.youtube", viewModel.interceptedLaunchPackage.value)
    }

    @Test
    fun `onHomeIntentReceived emits event`() = runTest {
        createViewModel()
        viewModel.onHomeIntentReceived()
        // Verify it doesn't crash - SharedFlow emission
    }

    @Test
    fun `renameApp with blank clears custom label`() = runTest {
        createViewModel()
        viewModel.renameApp("com.test", "")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appLabelRepository.clearCustomLabel("com.test") }
    }

    @Test
    fun `renameApp with text sets custom label`() = runTest {
        createViewModel()
        viewModel.renameApp("com.test", "My App")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { appLabelRepository.setCustomLabel("com.test", "My App") }
    }

    @Test
    fun `setDockLeftApp delegates to repository`() = runTest {
        createViewModel()
        viewModel.setDockLeftApp("com.phone")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { favoritesRepository.setDockLeftApp("com.phone") }
    }

    @Test
    fun `setDockRightApp delegates to repository`() = runTest {
        createViewModel()
        viewModel.setDockRightApp("com.camera")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { favoritesRepository.setDockRightApp("com.camera") }
    }

    @Test
    fun `createFolder delegates to repository`() = runTest {
        createViewModel()
        viewModel.createFolder("Social")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { folderRepository.createFolder("Social") }
    }

    @Test
    fun `deleteFolder delegates to repository`() = runTest {
        createViewModel()
        viewModel.deleteFolder("folder-1")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { folderRepository.deleteFolder("folder-1") }
    }

    @Test
    fun `renameFolder delegates to repository`() = runTest {
        createViewModel()
        viewModel.renameFolder("folder-1", "Work")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { folderRepository.renameFolder("folder-1", "Work") }
    }

    @Test
    fun `addAppToFolder delegates to repository`() = runTest {
        createViewModel()
        viewModel.addAppToFolder("folder-1", "com.app")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { folderRepository.addAppToFolder("folder-1", "com.app") }
    }

    @Test
    fun `removeAppFromFolder delegates to repository`() = runTest {
        createViewModel()
        viewModel.removeAppFromFolder("folder-1", "com.app")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { folderRepository.removeAppFromFolder("folder-1", "com.app") }
    }

    @Test
    fun `updateBirthDate delegates to preferencesRepository`() = runTest {
        createViewModel()
        val date = java.time.LocalDate.of(1995, 5, 1)
        viewModel.updateBirthDate(date)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.saveBirthDate(date) }
    }

    @Test
    fun `updateLifeExpectancy delegates to preferencesRepository`() = runTest {
        createViewModel()
        viewModel.updateLifeExpectancy(90)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.saveLifeExpectancy(90) }
    }

    @Test
    fun `updateClockStyle delegates to preferencesRepository`() = runTest {
        createViewModel()
        viewModel.updateClockStyle(ClockStyle.H12)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.saveClockStyle(ClockStyle.H12) }
    }

    @Test
    fun `toggleAppVisibility delegates to preferencesRepository`() = runTest {
        createViewModel()
        viewModel.toggleAppVisibility("com.hidden")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.togglePackageVisibility("com.hidden") }
    }

    @Test
    fun `toggleDistractingPackage delegates to preferencesRepository`() = runTest {
        createViewModel()
        viewModel.toggleDistractingPackage("com.distracting")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.toggleDistractingPackage("com.distracting") }
    }

    @Test
    fun `updateMindfulMessage delegates to preferencesRepository`() = runTest {
        createViewModel()
        viewModel.updateMindfulMessage("Stay focused")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferencesRepository.saveMindfulMessage("Stay focused") }
    }

    @Test
    fun `filteredApps returns all apps when query is blank`() = runTest {
        val apps = listOf(
            AppInfo("Alpha", "com.a", "a"),
            AppInfo("Beta", "com.b", "b")
        )
        every { appRepository.observeApps() } returns flowOf(apps)
        createViewModel()

        viewModel.filteredApps.test {
            val result = awaitItem()
            assertEquals(2, result.size)
        }
    }

    @Test
    fun `filteredApps case-insensitive search`() = runTest {
        val apps = listOf(
            AppInfo("YouTube", "com.youtube", "yt"),
            AppInfo("Calculator", "com.calc", "calc")
        )
        every { appRepository.observeApps() } returns flowOf(apps)
        createViewModel()

        viewModel.updateSearchQuery("youtube")

        viewModel.filteredApps.test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("YouTube", result[0].label)
        }
    }

    // ═══════════════════════════════════════════
    // Life metrics resilience
    // ═══════════════════════════════════════════

    private fun prefs(birthDate: LocalDate?, lifeExpectancy: Int = 80) = UserPreferences(
        birthDate = birthDate,
        lifeExpectancy = lifeExpectancy,
        wallpaperTarget = WallpaperTarget.BOTH,
        theme = CalendarTheme.DARK,
        dotStyle = DotStyle.FILLED_CIRCLE,
        backgroundStyle = BackgroundStyle.MATRIX_GRID,
        fontSize = FontSize.MEDIUM,
        isSetupComplete = true,
        autoOpenKeyboard = true,
        clockStyle = ClockStyle.H24,
        searchBarPosition = SearchBarPosition.BOTTOM,
        hiddenPackages = emptySet(),
        distractingPackages = emptySet(),
        mindfulMessage = "Breathe.",
        blockShortFormContent = false,
        usageNudgeEnabled = false,
        usageNudgeMinutes = 15
    )

    @Test
    fun `a stored future birth date does not kill the preferences collector`() {
        // calculateMetrics rejects future birth dates. An unhandled throw in the preferences
        // collector would tear that collector down and crash the launcher on start, which for a
        // HOME app leaves the device without a usable home screen. A future date can be
        // persisted via a hand-edited backup.
        //
        // Asserting "no metrics" alone would pass even when broken — the throw happens before
        // anything is assigned. The real signal is that the collector is still alive afterwards,
        // so this feeds a valid date next and requires it to be processed.
        val prefsFlow = MutableStateFlow(prefs(birthDate = LocalDate.now().plusYears(1)))
        every { preferencesRepository.getUserPreferences() } returns prefsFlow

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("an invalid date must not produce metrics", viewModel.lifeMetrics.value)
        assertEquals("", viewModel.lifeProgressText.value)

        prefsFlow.value = prefs(birthDate = LocalDate.now().minusYears(30))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(
            "collector died on the invalid date and never saw the valid one",
            viewModel.lifeMetrics.value
        )
    }

    @Test
    fun `a valid birth date produces life metrics`() {
        every { preferencesRepository.getUserPreferences() } returns
                flowOf(prefs(birthDate = LocalDate.now().minusYears(30)))

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val metrics = viewModel.lifeMetrics.value
        assertNotNull(metrics)
        assertEquals(80 * 52, metrics!!.totalWeeks)
        assertTrue(viewModel.lifeProgressText.value.startsWith("WEEK "))
    }

    @Test
    fun `life metrics are cleared when the birth date is removed`() {
        // Restoring a backup with no birth date used to reset only the progress text, leaving
        // the calendar rendering stale metrics from the previous user.
        val prefsFlow = MutableStateFlow(prefs(birthDate = LocalDate.now().minusYears(30)))
        every { preferencesRepository.getUserPreferences() } returns prefsFlow

        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull("precondition: metrics present", viewModel.lifeMetrics.value)

        prefsFlow.value = prefs(birthDate = null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull("stale metrics must not survive birth date removal", viewModel.lifeMetrics.value)
        assertEquals("", viewModel.lifeProgressText.value)
    }

    // ═══════════════════════════════════════════
    // Drawer grouping — folder search
    // ═══════════════════════════════════════════

    @Test
    fun `search keeps folders matching by name or by contents and drops the rest`() = runTest {
        val apps = listOf(
            AppInfo("Calculator", "com.calc", "a"),
            AppInfo("Zebra", "com.zebra", "z")
        )
        every { appRepository.observeApps() } returns flowOf(apps)
        every { folderRepository.folders } returns flowOf(
            listOf(
                // Matches because it contains "Calculator".
                AppFolder(id = "f1", name = "Utilities", packages = listOf("com.calc")),
                // Matches on its own name.
                AppFolder(id = "f2", name = "Calc Stuff", packages = emptyList()),
                // Matches neither — previously still rendered during a search.
                AppFolder(id = "f3", name = "Media", packages = listOf("com.zebra"))
            )
        )

        createViewModel()
        viewModel.updateSearchQuery("Calc")

        viewModel.groupedDrawerItems.test {
            var grouped = awaitItem()
            while (grouped.isEmpty()) grouped = awaitItem()

            val folderNames = grouped.values.flatten()
                .filterIsInstance<AppDrawerItem.Folder>()
                .map { it.folder.name }
                .sorted()

            assertEquals(listOf("Calc Stuff", "Utilities"), folderNames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a blank query keeps every folder including empty ones`() = runTest {
        every { appRepository.observeApps() } returns flowOf(emptyList())
        every { folderRepository.folders } returns flowOf(
            listOf(
                AppFolder(id = "f1", name = "Alpha", packages = emptyList()),
                AppFolder(id = "f2", name = "Beta", packages = emptyList())
            )
        )

        createViewModel()

        viewModel.groupedDrawerItems.test {
            var grouped = awaitItem()
            while (grouped.isEmpty()) grouped = awaitItem()

            val folderNames = grouped.values.flatten()
                .filterIsInstance<AppDrawerItem.Folder>()
                .map { it.folder.name }
                .sorted()

            assertEquals(listOf("Alpha", "Beta"), folderNames)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
