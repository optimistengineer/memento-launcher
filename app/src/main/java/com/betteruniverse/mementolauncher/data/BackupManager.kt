package com.betteruniverse.mementolauncher.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = 1,
    // Preferences
    val birthDateEpochDays: Long? = null,
    val lifeExpectancy: Int = 80,
    val wallpaperTarget: String = "BOTH",
    val theme: String = "DARK",
    val dotStyle: String = "FILLED_CIRCLE",
    val backgroundStyle: String = "MATRIX_GRID",
    val fontSize: String = "MEDIUM",
    val isSetupComplete: Boolean = false,
    val autoOpenKeyboard: Boolean = true,
    val clockStyle: String = "H24",
    val searchBarPosition: String = "TOP",
    val hiddenPackages: Set<String> = emptySet(),
    val distractingPackages: Set<String> = emptySet(),
    val mindfulMessage: String = "IS THIS\nINTENTIONAL?",
    val blockShortFormContent: Boolean = false,
    val usageNudgeEnabled: Boolean = false,
    val usageNudgeMinutes: Int = 15,
    val showLifeCalendar: Boolean = true,
    // Favorites
    val favorites: List<String> = emptyList(),
    val dockLeft: String? = null,
    val dockRight: String? = null,
    // Custom labels
    val customLabels: Map<String, String> = emptyMap(),
    // Folders
    val folders: List<AppFolder> = emptyList()
)

class BackupManager(
    private val preferencesRepository: PreferencesRepository,
    private val favoritesRepository: FavoritesRepository,
    private val appLabelRepository: AppLabelRepository,
    private val folderRepository: FolderRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        // Without this, `version` is omitted from every export: it always equals its default of 1
        // and kotlinx.serialization drops defaults unless told otherwise. The field existed but
        // was never actually written, so no future build could tell a v1 file from anything else.
        encodeDefaults = true
    }

    companion object {
        /** The schema version this build writes. */
        const val CURRENT_VERSION = 1
    }

    suspend fun exportBackup(): String {
        val prefs = preferencesRepository.getUserPreferences().first()
        val favorites = favoritesRepository.getFavorites().first()
        val dockLeft = favoritesRepository.getDockLeftApp().first()
        val dockRight = favoritesRepository.getDockRightApp().first()
        val labels = appLabelRepository.getCustomLabels().first()
        val folders = folderRepository.folders.first()

        val data = BackupData(
            birthDateEpochDays = prefs.birthDate?.toEpochDay(),
            lifeExpectancy = prefs.lifeExpectancy,
            wallpaperTarget = prefs.wallpaperTarget.name,
            theme = prefs.theme.name,
            dotStyle = prefs.dotStyle.name,
            backgroundStyle = prefs.backgroundStyle.name,
            fontSize = prefs.fontSize.name,
            isSetupComplete = prefs.isSetupComplete,
            autoOpenKeyboard = prefs.autoOpenKeyboard,
            clockStyle = prefs.clockStyle.name,
            searchBarPosition = prefs.searchBarPosition.name,
            hiddenPackages = prefs.hiddenPackages,
            distractingPackages = prefs.distractingPackages,
            mindfulMessage = prefs.mindfulMessage,
            blockShortFormContent = prefs.blockShortFormContent,
            usageNudgeEnabled = prefs.usageNudgeEnabled,
            usageNudgeMinutes = prefs.usageNudgeMinutes,
            showLifeCalendar = prefs.showLifeCalendar,
            favorites = favorites,
            dockLeft = dockLeft,
            dockRight = dockRight,
            customLabels = labels,
            folders = folders
        )

        return json.encodeToString(data)
    }

    /**
     * Restores a backup.
     *
     * Everything is decoded, version-checked and sanitised BEFORE the first write, so a malformed
     * or hostile file fails without having touched any stored state. The four repositories sit on
     * four separate DataStore files and cannot share a transaction, so a failure partway through
     * the writes is still possible; prior state is captured up front and rolled back on failure to
     * narrow that window as far as it can go without a real transaction.
     *
     * Restore also has to enforce the invariants the interactive paths enforce, because it is the
     * one write path that accepts arbitrary values from outside the app.
     *
     * @throws IllegalArgumentException if the file is from a newer schema version.
     */
    suspend fun importBackup(jsonString: String) {
        val raw = json.decodeFromString<BackupData>(jsonString)

        require(raw.version <= CURRENT_VERSION) {
            "This backup was written by a newer version of Memento (v${raw.version}); " +
                "this build understands up to v$CURRENT_VERSION."
        }

        val data = sanitize(raw)

        // Snapshot for rollback. Captured before any write.
        val priorPrefs = preferencesRepository.getUserPreferences().first()
        val priorFavorites = favoritesRepository.getFavorites().first()
        val priorDockLeft = favoritesRepository.getDockLeftApp().first()
        val priorDockRight = favoritesRepository.getDockRightApp().first()
        val priorLabels = appLabelRepository.getCustomLabels().first()
        val priorFolders = folderRepository.folders.first()

        try {
            applyRestore(data)
        } catch (failure: Throwable) {
            runCatching {
                preferencesRepository.restoreAll(
                    birthDateEpochDays = priorPrefs.birthDate?.toEpochDay(),
                    lifeExpectancy = priorPrefs.lifeExpectancy,
                    wallpaperTarget = priorPrefs.wallpaperTarget.name,
                    theme = priorPrefs.theme.name,
                    dotStyle = priorPrefs.dotStyle.name,
                    backgroundStyle = priorPrefs.backgroundStyle.name,
                    fontSize = priorPrefs.fontSize.name,
                    isSetupComplete = priorPrefs.isSetupComplete,
                    autoOpenKeyboard = priorPrefs.autoOpenKeyboard,
                    clockStyle = priorPrefs.clockStyle.name,
                    searchBarPosition = priorPrefs.searchBarPosition.name,
                    hiddenPackages = priorPrefs.hiddenPackages,
                    distractingPackages = priorPrefs.distractingPackages,
                    mindfulMessage = priorPrefs.mindfulMessage,
                    blockShortFormContent = priorPrefs.blockShortFormContent,
                    usageNudgeEnabled = priorPrefs.usageNudgeEnabled,
                    usageNudgeMinutes = priorPrefs.usageNudgeMinutes,
                    showLifeCalendar = priorPrefs.showLifeCalendar
                )
                favoritesRepository.restoreAll(priorFavorites, priorDockLeft, priorDockRight)
                appLabelRepository.restoreAll(priorLabels)
                folderRepository.restoreAll(priorFolders)
            }
            throw failure
        }
    }

    /**
     * Brings an arbitrary decoded backup in line with the invariants the interactive paths
     * enforce. Restore is the only write path fed by a file the user could have hand-edited.
     */
    private fun sanitize(data: BackupData): BackupData {
        val favorites = data.favorites
            .filter { it.isNotBlank() }
            .distinct()
            .take(FavoritesRepository.MAX_FAVORITES)

        val seenIds = mutableSetOf<String>()
        val seenNames = mutableSetOf<String>()
        val folders = data.folders.mapNotNull { folder ->
            val name = folder.name.trim()
            // Duplicate ids make every id-keyed operation act on all matching folders at once;
            // duplicate names deadlock renaming, because the uniqueness guard silently no-ops.
            if (name.isBlank()) return@mapNotNull null
            if (!seenIds.add(folder.id)) return@mapNotNull null
            if (!seenNames.add(name.lowercase())) return@mapNotNull null
            folder.copy(name = name, packages = folder.packages.filter { it.isNotBlank() }.distinct())
        }

        return data.copy(
            lifeExpectancy = data.lifeExpectancy.coerceIn(
                com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator.MIN_LIFE_EXPECTANCY,
                com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator.MAX_LIFE_EXPECTANCY
            ),
            usageNudgeMinutes = data.usageNudgeMinutes.coerceIn(1, 24 * 60),
            favorites = favorites,
            dockLeft = data.dockLeft?.takeIf { it.isNotBlank() },
            dockRight = data.dockRight?.takeIf { it.isNotBlank() },
            // A blank custom label renders the app with no name at all in the drawer.
            customLabels = data.customLabels.filterValues { it.isNotBlank() },
            folders = folders
        )
    }

    private suspend fun applyRestore(data: BackupData) {
        preferencesRepository.restoreAll(
            birthDateEpochDays = data.birthDateEpochDays,
            lifeExpectancy = data.lifeExpectancy,
            wallpaperTarget = data.wallpaperTarget,
            theme = data.theme,
            dotStyle = data.dotStyle,
            backgroundStyle = data.backgroundStyle,
            fontSize = data.fontSize,
            isSetupComplete = data.isSetupComplete,
            autoOpenKeyboard = data.autoOpenKeyboard,
            clockStyle = data.clockStyle,
            searchBarPosition = data.searchBarPosition,
            hiddenPackages = data.hiddenPackages,
            distractingPackages = data.distractingPackages,
            mindfulMessage = data.mindfulMessage,
            blockShortFormContent = data.blockShortFormContent,
            usageNudgeEnabled = data.usageNudgeEnabled,
            usageNudgeMinutes = data.usageNudgeMinutes,
            showLifeCalendar = data.showLifeCalendar
        )

        favoritesRepository.restoreAll(
            favorites = data.favorites,
            dockLeft = data.dockLeft,
            dockRight = data.dockRight
        )

        appLabelRepository.restoreAll(data.customLabels)

        folderRepository.restoreAll(data.folders)
    }
}
