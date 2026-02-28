package com.optimistswe.mementolauncher.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.domain.CalendarMetrics
import com.optimistswe.mementolauncher.ui.components.DotText
import com.optimistswe.mementolauncher.ui.components.DotIcon
import com.optimistswe.mementolauncher.ui.components.DotIconType

/**
 * Home screen showing the calendar preview and main actions.
 *
 * Displays:
 * - The generated life calendar as a preview
 * - Statistics about weeks lived/remaining
 * - Actions to set wallpaper or update manually
 *
 * @param metrics Current life calendar metrics (null if loading)
 * @param previewBitmap Generated calendar bitmap for preview
 * @param isLoading Whether the calendar is being generated
 * @param wallpaperSet Whether wallpaper was just set successfully
 * @param onSetWallpaper Callback to set the calendar as wallpaper
 * @param onRefresh Callback to regenerate the calendar
 * @param onSettingsClick Callback to navigate to settings
 */
@Composable
fun HomeScreen(
    metrics: CalendarMetrics?,
    previewBitmap: Bitmap?,
    isLoading: Boolean,
    wallpaperSet: Boolean,
    onSetWallpaper: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 48.dp, bottom = 16.dp),  // Increased top padding for status bar
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotText(
                text = "MEMENTO LAUNCHER",
                color = MaterialTheme.colorScheme.onBackground,
                dotSize = 4.dp,
                spacing = 1.dp
            )

            IconButton(onClick = onSettingsClick) {
                DotIcon(
                    type = DotIconType.SETTINGS,
                    color = MaterialTheme.colorScheme.onBackground,
                    dotSize = 0.8.dp,
                    spacing = 0.4.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        // Calendar Preview
        val configuration = LocalConfiguration.current
        val screenRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()           // Fill the available padded box area
                    .clip(RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black // Blend with the actual wallpaper background
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        previewBitmap != null -> {
                            val imageBitmap = remember(previewBitmap) { previewBitmap.asImageBitmap() }
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Memento Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillWidth // Fill exact width, relying on new margin calculations
                            )
                        }
                        else -> {
                            Text(
                                text = "Preview not available",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp)) // Reduced spacer

        // Statistics
        if (metrics != null) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                StatsCard(metrics)
            }
            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacer
        }

        // Action Buttons
        Button(
            onClick = onSetWallpaper,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = previewBitmap != null && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (wallpaperSet)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.onBackground,  // White background
                contentColor = MaterialTheme.colorScheme.background  // Black text
            )
        ) {
            if (wallpaperSet) {
                DotText(
                    text = "WALLPAPER SET!",
                    color = MaterialTheme.colorScheme.background,
                    dotSize = 2.dp,
                    spacing = 1.dp
                )
            } else {
                DotText(
                    text = "SET AS WALLPAPER",
                    color = MaterialTheme.colorScheme.background,
                    dotSize = 2.dp,
                    spacing = 1.dp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp)) // Small bottom spacer
    }
}

/**
 * Card displaying life calendar statistics.
 *
 * @param metrics The calculated life calendar metrics
 */
@Composable
private fun StatsCard(metrics: CalendarMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = metrics.weeksLived.toString(),
                label = "Weeks\nLived"
            )
            StatItem(
                value = metrics.weeksRemaining.toString(),
                label = "Weeks\nRemaining"
            )
            StatItem(
                value = "%.1f%%".format(metrics.percentageLived),
                label = "Life\nProgress"
            )
        }
    }
}

/**
 * Individual statistic display item.
 *
 * @param value The numeric value to display
 * @param label Description of the statistic
 */
@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DotText(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            dotSize = 3.dp,
            spacing = 1.dp
        )
        DotText(
            text = label.uppercase().replace("\n", " "), // Keep on single line
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            dotSize = 1.5.dp,
            spacing = 0.5.dp,
            alignment = Alignment.CenterHorizontally
        )
    }
}
