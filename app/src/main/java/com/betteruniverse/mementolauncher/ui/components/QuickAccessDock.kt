package com.betteruniverse.mementolauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.betteruniverse.mementolauncher.data.AppInfo

/**
 * A single circular dock icon for the quick access corners.
 *
 * Uses custom dot-matrix icons for recognized apps (Phone, Camera, Messages,
 * Calculator, Maps, Photos) and falls back to the real system icon for others.
 */
@Composable
fun DockCornerIcon(
    app: AppInfo,
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background

    val customIconType = remember(app.packageName) { getIconTypeForPackage(app.packageName) }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(onBg.copy(alpha = 0.12f))
            .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLaunchApp(app.packageName)
            },
        contentAlignment = Alignment.Center
    ) {
        if (customIconType != null) {
            // Use our custom dot-matrix icon
            DotIcon(
                type = customIconType,
                color = onBg,
                dotSize = 2.5.dp,
                spacing = 0.5.dp
            )
        } else {
            // Monogram, not the system icon.
            //
            // Twenty-six category glyphs cover every app people actually dock — phone, camera,
            // messages, browser, maps, music, mail, wallet and so on all resolve — but they
            // cover only ~29% of a real 222-app device, and the tail (food delivery, banking,
            // airline, local apps) cannot be hand-drawn: there is no finite set to draw.
            // Dropping a full-colour launcher icon into a monochrome dot-matrix dock was the
            // single loudest thing on the home screen. The app's initial in the launcher's own
            // font identifies it just as well at 44dp, needs no per-app work, and covers 100%.
            val initial = remember(app.label) {
                app.label.uppercase().firstOrNull { it in 'A'..'Z' || it in '0'..'9' }?.toString()
            }
            if (initial != null) {
                DotText(
                    text = initial,
                    color = onBg,
                    dotSize = 2.5.dp,
                    spacing = 0.8.dp
                )
            } else {
                // No Latin letter or digit in the name at all (a fully non-Latin label): the
                // dot font has no glyph for it, so show the neutral app mark rather than a
                // placeholder blob.
                DotIcon(
                    type = DotIconType.STORE,
                    color = onBg,
                    dotSize = 2.5.dp,
                    spacing = 0.5.dp
                )
            }
        }
    }
}

/**
 * Maps a package name to a custom [DotIconType], or null to fall back to a monogram.
 *
 * Delegates to the shared resolver in DotIcon.kt so the dock, the dock picker and anywhere
 * else that wants a dot glyph all agree on the same mapping.
 */
private fun getIconTypeForPackage(packageName: String): DotIconType? =
    dotIconForPackage(packageName)
