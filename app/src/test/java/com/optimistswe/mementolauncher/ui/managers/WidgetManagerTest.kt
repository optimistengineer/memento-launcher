package com.optimistswe.mementolauncher.ui.managers

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>()
    private val contentResolver = mockk<ContentResolver>()
    private val testZoneId = ZoneId.of("UTC")
    private val widgetManager = WidgetManager(context, testZoneId)

    @Before
    fun setup() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.contentResolver } returns contentResolver
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `refreshAlarm updates nextAlarm correctly when alarm exists`() {
        val triggerTimeMs: Long = Instant.parse("2024-02-23T08:30:00Z").toEpochMilli()
        val alarmClockInfo = mockk<AlarmClockInfo>()
        every { alarmClockInfo.triggerTime } returns triggerTimeMs
        every { alarmManager.nextAlarmClock } returns alarmClockInfo

        widgetManager.refresh()

        assertEquals("ALARM 08:30", widgetManager.nextAlarm.value)
    }

    @Test
    fun `refreshAlarm sets nextAlarm to null when no alarm exists`() {
        every { alarmManager.nextAlarmClock } returns null

        widgetManager.refresh()

        assertNull(widgetManager.nextAlarm.value)
    }

    // ═══════════════════════════════════════════
    // Additional edge cases
    // ═══════════════════════════════════════════

    @Test
    fun `refreshAlarm handles midnight alarm`() {
        val midnight: Long = Instant.parse("2024-02-23T00:00:00Z").toEpochMilli()
        val alarmClockInfo = mockk<AlarmClockInfo>()
        every { alarmClockInfo.triggerTime } returns midnight
        every { alarmManager.nextAlarmClock } returns alarmClockInfo

        widgetManager.refresh()

        assertEquals("ALARM 00:00", widgetManager.nextAlarm.value)
    }

    @Test
    fun `refreshAlarm handles end of day alarm`() {
        val endOfDay: Long = Instant.parse("2024-02-23T23:59:00Z").toEpochMilli()
        val alarmClockInfo = mockk<AlarmClockInfo>()
        every { alarmClockInfo.triggerTime } returns endOfDay
        every { alarmManager.nextAlarmClock } returns alarmClockInfo

        widgetManager.refresh()

        assertEquals("ALARM 23:59", widgetManager.nextAlarm.value)
    }

    @Test
    fun `refreshAlarm handles exception gracefully`() {
        every { alarmManager.nextAlarmClock } throws RuntimeException("Test error")

        widgetManager.refresh()

        assertNull(widgetManager.nextAlarm.value)
    }

    @Test
    fun `nextAlarm is null initially`() {
        assertNull(widgetManager.nextAlarm.value)
    }

    @Test
    fun `refresh clears alarm when removed`() {
        // First set an alarm
        val triggerTimeMs: Long = Instant.parse("2024-02-23T08:30:00Z").toEpochMilli()
        val alarmClockInfo = mockk<AlarmClockInfo>()
        every { alarmClockInfo.triggerTime } returns triggerTimeMs
        every { alarmManager.nextAlarmClock } returns alarmClockInfo
        widgetManager.refresh()
        assertEquals("ALARM 08:30", widgetManager.nextAlarm.value)

        // Then remove alarm
        every { alarmManager.nextAlarmClock } returns null
        widgetManager.refresh()
        assertNull(widgetManager.nextAlarm.value)
    }
}
