package com.optimistswe.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FolderRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: FolderRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_folders.preferences_pb") }
        )
        repository = FolderRepository(dataStore)
    }

    @Test
    fun `folders returns empty list initially`() = runTest(testDispatcher) {
        val folders = repository.folders.first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `createFolder adds new folder`() = runTest(testDispatcher) {
        val name = "Social"
        repository.createFolder(name)
        
        val folders = repository.folders.first()
        assertEquals(1, folders.size)
        assertEquals(name, folders[0].name)
        assertTrue(folders[0].packages.isEmpty())
    }

    @Test
    fun `createFolder prevents blank name`() = runTest(testDispatcher) {
        repository.createFolder("  ")
        val folders = repository.folders.first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `createFolder prevents duplicate name`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        repository.createFolder("social") // Case-insensitive duplicate
        
        val folders = repository.folders.first()
        assertEquals(1, folders.size)
    }

    @Test
    fun `deleteFolder removes existing folder`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id
        
        repository.deleteFolder(folderId)
        val folders = repository.folders.first()
        assertTrue(folders.isEmpty())
    }

    @Test
    fun `renameFolder changes name`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id
        
        repository.renameFolder(folderId, "Work")
        val folders = repository.folders.first()
        assertEquals("Work", folders[0].name)
    }

    @Test
    fun `renameFolder prevents blank name`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folder = repository.folders.first()[0]
        
        repository.renameFolder(folder.id, " ")
        val folders = repository.folders.first()
        assertEquals("Social", folders[0].name)
    }

    @Test
    fun `addAppToFolder adds package name`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id
        val pkg = "com.facebook.katana"
        
        repository.addAppToFolder(folderId, pkg)
        val folders = repository.folders.first()
        assertEquals(listOf(pkg), folders[0].packages)
    }

    @Test
    fun `removeAppFromFolder removes package name`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id
        val pkg = "com.facebook.katana"
        
        repository.addAppToFolder(folderId, pkg)
        repository.removeAppFromFolder(folderId, pkg)
        val folders = repository.folders.first()
        assertTrue(folders[0].packages.isEmpty())
    }

    @Test
    fun `scrubPackages removes uninstalled apps`() = runTest(testDispatcher) {
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.installed")
        repository.addAppToFolder(folderId, "pkg.uninstalled")

        repository.scrubPackages(setOf("pkg.installed"))

        val folders = repository.folders.first()
        assertEquals(listOf("pkg.installed"), folders[0].packages)
    }

    // ═══════════════════════════════════════════
    // Additional edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `addAppToFolder does not duplicate`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id

        repository.addAppToFolder(folderId, "com.fb")
        repository.addAppToFolder(folderId, "com.fb") // duplicate

        val folders = repository.folders.first()
        assertEquals(1, folders[0].packages.size)
    }

    @Test
    fun `deleteFolder of non-existent id is no-op`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        repository.deleteFolder("nonexistent-id")

        val folders = repository.folders.first()
        assertEquals(1, folders.size)
    }

    @Test
    fun `renameFolder of non-existent id is no-op`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        repository.renameFolder("nonexistent-id", "NewName")

        val folders = repository.folders.first()
        assertEquals("Social", folders[0].name)
    }

    @Test
    fun `renameFolder prevents duplicate name with other folder`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        repository.createFolder("Work")
        val workId = repository.folders.first()[1].id

        repository.renameFolder(workId, "Social") // Duplicate name

        val folders = repository.folders.first()
        assertEquals("Work", folders.find { it.id == workId }?.name)
    }

    @Test
    fun `multiple folders can coexist`() = runTest(testDispatcher) {
        repository.createFolder("A")
        repository.createFolder("B")
        repository.createFolder("C")

        val folders = repository.folders.first()
        assertEquals(3, folders.size)
    }

    @Test
    fun `removeAppFromFolder on non-existent package is no-op`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "com.fb")

        repository.removeAppFromFolder(folderId, "com.nonexistent")

        val folders = repository.folders.first()
        assertEquals(1, folders[0].packages.size)
    }

    @Test
    fun `scrubPackages with empty valid set removes all packages`() = runTest(testDispatcher) {
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.a")
        repository.addAppToFolder(folderId, "pkg.b")

        repository.scrubPackages(emptySet())

        val folders = repository.folders.first()
        assertTrue(folders[0].packages.isEmpty())
    }

    @Test
    fun `scrubPackages when all packages are valid changes nothing`() = runTest(testDispatcher) {
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.a")
        repository.addAppToFolder(folderId, "pkg.b")

        repository.scrubPackages(setOf("pkg.a", "pkg.b"))

        val folders = repository.folders.first()
        assertEquals(2, folders[0].packages.size)
    }

    @Test
    fun `addAppToFolder to non-existent folder is no-op`() = runTest(testDispatcher) {
        repository.createFolder("Social")
        repository.addAppToFolder("nonexistent", "com.fb")

        val folders = repository.folders.first()
        assertTrue(folders[0].packages.isEmpty())
    }
}
