package com.betteruniverse.mementolauncher.ui.components

/**
 * The sky for one minute of the day: how visible the stars are, and the two colours of the
 * vertical gradient behind everything (zenith at the top of the screen, horizon at the bottom).
 *
 * This is a PURE FUNCTION of the clock. That is the whole design: no animation ever runs, the
 * background simply looks different each time the (already lifecycle-gated, ~30s) clock tick
 * recomposes it, which makes the effect impossible to distinguish from a live one at sky speeds
 * — real dusk takes half an hour — while costing zero frames and having no state to corrupt.
 * Everything here is deterministic and unit-tested, including a ceiling on how bright the sky
 * may get, because the launcher's white-on-black text contrast must survive high noon.
 */
data class DaylightSpec(
    /** 0 = invisible, 1 = full night sky. */
    val starAlpha: Float,
    /** Sky colour at the top of the screen, packed ARGB. */
    val zenithArgb: Long,
    /** Sky colour at the bottom of the screen, packed ARGB. */
    val horizonArgb: Long
)

object DaylightModel {

    /**
     * No colour channel may exceed this, ever. 0x40 on black keeps white dot-matrix text above
     * 7:1 contrast; a test pins the whole 24h curve under it so a future keyframe edit cannot
     * quietly wash the launcher out at noon.
     */
    const val MAX_CHANNEL = 0x40

    /**
     * Keyframes as (minute-of-day, starAlpha, zenith, horizon). Linear interpolation between
     * neighbours, wrapping across midnight. Times use a fixed temperate day (dawn ~06:00,
     * dusk ~19:00) on purpose: real sunrise times would need the location permission, which
     * this app does not have and will not add for a wallpaper effect.
     */
    private data class Key(val minute: Int, val stars: Float, val zenith: Long, val horizon: Long)

    private val KEYS = listOf(
        Key(0 * 60, 1.00f, 0xFF000000, 0xFF04050A),        // deep night: black, hint of blue floor
        Key(4 * 60 + 30, 1.00f, 0xFF000000, 0xFF04050A),   // night holds until pre-dawn
        Key(5 * 60 + 30, 0.75f, 0xFF03040A, 0xFF140D08),   // first light: stars start to go
        Key(6 * 60 + 30, 0.30f, 0xFF070A12, 0xFF2A1608),   // dawn: warm horizon glow
        Key(8 * 60, 0.00f, 0xFF0C1018, 0xFF1A2028),        // morning: cool, even light
        Key(13 * 60, 0.00f, 0xFF121A26, 0xFF20293A),       // midday: brightest the sky gets
        Key(17 * 60, 0.00f, 0xFF0E1420, 0xFF241C14),       // late afternoon: light warms
        Key(19 * 60, 0.15f, 0xFF0A0A16, 0xFF321409),       // sunset: deep orange floor, first stars
        Key(20 * 60 + 30, 0.70f, 0xFF020308, 0xFF0C0810),  // dusk: glow dies, stars fill in
        Key(22 * 60, 1.00f, 0xFF000000, 0xFF04050A)        // night again
    )

    private const val DAY = 24 * 60

    fun specFor(minuteOfDay: Int): DaylightSpec {
        val m = ((minuteOfDay % DAY) + DAY) % DAY
        // Find the surrounding keyframes, wrapping midnight in both directions.
        var before = KEYS.last()
        var after = KEYS.first()
        var beforeMin = before.minute - DAY
        var afterMin = after.minute + DAY
        for (k in KEYS) {
            if (k.minute <= m && k.minute >= beforeMin) { before = k; beforeMin = k.minute }
            if (k.minute >= m && k.minute < afterMin) { after = k; afterMin = k.minute }
        }
        if (beforeMin == afterMin) {
            return DaylightSpec(before.stars, before.zenith, before.horizon)
        }
        val t = (m - beforeMin).toFloat() / (afterMin - beforeMin).toFloat()
        return DaylightSpec(
            starAlpha = before.stars + (after.stars - before.stars) * t,
            zenithArgb = lerpArgb(before.zenith, after.zenith, t),
            horizonArgb = lerpArgb(before.horizon, after.horizon, t)
        )
    }

    private fun lerpArgb(a: Long, b: Long, t: Float): Long {
        fun ch(shift: Int): Long {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return (ca + ((cb - ca) * t).toLong()).coerceIn(0, 255)
        }
        return (0xFFL shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
