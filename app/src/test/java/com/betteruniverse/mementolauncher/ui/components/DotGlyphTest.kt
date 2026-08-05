package com.betteruniverse.mementolauncher.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural guards on the dot-matrix glyph tables.
 *
 * These are pure data, so they are cheap to check exhaustively — and two real bugs in this area
 * were only visible on a device: digits that were a row taller than letters, and an icon 2.8x the
 * size of its neighbours reaching the dock.
 */
class DotGlyphTest {

    // ═══════════════════════════════════════════
    // Text glyphs
    // ═══════════════════════════════════════════

    private val textChars: List<Char> =
        ('A'..'Z') + ('0'..'9') + listOf('.', '%', '!', '*', '<', '>', '[', ']',
            '(', ')', '+', '-', ':', '/', '?', ' ')

    @Test
    fun `every text glyph has rows of equal length`() {
        // A ragged glyph indexes pattern[r][c] out of bounds while drawing.
        textChars.forEach { ch ->
            val rows = getPattern(ch)
            assertTrue("'$ch' has no rows", rows.isNotEmpty())
            val widths = rows.map { it.length }.distinct()
            assertEquals("'$ch' has ragged rows: $widths", 1, widths.size)
        }
    }

    @Test
    fun `every text glyph is five rows tall`() {
        // Mixed heights are laid out from the top of the line, so any outlier visibly sits
        // above or below its neighbours. Digits used to be 6 rows against letters' 5.
        textChars.forEach { ch ->
            assertEquals("'$ch' breaks the uniform cap height", 5, getPattern(ch).size)
        }
    }

    @Test
    fun `an unmapped character falls back to a five row placeholder`() {
        val rows = getPattern('−')  // U+2212 MINUS SIGN — deliberately not in the table
        assertEquals(5, rows.size)
        assertEquals(1, rows.map { it.length }.distinct().size)
    }

    // ═══════════════════════════════════════════
    // Icon glyphs
    // ═══════════════════════════════════════════

    @Test
    fun `every icon glyph has rows of equal length`() {
        DotIconType.entries.forEach { type ->
            val rows = getIconPattern(type)
            assertTrue("$type has no rows", rows.isNotEmpty())
            val widths = rows.map { it.length }.distinct()
            assertEquals("$type has ragged rows: $widths", 1, widths.size)
        }
    }

    @Test
    fun `every icon the package resolver can return is nine rows tall`() {
        // DotIcon derives its absolute size from pattern dimensions x dotSize, so a taller
        // pattern overflows its container. SETTINGS is 25x25 and drawn at 0.8.dp for the drawer's
        // gear button; at the dock's 2.5.dp it would measure 74.5dp inside a 44dp circle.
        val reachable = probePackages.mapNotNull { dotIconForPackage(it) }.distinct()
        assertTrue("resolver matched nothing — the probe list is wrong", reachable.size > 10)
        reachable.forEach { type ->
            assertEquals(
                "$type is reachable from dotIconForPackage but is not 9 rows tall",
                9,
                getIconPattern(type).size
            )
        }
    }

    @Test
    fun `well known packages resolve to a sensible glyph`() {
        assertEquals(DotIconType.MAIL, dotIconForPackage("com.google.android.gm"))
        assertEquals(DotIconType.PHONE, dotIconForPackage("com.google.android.dialer"))
        assertEquals(DotIconType.MESSAGE, dotIconForPackage("com.google.android.apps.messaging"))
        assertEquals(DotIconType.CAMERA, dotIconForPackage("com.android.camera2"))
        assertEquals(DotIconType.GLOBE, dotIconForPackage("com.android.chrome"))
        assertEquals(DotIconType.CLOCK, dotIconForPackage("com.google.android.deskclock"))
        assertEquals(DotIconType.STORE, dotIconForPackage("com.android.vending"))
        assertEquals(DotIconType.SOCIAL, dotIconForPackage("com.instagram.android"))
        assertEquals(DotIconType.MUSIC, dotIconForPackage("com.spotify.music"))
        assertEquals(DotIconType.VIDEO, dotIconForPackage("com.google.android.youtube"))
        assertEquals(DotIconType.CHAT, dotIconForPackage("com.whatsapp"))
        assertEquals(DotIconType.GEAR, dotIconForPackage("com.android.settings"))
    }

    @Test
    fun `an unknown package resolves to null so the caller can fall back`() {
        assertNull(dotIconForPackage("com.some.unknown.thing"))
    }

    @Test
    fun `resolving is case insensitive`() {
        assertNotNull(dotIconForPackage("COM.ANDROID.CHROME"))
    }

    private val probePackages = listOf(
        "com.google.android.dialer", "com.whatsapp", "com.google.android.apps.messaging",
        "com.google.android.gm", "com.spotify.music", "com.google.android.youtube",
        "com.android.camera2", "com.google.android.apps.photos", "com.instagram.android",
        "com.android.chrome", "com.google.android.deskclock", "com.google.android.calculator",
        "com.google.android.calendar", "com.google.android.apps.maps",
        "com.android.documentsui", "com.android.vending", "com.google.android.keep",
        "com.weather.app", "com.google.android.apps.wallet", "com.google.android.apps.fitness",
        "com.amazon.mShop", "com.ubercab", "com.amazon.kindle", "com.valvesoftware.steam",
        "com.google.android.googlequicksearchbox", "com.android.settings"
    )
}
