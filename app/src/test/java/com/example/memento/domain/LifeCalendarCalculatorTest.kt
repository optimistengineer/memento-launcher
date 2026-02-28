package com.optimistswe.mementolauncher.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Exhaustive unit tests for [LifeCalendarCalculator].
 *
 * Covers week calculation, year-of-life, week-of-year, full metrics,
 * boundary conditions, leap years, and edge cases.
 */
class LifeCalendarCalculatorTest {

    private lateinit var calculator: LifeCalendarCalculator

    @Before
    fun setup() {
        calculator = LifeCalendarCalculator()
    }

    // ═══════════════════════════════════════════
    // calculateWeeksLived
    // ═══════════════════════════════════════════

    @Test
    fun `calculateWeeksLived returns 0 for same day`() {
        val date = LocalDate.of(2000, 1, 1)
        assertEquals(0, calculator.calculateWeeksLived(date, date))
    }

    @Test
    fun `calculateWeeksLived returns 0 for less than 7 days`() {
        val birth = LocalDate.of(2000, 1, 1)
        val ref = LocalDate.of(2000, 1, 6)
        assertEquals(0, calculator.calculateWeeksLived(birth, ref))
    }

    @Test
    fun `calculateWeeksLived returns 1 for exactly 7 days`() {
        val birth = LocalDate.of(2000, 1, 1)
        val ref = LocalDate.of(2000, 1, 8)
        assertEquals(1, calculator.calculateWeeksLived(birth, ref))
    }

    @Test
    fun `calculateWeeksLived returns 52 for exactly one year (non-leap)`() {
        val birth = LocalDate.of(2001, 1, 1)
        val ref = LocalDate.of(2002, 1, 1)
        assertEquals(52, calculator.calculateWeeksLived(birth, ref))
    }

    @Test
    fun `calculateWeeksLived handles leap year correctly`() {
        val birth = LocalDate.of(2000, 1, 1) // 2000 is a leap year
        val ref = LocalDate.of(2001, 1, 1) // 366 days = 52 weeks + 2 days
        assertEquals(52, calculator.calculateWeeksLived(birth, ref))
    }

    @Test
    fun `calculateWeeksLived for 30 year old`() {
        val birth = LocalDate.of(1994, 6, 15)
        val ref = LocalDate.of(2024, 6, 15)
        val weeks = calculator.calculateWeeksLived(birth, ref)
        // ~30 years * 52 weeks = ~1566 (varies with leap years)
        assertTrue(weeks in 1560..1570)
    }

    @Test
    fun `calculateWeeksLived uses today when no reference date provided`() {
        val birth = LocalDate.of(2000, 1, 1)
        val weeks = calculator.calculateWeeksLived(birth)
        assertTrue(weeks > 0)
    }

    @Test
    fun `calculateWeeksLived coerces negative to 0 for future date`() {
        // birthDate after referenceDate should still return >= 0
        val birth = LocalDate.of(2025, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        assertEquals(0, calculator.calculateWeeksLived(birth, ref))
    }

    // ═══════════════════════════════════════════
    // calculateTotalWeeks
    // ═══════════════════════════════════════════

    @Test
    fun `calculateTotalWeeks with default life expectancy`() {
        assertEquals(
            LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY * 52,
            calculator.calculateTotalWeeks()
        )
    }

    @Test
    fun `calculateTotalWeeks with custom life expectancy`() {
        assertEquals(90 * 52, calculator.calculateTotalWeeks(90))
    }

    @Test
    fun `calculateTotalWeeks with minimum life expectancy`() {
        assertEquals(
            LifeCalendarCalculator.MIN_LIFE_EXPECTANCY * 52,
            calculator.calculateTotalWeeks(LifeCalendarCalculator.MIN_LIFE_EXPECTANCY)
        )
    }

    @Test
    fun `calculateTotalWeeks with maximum life expectancy`() {
        assertEquals(
            LifeCalendarCalculator.MAX_LIFE_EXPECTANCY * 52,
            calculator.calculateTotalWeeks(LifeCalendarCalculator.MAX_LIFE_EXPECTANCY)
        )
    }

    // ═══════════════════════════════════════════
    // calculateCurrentYearOfLife
    // ═══════════════════════════════════════════

    @Test
    fun `calculateCurrentYearOfLife returns 1 on birth day`() {
        val date = LocalDate.of(2000, 6, 15)
        assertEquals(1, calculator.calculateCurrentYearOfLife(date, date))
    }

    @Test
    fun `calculateCurrentYearOfLife returns 1 during first year`() {
        val birth = LocalDate.of(2000, 6, 15)
        val ref = LocalDate.of(2001, 3, 1)
        assertEquals(1, calculator.calculateCurrentYearOfLife(birth, ref))
    }

    @Test
    fun `calculateCurrentYearOfLife returns 2 on first birthday`() {
        val birth = LocalDate.of(2000, 6, 15)
        val ref = LocalDate.of(2001, 6, 15)
        assertEquals(2, calculator.calculateCurrentYearOfLife(birth, ref))
    }

    @Test
    fun `calculateCurrentYearOfLife returns 31 for 30 year old`() {
        val birth = LocalDate.of(1994, 1, 1)
        val ref = LocalDate.of(2024, 6, 15)
        assertEquals(31, calculator.calculateCurrentYearOfLife(birth, ref))
    }

    @Test
    fun `calculateCurrentYearOfLife minimum is 1`() {
        val date = LocalDate.of(2000, 1, 1)
        assertTrue(calculator.calculateCurrentYearOfLife(date, date) >= 1)
    }

    // ═══════════════════════════════════════════
    // calculateCurrentWeekOfYear
    // ═══════════════════════════════════════════

    @Test
    fun `calculateCurrentWeekOfYear returns 1 on birth day`() {
        val date = LocalDate.of(2000, 1, 1)
        assertEquals(1, calculator.calculateCurrentWeekOfYear(date, date))
    }

    @Test
    fun `calculateCurrentWeekOfYear returns 2 after one week`() {
        val birth = LocalDate.of(2000, 1, 1)
        val ref = LocalDate.of(2000, 1, 8)
        assertEquals(2, calculator.calculateCurrentWeekOfYear(birth, ref))
    }

    @Test
    fun `calculateCurrentWeekOfYear wraps back to 1 after 52 weeks`() {
        val birth = LocalDate.of(2000, 1, 1)
        val ref = LocalDate.of(2001, 1, 1) // 52 weeks later
        assertEquals(1, calculator.calculateCurrentWeekOfYear(birth, ref))
    }

    @Test
    fun `calculateCurrentWeekOfYear is between 1 and 52`() {
        val birth = LocalDate.of(1990, 5, 20)
        for (daysOffset in 0L..3650L step 7) {
            val ref = birth.plusDays(daysOffset)
            val week = calculator.calculateCurrentWeekOfYear(birth, ref)
            assertTrue("Week $week out of range for offset $daysOffset", week in 1..52)
        }
    }

    @Test
    fun `calculateCurrentWeekOfYear uses precomputed weeks lived`() {
        val birth = LocalDate.of(2000, 1, 1)
        val ref = LocalDate.of(2000, 2, 12) // 6 weeks later
        val precomputed = calculator.calculateWeeksLived(birth, ref)
        val withPrecomputed = calculator.calculateCurrentWeekOfYear(birth, ref, precomputedWeeksLived = precomputed)
        val withoutPrecomputed = calculator.calculateCurrentWeekOfYear(birth, ref)
        assertEquals(withoutPrecomputed, withPrecomputed)
    }

    // ═══════════════════════════════════════════
    // calculateMetrics
    // ═══════════════════════════════════════════

    @Test
    fun `calculateMetrics returns valid result for typical user`() {
        val birth = LocalDate.of(1990, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        val metrics = calculator.calculateMetrics(birth, lifeExpectancy = 80, referenceDate = ref)

        assertEquals(80, metrics.lifeExpectancy)
        assertEquals(80 * 52, metrics.totalWeeks)
        assertTrue(metrics.weeksLived > 0)
        assertTrue(metrics.weeksRemaining > 0)
        assertEquals(metrics.totalWeeks, metrics.weeksLived + metrics.weeksRemaining)
        assertTrue(metrics.percentageLived in 0f..100f)
        assertTrue(metrics.currentYearOfLife >= 1)
        assertTrue(metrics.currentWeekOfYear in 1..52)
    }

    @Test
    fun `calculateMetrics weeksRemaining is 0 when exceeded life expectancy`() {
        val birth = LocalDate.of(1900, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        val metrics = calculator.calculateMetrics(birth, lifeExpectancy = 80, referenceDate = ref)

        assertEquals(0, metrics.weeksRemaining)
        assertEquals(100f, metrics.percentageLived, 0.01f)
    }

    @Test
    fun `calculateMetrics percentage is clamped to 0-100`() {
        // User well past life expectancy
        val birth = LocalDate.of(1900, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        val metrics = calculator.calculateMetrics(birth, lifeExpectancy = 50, referenceDate = ref)

        assertTrue(metrics.percentageLived <= 100f)
        assertTrue(metrics.percentageLived >= 0f)
    }

    @Test
    fun `calculateMetrics with default life expectancy`() {
        val birth = LocalDate.of(2000, 6, 1)
        val ref = LocalDate.of(2024, 6, 1)
        val metrics = calculator.calculateMetrics(birth, referenceDate = ref)

        assertEquals(LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY, metrics.lifeExpectancy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateMetrics throws for future birth date`() {
        val birth = LocalDate.of(2030, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        calculator.calculateMetrics(birth, referenceDate = ref)
    }

    @Test
    fun `calculateMetrics for newborn`() {
        val birth = LocalDate.of(2024, 1, 1)
        val ref = LocalDate.of(2024, 1, 1)
        val metrics = calculator.calculateMetrics(birth, referenceDate = ref)

        assertEquals(0, metrics.weeksLived)
        assertEquals(80 * 52, metrics.weeksRemaining)
        assertEquals(0f, metrics.percentageLived, 0.01f)
        assertEquals(1, metrics.currentYearOfLife)
        assertEquals(1, metrics.currentWeekOfYear)
    }

    @Test
    fun `calculateMetrics consistency - weeksLived plus weeksRemaining le totalWeeks`() {
        val birth = LocalDate.of(1995, 8, 25)
        val ref = LocalDate.of(2024, 3, 15)
        val metrics = calculator.calculateMetrics(birth, lifeExpectancy = 85, referenceDate = ref)

        assertTrue(metrics.weeksLived + metrics.weeksRemaining <= metrics.totalWeeks)
    }

    // ═══════════════════════════════════════════
    // Companion Object Constants
    // ═══════════════════════════════════════════

    @Test
    fun `WEEKS_PER_YEAR is 52`() {
        assertEquals(52, LifeCalendarCalculator.WEEKS_PER_YEAR)
    }

    @Test
    fun `DEFAULT_LIFE_EXPECTANCY is 80`() {
        assertEquals(80, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
    }

    @Test
    fun `MIN_LIFE_EXPECTANCY is 50`() {
        assertEquals(50, LifeCalendarCalculator.MIN_LIFE_EXPECTANCY)
    }

    @Test
    fun `MAX_LIFE_EXPECTANCY is 120`() {
        assertEquals(120, LifeCalendarCalculator.MAX_LIFE_EXPECTANCY)
    }
}
