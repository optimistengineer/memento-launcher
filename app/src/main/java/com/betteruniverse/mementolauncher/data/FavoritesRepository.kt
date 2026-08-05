package com.betteruniverse.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for managing favorite (pinned) apps on the launcher home screen.
 *
 * This repository handles the persistence of the user's favorite apps, which are displayed
 * prominently on the home screen. It stores a list of package names as a comma-separated string
 * within Jetpack DataStore.
 *
 * @property dataStore The [DataStore] instance used for persisting favorites.
 */
class FavoritesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val FAVORITES_KEY = stringPreferencesKey("favorite_apps")
        private val SEEDED_KEY = booleanPreferencesKey("favorites_seeded")

        private val DOCK_LEFT_KEY = stringPreferencesKey("dock_left_app")
        private val DOCK_RIGHT_KEY = stringPreferencesKey("dock_right_app")
        private val DOCK_SEEDED_KEY = booleanPreferencesKey("dock_seeded")

        /** The maximum number of apps allowed in the favorites list. */
        const val MAX_FAVORITES = 7

        /**
         * Common package names for system-level Dialer apps.
         */
        val DEFAULT_PHONE_PACKAGES = listOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.samsung.android.dialer"
        )

        /**
         * Common package names for system-level Messaging apps.
         */
        val DEFAULT_MESSAGES_PACKAGES = listOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging"
        )

        /**
         * Common package names for system-level Camera apps.
         */
        val DEFAULT_CAMERA_PACKAGES = listOf(
            "com.google.android.GoogleCamera",
            "com.android.camera",
            "com.android.camera2",
            "com.samsung.android.camera"
        )

        /**
         * Common package names for system-level Browser apps.
         */
        val DEFAULT_CHROME_PACKAGES = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx"
        )

        /**
         * Common package names for system-level Calendar apps.
         */
        val DEFAULT_CALENDAR_PACKAGES = listOf(
            "com.google.android.calendar",
            "com.android.calendar",
            "com.samsung.android.calendar"
        )
    }

    // ═══════════════════════════════════════════
    // FAVORITES
    // ═══════════════════════════════════════════

    /** Returns a [Flow] of favorite app package names. */
    fun getFavorites(): Flow<List<String>> {
        return dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val raw = preferences[FAVORITES_KEY] ?: ""
                if (raw.isBlank()) emptyList()
                else raw.split(",").filter { it.isNotBlank() }
            }
    }

    /** Seeds the favorites list with common default apps (Phone, Messages, Camera) on first launch. */
    suspend fun seedDefaultFavorites(installedPackages: Set<String>) {
        dataStore.edit { preferences ->
            if (preferences[SEEDED_KEY] == true) return@edit

            val defaults = mutableListOf<String>()
            DEFAULT_PHONE_PACKAGES.firstOrNull { it in installedPackages }?.let { defaults.add(it) }
            DEFAULT_MESSAGES_PACKAGES.firstOrNull { it in installedPackages }?.let { defaults.add(it) }
            DEFAULT_CAMERA_PACKAGES.firstOrNull { it in installedPackages }?.let { defaults.add(it) }

            if (defaults.isNotEmpty()) {
                preferences[FAVORITES_KEY] = defaults.joinToString(",")
            }
            preferences[SEEDED_KEY] = true
        }
    }

    /**
     * Adds an app to the favorites list if the [MAX_FAVORITES] limit has not been reached.
     *
     * @param installedPackages when provided, only stored favourites that are actually installed
     *   count toward the cap. This matters because [removePackages] deliberately keeps entries
     *   for not-installed apps (restored backups, uninstalls missed while the process was dead) —
     *   those entries render nowhere, so if they occupied cap slots the user would long-press an
     *   app, nothing would happen, and there would be nothing visible to remove to free the slot.
     *   The cap must bound what the user can SEE, not what the store happens to hold.
     * @return `true` if the app was added, `false` if it was already present or the list is full.
     */
    suspend fun addFavorite(packageName: String, installedPackages: Set<String>? = null): Boolean {
        var added = false
        dataStore.edit { preferences ->
            val current = (preferences[FAVORITES_KEY] ?: "")
                .split(",").filter { it.isNotBlank() }.toMutableList()
            val occupiedSlots = if (installedPackages != null) {
                current.count { it in installedPackages }
            } else {
                current.size
            }
            if (!current.contains(packageName) && occupiedSlots < MAX_FAVORITES) {
                current.add(packageName)
                preferences[FAVORITES_KEY] = current.joinToString(",")
                added = true
            }
        }
        return added
    }

    /** Removes an app from the favorites list. */
    suspend fun removeFavorite(packageName: String) {
        dataStore.edit { preferences ->
            val current = (preferences[FAVORITES_KEY] ?: "")
                .split(",").filter { it.isNotBlank() }.toMutableList()
            current.remove(packageName)
            preferences[FAVORITES_KEY] = current.joinToString(",")
        }
    }

    suspend fun restoreAll(favorites: List<String>, dockLeft: String?, dockRight: String?) {
        dataStore.edit { preferences ->
            preferences[FAVORITES_KEY] = favorites.joinToString(",")
            preferences[SEEDED_KEY] = true
            if (dockLeft != null) preferences[DOCK_LEFT_KEY] = dockLeft
            else preferences.remove(DOCK_LEFT_KEY)
            if (dockRight != null) preferences[DOCK_RIGHT_KEY] = dockRight
            else preferences.remove(DOCK_RIGHT_KEY)
            preferences[DOCK_SEEDED_KEY] = true
        }
    }

    /**
     * Drops favourites and dock corners whose package is no longer installed.
     *
     * Stale package names arrive from more than one direction: a restored cloud backup, an app
     * uninstalled while the launcher was not running, or a user switching devices. Left in place
     * they render as home screen entries that look tappable and silently do nothing, because
     * getLaunchIntentForPackage returns null. FolderRepository already does this for folders.
     *
     * No-ops when [validPackages] is empty, since that means the app list has not loaded yet
     * rather than that nothing is installed.
     */
    /**
     * Removes the given packages from favourites and the dock corners.
     *
     * Removal-based, not allow-list-based, and that distinction is load-bearing. This used to be
     * `scrubPackages(validPackages)`, which deleted everything not currently installed — but "not
     * installed right now" is not "gone": a JSON backup restored on a new device names apps the
     * user has not reinstalled yet, and the old scrub permanently destroyed exactly the
     * favourites and dock slots the restore had just written, on the very next app-list emission.
     * Not-installed entries are already invisible at render time (LauncherViewModel filters
     * against the live app list), so keeping them stored costs nothing and lets them reappear
     * when their app is installed. Only an observed uninstall may delete.
     */
    suspend fun removePackages(removedPackages: Set<String>) {
        if (removedPackages.isEmpty()) return
        dataStore.edit { preferences ->
            val current = (preferences[FAVORITES_KEY] ?: "")
                .split(",").filter { it.isNotBlank() }
            val kept = current.filterNot { removedPackages.contains(it) }
            if (kept.size != current.size) {
                preferences[FAVORITES_KEY] = kept.joinToString(",")
            }
            preferences[DOCK_LEFT_KEY]?.let { if (it in removedPackages) preferences.remove(DOCK_LEFT_KEY) }
            preferences[DOCK_RIGHT_KEY]?.let { if (it in removedPackages) preferences.remove(DOCK_RIGHT_KEY) }
        }
    }

    /** Returns a [Flow] that emits whether the given package is in the favorites list. */
    fun isFavorite(packageName: String): Flow<Boolean> {
        return getFavorites().map { it.contains(packageName) }
    }

    // ═══════════════════════════════════════════
    // DOCK — Left & Right corner apps
    // ═══════════════════════════════════════════

    /** Flow of left dock app package name (nullable). */
    fun getDockLeftApp(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[DOCK_LEFT_KEY] }
    }

    /** Flow of right dock app package name (nullable). */
    fun getDockRightApp(): Flow<String?> {
        return dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[DOCK_RIGHT_KEY] }
    }

    /** Sets the left corner dock app. Pass null to clear. */
    suspend fun setDockLeftApp(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName != null) prefs[DOCK_LEFT_KEY] = packageName
            else prefs.remove(DOCK_LEFT_KEY)
        }
    }

    /** Sets the right corner dock app. Pass null to clear. */
    suspend fun setDockRightApp(packageName: String?) {
        dataStore.edit { prefs ->
            if (packageName != null) prefs[DOCK_RIGHT_KEY] = packageName
            else prefs.remove(DOCK_RIGHT_KEY)
        }
    }

    /**
     * Seeds default dock corner apps: Phone (left), Camera (right).
     */
    suspend fun seedDefaultDockApps(installedPackages: Set<String>) {
        dataStore.edit { preferences ->
            if (preferences[DOCK_SEEDED_KEY] == true) return@edit

            val phoneApp = DEFAULT_PHONE_PACKAGES.firstOrNull { it in installedPackages }
            val cameraApp = DEFAULT_CAMERA_PACKAGES.firstOrNull { it in installedPackages }

            phoneApp?.let { preferences[DOCK_LEFT_KEY] = it }
            cameraApp?.let { preferences[DOCK_RIGHT_KEY] = it }
            preferences[DOCK_SEEDED_KEY] = true
        }
    }
}
