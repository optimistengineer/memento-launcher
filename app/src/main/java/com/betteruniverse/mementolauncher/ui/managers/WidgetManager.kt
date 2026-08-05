package com.betteruniverse.mementolauncher.ui.managers

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/**
 * Manages the discovery and status reporting of system "widgets" like Alarms and Calendar events.
 *
 * This class queries Android system providers (AlarmManager and CalendarContract) to find the next
 * upcoming events and exposes them as [StateFlow]s for the launcher UI.
 *
 * @property context Application context for system service access and content resolution.
 * @param fixedZoneId Pins the time zone, for deterministic tests. Left null in production so the
 *   zone is re-resolved on every query: this class is a @Singleton, so a zone captured once would
 *   be frozen for the life of the process and would keep using the old "today" boundary and the
 *   old alarm offset after the user crossed a time zone.
 */
class WidgetManager(
    private val context: Context,
    private val fixedZoneId: ZoneId? = null
) {

    private fun zone(): ZoneId = fixedZoneId ?: ZoneId.systemDefault()

    private companion object {
        /**
         * How far before midnight to start the event query, so an app that was already in the
         * foreground when the day rolled over is visible. Its interval is clipped to the start
         * of the day, so a longer window cannot inflate the total — it only avoids missing one.
         */
        const val PRE_MIDNIGHT_LOOKBACK_MS = 12L * 60 * 60 * 1000
    }

    private val _nextAlarm = MutableStateFlow<String?>(null)
    /** A flow emitting the next scheduled alarm formatted as "ALARM HH:mm", or null if none. */
    val nextAlarm: StateFlow<String?> = _nextAlarm.asStateFlow()

    private val _screenTime = MutableStateFlow<String?>(null)
    /** A flow emitting today's screen time formatted as "XH YM TODAY", or null if no permission. */
    val screenTime: StateFlow<String?> = _screenTime.asStateFlow()

    /**
     * Refreshes alarm and screen time states.
     */
    fun refresh() {
        refreshAlarm()
        refreshScreenTime()
    }

    /**
     * Checks whether the app has been granted usage stats access.
     */
    fun hasUsagePermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Computes today's foreground time from raw usage events and updates [_screenTime].
     *
     * Deliberately not queryUsageStats(INTERVAL_DAILY, …). Those buckets are pre-aggregated and
     * documented to span a period *longer* than the range asked for, so summing
     * totalTimeInForeground over them folds earlier usage into "today" — commonly close to
     * double-counting. The figure was simply wrong, and wrong in the direction that makes a
     * digital-wellbeing number alarming.
     *
     * Instead this walks MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND transitions and sums only the
     * parts of each interval that fall inside [startOfDay, now]. Only one app is in the
     * foreground at a time, so tracking a single open interval both avoids double-counting
     * overlapping packages and yields total screen time rather than per-app time.
     *
     * The query window starts before midnight so that an app already in the foreground at the
     * rollover is seen; its interval is then clipped to the start of the day.
     */
    private fun refreshScreenTime() {
        try {
            if (!hasUsagePermission()) {
                _screenTime.value = null
                return
            }
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            // Re-resolve the zone on every call: this class is a @Singleton, so a zone captured
            // in a constructor default would be frozen for the life of the process and would
            // keep using the old "today" boundary after the user changed time zone.
            val zone = zone()
            val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

            val events = usm.queryEvents(startOfDay - PRE_MIDNIGHT_LOOKBACK_MS, now)
            if (events == null) {
                _screenTime.value = null
                return
            }

            var totalMs = 0L
            var openedAt: Long? = null
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        // A second foreground without an intervening background means the first
                        // interval was never closed; take the later start rather than dropping it.
                        openedAt = event.timeStamp
                    }
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val start = openedAt ?: continue
                        totalMs += overlapWithToday(start, event.timeStamp, startOfDay, now)
                        openedAt = null
                    }
                }
            }
            // Whatever is in the foreground right now is still accruing.
            openedAt?.let { totalMs += overlapWithToday(it, now, startOfDay, now) }

            val totalMinutes = (totalMs / 60_000).toInt()
            _screenTime.value = String.format(
                Locale.US, "%dH %02dM TODAY", totalMinutes / 60, totalMinutes % 60
            )
        } catch (_: Exception) {
            _screenTime.value = null
        }
    }

    /** Milliseconds of [start, end] that fall inside [dayStart, dayEnd]. */
    private fun overlapWithToday(start: Long, end: Long, dayStart: Long, dayEnd: Long): Long =
        (minOf(end, dayEnd) - maxOf(start, dayStart)).coerceAtLeast(0L)

    /**
     * Queries the [AlarmManager] for the next scheduled alarm and updates [_nextAlarm].
     */
    private fun refreshAlarm() {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmInfo = am.nextAlarmClock
            if (alarmInfo != null) {
                val triggerTime = alarmInfo.triggerTime
                val instant = Instant.ofEpochMilli(triggerTime)
                val localTime = instant.atZone(zone()).toLocalTime()
                _nextAlarm.value = String.format(Locale.US, "ALARM %02d:%02d", localTime.hour, localTime.minute)
            } else {
                _nextAlarm.value = null
            }
        } catch (_: Exception) {
            _nextAlarm.value = null
        }
    }
}
