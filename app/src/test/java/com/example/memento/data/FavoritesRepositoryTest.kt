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
}
