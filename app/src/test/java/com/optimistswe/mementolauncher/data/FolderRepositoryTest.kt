package com.optimistswe.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    fun `removePackages removes exactly the uninstalled apps`() = runTest(testDispatcher) {
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.installed")
        repository.addAppToFolder(folderId, "pkg.uninstalled")

        repository.removePackages(setOf("pkg.uninstalled"))

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
    fun `removePackages with an empty set changes nothing`() = runTest(testDispatcher) {
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.a")
        repository.addAppToFolder(folderId, "pkg.b")

        repository.removePackages(emptySet())

        val folders = repository.folders.first()
        assertEquals(2, folders[0].packages.size)
    }

    @Test
    fun `removePackages keeps folder entries for apps that are merely not installed`() = runTest(testDispatcher) {
        // The restored-backup case: folder contents restored onto a device where the apps are
        // not installed yet must survive removals that do not name them.
        repository.createFolder("Tools")
        val folderId = repository.folders.first()[0].id
        repository.addAppToFolder(folderId, "pkg.restored.a")
        repository.addAppToFolder(folderId, "pkg.restored.b")

        repository.removePackages(setOf("pkg.something.else"))

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

    // ═══════════════════════════════════════════
    // Unreadable / partially bad stored JSON
    // ═══════════════════════════════════════════

    private suspend fun writeRawFolders(raw: String) {
        dataStore.edit { it[stringPreferencesKey("app_folders")] = raw }
    }

    @Test
    fun `a mutation does not destroy folders when the stored json is unreadable`() = runTest(testDispatcher) {
        // Every mutator writes the decoded list straight back. Decoding unreadable JSON to an
        // empty list therefore meant the first folder interaction permanently destroyed data
        // that was still intact on disk.
        writeRawFolders("this is not json at all")

        repository.createFolder("NEW")

        val stored = dataStore.data.first()[stringPreferencesKey("app_folders")]
        assertEquals("the unreadable blob must be left untouched", "this is not json at all", stored)
    }

    @Test
    fun `delete does not destroy folders when the stored json is unreadable`() = runTest(testDispatcher) {
        writeRawFolders("{{{ broken")

        repository.deleteFolder("whatever")

        assertEquals("{{{ broken", dataStore.data.first()[stringPreferencesKey("app_folders")])
    }

    @Test
    fun `one malformed entry does not take the other folders down with it`() = runTest(testDispatcher) {
        // Previously a single bad element failed the whole array decode, so every folder vanished.
        writeRawFolders(
            """[{"id":"a","name":"GOOD","packages":["com.a"]},""" +
            """{"id":"b","name":12345,"packages":["com.b"]},""" +
            """{"id":"c","name":"ALSO GOOD","packages":["com.c"]}]"""
        )

        val folders = repository.folders.first()

        assertEquals(2, folders.size)
        assertEquals(listOf("ALSO GOOD", "GOOD"), folders.map { it.name }.sorted())
    }

    @Test
    fun `a null packages array coerces to empty instead of failing the decode`() = runTest(testDispatcher) {
        writeRawFolders("""[{"id":"a","name":"NULLPKGS","packages":null}]""")

        val folders = repository.folders.first()

        assertEquals(1, folders.size)
        assertEquals("NULLPKGS", folders[0].name)
        assertTrue(folders[0].packages.isEmpty())
    }
}
