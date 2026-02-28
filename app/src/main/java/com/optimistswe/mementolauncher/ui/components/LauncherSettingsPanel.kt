package com.optimistswe.mementolauncher.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.data.BackgroundStyle
import com.optimistswe.mementolauncher.data.ClockStyle
import com.optimistswe.mementolauncher.data.FontSize
import com.optimistswe.mementolauncher.data.SearchBarPosition
import com.optimistswe.mementolauncher.data.AppInfo
import com.optimistswe.mementolauncher.ui.components.settings.*
import java.time.LocalDate

/**
 * Full-screen settings panel for the Memento launcher.
 *
 * This panel is triggered by a long-press on the settings icon (if present) or a long right
 * swipe from the wallpaper page. It uses a minimalist, text-only aesthetic consistent with
 * the rest of the app.
 *
 * The panel is modularly divided into several sub-sections:
 * - [AppearanceSettings]: themes, font sizes, background styles, clock formats.
 * - [BehaviorSettings]: keyboard auto-open, search bar position, calendar event visibility.
 * - [HiddenAppsSettings]: management of apps excluded from the drawer.
 * - [FolderSettings]: creation of custom app groupings.
 * - [WallpaperSettings]: quick access to system wallpaper picking.
 *
 * @param onDismiss Callback to close the settings overlay and return to the launcher.
 */
@Composable
fun LauncherSettingsPanel(
    birthDate: LocalDate?,
    lifeExpectancy: Int,
    onBirthDateChange: (LocalDate) -> Unit,
    onLifeExpectancyChange: (Int) -> Unit,
    backgroundStyle: BackgroundStyle,
    onBackgroundStyleChange: (BackgroundStyle) -> Unit,
    fontSize: FontSize,
    onFontSizeChange: (FontSize) -> Unit,
    autoOpenKeyboard: Boolean,
    onAutoOpenKeyboardChange: (Boolean) -> Unit,
    clockStyle: ClockStyle,
    onClockStyleChange: (ClockStyle) -> Unit,

    searchBarPosition: SearchBarPosition,
    onSearchBarPositionChange: (SearchBarPosition) -> Unit,
    blockShortFormContent: Boolean,
    onBlockShortFormContentChange: (Boolean) -> Unit,
    usageNudgeEnabled: Boolean,
    onUsageNudgeEnabledChange: (Boolean) -> Unit,
    usageNudgeMinutes: Int,
    onUsageNudgeMinutesChange: (Int) -> Unit,
    onCreateFolder: (String) -> Unit,

    allApps: List<AppInfo>,
    hiddenPackages: Set<String>,
    onToggleVisibility: (String) -> Unit,

    distractingPackages: Set<String>,
    onToggleDistracting: (String) -> Unit,
    mindfulMessage: String,
    onMindfulMessageChange: (String) -> Unit,

    dockLeftPkg: String?,
    dockRightPkg: String?,
    onSetDockLeft: (String?) -> Unit,
    onSetDockRight: (String?) -> Unit,

    onBackup: () -> Unit = {},
    onRestore: () -> Unit = {},
    backupStatusText: String = "",

    onDismiss: () -> Unit
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val dimmed = onBg.copy(alpha = 0.5f)
    val faint = onBg.copy(alpha = 0.15f)
    val cardBg = onBg.copy(alpha = 0.06f)

    // ═══════════════════════════════════════════
    // BUFFERED STATE (Only saved on click)
    // ═══════════════════════════════════════════
    var bufferedBirthDate by remember { mutableStateOf(birthDate) }
    var bufferedLifeExpectancy by remember { mutableIntStateOf(lifeExpectancy) }
    var bufferedBackgroundStyle by remember { mutableStateOf(backgroundStyle) }
    var bufferedFontSize by remember { mutableStateOf(fontSize) }
    var bufferedAutoOpenKeyboard by remember { mutableStateOf(autoOpenKeyboard) }
    var bufferedClockStyle by remember { mutableStateOf(clockStyle) }
    var bufferedSearchBarPosition by remember { mutableStateOf(searchBarPosition) }
    var bufferedBlockShortFormContent by remember { mutableStateOf(blockShortFormContent) }
    var bufferedUsageNudgeEnabled by remember { mutableStateOf(usageNudgeEnabled) }
    var bufferedUsageNudgeMinutes by remember { mutableIntStateOf(usageNudgeMinutes) }
    var bufferedHiddenPackages by remember { mutableStateOf(hiddenPackages) }
    var bufferedDistractingPackages by remember { mutableStateOf(distractingPackages) }
    var bufferedMindfulMessage by remember { mutableStateOf(mindfulMessage) }
    var bufferedDockLeft by remember { mutableStateOf(dockLeftPkg) }
    var bufferedDockRight by remember { mutableStateOf(dockRightPkg) }

    // Track whether the user was "unconfigured" when they first opened the panel.
    val initiallyUnconfigured = remember { birthDate == null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp) // status bar clearance
        ) {
            // ═══════════════════════════════════════════
            // HEADER — Back Arrow + Title
            // ═══════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button — DISCARDS CHANGES
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = "<",
                        color = onBg,
                        dotSize = 3.dp,
                        spacing = 1.dp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                DotText(
                    text = "SETTINGS",
                    color = onBg,
                    dotSize = 4.dp,
                    spacing = 1.2.dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Thin separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(faint)
            )

            // ═══════════════════════════════════════════
            // SCROLLABLE CONTENT
            // ═══════════════════════════════════════════
            Column(
                modifier = Modifier
                    .weight(1f) // Let the column take available space but respect the footer
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val calendarBlock = @Composable {
                    LifeCalendarSettings(
                        birthDate = bufferedBirthDate,
                        lifeExpectancy = bufferedLifeExpectancy,
                        initialExpanded = initiallyUnconfigured,
                        onBirthDateChange = { bufferedBirthDate = it },
                        onLifeExpectancyChange = { bufferedLifeExpectancy = it },
                        onBg = onBg, bg = bg, dimmed = dimmed, faint = faint, cardBg = cardBg
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // If first time (birthDate was null on open), show calendar at top.
                if (initiallyUnconfigured) {
                    calendarBlock()
                }

                AppearanceSettings(
                    clockStyle = bufferedClockStyle,
                    onClockStyleChange = { bufferedClockStyle = it },
                    backgroundStyle = bufferedBackgroundStyle,
                    onBackgroundStyleChange = { bufferedBackgroundStyle = it },
                    fontSize = bufferedFontSize,
                    onFontSizeChange = { bufferedFontSize = it },
                    onBg = onBg, bg = bg, dimmed = dimmed, faint = faint, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                BehaviorSettings(
                    autoOpenKeyboard = bufferedAutoOpenKeyboard,
                    onAutoOpenKeyboardChange = { bufferedAutoOpenKeyboard = it },
                    searchBarPosition = bufferedSearchBarPosition,
                    onSearchBarPositionChange = { bufferedSearchBarPosition = it },
                    blockShortFormContent = bufferedBlockShortFormContent,
                    onBlockShortFormContentChange = { bufferedBlockShortFormContent = it },
                    usageNudgeEnabled = bufferedUsageNudgeEnabled,
                    onUsageNudgeEnabledChange = { bufferedUsageNudgeEnabled = it },
                    usageNudgeMinutes = bufferedUsageNudgeMinutes,
                    onUsageNudgeMinutesChange = { bufferedUsageNudgeMinutes = it },
                    onBg = onBg, bg = bg, dimmed = dimmed, faint = faint, surface = surface, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                DistractingAppsSettings(
                    allApps = allApps,
                    distractingPackages = bufferedDistractingPackages,
                    onToggleDistracting = { pkg ->
                        bufferedDistractingPackages = if (bufferedDistractingPackages.contains(pkg)) {
                            bufferedDistractingPackages.minus(pkg)
                        } else {
                            bufferedDistractingPackages.plus(pkg)
                        }
                    },
                    mindfulMessage = bufferedMindfulMessage,
                    onMindfulMessageChange = { bufferedMindfulMessage = it },
                    onBg = onBg, bg = bg, dimmed = dimmed, surface = surface, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                HiddenAppsSettings(
                    allApps = allApps,
                    hiddenPackages = bufferedHiddenPackages,
                    onToggleVisibility = { pkg ->
                        bufferedHiddenPackages = if (bufferedHiddenPackages.contains(pkg)) {
                            bufferedHiddenPackages.minus(pkg)
                        } else {
                            bufferedHiddenPackages.plus(pkg)
                        }
                    },
                    onBg = onBg, bg = bg, dimmed = dimmed, surface = surface, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                FolderSettings(
                    onCreateFolder = onCreateFolder,
                    onBg = onBg, bg = bg, dimmed = dimmed, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                WallpaperSettings(
                    onBg = onBg, bg = bg, dimmed = dimmed, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                DataSettings(
                    onBackup = onBackup,
                    onRestore = onRestore,
                    statusText = backupStatusText,
                    onBg = onBg, bg = bg, dimmed = dimmed, cardBg = cardBg
                )

                Spacer(modifier = Modifier.height(8.dp))

                DockAppsSettings(
                    allApps = allApps,
                    dockLeftPkg = bufferedDockLeft,
                    dockRightPkg = bufferedDockRight,
                    onSetLeftApp = { bufferedDockLeft = it },
                    onSetRightApp = { bufferedDockRight = it },
                    onBg = onBg, bg = bg, dimmed = dimmed, surface = surface, cardBg = cardBg
                )

                // If already configured (returned to settings), show calendar at bottom.
                if (!initiallyUnconfigured) {
                    calendarBlock()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Subtle "Exit" hatch at the very end
                val context = LocalContext.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            }
                        }
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoScaledDotText(
                        text = "CHANGE DEFAULT LAUNCHER",
                        color = dimmed,
                        baseDotSize = 2.dp,
                        baseSpacing = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AutoScaledDotText(
                        text = "USE ANOTHER SYSTEM HOME APP",
                        color = faint,
                        baseDotSize = 1.5.dp,
                        baseSpacing = 0.5.dp
                    )
                }

                // Bottom padding to avoid the sticky button
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // ═══════════════════════════════════════════
        // STICKY FOOTER ACTION — SAVES EVERYTHING
        // ═══════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(vertical = 24.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(onBg, RoundedCornerShape(12.dp))
                    .clickable {
                        // Apply all buffered changes
                        bufferedBirthDate?.let { onBirthDateChange(it) }
                        onLifeExpectancyChange(bufferedLifeExpectancy)
                        onBackgroundStyleChange(bufferedBackgroundStyle)
                        onFontSizeChange(bufferedFontSize)
                        onAutoOpenKeyboardChange(bufferedAutoOpenKeyboard)
                        onClockStyleChange(bufferedClockStyle)
                        onSearchBarPositionChange(bufferedSearchBarPosition)
                        onBlockShortFormContentChange(bufferedBlockShortFormContent)
                        onUsageNudgeEnabledChange(bufferedUsageNudgeEnabled)
                        onUsageNudgeMinutesChange(bufferedUsageNudgeMinutes)
                        onMindfulMessageChange(bufferedMindfulMessage)

                        // Toggling visibility and distracting apps
                        // We check difference to call the callbacks or we can just update the whole list
                        // The current ViewModel expects per-package toggles, but since we are overriding,
                        // we can just loop through changes. However, for sheer simplicity in this UI pattern,
                        // we can just call the onToggle for those that changed.
                        // Better: We should ideally have an onToggleAll or similar, but for now we'll match current.
                        
                        hiddenPackages.forEach { pkg: String -> if (!bufferedHiddenPackages.contains(pkg)) onToggleVisibility(pkg) }
                        bufferedHiddenPackages.forEach { pkg: String -> if (!hiddenPackages.contains(pkg)) onToggleVisibility(pkg) }

                        distractingPackages.forEach { pkg: String -> if (!bufferedDistractingPackages.contains(pkg)) onToggleDistracting(pkg) }
                        bufferedDistractingPackages.forEach { pkg: String -> if (!distractingPackages.contains(pkg)) onToggleDistracting(pkg) }

                        // Apply dock changes
                        if (bufferedDockLeft != dockLeftPkg) onSetDockLeft(bufferedDockLeft)
                        if (bufferedDockRight != dockRightPkg) onSetDockRight(bufferedDockRight)

                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                DotText(
                    text = "SAVE",
                    color = bg,
                    dotSize = 3.dp,
                    spacing = 1.dp
                )
            }
        }
    }
}


