package com.optimistswe.mementolauncher.ui.components.settings

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
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.ui.components.DotText

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
                    dotSize = 1.dp,
                    spacing = 0.4.dp
                )
                DotText(
                    text = "SETTINGS AS JSON",
                    color = dimmed.copy(alpha = 0.6f),
                    dotSize = 1.dp,
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
                            dotSize = 1.dp,
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
                            dotSize = 1.dp,
                            spacing = 0.5.dp
                        )
                    }
                }

                if (statusText.isNotEmpty()) {
                    DotText(
                        text = statusText,
                        color = dimmed,
                        dotSize = 1.dp,
                        spacing = 0.4.dp
                    )
                }
            }
        }
    }
}
