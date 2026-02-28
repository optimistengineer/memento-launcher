package com.optimistswe.mementolauncher.ui.managers

import android.app.AlarmManager
import android.app.AppOpsManager
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
 * @property zoneId The time zone to use for calculating local event times. Defaults to system default.
 */
class WidgetManager(
    private val context: Context,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

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
     * Queries [UsageStatsManager] for today's total foreground time and updates [_screenTime].
     */
    private fun refreshScreenTime() {
        try {
            if (!hasUsagePermission()) {
                _screenTime.value = null
                return
            }
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
            if (stats.isNullOrEmpty()) {
                _screenTime.value = null
                return
            }
            val totalMs = stats.sumOf { it.totalTimeInForeground }
            val totalMinutes = (totalMs / 60_000).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            _screenTime.value = String.format(Locale.US, "%dH %02dM TODAY", hours, minutes)
        } catch (_: Exception) {
            _screenTime.value = null
        }
    }

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
                val localTime = instant.atZone(zoneId).toLocalTime()
                _nextAlarm.value = String.format(Locale.US, "ALARM %02d:%02d", localTime.hour, localTime.minute)
            } else {
                _nextAlarm.value = null
            }
        } catch (_: Exception) {
            _nextAlarm.value = null
        }
    }
}
