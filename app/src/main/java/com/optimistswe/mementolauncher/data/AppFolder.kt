package com.optimistswe.mementolauncher.data

import kotlinx.serialization.Serializable

/**
 * Represents a user-created folder that groups apps together in the launcher.
 *
 * @property id Unique identifier for the folder, auto-generated as a UUID.
 * @property name Display name of the folder.
 * @property packages List of package names for apps contained in this folder.
 */
@Serializable
data class AppFolder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val packages: List<String>
)
