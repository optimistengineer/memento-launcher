package com.optimistswe.mementolauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.data.AppLabelRepository
import com.optimistswe.mementolauncher.data.AppRepository
import com.optimistswe.mementolauncher.data.BackupManager
import com.optimistswe.mementolauncher.data.FavoritesRepository
import com.optimistswe.mementolauncher.data.FolderRepository
import com.optimistswe.mementolauncher.data.PreferencesRepository
import com.optimistswe.mementolauncher.data.AppFolder
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.ui.managers.TimeManager
import com.optimistswe.mementolauncher.ui.managers.WidgetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The central ViewModel for the Memento launcher, coordinating all data and UI state.
 *
 * This ViewModel serves as the glue between the data repositories ([AppRepository], [FavoritesRepository],
 * [PreferencesRepository], [AppLabelRepository], [FolderRepository]) and the UI managers
 * ([TimeManager], [WidgetManager]).
 *
 * It uses Kotlin's [StateFlow] and [combine] operators to create a reactive, single source of truth
 * for the launcher's UI, ensuring that any data change (e.g., an app being uninstalled, a preference
 * being changed) is immediately reflected in the interface.
 *
 * @param ioDispatcher A [CoroutineDispatcher] used for grouped drawer item sorting off the main thread.
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferencesRepository: PreferencesRepository,
    private val appLabelRepository: AppLabelRepository,
    private val folderRepository: FolderRepository,
    private val backupManager: BackupManager,
    private val timeManager: TimeManager,
    private val widgetManager: WidgetManager,
    @com.optimistswe.mementolauncher.di.IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
) : ViewModel() {

    private val calculator = LifeCalendarCalculator()
    
    // --- Clock (Delegated) ---
    val currentTime: StateFlow<String> = timeManager.currentTime
    val currentDate: StateFlow<String> = timeManager.currentDate

    // --- System UI Events ---
    private val _homeIntentEvents = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val homeIntentEvents = _homeIntentEvents.asSharedFlow()

    fun onHomeIntentReceived() {
        _homeIntentEvents.tryEmit(Unit)
    }

    // --- Widgets (Delegated) ---
    val nextAlarm: StateFlow<String?> = widgetManager.nextAlarm
    val screenTime: StateFlow<String?> = widgetManager.screenTime

    fun hasUsagePermission(): Boolean = widgetManager.hasUsagePermission()

    /**
     * A flow of all installed apps, unfiltered.
     */
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps.asStateFlow()

    /**
     * The current search query string from the app drawer.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Reactive user preferences.
     */
    val preferences = preferencesRepository.getUserPreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * True only on the user's birthday (month + day match today).
     * Derived from the single shared preferences flow to avoid redundant DataStore subscriptions.
     */
    val isBirthday: StateFlow<Boolean> = preferences
        .filterNotNull()
        .map { prefs ->
            val birthDate = prefs.birthDate ?: return@map false
            val today = java.time.LocalDate.now()
            birthDate.monthValue == today.monthValue && birthDate.dayOfMonth == today.dayOfMonth
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _favoritePackages = MutableStateFlow<List<String>>(emptyList())
    private val _dockLeftPkg = MutableStateFlow<String?>(null)
    private val _dockRightPkg = MutableStateFlow<String?>(null)

    private val _customLabels = MutableStateFlow<Map<String, String>>(emptyMap())
    /** A map of package names to their user-defined names. */
    val customLabels: StateFlow<Map<String, String>> = _customLabels.asStateFlow()

    /**
     * Favorite apps resolved to [AppInfo] objects with custom labels applied.
     */
    val favoriteApps: StateFlow<List<AppInfo>> = combine(
        _allApps, _favoritePackages, _customLabels
    ) { apps, favPackages, labels ->
        favPackages.mapNotNull { pkg ->
            apps.find { it.packageName == pkg }?.let { app ->
                val customLabel = labels[pkg]
                if (customLabel != null) app.copy(label = customLabel) else app
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Left corner dock app resolved to [AppInfo].
     */
    val dockLeftApp: StateFlow<AppInfo?> = combine(
        _allApps, _dockLeftPkg, _customLabels
    ) { apps, pkg, labels ->
        if (pkg == null) null
        else apps.find { it.packageName == pkg }?.let { app ->
            val customLabel = labels[pkg]
            if (customLabel != null) app.copy(label = customLabel) else app
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Right corner dock app resolved to [AppInfo].
     */
    val dockRightApp: StateFlow<AppInfo?> = combine(
        _allApps, _dockRightPkg, _customLabels
    ) { apps, pkg, labels ->
        if (pkg == null) null
        else apps.find { it.packageName == pkg }?.let { app ->
            val customLabel = labels[pkg]
            if (customLabel != null) app.copy(label = customLabel) else app
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _folders = MutableStateFlow<List<AppFolder>>(emptyList())
    /** The list of custom app folders. */
    val folders: StateFlow<List<AppFolder>> = _folders.asStateFlow()

    /**
     * The list of apps filtered by the current [searchQuery], with custom labels applied
     * and hidden apps removed.
     */
    val filteredApps: StateFlow<List<AppInfo>> = combine(
        _allApps, _searchQuery, _customLabels, preferences
    ) { apps, query, labels, prefs ->
        val hidden = prefs?.hiddenPackages ?: emptySet()
        val labeled = apps
            .filterNot { hidden.contains(it.packageName) }
            .map { app ->
                val customLabel = labels[app.packageName]
                if (customLabel != null) app.copy(label = customLabel) else app
            }
        
        if (query.isBlank()) labeled
        else labeled.filter { it.label.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Groups and sorts folders and apps off the main thread.
     *  When a search query is active, groups whose key matches the query's first letter come first. */
    val groupedDrawerItems: StateFlow<Map<Char, List<com.optimistswe.mementolauncher.ui.screens.AppDrawerItem>>> = combine(
        filteredApps, _folders, _searchQuery
    ) { apps, folders, query ->
        val assignedPackages = folders.flatMap { it.packages }.toSet()
        val unassignedApps = apps.filterNot { assignedPackages.contains(it.packageName) }

        val allItems = unassignedApps.map { com.optimistswe.mementolauncher.ui.screens.AppDrawerItem.App(it) } +
                       folders.map { folder ->
                           val resolvedApps = folder.packages.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                               .sortedBy { it.label.lowercase() }
                           com.optimistswe.mementolauncher.ui.screens.AppDrawerItem.Folder(folder, resolvedApps)
                       }

        val grouped = allItems.groupBy { item ->
            val first = item.displayName.firstOrNull()?.uppercaseChar() ?: '#'
            if (first.isLetter()) first else '#'
        }

        if (query.isBlank()) {
            grouped.toSortedMap()
        } else {
            // Put the group matching the query's first letter at the top
            val queryChar = query.firstOrNull()?.uppercaseChar()
            grouped.toSortedMap(compareBy<Char> { it != queryChar }.thenBy { it })
        }
    }.flowOn(ioDispatcher)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- App Launch Interception ---
    private val _interceptedLaunchPackage = MutableStateFlow<String?>(null)
    val interceptedLaunchPackage: StateFlow<String?> = _interceptedLaunchPackage.asStateFlow()

    /**
     * Requests an app launch. If the app is marked as distracting, it sets up an intercept.
     * Otherwise, it immediately invokes the direct launch callback.
     */
    fun requestAppLaunch(packageName: String, onDirectLaunch: (String) -> Unit) {
        val distracting = preferences.value?.distractingPackages?.contains(packageName) == true
        if (distracting) {
            _interceptedLaunchPackage.value = packageName
        } else {
            onDirectLaunch(packageName)
        }
    }

    /**
     * Clears the current intercepted launch state.
     */
    fun clearInterceptedLaunch() {
        _interceptedLaunchPackage.value = null
    }

    /**
     * Manually triggers the mindful delay overlay (e.g., from the Accessibility Service block).
     */
    fun triggerMindfulInterruption(packageName: String) {
        _interceptedLaunchPackage.value = packageName
    }

    // --- Life Metrics ---
    private val _lifeMetrics = MutableStateFlow<CalendarMetrics?>(null)
    val lifeMetrics: StateFlow<CalendarMetrics?> = _lifeMetrics.asStateFlow()

    private val _lifeProgressText = MutableStateFlow("")
    val lifeProgressText: StateFlow<String> = _lifeProgressText.asStateFlow()

    init {
        observeApps()
        observeFavorites()
        observeDockApps()
        observePreferencesDerived()
        observeCustomLabels()
        observeFolders()
    }

    /**
     * Derives clock style and life metrics from the single shared preferences flow,
     * eliminating redundant DataStore subscriptions.
     */
    private fun observePreferencesDerived() {
        viewModelScope.launch {
            preferences.filterNotNull().collect { prefs ->
                // Update clock style
                timeManager.updateClockStyle(prefs.clockStyle)

                // Update life metrics
                val birthDate = prefs.birthDate
                if (birthDate != null) {
                    val metrics = calculator.calculateMetrics(birthDate, prefs.lifeExpectancy)
                    _lifeMetrics.value = metrics
                    _lifeProgressText.value = "WEEK ${metrics.weeksLived} OF ${metrics.totalWeeks}"
                } else {
                    _lifeProgressText.value = ""
                }
            }
        }
    }

    private fun observeApps() {
        viewModelScope.launch {
            appRepository.observeApps().collect { apps ->
                _allApps.value = apps
                // Seed default favorites on first load
                seedDefaultsIfNeeded(apps)
                // Scrub orphaned packages from custom folders
                val validPackages = apps.map { it.packageName }.toSet()
                if (validPackages.isNotEmpty()) {
                    folderRepository.scrubPackages(validPackages)
                }
            }
        }
    }

    private suspend fun seedDefaultsIfNeeded(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        val installedPackages = apps.map { it.packageName }.toSet()
        favoritesRepository.seedDefaultFavorites(installedPackages)
        favoritesRepository.seedDefaultDockApps(installedPackages)
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.getFavorites().collect { favorites ->
                _favoritePackages.value = favorites
            }
        }
    }

    private fun observeDockApps() {
        viewModelScope.launch {
            favoritesRepository.getDockLeftApp().collect { pkg ->
                _dockLeftPkg.value = pkg
            }
        }
        viewModelScope.launch {
            favoritesRepository.getDockRightApp().collect { pkg ->
                _dockRightPkg.value = pkg
            }
        }
    }

    private fun observeCustomLabels() {
        viewModelScope.launch {
            appLabelRepository.getCustomLabels().collect { labels ->
                _customLabels.value = labels
            }
        }
    }

    private fun observeFolders() {
        viewModelScope.launch {
            folderRepository.folders.collect { f ->
                _folders.value = f
            }
        }
    }

    fun refreshClock() {
        timeManager.refresh()
    }

    /**
     * Updates the search query string and triggers a refresh of [filteredApps].
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clears the current search query.
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Pushes or pulls an app from the home screen favorites.
     */
    fun toggleFavorite(packageName: String) {
        viewModelScope.launch {
            if (_favoritePackages.value.contains(packageName)) {
                favoritesRepository.removeFavorite(packageName)
            } else {
                favoritesRepository.addFavorite(packageName)
            }
        }
    }

    /**
     * Simple check to see if a package is pinned.
     */
    fun isFavorite(packageName: String): Boolean {
        return _favoritePackages.value.contains(packageName)
    }

    /**
     * Sets the left corner dock app.
     */
    fun setDockLeftApp(packageName: String?) {
        viewModelScope.launch {
            favoritesRepository.setDockLeftApp(packageName)
        }
    }

    /**
     * Sets the right corner dock app.
     */
    fun setDockRightApp(packageName: String?) {
        viewModelScope.launch {
            favoritesRepository.setDockRightApp(packageName)
        }
    }

    /**
     * Persists a custom name for an app.
     *
     * @param packageName The package to rename.
     * @param newLabel The new name, or blank to revert to system default.
     */
    fun renameApp(packageName: String, newLabel: String) {
        viewModelScope.launch {
            if (newLabel.isBlank()) {
                appLabelRepository.clearCustomLabel(packageName)
            } else {
                appLabelRepository.setCustomLabel(packageName, newLabel)
            }
        }
    }

    // --- Folders ---

    /**
     * Creates a new empty folder in the app drawer.
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            folderRepository.createFolder(name)
        }
    }

    /**
     * Deletes a folder by ID. Content (apps) are released back to the main list.
     */
    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
        }
    }
    
    /**
     * Changes the display name of an existing folder.
     */
    fun renameFolder(folderId: String, newName: String) {
        viewModelScope.launch {
            folderRepository.renameFolder(folderId, newName)
        }
    }

    /**
     * Assigns an app package to a folder.
     */
    fun addAppToFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            folderRepository.addAppToFolder(folderId, packageName)
        }
    }

    /**
     * Removes an app package from a folder.
     */
    fun removeAppFromFolder(folderId: String, packageName: String) {
        viewModelScope.launch {
            folderRepository.removeAppFromFolder(folderId, packageName)
        }
    }

    // --- Preferences ---
    /**
     * Updates the user's birth date.
     */
    fun updateBirthDate(birthDate: java.time.LocalDate) {
        viewModelScope.launch {
            preferencesRepository.saveBirthDate(birthDate)
        }
    }

    /**
     * Updates the life expectancy setting.
     */
    fun updateLifeExpectancy(years: Int) {
        viewModelScope.launch {
            preferencesRepository.saveLifeExpectancy(years)
        }
    }

    /**
     * Sets whether the keyboard should auto-open in the app drawer.
     */
    fun updateAutoOpenKeyboard(autoOpen: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveAutoOpenKeyboard(autoOpen)
        }
    }

    /**
     * Updates the wallpaper/background visual style.
     */
    fun updateBackgroundStyle(style: com.optimistswe.mementolauncher.data.BackgroundStyle) {
        viewModelScope.launch {
            preferencesRepository.saveBackgroundStyle(style)
        }
    }

    /**
     * Updates the global font and icon scaling.
     */
    fun updateFontSize(size: com.optimistswe.mementolauncher.data.FontSize) {
        viewModelScope.launch {
            preferencesRepository.saveFontSize(size)
        }
    }

    /**
     * Updates the clock display style (12h, 24h, etc.).
     */
    fun updateClockStyle(style: com.optimistswe.mementolauncher.data.ClockStyle) {
        viewModelScope.launch {
            preferencesRepository.saveClockStyle(style)
        }
    }

    /**
     * Sets the vertical position of the search bar (Top or Bottom).
     */
    fun updateSearchBarPosition(position: com.optimistswe.mementolauncher.data.SearchBarPosition) {
        viewModelScope.launch {
            preferencesRepository.saveSearchBarPosition(position)
        }
    }

    /**
     * Toggles the visibility of a package in the app drawer.
     */
    fun toggleAppVisibility(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.togglePackageVisibility(packageName)
        }
    }

    /**
     * Toggles whether an app package should trigger the mindful launch delay.
     */
    fun toggleDistractingPackage(packageName: String) {
        viewModelScope.launch {
            preferencesRepository.toggleDistractingPackage(packageName)
        }
    }

    /**
     * Replaces the entire hidden packages set in a single write.
     */
    fun setHiddenPackages(packages: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setHiddenPackages(packages)
        }
    }

    /**
     * Replaces the entire distracting packages set in a single write.
     */
    fun setDistractingPackages(packages: Set<String>) {
        viewModelScope.launch {
            preferencesRepository.setDistractingPackages(packages)
        }
    }

    /**
     * Updates the custom mindful delay message.
     */
    fun updateMindfulMessage(message: String) {
        viewModelScope.launch {
            preferencesRepository.saveMindfulMessage(message)
        }
    }

    /**
     * Toggles the intent to use the accessibility service to block shorts/reels.
     */
    fun updateBlockShortFormContent(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveBlockShortFormContent(enabled)
        }
    }

    fun updateUsageNudgeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveUsageNudgeEnabled(enabled)
        }
    }

    fun updateUsageNudgeMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.saveUsageNudgeMinutes(minutes)
        }
    }

    fun refreshWidgets() {
        widgetManager.refresh()
    }

    // --- Backup & Restore ---

    fun getBackupJson(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupManager.exportBackup()
                onResult(json)
            } catch (_: Exception) {
                onResult(null)
            }
        }
    }

    fun importBackup(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                backupManager.importBackup(json)
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }
}
