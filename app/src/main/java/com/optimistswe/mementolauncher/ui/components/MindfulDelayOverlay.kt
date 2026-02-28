package com.optimistswe.mementolauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A full-screen overlay that introduces friction before launching a distracting app.
 *
 * It displays a mindful message and a 5-second countdown using the dot-matrix font.
 * The user can either cancel the launch immediately or proceed once the countdown finishes.
 *
 * @param appName The name of the app the user is trying to open (optional, for context).
 * @param onProceed Callback invoked when the user chooses to continue launching the app.
 * @param onCancel Callback invoked to abort the launch and close the overlay.
 */
@Composable
fun MindfulDelayOverlay(
    message: String,
    appName: String? = null,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val dimmed = onBg.copy(alpha = 0.5f)

    var remainingSeconds by remember { mutableIntStateOf(5) }
    var countdownFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
        countdownFinished = true
    }

    val displayMessage = remember(message) {
        val words = message.uppercase().trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""
        val maxCharsPerLine = 12 // Slightly tighter for better vertical stacking
        
        for (i in words.indices) {
            val word = words[i]
            val nextWord = if (i + 1 < words.size) words[i+1] else null
            
            // Heuristic: if next word is just a question mark, keep it with this line
            val forceWrap = currentLine.isNotEmpty() && (currentLine.length + word.length + 1 > maxCharsPerLine)
            
            if (forceWrap) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        lines.joinToString("\n")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)
        ) {
            // Optical top balance
            Spacer(modifier = Modifier.weight(1.2f))

            DotText(
                text = displayMessage,
                color = onBg,
                dotSize = 3.5.dp, // Slightly larger for emphasis
                spacing = 1.2.dp,
                alignment = Alignment.CenterHorizontally
            )

            if (appName != null) {
                Spacer(modifier = Modifier.height(24.dp))
                DotText(
                    text = "OPENING ${appName.uppercase()}",
                    color = dimmed,
                    dotSize = 1.2.dp,
                    spacing = 0.5.dp
                )
            }

            // Central gap
            Spacer(modifier = Modifier.weight(0.8f))

            // The countdown / Actions container
            Box(
                modifier = Modifier.height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!countdownFinished) {
                    DotText(
                        text = remainingSeconds.toString(),
                        color = onBg,
                        dotSize = 6.dp,
                        spacing = 1.8.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable { onCancel() }
                                .padding(16.dp)
                        ) {
                            DotText(
                                text = "CANCEL",
                                color = dimmed,
                                dotSize = 2.dp,
                                spacing = 0.7.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .clickable { onProceed() }
                                .padding(16.dp)
                        ) {
                            DotText(
                                text = "PROCEED",
                                color = onBg,
                                dotSize = 2.dp,
                                spacing = 0.7.dp
                            )
                        }
                    }
                }
            }

            // Bottom gap / Optical balance
            Spacer(modifier = Modifier.weight(1f))
            
            // Allow cancelling even during countdown
            if (!countdownFinished) {
                Box(
                    modifier = Modifier
                        .clickable { onCancel() }
                        .padding(16.dp)
                ) {
                    DotText(
                        text = "CANCEL NOW",
                        color = dimmed,
                        dotSize = 1.5.dp,
                        spacing = 0.5.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
