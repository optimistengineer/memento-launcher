package com.optimistswe.mementolauncher.data

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
            favorites = favorites,
            dockLeft = dockLeft,
            dockRight = dockRight,
            customLabels = labels,
            folders = folders
        )

        return json.encodeToString(data)
    }

    suspend fun importBackup(jsonString: String) {
        val data = json.decodeFromString<BackupData>(jsonString)

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
            usageNudgeMinutes = data.usageNudgeMinutes
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
