package com.betteruniverse.mementolauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.betteruniverse.mementolauncher.data.AppInfo
import com.betteruniverse.mementolauncher.data.AppLabelRepository
import com.betteruniverse.mementolauncher.data.AppRepository
import com.betteruniverse.mementolauncher.data.BackupManager
import com.betteruniverse.mementolauncher.data.FavoritesRepository
import com.betteruniverse.mementolauncher.data.FolderRepository
import com.betteruniverse.mementolauncher.data.PreferencesRepository
import com.betteruniverse.mementolauncher.data.AppFolder
import com.betteruniverse.mementolauncher.domain.CalendarMetrics
import com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator
import com.betteruniverse.mementolauncher.ui.managers.TimeManager
import com.betteruniverse.mementolauncher.ui.managers.WidgetManager
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
    @com.betteruniverse.mementolauncher.di.IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
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
    val isBirthday: StateFlow<Boolean> = combine(
        preferences.filterNotNull(),
        timeManager.today
    ) { prefs, today ->
        val birthDate = prefs.birthDate ?: return@combine false
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
    val groupedDrawerItems: StateFlow<Map<Char, List<com.betteruniverse.mementolauncher.ui.screens.AppDrawerItem>>> = combine(
        filteredApps, _folders, _searchQuery
    ) { apps, folders, query ->
        val assignedPackages = folders.flatMap { it.packages }.toSet()
        val unassignedApps = apps.filterNot { assignedPackages.contains(it.packageName) }

        // Folders were previously emitted unconditionally, so every folder stayed on screen
        // during a search even when neither it nor anything inside it matched.
        val folderItems = folders.mapNotNull { folder ->
            val resolvedApps = folder.packages.mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                .sortedBy { it.label.lowercase() }
            val matchesQuery = query.isBlank() ||
                    folder.name.contains(query, ignoreCase = true) ||
                    resolvedApps.isNotEmpty()
            if (matchesQuery) {
                com.betteruniverse.mementolauncher.ui.screens.AppDrawerItem.Folder(folder, resolvedApps)
            } else null
        }

        val allItems = unassignedApps.map { com.betteruniverse.mementolauncher.ui.screens.AppDrawerItem.App(it) } +
                       folderItems

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
            // Combined with today's date: the metrics are a function of (preferences, date), and
            // the preferences flow is DataStore-backed so it only emits when a setting is written.
            // Observing it alone froze the weeks-lived counter and the whole dot grid at whatever
            // they were when the process started — for a HOME app, potentially for days.
            combine(preferences.filterNotNull(), timeManager.today) { prefs, today ->
                prefs to today
            }.collect { (prefs, today) ->
                // Update clock style
                timeManager.updateClockStyle(prefs.clockStyle)

                // Update life metrics.
                //
                // calculateMetrics rejects a birth date in the future. A stored date can still be
                // invalid — e.g. restored from a hand-edited backup — and letting that throw here
                // would kill this collector and crash the launcher on every start, which for a HOME
                // app means an unusable device. Degrade to "no metrics" instead.
                //
                // Assigning _lifeMetrics unconditionally also clears stale metrics when the birth
                // date is removed; previously only the progress text was reset.
                val metrics = prefs.birthDate?.let { birthDate ->
                    runCatching {
                        calculator.calculateMetrics(birthDate, prefs.lifeExpectancy, today)
                    }.getOrNull()
                }
                _lifeMetrics.value = metrics
                _lifeProgressText.value =
                    metrics?.let { "WEEK ${it.weeksLived} OF ${it.totalWeeks}" } ?: ""
            }
        }
    }


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
                android.util.Log.e("LauncherViewModel", "persistence write failed", e)
            }
        }
    }

    private fun observeApps() {
        viewModelScope.launch {
            appRepository.observeApps().collect { update ->
                _allApps.value = update.apps
                // Seeding and removal are unguarded DataStore writes, which throw IOException
                // on a full disk or a corrupt store. This collector runs on every launch and on
                // every package broadcast, so an uncaught throw here would crash the HOME app
                // repeatedly. The app list itself is already published above, so a failure to
                // seed or clean up degrades gracefully rather than taking down the launcher.
                runCatching {
                    // Seed default favorites on first load
                    seedDefaultsIfNeeded(update.apps)
                    // Delete stored placements ONLY for a package the system told us was
                    // uninstalled (ACTION_PACKAGE_REMOVED without EXTRA_REPLACING), carried on
                    // the emission itself. Two rejected alternatives, both destructive:
                    //  - scrubbing everything not currently installed wiped the favourites, dock
                    //    slots and folder contents a JSON backup had just restored onto a new
                    //    device, before the user could reinstall their apps;
                    //  - diffing consecutive emissions deleted placements for apps that merely
                    //    *look* gone for a moment — disabled in settings, mid-update, or on
                    //    SD/adoptable storage that unmounted — all of which come back.
                    // Entries for not-installed packages render nowhere and (via the installed-
                    // set-aware favourites cap) occupy no pin slots, so keeping them is free,
                    // and they self-heal when their app appears.
                    update.removedPackage?.let { pkg ->
                        folderRepository.removePackages(setOf(pkg))
                        favoritesRepository.removePackages(setOf(pkg))
                    }
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
        persist {
            if (_favoritePackages.value.contains(packageName)) {
                favoritesRepository.removeFavorite(packageName)
            } else {
                // The installed set makes the 7-slot cap count only favourites the user can see.
                // Stored entries for not-installed apps (kept on purpose to survive backup
                // restores) render nowhere — if they held cap slots, pinning would silently fail
                // with nothing visible to remove.
                favoritesRepository.addFavorite(
                    packageName,
                    _allApps.value.mapTo(mutableSetOf()) { it.packageName }
                )
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
        persist {
            favoritesRepository.setDockLeftApp(packageName)
        }
    }

    /**
     * Sets the right corner dock app.
     */
    fun setDockRightApp(packageName: String?) {
        persist {
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
        persist {
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
        persist {
            folderRepository.createFolder(name)
        }
    }

    /**
     * Deletes a folder by ID. Content (apps) are released back to the main list.
     */
    fun deleteFolder(folderId: String) {
        persist {
            folderRepository.deleteFolder(folderId)
        }
    }
    
    /**
     * Changes the display name of an existing folder.
     */
    fun renameFolder(folderId: String, newName: String) {
        persist {
            folderRepository.renameFolder(folderId, newName)
        }
    }

    /**
     * Assigns an app package to a folder.
     */
    fun addAppToFolder(folderId: String, packageName: String) {
        persist {
            folderRepository.addAppToFolder(folderId, packageName)
        }
    }

    /**
     * Removes an app package from a folder.
     */
    fun removeAppFromFolder(folderId: String, packageName: String) {
        persist {
            folderRepository.removeAppFromFolder(folderId, packageName)
        }
    }

    // --- Preferences ---
    /**
     * Updates the user's birth date.
     */
    fun updateBirthDate(birthDate: java.time.LocalDate) {
        persist {
            preferencesRepository.saveBirthDate(birthDate)
        }
    }

    /**
     * Updates the life expectancy setting.
     */
    fun updateLifeExpectancy(years: Int) {
        persist {
            preferencesRepository.saveLifeExpectancy(years)
        }
    }

    /**
     * Sets whether the keyboard should auto-open in the app drawer.
     */
    fun updateAutoOpenKeyboard(autoOpen: Boolean) {
        persist {
            preferencesRepository.saveAutoOpenKeyboard(autoOpen)
        }
    }

    /**
     * Updates the wallpaper/background visual style.
     */
    fun updateBackgroundStyle(style: com.betteruniverse.mementolauncher.data.BackgroundStyle) {
        persist {
            preferencesRepository.saveBackgroundStyle(style)
        }
    }

    /**
     * Updates the global font and icon scaling.
     */
    fun updateFontSize(size: com.betteruniverse.mementolauncher.data.FontSize) {
        persist {
            preferencesRepository.saveFontSize(size)
        }
    }

    /**
     * Updates the clock display style (12h, 24h, etc.).
     */
    fun updateClockStyle(style: com.betteruniverse.mementolauncher.data.ClockStyle) {
        persist {
            preferencesRepository.saveClockStyle(style)
        }
    }

    /**
     * Sets the vertical position of the search bar (Top or Bottom).
     */
    fun updateSearchBarPosition(position: com.betteruniverse.mementolauncher.data.SearchBarPosition) {
        persist {
            preferencesRepository.saveSearchBarPosition(position)
        }
    }

    /**
     * Toggles the visibility of a package in the app drawer.
     */
    fun toggleAppVisibility(packageName: String) {
        persist {
            preferencesRepository.togglePackageVisibility(packageName)
        }
    }

    /**
     * Toggles whether an app package should trigger the mindful launch delay.
     */
    fun toggleDistractingPackage(packageName: String) {
        persist {
            preferencesRepository.toggleDistractingPackage(packageName)
        }
    }

    /**
     * Replaces the entire hidden packages set in a single write.
     */
    fun setHiddenPackages(packages: Set<String>) {
        persist {
            preferencesRepository.setHiddenPackages(packages)
        }
    }

    /**
     * Replaces the entire distracting packages set in a single write.
     */
    fun setDistractingPackages(packages: Set<String>) {
        persist {
            preferencesRepository.setDistractingPackages(packages)
        }
    }

    /**
     * Updates the custom mindful delay message.
     */
    fun updateMindfulMessage(message: String) {
        persist {
            preferencesRepository.saveMindfulMessage(message)
        }
    }

    /**
     * Toggles the intent to use the accessibility service to block shorts/reels.
     */
    fun updateBlockShortFormContent(enabled: Boolean) {
        persist {
            preferencesRepository.saveBlockShortFormContent(enabled)
        }
    }

    /** Shows or hides the life calendar page. */
    fun updateShowLifeCalendar(show: Boolean) {
        persist {
            preferencesRepository.saveShowLifeCalendar(show)
        }
    }

    fun updateUsageNudgeEnabled(enabled: Boolean) {
        persist {
            preferencesRepository.saveUsageNudgeEnabled(enabled)
        }
    }

    fun updateUsageNudgeMinutes(minutes: Int) {
        persist {
            preferencesRepository.saveUsageNudgeMinutes(minutes)
        }
    }

    fun refreshWidgets() {
        // WidgetManager.refresh() makes three blocking binder calls — AppOpsManager, plus
        // UsageStatsManager.queryUsageStats which marshals one UsageStats per installed package
        // out of system_server, routinely tens to hundreds of ms. This is driven by a 60s
        // LaunchedEffect loop that runs on the Compose main dispatcher, so leaving it inline
        // dropped a frame on the home screen every minute. Every other heavy path here already
        // moves off the main thread.
        viewModelScope.launch(ioDispatcher) {
            // Guarded: this runs on a 60s timer for the life of the HOME process, and the
            // UsageStats/AppOps binder calls inside can throw (e.g. SecurityException the moment
            // the user revokes usage access in system settings while the loop is mid-flight).
            // One failed refresh must cost one stale widget reading, not the launcher process.
            runCatching { widgetManager.refresh() }
                .onFailure { android.util.Log.e("LauncherViewModel", "widget refresh failed", it) }
        }
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
