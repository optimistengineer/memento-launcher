package com.betteruniverse.mementolauncher.data

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
    // Defaulted so a `"packages": null` in a hand-edited backup coerces to empty
    // instead of failing the whole decode.
    val packages: List<String> = emptyList()
)
