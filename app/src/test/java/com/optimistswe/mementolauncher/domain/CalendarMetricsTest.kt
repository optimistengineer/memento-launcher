package com.optimistswe.mementolauncher.domain

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [CalendarMetrics] data class.
 */
class CalendarMetricsTest {

    @Test
    fun `constructor sets all fields`() {
        val metrics = CalendarMetrics(
            weeksLived = 1560,
            totalWeeks = 4160,
            weeksRemaining = 2600,
            currentYearOfLife = 31,
            currentWeekOfYear = 5,
            lifeExpectancy = 80,
            percentageLived = 37.5f
        )
        assertEquals(1560, metrics.weeksLived)
        assertEquals(4160, metrics.totalWeeks)
        assertEquals(2600, metrics.weeksRemaining)
        assertEquals(31, metrics.currentYearOfLife)
        assertEquals(5, metrics.currentWeekOfYear)
        assertEquals(80, metrics.lifeExpectancy)
        assertEquals(37.5f, metrics.percentageLived, 0.01f)
    }

    @Test
    fun `data class equality works`() {
        val a = CalendarMetrics(100, 4160, 4060, 2, 49, 80, 2.4f)
        val b = CalendarMetrics(100, 4160, 4060, 2, 49, 80, 2.4f)
        assertEquals(a, b)
    }

    @Test
    fun `data class inequality when weeksLived differs`() {
        val a = CalendarMetrics(100, 4160, 4060, 2, 49, 80, 2.4f)
        val b = CalendarMetrics(101, 4160, 4059, 2, 49, 80, 2.4f)
        assertNotEquals(a, b)
    }

    @Test
    fun `copy with modified weeksLived`() {
        val original = CalendarMetrics(100, 4160, 4060, 2, 49, 80, 2.4f)
        val modified = original.copy(weeksLived = 200)
        assertEquals(200, modified.weeksLived)
        assertEquals(original.totalWeeks, modified.totalWeeks)
    }

    @Test
    fun `toString contains meaningful fields`() {
        val metrics = CalendarMetrics(1560, 4160, 2600, 31, 5, 80, 37.5f)
        val str = metrics.toString()
        assertTrue(str.contains("1560"))
        assertTrue(str.contains("4160"))
        assertTrue(str.contains("80"))
    }
}
