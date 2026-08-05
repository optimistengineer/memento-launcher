package com.betteruniverse.mementolauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Tests for [BackupManager], which had no coverage at all despite being the only write path that
 * accepts arbitrary values from a file the user can hand-edit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupManagerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private val scope = TestScope(dispatcher)

    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var favoritesRepo: FavoritesRepository
    private lateinit var labelRepo: AppLabelRepository
    private lateinit var folderRepo: FolderRepository
    private lateinit var manager: BackupManager

    private fun store(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { tmp.newFile(name) })

    @Before
    fun setup() {
        prefsRepo = PreferencesRepository(store("p.preferences_pb"))
        favoritesRepo = FavoritesRepository(store("f.preferences_pb"))
        labelRepo = AppLabelRepository(store("l.preferences_pb"))
        folderRepo = FolderRepository(store("d.preferences_pb"))
        manager = BackupManager(prefsRepo, favoritesRepo, labelRepo, folderRepo)
    }

    @Test
    fun `export writes the schema version`() = runTest(dispatcher) {
        // `version` always equals its default, and kotlinx drops defaults unless told otherwise,
        // so the key was absent from every exported file.
        val exported = manager.exportBackup()

        assertTrue("exported JSON must carry a version: $exported", exported.contains("\"version\""))
    }

    @Test
    fun `export round trips through import`() = runTest(dispatcher) {
        prefsRepo.saveLifeExpectancy(72)
        favoritesRepo.addFavorite("com.a")
        labelRepo.setCustomLabel("com.a", "AAA")

        val exported = manager.exportBackup()
        prefsRepo.saveLifeExpectancy(90)

        manager.importBackup(exported)

        assertEquals(72, prefsRepo.getUserPreferences().first().lifeExpectancy)
        assertEquals(listOf("com.a"), favoritesRepo.getFavorites().first())
        assertEquals("AAA", labelRepo.getCustomLabels().first()["com.a"])
    }

    @Test
    fun `a backup from a newer schema version is refused before anything is written`() =
        runTest(dispatcher) {
            prefsRepo.saveLifeExpectancy(72)

            val fromTheFuture = """{"version":99,"lifeExpectancy":55}"""
            val failure = runCatching { manager.importBackup(fromTheFuture) }.exceptionOrNull()

            assertTrue(
                "expected a rejection, got $failure",
                failure is IllegalArgumentException
            )
            assertEquals(
                "existing state must be untouched",
                72,
                prefsRepo.getUserPreferences().first().lifeExpectancy
            )
        }

    @Test
    fun `restore clamps an out of range life expectancy`() = runTest(dispatcher) {
        manager.importBackup("""{"version":1,"lifeExpectancy":0}""")
        assertEquals(
            LifeCalendarCalculator.MIN_LIFE_EXPECTANCY,
            prefsRepo.getUserPreferences().first().lifeExpectancy
        )

        manager.importBackup("""{"version":1,"lifeExpectancy":9999}""")
        assertEquals(
            LifeCalendarCalculator.MAX_LIFE_EXPECTANCY,
            prefsRepo.getUserPreferences().first().lifeExpectancy
        )
    }

    @Test
    fun `restore drops duplicate favourites and enforces the maximum`() = runTest(dispatcher) {
        // Every interactive path enforces both; restoreAll enforced neither. Duplicates render the
        // app twice on the home screen, and remove-by-long-press only drops the first occurrence.
        val many = (1..20).joinToString(",") { "\"com.app$it\"" }
        manager.importBackup("""{"version":1,"favorites":[$many,"com.app1","com.app1"]}""")

        val favorites = favoritesRepo.getFavorites().first()

        assertEquals(FavoritesRepository.MAX_FAVORITES, favorites.size)
        assertEquals("no duplicates", favorites.size, favorites.distinct().size)
    }

    @Test
    fun `restore drops blank custom labels`() = runTest(dispatcher) {
        // A blank label renders the app with no name at all.
        manager.importBackup("""{"version":1,"customLabels":{"com.a":"","com.b":"BEE"}}""")

        val labels = labelRepo.getCustomLabels().first()

        assertNull(labels["com.a"])
        assertEquals("BEE", labels["com.b"])
    }

    @Test
    fun `restore drops folders with duplicate ids blank names or duplicate names`() =
        runTest(dispatcher) {
            // Duplicate ids make every id-keyed operation act on all matches at once; duplicate
            // names deadlock renaming because the uniqueness guard silently no-ops.
            manager.importBackup(
                """{"version":1,"folders":[
                   {"id":"a","name":"TOOLS","packages":[]},
                   {"id":"a","name":"OTHER","packages":[]},
                   {"id":"b","name":"tools","packages":[]},
                   {"id":"c","name":"   ","packages":[]},
                   {"id":"d","name":"MEDIA","packages":[]}]}"""
            )

            val folders = folderRepo.folders.first()

            assertEquals(listOf("MEDIA", "TOOLS"), folders.map { it.name }.sorted())
            assertEquals("ids must be unique", folders.size, folders.map { it.id }.distinct().size)
        }

    @Test
    fun `restore drops a blank dock package`() = runTest(dispatcher) {
        manager.importBackup("""{"version":1,"dockLeft":"","dockRight":"com.right"}""")

        assertNull(favoritesRepo.getDockLeftApp().first())
        assertEquals("com.right", favoritesRepo.getDockRightApp().first())
    }
}
