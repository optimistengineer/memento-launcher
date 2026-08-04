package com.optimistswe.mementolauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Repository for managing user-defined folders in the app drawer.
 *
 * This repository handles the creation, deletion, and modification of app folders.
 * Folder data is stored as a JSON string within Jetpack DataStore and parsed using
 * kotlinx.serialization.
 *
 * @property dataStore The [DataStore] instance used for persisting folder data.
 */
class FolderRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private object PreferencesKeys {
        val FOLDERS = stringPreferencesKey("app_folders")
    }

    /**
     * A flow of all custom app folders.
     *
     * Emits an empty list initially if no folders exist or if decoding fails.
     */
    val folders: Flow<List<AppFolder>> = dataStore.data
        .catch { exception ->
            if (exception is java.io.IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            parseFoldersJson(jsonString)
        }

    /**
     * Creates a new folder with the given name.
     *
     * This method validates that the name is not blank and is not a duplicate
     * (case-insensitive) of an existing folder name.
     *
     * @param name The name of the folder to create.
     */
    suspend fun createFolder(name: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            // Refuse to write over data we could not read — that would destroy it.
            val currentFolders = (parseFoldersOrNull(jsonString) ?: return@edit).toMutableList()
            
            val trimmedName = name.trim()
            if (trimmedName.isBlank() || currentFolders.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                return@edit
            }
            
            val newFolder = AppFolder(
                name = trimmedName,
                packages = emptyList()
            )
            
            currentFolders.add(newFolder)
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(currentFolders)
        }
    }

    /**
     * Deletes an existing folder.
     *
     * @param folderId The unique ID of the folder to delete.
     */
    suspend fun deleteFolder(folderId: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            val currentFolders =
                (parseFoldersOrNull(jsonString) ?: return@edit).filterNot { it.id == folderId }
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(currentFolders)
        }
    }

    /**
     * Renames an existing folder.
     *
     * Validates that the new name is not blank and is not a duplicate of another folder.
     *
     * @param folderId The unique ID of the folder to rename.
     * @param newName The new name for the folder.
     */
    suspend fun renameFolder(folderId: String, newName: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            val currentFolders = parseFoldersOrNull(jsonString) ?: return@edit
            
            val trimmedName = newName.trim()
            if (trimmedName.isBlank() || currentFolders.any { it.name.equals(trimmedName, ignoreCase = true) && it.id != folderId }) {
                return@edit
            }
            
            val mapped = currentFolders.map {
                if (it.id == folderId) it.copy(name = trimmedName) else it
            }
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(mapped)
        }
    }

    /**
     * Adds an app package to a folder.
     *
     * If the app is already in the folder, this is a no-op.
     *
     * @param folderId The unique ID of the folder.
     * @param packageName The package name to add.
     */
    suspend fun addAppToFolder(folderId: String, packageName: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            val currentFolders = (parseFoldersOrNull(jsonString) ?: return@edit).map { folder ->
                if (folder.id == folderId && !folder.packages.contains(packageName)) {
                    folder.copy(packages = folder.packages + packageName)
                } else {
                    folder
                }
            }
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(currentFolders)
        }
    }

    /**
     * Removes an app package from a folder.
     *
     * @param folderId The unique ID of the folder.
     * @param packageName The package name to remove.
     */
    suspend fun removeAppFromFolder(folderId: String, packageName: String) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            val currentFolders = (parseFoldersOrNull(jsonString) ?: return@edit).map { folder ->
                if (folder.id == folderId) {
                    folder.copy(packages = folder.packages.filterNot { it == packageName })
                } else {
                    folder
                }
            }
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(currentFolders)
        }
    }

    suspend fun restoreAll(folders: List<AppFolder>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(folders)
        }
    }

    /**
     * Removes any package names from all folders that are not present in [validPackages].
     *
     * Useful for cleaning up folders after apps have been uninstalled from the device.
     *
     * @param validPackages The set of currently installed package names.
     */
    suspend fun scrubPackages(validPackages: Set<String>) {
        dataStore.edit { preferences ->
            val jsonString = preferences[PreferencesKeys.FOLDERS] ?: "[]"
            val currentFolders = parseFoldersOrNull(jsonString) ?: return@edit

            var changed = false
            val scrubbedFolders = currentFolders.map { folder ->
                val valid = folder.packages.filter { validPackages.contains(it) }
                if (valid.size != folder.packages.size) {
                    changed = true
                    folder.copy(packages = valid)
                } else {
                    folder
                }
            }
            if (changed) {
                preferences[PreferencesKeys.FOLDERS] = serializeFoldersJson(scrubbedFolders)
            }
        }
    }

    // --- JSON Helpers ---

    /**
     * Decodes the stored folder blob, or returns null if it cannot be read at all.
     *
     * Null and empty mean different things and must not be conflated. Every mutator writes the
     * decoded list straight back, so returning an empty list for unreadable JSON meant the first
     * folder interaction after any decode failure silently and permanently destroyed folders that
     * were still intact on disk. Callers now refuse to write when this returns null.
     *
     * A single malformed entry no longer takes the rest down with it: the blob is decoded as an
     * array first, then element by element, so bad entries are dropped individually.
     */
    private fun parseFoldersOrNull(jsonString: String): List<AppFolder>? {
        return try {
            json.decodeFromString<List<AppFolder>>(jsonString)
        } catch (wholeListFailed: Exception) {
            try {
                val elements = json.parseToJsonElement(jsonString).jsonArray
                val recovered = elements.mapNotNull { element ->
                    runCatching { json.decodeFromJsonElement<AppFolder>(element) }.getOrNull()
                }
                android.util.Log.w(
                    "FolderRepository",
                    "Recovered ${recovered.size} of ${elements.size} folders after a decode failure",
                    wholeListFailed
                )
                recovered
            } catch (notEvenAnArray: Exception) {
                android.util.Log.e("FolderRepository", "Folder JSON is unreadable", notEvenAnArray)
                null
            }
        }
    }

    /** Read-only view for the [folders] flow, where an unreadable blob simply shows as empty. */
    private fun parseFoldersJson(jsonString: String): List<AppFolder> =
        parseFoldersOrNull(jsonString) ?: emptyList()

    private fun serializeFoldersJson(folders: List<AppFolder>): String {
        return json.encodeToString(folders)
    }
}
