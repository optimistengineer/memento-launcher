package com.betteruniverse.mementolauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
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
            // Fallback to the real system app icon
            val appIcon: Drawable? = remember(app.packageName) {
                try {
                    context.packageManager.getApplicationIcon(app.packageName)
                } catch (_: Exception) {
                    null
                }
            }
            if (appIcon != null) {
                val imageBitmap = remember(appIcon) {
                    appIcon.toBitmap(width = 64, height = 64).asImageBitmap()
                }
                Image(
                    bitmap = imageBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Maps a package name to a custom [DotIconType], or null to fall back to the system icon.
 *
 * Delegates to the shared resolver in DotIcon.kt so the dock, the dock picker and anywhere
 * else that wants a dot glyph all agree on the same mapping.
 */
private fun getIconTypeForPackage(packageName: String): DotIconType? =
    dotIconForPackage(packageName)
