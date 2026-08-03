package com.optimistswe.mementolauncher.ui.managers

import com.optimistswe.mementolauncher.data.ClockStyle
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TimeManagerTest {

    private val timeManager = TimeManager()

    @Test
    fun `formatTime H24 formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H24)
        val time = LocalTime.of(14, 30)
        assertEquals("14:30", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H12 PM formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(14, 30)
        assertEquals("2:30 PM", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H12 AM formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(2, 30)
        assertEquals("2:30 AM", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H12 Midnight formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(0, 30)
        assertEquals("12:30 AM", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H24_SEC formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H24_SEC)
        val time = LocalTime.of(14, 30, 45)
        assertEquals("14:30:45", timeManager.formatTime(time))
    }

    @Test
    fun `formatDate formats correctly`() {
        val date = LocalDate.of(2024, 2, 23)
        // Expected format: EEE d MMM -> Fri 23 Feb
        assertEquals("FRI 23 FEB", timeManager.formatDate(date))
    }

    // ═══════════════════════════════════════════
    // Additional edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `formatTime H12 Noon formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(12, 0)
        assertEquals("12:00 PM", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H24 midnight formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H24)
        val time = LocalTime.of(0, 0)
        assertEquals("00:00", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H24 end of day formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H24)
        val time = LocalTime.of(23, 59)
        assertEquals("23:59", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H24_SEC midnight formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H24_SEC)
        val time = LocalTime.of(0, 0, 0)
        assertEquals("00:00:00", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H12 one minute before midnight formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(23, 59)
        assertEquals("11:59 PM", timeManager.formatTime(time))
    }

    @Test
    fun `formatTime H12 one AM formats correctly`() {
        timeManager.updateClockStyle(ClockStyle.H12)
        val time = LocalTime.of(1, 0)
        assertEquals("1:00 AM", timeManager.formatTime(time))
    }

    @Test
    fun `formatDate first day of year`() {
        val date = LocalDate.of(2024, 1, 1)
        assertEquals("MON 1 JAN", timeManager.formatDate(date))
    }

    @Test
    fun `formatDate last day of year`() {
        val date = LocalDate.of(2024, 12, 31)
        assertEquals("TUE 31 DEC", timeManager.formatDate(date))
    }

    @Test
    fun `updateClockStyle changes subsequent formatTime output`() {
        val time = LocalTime.of(15, 30)

        timeManager.updateClockStyle(ClockStyle.H24)
        assertEquals("15:30", timeManager.formatTime(time))

        timeManager.updateClockStyle(ClockStyle.H12)
        assertEquals("3:30 PM", timeManager.formatTime(time))

        timeManager.updateClockStyle(ClockStyle.H24_SEC)
        assertEquals("15:30:00", timeManager.formatTime(time))
    }

    @Test
    fun `refresh updates currentTime flow`() {
        timeManager.refresh()
        val current = timeManager.currentTime.value
        assertNotNull(current)
        assertTrue(current.isNotEmpty())
    }

    @Test
    fun `refresh updates currentDate flow`() {
        timeManager.refresh()
        val current = timeManager.currentDate.value
        assertNotNull(current)
        assertTrue(current.isNotEmpty())
    }
}
