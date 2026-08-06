package com.betteruniverse.mementolauncher.ui.components.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.betteruniverse.mementolauncher.ui.components.DotText

@Composable
fun DataSettings(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    statusText: String,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        SettingsSectionHeader(
            title = "DATA",
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
                    text = "EXPORT OR IMPORT ALL",
                    color = dimmed.copy(alpha = 0.6f),
                    dotSize = 1.3.dp,
                    spacing = 0.4.dp
                )
                DotText(
                    text = "SETTINGS AS JSON",
                    color = dimmed.copy(alpha = 0.6f),
                    dotSize = 1.3.dp,
                    spacing = 0.4.dp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(onBg, RoundedCornerShape(12.dp))
                            .clickable { onBackup() },
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(
                            text = "BACKUP",
                            color = bg,
                            dotSize = 1.3.dp,
                            spacing = 0.5.dp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(onBg, RoundedCornerShape(12.dp))
                            .clickable { onRestore() },
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(
                            text = "RESTORE",
                            color = bg,
                            dotSize = 1.3.dp,
                            spacing = 0.5.dp
                        )
                    }
                }

                if (statusText.isNotEmpty()) {
                    DotText(
                        text = statusText,
                        color = dimmed,
                        dotSize = 1.3.dp,
                        spacing = 0.4.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Google Play's User Data policy requires the privacy policy to be reachable
                // from inside the app, not only from the store listing.
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable {
                            // Guarded: a device with no browser (or a restricted profile) throws
                            // ActivityNotFoundException, which must not crash the HOME app.
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                                )
                            }
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    DotText(
                        text = "PRIVACY POLICY",
                        color = dimmed,
                        dotSize = 1.5.dp,
                        spacing = 0.5.dp
                    )
                }
            }
        }
    }
}

/**
 * Public, stable home of the privacy policy. Served by GitHub Pages from the repository's
 * docs/ directory; the same URL goes in the Play Console App content section, so the two can
 * never drift apart.
 */
internal const val PRIVACY_POLICY_URL =
    "https://optimistengineer.github.io/memento-launcher/privacy-policy.html"
