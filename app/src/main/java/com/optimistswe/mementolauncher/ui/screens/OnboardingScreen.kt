package com.optimistswe.mementolauncher.ui.screens

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.ui.components.AutoScaledDotText
import com.optimistswe.mementolauncher.ui.components.DotText
import com.optimistswe.mementolauncher.ui.components.DottedDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * Onboarding screen for new users.
 *
 * Flow:
 * - Step 0: Prompt user to set a black wallpaper for the best experience.
 * - Step 1: Collect birth date and life expectancy (core feature setup).
 * - Step 2: Explain mindful delay feature.
 * - Step 3: Prompt user to set Memento as their default home app.
 *
 * @param onComplete Callback invoked when user completes setup.
 */
@Composable
fun OnboardingScreen(
    onComplete: (birthDate: LocalDate?, lifeExpectancy: Int) -> Unit
) {
    // 0 = wallpaper nudge, 1 = default launcher
    // Birth date is set later by swiping left to the calendar screen.
    var step by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val dimmed = onBg.copy(alpha = 0.4f)

    if (step == 0) {
        // ═══════════════════════════════════════════
        // STEP 0: Wallpaper Nudge
        // ═══════════════════════════════════════════
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var wallpaperApplied by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DotText(
                text = "WELCOME",
                color = onBg,
                dotSize = 3.dp,
                spacing = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            DotText(
                text = "TO MEMENTO LAUNCHER",
                color = onBg,
                dotSize = 3.dp,
                spacing = 1.dp
            )

            Spacer(modifier = Modifier.height(32.dp))

            DotText(
                text = "FOR THE BEST EXPERIENCE",
                color = dimmed,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            DotText(
                text = "SET YOUR WALLPAPER TO BLACK",
                color = dimmed,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Apply button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (wallpaperApplied) dimmed.copy(alpha = 0.15f) else onBg,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !wallpaperApplied) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val wallpaperManager = WallpaperManager.getInstance(context)
                                val blackBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                blackBitmap.setPixel(0, 0, android.graphics.Color.BLACK)
                                wallpaperManager.setBitmap(blackBitmap)
                                blackBitmap.recycle()
                                
                                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    wallpaperApplied = true
                                    step = 1
                                }
                            } catch (_: Exception) {
                                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    step = 1
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                DotText(
                    text = if (wallpaperApplied) "APPLIED" else "APPLY BLACK WALLPAPER",
                    color = if (wallpaperApplied) dimmed else bg,
                    dotSize = 2.5.dp,
                    spacing = 0.8.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { step = 1 },
                contentAlignment = Alignment.Center
            ) {
                DotText(
                    text = "SKIP FOR NOW",
                    color = dimmed,
                    dotSize = 2.dp,
                    spacing = 0.7.dp
                )
            }
        }
    } else if (step == 1) {
        // ═══════════════════════════════════════════
        // STEP 1: Default Launcher Prompt
        // ═══════════════════════════════════════════
        var isDefault by remember { mutableStateOf(false) }

        val roleRequestLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            // Re-check after returning from the role picker
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                val nowDefault = rm?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
                isDefault = nowDefault
                if (nowDefault) {
                    onComplete(null, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AutoScaledDotText(
                text = "MEMENTO LAUNCHER",
                color = onBg,
                baseDotSize = 6.dp,
                baseSpacing = 1.5.dp,
                alignment = Alignment.CenterHorizontally
            )

            Spacer(modifier = Modifier.height(48.dp))

            DotText(
                text = "SET AS",
                color = dimmed,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            DotText(
                text = "DEFAULT HOME SCREEN",
                color = dimmed,
                dotSize = 1.5.dp,
                spacing = 0.5.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            DotText(
                text = "TO USE THIS LAUNCHER",
                color = dimmed.copy(alpha = 0.3f),
                dotSize = 1.dp,
                spacing = 0.4.dp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Set Default button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(onBg, RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                                if (rm != null && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                                    val intent = rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                                    roleRequestLauncher.launch(intent)
                                } else {
                                    isDefault = true
                                    onComplete(null, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
                                }
                            } else {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                                context.startActivity(intent)
                                onComplete(null, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
                            }
                        } catch (_: Exception) {
                            onComplete(null, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                DotText(
                    text = "SET AS DEFAULT",
                    color = bg,
                    dotSize = 2.5.dp,
                    spacing = 0.8.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable {
                        onComplete(null, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY)
                    },
                contentAlignment = Alignment.Center
            ) {
                DotText(
                    text = "SKIP THIS STEP",
                    color = dimmed,
                    dotSize = 2.dp,
                    spacing = 0.7.dp
                )
            }
        }
    }
}
