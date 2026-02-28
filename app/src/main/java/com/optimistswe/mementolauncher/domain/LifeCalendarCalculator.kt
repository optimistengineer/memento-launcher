package com.optimistswe.mementolauncher.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calculator for life calendar metrics.
 *
 * This class provides pure functions to calculate the number of weeks lived,
 * total weeks in a lifetime, and current position in the life calendar grid.
 * All calculations are based on the user's birth date and expected life span.
 *
 * ## Usage Example
 * ```kotlin
 * val calculator = LifeCalendarCalculator()
 * val birthDate = LocalDate.of(1990, 5, 15)
 * val weeksLived = calculator.calculateWeeksLived(birthDate)
 * val totalWeeks = calculator.calculateTotalWeeks(lifeExpectancy = 80)
 * ```
 *
 * @see CalendarMetrics for the complete result data class
 */
class LifeCalendarCalculator {

    companion object {
        /** Number of weeks in a year (used for grid columns) */
        const val WEEKS_PER_YEAR = 52

        /** Default life expectancy in years if not specified by user */
        const val DEFAULT_LIFE_EXPECTANCY = 80

        /** Minimum allowed life expectancy for validation */
        const val MIN_LIFE_EXPECTANCY = 50

        /** Maximum allowed life expectancy for validation */
        const val MAX_LIFE_EXPECTANCY = 120
    }

    /**
     * Calculates all life calendar metrics for the given birth date.
     *
     * This is the primary method to get all necessary data for rendering
     * the life calendar grid. It combines multiple calculations into a
     * single result object for convenience.
     *
     * @param birthDate The user's date of birth. Must be in the past.
     * @param lifeExpectancy Expected lifespan in years. Defaults to [DEFAULT_LIFE_EXPECTANCY].
     * @param referenceDate The date to calculate against. Defaults to today.
     *                      Useful for testing with fixed dates.
     * @return [CalendarMetrics] containing all calculated values
     * @throws IllegalArgumentException if birthDate is in the future
     */
    fun calculateMetrics(
        birthDate: LocalDate,
        lifeExpectancy: Int = DEFAULT_LIFE_EXPECTANCY,
        referenceDate: LocalDate = LocalDate.now()
    ): CalendarMetrics {
        require(birthDate <= referenceDate) {
            "Birth date cannot be in the future"
        }

        val weeksLived = calculateWeeksLived(birthDate, referenceDate)
        val totalWeeks = calculateTotalWeeks(lifeExpectancy)
        val currentYearOfLife = calculateCurrentYearOfLife(birthDate, referenceDate)
        val currentWeekOfYear = calculateCurrentWeekOfYear(birthDate, referenceDate, precomputedWeeksLived = weeksLived)

        return CalendarMetrics(
            weeksLived = weeksLived,
            totalWeeks = totalWeeks,
            weeksRemaining = (totalWeeks - weeksLived).coerceAtLeast(0),
            currentYearOfLife = currentYearOfLife,
            currentWeekOfYear = currentWeekOfYear,
            lifeExpectancy = lifeExpectancy,
            percentageLived = (weeksLived.toFloat() / totalWeeks * 100).coerceIn(0f, 100f)
        )
    }

    /**
     * Calculates the number of complete weeks lived since birth.
     *
     * Uses [ChronoUnit.WEEKS] for accurate week counting that properly
     * handles leap years and varying month lengths.
     *
     * @param birthDate The user's date of birth
     * @param referenceDate The end date for calculation (typically today)
     * @return Number of complete weeks lived, always >= 0
     */
    fun calculateWeeksLived(
        birthDate: LocalDate,
        referenceDate: LocalDate = LocalDate.now()
    ): Int {
        return ChronoUnit.WEEKS.between(birthDate, referenceDate).toInt().coerceAtLeast(0)
    }

    /**
     * Calculates total weeks in the expected lifetime.
     *
     * @param lifeExpectancy Number of years expected to live
     * @return Total number of weeks (lifeExpectancy * 52)
     */
    fun calculateTotalWeeks(lifeExpectancy: Int = DEFAULT_LIFE_EXPECTANCY): Int {
        return lifeExpectancy * WEEKS_PER_YEAR
    }

    /**
     * Calculates which year of life the user is currently in.
     *
     * Year 1 starts at birth, Year 2 starts on the first birthday, etc.
     * This value corresponds to the row in the calendar grid (1-indexed).
     *
     * @param birthDate The user's date of birth
     * @param referenceDate The current date
     * @return Current year of life (1-indexed), minimum 1
     */
    fun calculateCurrentYearOfLife(
        birthDate: LocalDate,
        referenceDate: LocalDate = LocalDate.now()
    ): Int {
        val years = ChronoUnit.YEARS.between(birthDate, referenceDate).toInt()
        return (years + 1).coerceAtLeast(1)
    }

    /**
     * Calculates the current week within the current year of life.
     *
     * Week 1 starts on the birthday, Week 52 ends just before the next birthday.
     * This value corresponds to the column in the calendar grid (1-indexed).
     *
     * @param birthDate The user's date of birth
     * @param referenceDate The current date
     * @return Current week within the year (1-52)
     */
    fun calculateCurrentWeekOfYear(
        birthDate: LocalDate,
        referenceDate: LocalDate = LocalDate.now(),
        precomputedWeeksLived: Int? = null
    ): Int {
        val totalWeeksLived = precomputedWeeksLived ?: calculateWeeksLived(birthDate, referenceDate)
        val weekInYear = (totalWeeksLived % WEEKS_PER_YEAR) + 1
        return weekInYear.coerceIn(1, WEEKS_PER_YEAR)
    }
}

/**
 * Data class containing all calculated life calendar metrics.
 *
 * This immutable data class holds all the values needed to render
 * the life calendar grid and display statistics to the user.
 *
 * @property weeksLived Number of complete weeks lived since birth
 * @property totalWeeks Total weeks in expected lifetime (lifeExpectancy * 52)
 * @property weeksRemaining Weeks remaining until life expectancy (may be 0 if exceeded)
 * @property currentYearOfLife Which year of life (1-indexed, row in grid)
 * @property currentWeekOfYear Which week within current year (1-52, column in grid)
 * @property lifeExpectancy The life expectancy used for calculation
 * @property percentageLived Percentage of life lived (0-100)
 */
data class CalendarMetrics(
    val weeksLived: Int,
    val totalWeeks: Int,
    val weeksRemaining: Int,
    val currentYearOfLife: Int,
    val currentWeekOfYear: Int,
    val lifeExpectancy: Int,
    val percentageLived: Float
)
