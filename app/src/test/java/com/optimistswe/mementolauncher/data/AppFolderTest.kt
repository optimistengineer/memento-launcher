package com.optimistswe.mementolauncher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [AppFolder] data class and its serialization.
 */
class AppFolderTest {

    @Test
    fun `constructor generates unique id by default`() {
        val a = AppFolder(name = "Social", packages = emptyList())
        val b = AppFolder(name = "Social", packages = emptyList())
        assertNotEquals(a.id, b.id)
    }

    @Test
    fun `constructor sets name and packages`() {
        val folder = AppFolder(name = "Work", packages = listOf("com.a", "com.b"))
        assertEquals("Work", folder.name)
        assertEquals(listOf("com.a", "com.b"), folder.packages)
    }

    @Test
    fun `copy preserves id when only name changes`() {
        val original = AppFolder(name = "Old", packages = listOf("com.a"))
        val copied = original.copy(name = "New")
        assertEquals(original.id, copied.id)
        assertEquals("New", copied.name)
        assertEquals(original.packages, copied.packages)
    }

    @Test
    fun `copy with added package`() {
        val original = AppFolder(name = "Test", packages = listOf("com.a"))
        val updated = original.copy(packages = original.packages + "com.b")
        assertEquals(2, updated.packages.size)
        assertTrue(updated.packages.contains("com.a"))
        assertTrue(updated.packages.contains("com.b"))
    }

    @Test
    fun `serialization round-trip preserves all fields`() {
        val json = Json { ignoreUnknownKeys = true }
        val original = AppFolder(id = "test-id-123", name = "Social", packages = listOf("com.fb", "com.ig"))

        val jsonString = json.encodeToString(original)
        val deserialized: AppFolder = json.decodeFromString(jsonString)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.packages, deserialized.packages)
    }

    @Test
    fun `serialization of empty packages`() {
        val json = Json { ignoreUnknownKeys = true }
        val folder = AppFolder(id = "empty", name = "Empty", packages = emptyList())

        val jsonString = json.encodeToString(folder)
        val deserialized: AppFolder = json.decodeFromString(jsonString)

        assertEquals("Empty", deserialized.name)
        assertTrue(deserialized.packages.isEmpty())
    }

    @Test
    fun `list serialization round-trip`() {
        val json = Json { ignoreUnknownKeys = true }
        val folders = listOf(
            AppFolder(id = "1", name = "A", packages = listOf("pkg.1")),
            AppFolder(id = "2", name = "B", packages = emptyList())
        )

        val jsonString = json.encodeToString(folders)
        val deserialized: List<AppFolder> = json.decodeFromString(jsonString)

        assertEquals(2, deserialized.size)
        assertEquals("A", deserialized[0].name)
        assertEquals("B", deserialized[1].name)
    }

    @Test
    fun `data class equality works with same id`() {
        val a = AppFolder(id = "same", name = "Folder", packages = emptyList())
        val b = AppFolder(id = "same", name = "Folder", packages = emptyList())
        assertEquals(a, b)
    }

    @Test
    fun `data class inequality with different id`() {
        val a = AppFolder(id = "id1", name = "Folder", packages = emptyList())
        val b = AppFolder(id = "id2", name = "Folder", packages = emptyList())
        assertNotEquals(a, b)
    }
}
