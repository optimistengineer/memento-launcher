package com.betteruniverse.mementolauncher.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards dot-matrix text against overflowing the screen.
 *
 * [DotText] neither wraps, truncates nor — until the accompanying fix — clipped, and its glyph
 * metrics are hand-rolled rather than delegated to a real text layout engine. So a string that is
 * simply too long for its container does not degrade gracefully; it draws straight past its own
 * node. That produced a bug that took a device to spot: the life calendar page's
 * "SET YOUR BIRTH DATE" hint needed 336dp at the default font scale but had ~240dp on a 360dp
 * phone, so it rendered as "SET YOUR BIRTH DAT" — and because a HorizontalPager page's neighbour
 * is composed and sits immediately to its right, the overflowing final "E" painted onto the *home
 * screen*, where it looked like a rendering artifact with no owner.
 *
 * These tests scan the real sources rather than restating a list of strings, so a newly added
 * over-long string fails here instead of on a user's phone.
 */
class DotTextWidthTest {

    /**
     * The narrowest width any fixed-size string must fit in: a 320dp phone — the smallest width
     * still shipping on minSdk-26 hardware — less the app's standard 32dp side padding, with the
     * user's font scale set to LARGE.
     */
    private val budgetDp = 320f - 64f
    private val largeFontScale = 1.2f

    private val uiSources: List<File> by lazy {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, "app/src/main/java/com/betteruniverse/mementolauncher") }
            .firstOrNull { it.isDirectory }
        requireNotNull(root) { "could not locate the main source set from ${File("").absolutePath}" }
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `source tree is actually being scanned`() {
        // Without this, a bad path would make every scanning test below vacuously pass.
        assertTrue("expected to find Kotlin sources to scan", uiSources.size > 20)
        assertTrue(
            "expected to find DotText call sites",
            findFixedSizeCallSites().size > 20
        )
    }

    @Test
    fun `no fixed-size string overflows the narrowest supported screen`() {
        val overflowing = findFixedSizeCallSites().filter { site ->
            val layout = calculateLayout(
                site.text,
                site.dotSize.dp * largeFontScale,
                site.spacing.dp * largeFontScale
            )
            layout.width.value > budgetDp
        }

        assertEquals(
            overflowing.joinToString(
                prefix = "These strings are wider than ${budgetDp}dp at LARGE font scale, so they " +
                    "will be clipped on a 320dp phone. Use AutoScaledDotText instead:\n",
                separator = "\n"
            ) { "  ${it.file}:${it.line} \"${it.text.replace("\n", "\\n")}\"" },
            0,
            overflowing.size
        )
    }

    @Test
    fun `the calendar birth-date hint fits the space it is actually given`() {
        // The regression that started this: the hint sits inside the calendar page's
        // padding(start = 8, end = 16) plus the hint Column's own padding(32) + padding(16),
        // leaving 360 - 24 - 96 = 240dp on a very common phone width.
        val usableDp = 360f - 24f - 96f
        val layout = calculateLayout("SET YOUR\nBIRTH DATE", 3.dp * largeFontScale, 1.dp * largeFontScale)

        assertTrue(
            "birth-date hint needs ${layout.width.value}dp but only has ${usableDp}dp",
            layout.width.value <= usableDp
        )

        // And prove the single-line form this replaced genuinely did not fit, so the two-line
        // split is not cargo-culted — at the *default* scale, not just at LARGE.
        val oneLine = calculateLayout("SET YOUR BIRTH DATE", 3.dp, 1.dp)
        assertTrue(
            "expected the old single-line hint to overflow, but it measured ${oneLine.width.value}dp",
            oneLine.width.value > usableDp
        )
    }

    @Test
    fun `a long app label overflows a phone row and so must be auto-scaled`() {
        // Justifies auto-scaling the home screen favourites and drawer rows: app labels are
        // unbounded user data, and real ones already exceed the space.
        val layout = calculateLayout("GOOGLE PLAY SERVICES", 2.5.dp * largeFontScale, 0.8.dp * largeFontScale)
        assertTrue(
            "expected a long label to exceed ${budgetDp}dp, measured ${layout.width.value}dp",
            layout.width.value > budgetDp
        )
    }

    @Test
    fun `multi-line width is the widest line, not the sum`() {
        val short = calculateLayout("SET YOUR", 3.dp, 1.dp).width.value
        val long = calculateLayout("BIRTH DATE", 3.dp, 1.dp).width.value
        val both = calculateLayout("SET YOUR\nBIRTH DATE", 3.dp, 1.dp).width.value

        assertEquals(maxOf(short, long), both, 0.01f)
        assertTrue("two lines must be taller than one",
            calculateLayout("SET YOUR\nBIRTH DATE", 3.dp, 1.dp).height.value >
                calculateLayout("SET YOUR", 3.dp, 1.dp).height.value)
    }

    // ═══════════════════════════════════════════
    // Source scanning
    // ═══════════════════════════════════════════

    private data class CallSite(
        val file: String,
        val line: Int,
        val text: String,
        val dotSize: Float,
        val spacing: Float
    )

    /**
     * Finds `DotText(text = "literal", ..., dotSize = N.dp, spacing = N.dp)` call sites.
     *
     * Deliberately narrow: it skips [AutoScaledDotText] (which fits by construction) and skips
     * interpolated strings, whose rendered length is not knowable from the source. Those dynamic
     * sites are the reason [AutoScaledDotText] exists; this test only polices the static ones.
     */
    private fun findFixedSizeCallSites(): List<CallSite> {
        val call = Regex("""(?<!AutoScaled)\bDotText\s*\(""")
        val textArg = Regex("""text\s*=\s*"((?:[^"\\]|\\.)*)"""")
        val dotArg = Regex("""dotSize\s*=\s*([0-9.]+)\.dp""")
        val spacingArg = Regex("""spacing\s*=\s*([0-9.]+)\.dp""")

        return uiSources.flatMap { file ->
            val source = file.readText()
            call.findAll(source).mapNotNull { match ->
                val body = balancedArgs(source, match.range.last + 1) ?: return@mapNotNull null
                val raw = textArg.find(body)?.groupValues?.get(1) ?: return@mapNotNull null
                if (raw.contains('$')) return@mapNotNull null
                val dot = dotArg.find(body)?.groupValues?.get(1)?.toFloatOrNull() ?: return@mapNotNull null
                val spacing = spacingArg.find(body)?.groupValues?.get(1)?.toFloatOrNull() ?: return@mapNotNull null
                CallSite(
                    file = file.name,
                    line = source.take(match.range.first).count { it == '\n' } + 1,
                    // Kotlin's own escape for a newline, as it appears in the source text.
                    text = raw.replace("\\n", "\n"),
                    dotSize = dot,
                    spacing = spacing
                )
            }
        }
    }

    /** Returns the argument list starting just after an opening paren, excluding the closing one. */
    private fun balancedArgs(source: String, openIndex: Int): String? {
        var depth = 1
        var i = openIndex
        while (i < source.length && depth > 0) {
            when (source[i]) {
                '(' -> depth++
                ')' -> depth--
            }
            i++
        }
        return if (depth == 0) source.substring(openIndex, i - 1) else null
    }
}
