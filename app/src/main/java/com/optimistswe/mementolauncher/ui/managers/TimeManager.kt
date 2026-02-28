package com.optimistswe.mementolauncher.ui.managers

import com.optimistswe.mementolauncher.data.ClockStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Manages clock and date formatting and provides reactive state for the launcher's UI.
 *
 * This class is responsible for converting [LocalTime] and [LocalDate] into human-readable strings
 * based on the user's selected [ClockStyle]. It exposes these strings as [StateFlow]s to ensure
 * the UI updates immediately when the time or preferences change.
 */
class TimeManager {
    private var clockStyle = ClockStyle.H24
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    private val _currentTime = MutableStateFlow(formatTime(LocalTime.now()))
    /** A flow emitting the current time formatted according to [clockStyle]. */
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow(formatDate(LocalDate.now()))
    /** A flow emitting the current date formatted as "EEE d MMM" (e.g., "MON 23 FEB"). */
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    /**
     * Updates the internal [ClockStyle] and refreshes the [currentTime] state.
     *
     * @param style The new [ClockStyle] to use for time formatting.
     */
    fun updateClockStyle(style: ClockStyle) {
        clockStyle = style
        refresh()
    }

    /**
     * Forces a refresh of the [currentTime] and [currentDate] flows with the latest system time.
     */
    fun refresh() {
        _currentTime.value = formatTime(LocalTime.now())
        _currentDate.value = formatDate(LocalDate.now())
    }

    /**
     * Formats a [LocalTime] based on the current [clockStyle].
     *
     * @param time The time to format.
     * @return A formatted string (e.g., "14:30", "2:30 PM", or "14:30:45").
     */
    internal fun formatTime(time: LocalTime): String {
        return when (clockStyle) {
            ClockStyle.H24 ->
                String.format(Locale.US, "%02d:%02d", time.hour, time.minute)
            ClockStyle.H12 -> {
                val hour12 = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
                val amPm = if (time.hour < 12) "AM" else "PM"
                String.format(Locale.US, "%d:%02d %s", hour12, time.minute, amPm)
            }
            ClockStyle.H24_SEC ->
                String.format(Locale.US, "%02d:%02d:%02d", time.hour, time.minute, time.second)
        }
    }

    /**
     * Formats a [LocalDate] into an uppercase string (e.g., "MON 23 FEB").
     *
     * @param date The date to format.
     * @return A formatted string using English locale.
     */
    internal fun formatDate(date: LocalDate): String {
        return date.format(dateFormatter).uppercase(Locale.ENGLISH)
    }
}
