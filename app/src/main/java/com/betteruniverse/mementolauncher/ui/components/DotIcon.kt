package com.betteruniverse.mementolauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Types of custom icons supported by the DotIcon component.
 */
enum class DotIconType {
    EDIT,
    HEART,
    STAR,
    SETTINGS,
    PHONE,
    MESSAGE,
    GLOBE,
    CALENDAR,
    CAMERA,
    CALCULATOR,
    MAPS,
    PHOTOS,
    // Categories for the wider app ecosystem, so common apps get a glyph in the
    // launcher's own language instead of falling back to their full-colour icon.
    MUSIC,
    CHAT,
    VIDEO,
    MAIL,
    CLOCK,
    FILES,
    STORE,
    WALLET,
    NOTES,
    WEATHER,
    FITNESS,
    CART,
    CAR,
    BOOK,
    GAME,
    SEARCH,
    SOCIAL,
    /** An X, for clearing the search field. */
    CLOSE,
    /**
     * A settings gear sized for app icons. Distinct from [SETTINGS], which is a 25x25 pattern
     * drawn at 0.8.dp for the app drawer's gear button — at the dock's 2.5.dp that pattern would
     * measure 74.5dp inside a 44dp circle and render as a cropped fragment.
     */
    GEAR,
    APP
}

/**
 * Best-effort mapping from a package name to a dot-matrix glyph.
 *
 * Matching is on package-name substrings because it must work across OEM builds and
 * regional variants without shipping a per-app lookup table. Order matters: more specific
 * vendors are checked before the generic category words they contain.
 *
 * Returns null when nothing matches, so callers can fall back to the real app icon.
 */
fun dotIconForPackage(packageName: String): DotIconType? {
    val p = packageName.lowercase()
    return when {
        // Communication
        p.contains("dialer") || p.contains("incallui") || p.contains(".phone") -> DotIconType.PHONE
        p.contains("whatsapp") || p.contains("telegram") || p.contains("signal") ||
            p.contains("messenger") || p.contains("discord") || p.contains("slack") -> DotIconType.CHAT
        p.contains("messaging") || p.contains("mms") || p.contains(".sms") -> DotIconType.MESSAGE
        // "android.gm" catches Gmail, whose package (com.google.android.gm) contains no "mail".
        p.contains("android.gm") || p.contains("gmail") || p.contains("outlook") ||
            p.contains("mail") || p.contains("proton") -> DotIconType.MAIL

        // Media
        p.contains("spotify") || p.contains("music") || p.contains("soundcloud") ||
            p.contains("deezer") || p.contains("audible") || p.contains("podcast") -> DotIconType.MUSIC
        p.contains("youtube") || p.contains("netflix") || p.contains("primevideo") ||
            p.contains("disney") || p.contains("hotstar") || p.contains("video") ||
            p.contains("vlc") || p.contains("twitch") -> DotIconType.VIDEO
        p.contains("camera") -> DotIconType.CAMERA
        p.contains("gallery") || p.contains("photos") -> DotIconType.PHOTOS

        // Social
        p.contains("instagram") || p.contains("facebook") || p.contains("twitter") ||
            p.contains("snapchat") || p.contains("reddit") || p.contains("linkedin") ||
            p.contains("pinterest") || p.contains("threads") || p.contains("musically") ||
            p.contains("tiktok") || p.contains("bsky") -> DotIconType.SOCIAL

        // Utility
        p.contains("chrome") || p.contains("firefox") || p.contains("browser") ||
            p.contains("edge") || p.contains("opera") || p.contains("brave") ||
            p.contains("duckduckgo") -> DotIconType.GLOBE
        p.contains("deskclock") || p.contains("clock") || p.contains("alarm") -> DotIconType.CLOCK
        p.contains("calculator") -> DotIconType.CALCULATOR
        p.contains("calendar") -> DotIconType.CALENDAR
        p.contains("maps") || p.contains("waze") || p.contains("navigation") -> DotIconType.MAPS
        p.contains("documentsui") || p.contains("files") || p.contains("filemanager") ||
            p.contains("drive") || p.contains("dropbox") -> DotIconType.FILES
        p.contains("vending") || p.contains("appstore") || p.contains("galaxystore") ||
            p.contains("fdroid") || p.contains("aurora") -> DotIconType.STORE
        p.contains("keep") || p.contains("notes") || p.contains("notion") ||
            p.contains("obsidian") || p.contains("evernote") -> DotIconType.NOTES
        p.contains("weather") || p.contains("accuweather") -> DotIconType.WEATHER
        p.contains("wallet") || p.contains("gpay") || p.contains("paytm") ||
            p.contains("phonepe") || p.contains("bank") || p.contains("paypal") ||
            p.contains("venmo") || p.contains("revolut") -> DotIconType.WALLET
        p.contains("fit") || p.contains("strava") || p.contains("health") ||
            p.contains("workout") -> DotIconType.FITNESS
        p.contains("amazon") || p.contains("flipkart") || p.contains("ebay") ||
            p.contains("shop") || p.contains("etsy") -> DotIconType.CART
        p.contains("uber") || p.contains("lyft") || p.contains("ola") ||
            p.contains("bolt") || p.contains("rideshare") -> DotIconType.CAR
        p.contains("kindle") || p.contains("book") || p.contains("reader") ||
            p.contains("wattpad") -> DotIconType.BOOK
        p.contains("game") || p.contains("play.games") || p.contains("steam") -> DotIconType.GAME
        p.contains("googlequicksearchbox") || p.contains("search") -> DotIconType.SEARCH
        p.contains("settings") -> DotIconType.GEAR
        else -> null
    }
}

/**
 * A custom icon component that renders shapes using a dot matrix style
 * to match the Memento aesthetic.
 */
/**
 * @param contentDescription Label announced by accessibility services. The icon is drawn as raw
 *   circles on a Canvas, so without this it is invisible to TalkBack. Defaults to a readable form
 *   of [type]; pass null for icons that are purely decorative alongside a labelled sibling.
 */
@Composable
fun DotIcon(
    type: DotIconType,
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.dp,
    spacing: Dp = 1.dp,
    contentDescription: String? = type.name.lowercase().replaceFirstChar { it.uppercase() }
) {
    val fontScale = com.betteruniverse.mementolauncher.ui.components.LocalFontScale.current
    val scaledDotSize = dotSize * fontScale
    val scaledSpacing = spacing * fontScale

    val pattern = remember(type) { getIconPattern(type) }
    val rows = pattern.size
    val cols = if (pattern.isNotEmpty()) pattern[0].length else 0
    
    val width = (scaledDotSize * cols) + (scaledSpacing * (cols - 1))
    val height = (scaledDotSize * rows) + (scaledSpacing * (rows - 1))

    val accessibilityModifier = remember(contentDescription) {
        contentDescription
            ?.takeIf { it.isNotBlank() }
            ?.let { desc -> Modifier.semantics { this.contentDescription = desc } }
            ?: Modifier
    }

    Canvas(modifier = modifier.size(width, height).then(accessibilityModifier)) {
        val dotPx = scaledDotSize.toPx()
        val spacePx = scaledSpacing.toPx()
        val radius = dotPx / 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (pattern[r][c] == 'X') {
                    val x = c * (dotPx + spacePx) + radius
                    val y = r * (dotPx + spacePx) + radius
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

internal fun getIconPattern(type: DotIconType): List<String> {
    return when (type) {
        DotIconType.EDIT -> listOf(
            "....XX.",
            "...X..X",
            "..X..X.",
            ".X..X..",
            "X..X...",
            "XXX....",
            "XX....."
        )
        DotIconType.HEART -> listOf(
            " . X . X . ",
            " X X X X X ",
            " X X X X X ",
            " . X X X . ",
            " . . X . . "
        )
        DotIconType.STAR -> listOf(
            " . . X . . ",
            " . X X X . ",
            " X X X X X ",
            " . X X X . ",
            " X . . . X "
        )
        DotIconType.SETTINGS -> listOf(
            "                         ",
            "           XXX           ",
            "           XXX           ",
            "           XXX           ",
            "     XX    XXX    XX     ",
            "    XXX  XXXXXXX  XXX    ",
            "    XXXXXXXXXXXXXXXXX    ",
            "      XXXXXXXXXXXXX      ",
            "      XXXX     XXXX      ",
            "     XXXX       XXXX     ",
            "     XXX         XXX     ",
            " XXXXXXX         XXXXXXX ",
            " XXXXXXX         XXXXXXX ",
            " XXXXXXX         XXXXXXX ",
            "     XXX         XXX     ",
            "     XXXX       XXXX     ",
            "      XXXX     XXXX      ",
            "      XXXXXXXXXXXXX      ",
            "    XXXXXXXXXXXXXXXXX    ",
            "    XXX  XXXXXXX  XXX    ",
            "     XX    XXX    XX     ",
            "           XXX           ",
            "           XXX           ",
            "           XXX           ",
            "                         "
        )

        // ═══════════════════════════════════════════
        // DOCK ICONS — Crisp designs for small rendering
        // ═══════════════════════════════════════════

        // Phone: classic curved handset
        DotIconType.PHONE -> listOf(
            "XXX......",
            "X.X......",
            "XXX......",
            ".XX......",
            "..XX.....",
            "...XX....",
            "....X.XXX",
            "......X.X",
            "......XXX"
        )

        // Chat bubble with tail
        DotIconType.MESSAGE -> listOf(
            ".XXXXXXX.",
            "XXXXXXXXX",
            "X.XXXXX.X",
            "XXXXXXXXX",
            "X.XXX...X",
            "XXXXXXXXX",
            ".XXXXXXXX",
            "..XX.....",
            "..X......"
        )

        // Camera: body outline with viewfinder bump and circular lens
        DotIconType.CAMERA -> listOf(
            "...XXXX....",
            "XXXXXXXXXXX",
            "X..XXXXX..X",
            "X.XX...XX.X",
            "X.X..X..X.X",
            "X.XX...XX.X",
            "X..XXXXX..X",
            "XXXXXXXXXXX",
            "..........."
        )

        // Calculator: screen on top, button grid below
        DotIconType.CALCULATOR -> listOf(
            ".XXXXXXX.",
            ".X.XXX.X.",
            ".X.XXX.X.",
            ".X.....X.",
            ".X.X.X.X.",
            ".X.....X.",
            ".X.X.X.X.",
            ".X.....X.",
            ".XXXXXXX."
        )

        // Maps: location pin marker
        DotIconType.MAPS -> listOf(
            "...XXX...",
            "..XXXXX..",
            ".XX...XX.",
            ".XX.X.XX.",
            ".XX...XX.",
            "..XXXXX..",
            "...XXX...",
            "....X....",
            "....X...."
        )

        // Photos: sun + mountain landscape in frame
        DotIconType.PHOTOS -> listOf(
            "XXXXXXXXX",
            "X..X....X",
            "X.X.X...X",
            "X..X....X",
            "X.....X.X",
            "X....X.XX",
            "X.X.X.X.X",
            "XX.X..XXX",
            "XXXXXXXXX"
        )

        // 9x9 to match the rest of the app-icon set.
        DotIconType.GLOBE -> listOf(
            "..XXXXX..",
            ".X..X..X.",
            "X...X...X",
            "XXXXXXXXX",
            "X...X...X",
            "XXXXXXXXX",
            "X...X...X",
            ".X..X..X.",
            "..XXXXX.."
        )
        DotIconType.CALENDAR -> listOf(
            "..X...X..",
            "XXXXXXXXX",
            "XXXXXXXXX",
            "X.......X",
            "X.X.X.X.X",
            "X.......X",
            "X.X.X...X",
            "X.......X",
            "XXXXXXXXX"
        )

        // ═══════════════════════════════════════════
        // CATEGORY ICONS — 9x9, matching the dock set above
        // ═══════════════════════════════════════════

        // Music: beamed quaver
        DotIconType.MUSIC -> listOf(
            "......XXX",
            "....XXXXX",
            "....X...X",
            "....X....",
            "....X....",
            "....X....",
            ".XXXX....",
            "XXXXX....",
            ".XXX....."
        )

        // Chat: rounded bubble with typing dots and a tail
        DotIconType.CHAT -> listOf(
            ".XXXXXXX.",
            "XXXXXXXXX",
            "X.......X",
            "X.X.X.X.X",
            "X.......X",
            "XXXXXXXXX",
            ".XXXXXXX.",
            "..XX.....",
            ".XX......"
        )

        // Video: play triangle in a frame
        DotIconType.VIDEO -> listOf(
            "XXXXXXXXX",
            "X.......X",
            "X..X....X",
            "X..XX...X",
            "X..XXX..X",
            "X..XX...X",
            "X..X....X",
            "X.......X",
            "XXXXXXXXX"
        )

        // Mail: envelope with folded flap
        DotIconType.MAIL -> listOf(
            "XXXXXXXXX",
            "X.......X",
            "XX.....XX",
            "X.X...X.X",
            "X..X.X..X",
            "X...X...X",
            "X.......X",
            "X.......X",
            "XXXXXXXXX"
        )

        // Clock: dial with hands
        DotIconType.CLOCK -> listOf(
            "..XXXXX..",
            ".X.....X.",
            "X...X...X",
            "X...X...X",
            "X...XXX.X",
            "X.......X",
            "X.......X",
            ".X.....X.",
            "..XXXXX.."
        )

        // Files: folder with tab
        DotIconType.FILES -> listOf(
            ".........",
            "XXXX.....",
            "X...X....",
            "XXXXXXXXX",
            "X.......X",
            "X.......X",
            "X.......X",
            "X.......X",
            "XXXXXXXXX"
        )

        // Store: play triangle
        DotIconType.STORE -> listOf(
            "..X......",
            "..XX.....",
            "..XXX....",
            "..XXXX...",
            "..XXXXX..",
            "..XXXX...",
            "..XXX....",
            "..XX.....",
            "..X......"
        )

        // Wallet: billfold with clasp
        DotIconType.WALLET -> listOf(
            "XXXXXXXX.",
            "X......X.",
            "X......XX",
            "X....XXXX",
            "X....X..X",
            "X....XXXX",
            "X......XX",
            "X......X.",
            "XXXXXXXX."
        )

        // Notes: lined page
        DotIconType.NOTES -> listOf(
            "XXXXXXXXX",
            "X.......X",
            "X.XXXXX.X",
            "X.......X",
            "X.XXXXX.X",
            "X.......X",
            "X.XXX...X",
            "X.......X",
            "XXXXXXXXX"
        )

        // Weather: cloud
        DotIconType.WEATHER -> listOf(
            ".........",
            "...XXX...",
            "..X...X..",
            ".X.....X.",
            "X.......X",
            "X.......X",
            ".XXXXXXX.",
            ".........",
            "........."
        )

        // Fitness: heartbeat trace
        DotIconType.FITNESS -> listOf(
            ".........",
            ".........",
            "...X.....",
            "...X.X...",
            "XXXX.X.XX",
            ".....X...",
            ".....X...",
            ".........",
            "........."
        )

        // Cart: basket on wheels
        DotIconType.CART -> listOf(
            "X........",
            "X.XXXXXX.",
            "X.X....X.",
            "X.X....X.",
            "X.XXXXXX.",
            "X........",
            "..X...X..",
            ".XXX.XXX.",
            "..X...X.."
        )

        // Car: side profile
        DotIconType.CAR -> listOf(
            ".........",
            "..XXXXX..",
            ".X.....X.",
            "XXXXXXXXX",
            "X.......X",
            "XXXXXXXXX",
            ".X.....X.",
            ".XX...XX.",
            "........."
        )

        // Book: open spread
        DotIconType.BOOK -> listOf(
            "XXXX.XXXX",
            "X..X.X..X",
            "X..X.X..X",
            "X..X.X..X",
            "X..X.X..X",
            "X..X.X..X",
            "X..X.X..X",
            "X..X.X..X",
            "XXXX.XXXX"
        )

        // Game: controller
        DotIconType.GAME -> listOf(
            ".........",
            ".........",
            "XXXXXXXXX",
            "X.X...X.X",
            "XXXX.XXXX",
            "X.X...X.X",
            "XXXXXXXXX",
            ".........",
            "........."
        )

        // Search: magnifier
        // A symmetric X on the same 9x9 grid as the other glyphs, so it sits at the same
        // optical weight as the settings gear beside it in the search bar.
        DotIconType.CLOSE -> listOf(
            "XX.....XX",
            "XX.....XX",
            ".XX...XX.",
            "..XX.XX..",
            "...XXX...",
            "..XX.XX..",
            ".XX...XX.",
            "XX.....XX",
            "XX.....XX"
        )

        DotIconType.SEARCH -> listOf(
            ".XXXX....",
            "X....X...",
            "X....X...",
            "X....X...",
            ".XXXX....",
            "....XX...",
            ".....XX..",
            "......XX.",
            ".......XX"
        )

        // Social: two figures
        DotIconType.SOCIAL -> listOf(
            "..X...X..",
            ".XXX.XXX.",
            "..X...X..",
            ".........",
            "XXXXXXXXX",
            "X.......X",
            "X.......X",
            "X.......X",
            "........."
        )

        // Settings gear, sized for app icons
        DotIconType.GEAR -> listOf(
            "...XXX...",
            ".X.XXX.X.",
            ".XXXXXXX.",
            "XXXX.XXXX",
            "XXX...XXX",
            "XXXX.XXXX",
            ".XXXXXXX.",
            ".X.XXX.X.",
            "...XXX..."
        )

        // Generic app: rounded square, the neutral fallback
        DotIconType.APP -> listOf(
            ".XXXXXXX.",
            "XXXXXXXXX",
            "XX.....XX",
            "XX.....XX",
            "XX.....XX",
            "XX.....XX",
            "XX.....XX",
            "XXXXXXXXX",
            ".XXXXXXX."
        )
    }
}
