package com.optimistswe.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
class FavoritesRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: FavoritesRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_favorites.preferences_pb") }
        )
        repository = FavoritesRepository(dataStore)
    }

    @Test
    fun `getFavorites returns empty list initially`() = runTest(testDispatcher) {
        val favorites = repository.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `addFavorite adds package name`() = runTest(testDispatcher) {
        val pkg = "com.test.app"
        val added = repository.addFavorite(pkg)
        
        assertTrue(added)
        val favorites = repository.getFavorites().first()
        assertEquals(listOf(pkg), favorites)
    }

    @Test
    fun `addFavorite returns false if already present`() = runTest(testDispatcher) {
        val pkg = "com.test.app"
        repository.addFavorite(pkg)
        val addedAgain = repository.addFavorite(pkg)
        
        assertFalse(addedAgain)
        val favorites = repository.getFavorites().first()
        assertEquals(listOf(pkg), favorites)
    }

    @Test
    fun `addFavorite respects MAX_FAVORITES`() = runTest(testDispatcher) {
        for (i in 1..FavoritesRepository.MAX_FAVORITES) {
            assertTrue(repository.addFavorite("pkg.$i"))
        }
        
        // Try to add one more
        val addedExtra = repository.addFavorite("pkg.extra")
        assertFalse(addedExtra)
        
        val favorites = repository.getFavorites().first()
        assertEquals(FavoritesRepository.MAX_FAVORITES, favorites.size)
    }

    @Test
    fun `removeFavorite removes existing package`() = runTest(testDispatcher) {
        val pkg = "com.test.app"
        repository.addFavorite(pkg)
        repository.removeFavorite(pkg)
        
        val favorites = repository.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `seedDefaultFavorites seeds matching packages`() = runTest(testDispatcher) {
        val installed = setOf(
            "com.google.android.dialer", // Matches DEFAULT_PHONE
            "com.android.mms",           // Matches DEFAULT_MESSAGES
            "other.app"
        )
        
        repository.seedDefaultFavorites(installed)
        
        val favorites = repository.getFavorites().first()
        assertEquals(listOf("com.google.android.dialer", "com.android.mms"), favorites)
    }

    @Test
    fun `seedDefaultFavorites only runs once`() = runTest(testDispatcher) {
        val installed1 = setOf("com.google.android.dialer")
        repository.seedDefaultFavorites(installed1)
        
        val installed2 = setOf("com.android.mms")
        repository.seedDefaultFavorites(installed2) // Should be a no-op
        
        val favorites = repository.getFavorites().first()
        assertEquals(listOf("com.google.android.dialer"), favorites)
    }

    @Test
    fun `isFavorite returns true for favorite apps`() = runTest(testDispatcher) {
        val pkg = "com.test.app"
        repository.addFavorite(pkg)

        assertTrue(repository.isFavorite(pkg).first())
        assertFalse(repository.isFavorite("other.app").first())
    }

    // ═══════════════════════════════════════════
    // Dock Left/Right
    // ═══════════════════════════════════════════

    @Test
    fun `getDockLeftApp returns null initially`() = runTest(testDispatcher) {
        assertNull(repository.getDockLeftApp().first())
    }

    @Test
    fun `getDockRightApp returns null initially`() = runTest(testDispatcher) {
        assertNull(repository.getDockRightApp().first())
    }

    @Test
    fun `setDockLeftApp persists value`() = runTest(testDispatcher) {
        repository.setDockLeftApp("com.google.android.dialer")
        assertEquals("com.google.android.dialer", repository.getDockLeftApp().first())
    }

    @Test
    fun `setDockRightApp persists value`() = runTest(testDispatcher) {
        repository.setDockRightApp("com.android.camera")
        assertEquals("com.android.camera", repository.getDockRightApp().first())
    }

    @Test
    fun `setDockLeftApp null clears value`() = runTest(testDispatcher) {
        repository.setDockLeftApp("com.test.app")
        repository.setDockLeftApp(null)
        assertNull(repository.getDockLeftApp().first())
    }

    @Test
    fun `setDockRightApp null clears value`() = runTest(testDispatcher) {
        repository.setDockRightApp("com.test.app")
        repository.setDockRightApp(null)
        assertNull(repository.getDockRightApp().first())
    }

    @Test
    fun `seedDefaultDockApps seeds phone and camera`() = runTest(testDispatcher) {
        val installed = setOf("com.google.android.dialer", "com.google.android.GoogleCamera")
        repository.seedDefaultDockApps(installed)

        assertEquals("com.google.android.dialer", repository.getDockLeftApp().first())
        assertEquals("com.google.android.GoogleCamera", repository.getDockRightApp().first())
    }

    @Test
    fun `seedDefaultDockApps only runs once`() = runTest(testDispatcher) {
        repository.seedDefaultDockApps(setOf("com.google.android.dialer"))
        repository.seedDefaultDockApps(setOf("com.android.dialer"))

        // Should still have the first seeded value
        assertEquals("com.google.android.dialer", repository.getDockLeftApp().first())
    }

    @Test
    fun `seedDefaultDockApps does not set if no matching packages`() = runTest(testDispatcher) {
        repository.seedDefaultDockApps(setOf("com.random.app"))
        assertNull(repository.getDockLeftApp().first())
        assertNull(repository.getDockRightApp().first())
    }

    // ═══════════════════════════════════════════
    // Additional Favorites edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `addFavorite preserves order`() = runTest(testDispatcher) {
        repository.addFavorite("com.a")
        repository.addFavorite("com.b")
        repository.addFavorite("com.c")

        val favorites = repository.getFavorites().first()
        assertEquals(listOf("com.a", "com.b", "com.c"), favorites)
    }

    @Test
    fun `removeFavorite of non-existent is no-op`() = runTest(testDispatcher) {
        repository.addFavorite("com.a")
        repository.removeFavorite("com.nonexistent")

        val favorites = repository.getFavorites().first()
        assertEquals(listOf("com.a"), favorites)
    }

    @Test
    fun `multiple removes leave empty list`() = runTest(testDispatcher) {
        repository.addFavorite("com.a")
        repository.addFavorite("com.b")
        repository.removeFavorite("com.a")
        repository.removeFavorite("com.b")

        assertTrue(repository.getFavorites().first().isEmpty())
    }

    @Test
    fun `seedDefaultFavorites with no matching packages seeds nothing`() = runTest(testDispatcher) {
        repository.seedDefaultFavorites(setOf("com.unrelated.app"))
        assertTrue(repository.getFavorites().first().isEmpty())
    }

    // ═══════════════════════════════════════════
    // Removing uninstalled packages
    // ═══════════════════════════════════════════
    // removePackages is removal-based, NOT allow-list-based. Its predecessor, scrubPackages,
    // deleted everything not currently installed — which permanently destroyed the favourites
    // and dock slots a JSON backup had just restored onto a new device where the apps were not
    // installed yet. These tests pin the new contract: only packages explicitly observed as
    // removed may be deleted; everything else survives, installed or not.

    @Test
    fun `removePackages drops exactly the removed favourites`() = runTest(testDispatcher) {
        repository.addFavorite("com.installed")
        repository.addFavorite("com.gone")
        repository.addFavorite("com.also.installed")

        repository.removePackages(setOf("com.gone"))

        assertEquals(
            listOf("com.installed", "com.also.installed"),
            repository.getFavorites().first()
        )
    }

    @Test
    fun `removePackages keeps favourites for apps that are merely not installed`() = runTest(testDispatcher) {
        // The restored-backup case: these packages are absent from the device but must survive
        // any removal that does not name them.
        repository.addFavorite("com.restored.not.installed")
        repository.addFavorite("com.other.restored")

        repository.removePackages(setOf("com.some.uninstalled.app"))

        assertEquals(
            listOf("com.restored.not.installed", "com.other.restored"),
            repository.getFavorites().first()
        )
    }

    @Test
    fun `removePackages clears only a dock corner whose app was removed`() = runTest(testDispatcher) {
        repository.setDockLeftApp("com.gone")
        repository.setDockRightApp("com.installed")

        repository.removePackages(setOf("com.gone"))

        assertNull("a dock corner pointing at a removed app must be cleared",
            repository.getDockLeftApp().first())
        assertEquals("com.installed", repository.getDockRightApp().first())
    }

    @Test
    fun `stale favourites do not consume MAX_FAVORITES slots when the installed set is given`() = runTest(testDispatcher) {
        // 7 stored favourites, but 3 of them belong to apps that are no longer (or not yet)
        // installed — e.g. entries kept alive by a backup restore. The cap must bound what the
        // user can see, so a new pin from the installed world must still succeed.
        for (i in 1..7) repository.addFavorite("pkg.$i")
        val installed = setOf("pkg.1", "pkg.2", "pkg.3", "pkg.4", "pkg.new")

        val added = repository.addFavorite("pkg.new", installed)

        assertTrue("4 visible favourites of 7 slots — pinning must succeed", added)
        assertTrue(repository.getFavorites().first().contains("pkg.new"))
    }

    @Test
    fun `removePackages with an empty set changes nothing`() = runTest(testDispatcher) {
        repository.addFavorite("com.a")
        repository.setDockLeftApp("com.a")

        repository.removePackages(emptySet())

        assertEquals(listOf("com.a"), repository.getFavorites().first())
        assertEquals("com.a", repository.getDockLeftApp().first())
    }
}
