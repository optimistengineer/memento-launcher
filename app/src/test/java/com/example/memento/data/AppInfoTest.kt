package com.optimistswe.mementolauncher.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [AppInfo] data class.
 */
class AppInfoTest {

    @Test
    fun `constructor sets all fields`() {
        val info = AppInfo(
            label = "YouTube",
            packageName = "com.google.android.youtube",
            activityName = "com.google.android.youtube.HomeActivity"
        )
        assertEquals("YouTube", info.label)
        assertEquals("com.google.android.youtube", info.packageName)
        assertEquals("com.google.android.youtube.HomeActivity", info.activityName)
    }

    @Test
    fun `data class equality works`() {
        val a = AppInfo("App", "com.pkg", "act")
        val b = AppInfo("App", "com.pkg", "act")
        assertEquals(a, b)
    }

    @Test
    fun `data class inequality works`() {
        val a = AppInfo("App A", "com.a", "act")
        val b = AppInfo("App B", "com.b", "act")
        assertNotEquals(a, b)
    }

    @Test
    fun `copy with new label preserves other fields`() {
        val original = AppInfo("Original", "com.pkg", "act")
        val copied = original.copy(label = "Renamed")
        assertEquals("Renamed", copied.label)
        assertEquals("com.pkg", copied.packageName)
        assertEquals("act", copied.activityName)
    }

    @Test
    fun `hashCode is consistent for equal objects`() {
        val a = AppInfo("App", "com.pkg", "act")
        val b = AppInfo("App", "com.pkg", "act")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toString contains all fields`() {
        val info = AppInfo("MyApp", "com.my", "com.my.Main")
        val str = info.toString()
        assertTrue(str.contains("MyApp"))
        assertTrue(str.contains("com.my"))
        assertTrue(str.contains("com.my.Main"))
    }
}
