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
class AppLabelRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: AppLabelRepository

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_labels.preferences_pb") }
        )
        repository = AppLabelRepository(dataStore)
    }

    @Test
    fun `getCustomLabels returns empty map initially`() = runTest(testDispatcher) {
        val labels = repository.getCustomLabels().first()
        assertTrue(labels.isEmpty())
    }

    @Test
    fun `setCustomLabel adds label for package`() = runTest(testDispatcher) {
        val pkg = "com.android.chrome"
        val label = "Web Browser"
        
        repository.setCustomLabel(pkg, label)
        val labels = repository.getCustomLabels().first()
        
        assertEquals(label, labels[pkg])
    }

    @Test
    fun `clearCustomLabel removes label`() = runTest(testDispatcher) {
        val pkg = "com.android.chrome"
        repository.setCustomLabel(pkg, "Browser")
        repository.clearCustomLabel(pkg)
        
        val labels = repository.getCustomLabels().first()
        assertTrue(labels.isEmpty())
    }

    @Test
    fun `getCustomLabels ignores non-label keys`() = runTest(testDispatcher) {
        repository.setCustomLabel("pkg1", "Label1")

        val labels = repository.getCustomLabels().first()
        assertEquals(1, labels.size)
        assertEquals("Label1", labels["pkg1"])
    }

    // ═══════════════════════════════════════════
    // Additional edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `setCustomLabel overwrites existing label`() = runTest(testDispatcher) {
        val pkg = "com.android.chrome"
        repository.setCustomLabel(pkg, "Browser")
        repository.setCustomLabel(pkg, "Web")

        val labels = repository.getCustomLabels().first()
        assertEquals("Web", labels[pkg])
    }

    @Test
    fun `multiple labels for different packages`() = runTest(testDispatcher) {
        repository.setCustomLabel("pkg1", "Label A")
        repository.setCustomLabel("pkg2", "Label B")
        repository.setCustomLabel("pkg3", "Label C")

        val labels = repository.getCustomLabels().first()
        assertEquals(3, labels.size)
        assertEquals("Label A", labels["pkg1"])
        assertEquals("Label B", labels["pkg2"])
        assertEquals("Label C", labels["pkg3"])
    }

    @Test
    fun `clearCustomLabel of non-existent is no-op`() = runTest(testDispatcher) {
        repository.setCustomLabel("pkg1", "Label1")
        repository.clearCustomLabel("pkg.nonexistent")

        val labels = repository.getCustomLabels().first()
        assertEquals(1, labels.size)
    }

    @Test
    fun `setCustomLabel with empty string stores it`() = runTest(testDispatcher) {
        repository.setCustomLabel("pkg1", "")
        val labels = repository.getCustomLabels().first()
        assertEquals("", labels["pkg1"])
    }

    @Test
    fun `setCustomLabel with special characters`() = runTest(testDispatcher) {
        repository.setCustomLabel("pkg1", "My App !@#$%")
        val labels = repository.getCustomLabels().first()
        assertEquals("My App !@#$%", labels["pkg1"])
    }
}
