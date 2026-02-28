package com.optimistswe.mementolauncher.ui.components.settings

import android.app.WallpaperManager
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.ui.components.DotText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Settings section providing a shortcut for wallpaper management.
 *
 * Memento is designed for black-and-white minimalist screens. This component includes
 * a one-click button to set the system wallpaper to pure black, ensuring the best
 * visual experience and battery efficiency on OLED devices.
 */
@Composable
fun WallpaperSettings(
    onBg: Color,
    bg: Color,
    dimmed: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var wallpaperApplied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column {
        SettingsSectionHeader(
            title = "WALLPAPER",
            expanded = expanded,
            onToggle = { expanded = !expanded },
            onBg = onBg,
            dimmed = dimmed
        )

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DotText(
                    text = "TIP: BLACK WALLPAPER GIVES",
                    color = dimmed.copy(alpha = 0.6f),
                    dotSize = 1.dp,
                    spacing = 0.4.dp
                )
                DotText(
                    text = "THE BEST EXPERIENCE",
                    color = dimmed.copy(alpha = 0.6f),
                    dotSize = 1.dp,
                    spacing = 0.4.dp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            if (wallpaperApplied) dimmed.copy(alpha = 0.1f) else onBg,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (!wallpaperApplied) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val wallpaperManager = WallpaperManager.getInstance(context)
                                        val blackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                        blackBitmap.setPixel(0, 0, android.graphics.Color.BLACK)
                                        wallpaperManager.setBitmap(blackBitmap)
                                        blackBitmap.recycle()
                                        wallpaperApplied = true
                                    } catch (_: Exception) { }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = if (wallpaperApplied) "APPLIED" else "APPLY BLACK WALLPAPER",
                        color = if (wallpaperApplied) dimmed else bg,
                        dotSize = 1.dp,
                        spacing = 0.5.dp
                    )
                }
            }
        }
    }
}
