package com.optimistswe.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repository for managing user-defined custom app names (aliases).
 *
 * This repository allows users to rename apps within the launcher. Custom labels are stored
 * in Jetpack DataStore, keyed by the package name with a "label_" prefix.
 *
 * When no custom label is found for a package, the system default label (from [PackageManager])
 * should be used by the UI layer.
 *
 * @property dataStore The [DataStore] instance used for persisting custom labels.
 */
class AppLabelRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Retrieves all custom labels as a map.
     *
     * The map keys are package names, and the values are the user-defined custom labels.
     *
     * @return A [Flow] emitting a [Map] of package names to custom labels.
     */
    fun getCustomLabels(): Flow<Map<String, String>> {
        return dataStore.data
            .catch { exception ->
                if (exception is java.io.IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences.asMap()
                    .filter { it.key.name.startsWith("label_") }
                    .mapKeys { it.key.name.removePrefix("label_") }
                    .mapValues { it.value as? String ?: "" }
            }
    }

    /**
     * Sets a custom label for a specific app package.
     *
     * @param packageName The unique package name of the app.
     * @param label The new custom label to associate with the app.
     */
    suspend fun setCustomLabel(packageName: String, label: String) {
        val key = stringPreferencesKey("label_$packageName")
        dataStore.edit { preferences ->
            preferences[key] = label
        }
    }

    suspend fun restoreAll(labels: Map<String, String>) {
        dataStore.edit { preferences ->
            // Clear existing labels
            val existingKeys = preferences.asMap().keys.filter { it.name.startsWith("label_") }
            existingKeys.forEach { preferences.remove(it) }
            // Write new labels
            labels.forEach { (packageName, label) ->
                preferences[stringPreferencesKey("label_$packageName")] = label
            }
        }
    }

    /**
     * Clears any custom label for a specific app package, effectively reverting it to the
     * system default name.
     *
     * @param packageName The unique package name of the app.
     */
    suspend fun clearCustomLabel(packageName: String) {
        val key = stringPreferencesKey("label_$packageName")
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
