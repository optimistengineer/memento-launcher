package com.betteruniverse.mementolauncher.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.betteruniverse.mementolauncher.data.AppInfo
import com.betteruniverse.mementolauncher.data.BackgroundStyle
import com.betteruniverse.mementolauncher.data.ClockStyle
import com.betteruniverse.mementolauncher.data.FontSize
import com.betteruniverse.mementolauncher.data.SearchBarPosition
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Public, stable home of the privacy policy. Served by GitHub Pages from the repository's
 * docs/ directory; the same URL goes in the Play Console App content section, so the two can
 * never drift apart.
 */
internal const val PRIVACY_POLICY_URL =
    "https://optimistengineer.github.io/memento-launcher/privacy-policy.html"

/** The green already used for "the current week" — reused here as "the current choice". */
private val MARKER = Color(0xFF228B22)

/**
 * Which screen of the settings hub is showing.
 *
 * A hub of five short pages, rather than one long scroll of ten collapsible sections. The old
 * panel required the user to expand a section before they could see whether it held what they
 * wanted, so finding anything meant a scroll-and-expand hunt; every page here fits one screen.
 */
private enum class SettingsPage { ROOT, LIFE, APPEARANCE, HOME, FOCUS, DATA, HIDDEN_APPS, FOCUS_APPS, DOCK_LEFT, DOCK_RIGHT }

/** Where back leads from each page: pickers return to the page that opened them, not the root. */
private fun SettingsPage.parent(): SettingsPage = when (this) {
    SettingsPage.HIDDEN_APPS, SettingsPage.DOCK_LEFT, SettingsPage.DOCK_RIGHT -> SettingsPage.HOME
    SettingsPage.FOCUS_APPS -> SettingsPage.FOCUS
    else -> SettingsPage.ROOT
}

/**
 * Settings for the Memento launcher.
 *
 * Two decisions shape everything here:
 *
 *  1. EVERY CHANGE APPLIES IMMEDIATELY. The previous panel buffered thirteen values in
 *     `remember` and wrote them only when a floating SAVE was pressed, which meant the user
 *     could not tell what was committed, back silently discarded their work, and any
 *     configuration change (rotation, font-size change, theme switch) destroyed the lot. Writing
 *     on tap removes the ambiguity and that entire class of bug — there is no draft to lose.
 *  2. IT LOOKS LIKE THE LAUNCHER. Rows of dot-matrix text, and a single green dot marking the
 *     current choice — the same green that marks the current week on the calendar and sits at
 *     the centre of the app icon. Material Switches were the one element in the app that was
 *     not drawn in its own language.
 *
 * Rows are full-width and 56dp tall so the whole row is the target, not a 20dp control at the
 * end of it, and each carries a spoken label for TalkBack.
 */
@Composable
fun LauncherSettingsPanel(
    birthDate: LocalDate?,
    lifeExpectancy: Int,
    showLifeCalendar: Boolean = true,
    hasUsageAccess: Boolean = false,
    onShowLifeCalendarChange: (Boolean) -> Unit = {},
    onBirthDateChange: (LocalDate) -> Unit,
    onLifeExpectancyChange: (Int) -> Unit,
    /** Atomic ±1 for the stepper — read-modify-write happens inside the repository. */
    onAdjustLifeExpectancy: (Int) -> Unit,
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
    onCreateFolder: (String) -> Unit,
    allApps: List<AppInfo>,
    hiddenPackages: Set<String>,
    /** Atomic per-package toggle — see the picker comment for why not a whole-set write. */
    onToggleHidden: (String) -> Unit,
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
    val dimmed = onBg.copy(alpha = 0.55f)
    val faint = onBg.copy(alpha = 0.28f)

    // rememberSaveable: surviving a configuration change is the whole point of not buffering
    // edits, so the page you are on should survive one too.
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // Back steps out of a sub-page before it closes the panel, so a user three taps deep is not
    // thrown all the way out to the home screen. Registered before the root screen's own handler.
    BackHandler(enabled = true) {
        if (page == SettingsPage.ROOT) onDismiss() else page = page.parent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 56.dp)) {
            SettingsHeader(
                title = when (page) {
                    SettingsPage.ROOT -> "SETTINGS"
                    SettingsPage.LIFE -> "LIFE CALENDAR"
                    SettingsPage.APPEARANCE -> "APPEARANCE"
                    SettingsPage.HOME -> "HOME & DRAWER"
                    SettingsPage.FOCUS -> "FOCUS"
                    SettingsPage.DATA -> "DATA & PRIVACY"
                    SettingsPage.HIDDEN_APPS -> "HIDDEN APPS"
                    SettingsPage.FOCUS_APPS -> "MINDFUL APPS"
                    SettingsPage.DOCK_LEFT -> "LEFT CORNER"
                    SettingsPage.DOCK_RIGHT -> "RIGHT CORNER"
                },
                onBack = { if (page == SettingsPage.ROOT) onDismiss() else page = page.parent() },
                onBg = onBg,
                faint = faint
            )

            when (page) {
                SettingsPage.ROOT -> RootPage(
                    onOpen = { page = it },
                    onBg = onBg, dimmed = dimmed, faint = faint
                )

                SettingsPage.LIFE -> ScrollPage {
                    SectionLabel("YOUR LIFE", dimmed)
                    ActionRow(
                        label = "BIRTH DATE",
                        value = birthDate?.format(DateTimeFormatter.ofPattern("d MMM yyyy"))?.uppercase()
                            ?: "NOT SET",
                        onClick = { showDatePicker = true },
                        onBg = onBg, dimmed = dimmed
                    )
                    StepperRow(
                        label = "LIFE EXPECTANCY",
                        value = "$lifeExpectancy YRS",
                        // Delta, not displayed-value±1: the displayed value lags each write by a
                        // DataStore round-trip, so rapid taps computed from it collapse into one.
                        onDecrease = { onAdjustLifeExpectancy(-1) },
                        onIncrease = { onAdjustLifeExpectancy(+1) },
                        onBg = onBg, dimmed = dimmed, faint = faint
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("CALENDAR PAGE", dimmed)
                    ToggleRow(
                        label = "SHOW CALENDAR PAGE",
                        hint = "SWIPE LEFT FROM HOME",
                        checked = showLifeCalendar,
                        onToggle = { onShowLifeCalendarChange(!showLifeCalendar) },
                        onBg = onBg, dimmed = dimmed, faint = faint
                    )
                }

                SettingsPage.APPEARANCE -> ScrollPage {
                    SectionLabel("CLOCK", dimmed)
                    ClockStyle.entries.forEach { style ->
                        ChoiceRow(
                            label = when (style) {
                                ClockStyle.H24 -> "24 HOUR"
                                ClockStyle.H12 -> "12 HOUR"
                                ClockStyle.H24_SEC -> "24 HOUR WITH SECONDS"
                            },
                            selected = clockStyle == style,
                            onClick = { onClockStyleChange(style) },
                            onBg = onBg, faint = faint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("BACKGROUND", dimmed)
                    BackgroundStyle.entries.forEach { style ->
                        ChoiceRow(
                            label = when (style) {
                                BackgroundStyle.SOLID_BLACK -> "SOLID BLACK"
                                BackgroundStyle.MATRIX_GRID -> "MATRIX GRID"
                                BackgroundStyle.STARFIELD -> "STARFIELD"
                            },
                            selected = backgroundStyle == style,
                            onClick = { onBackgroundStyleChange(style) },
                            onBg = onBg, faint = faint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("TEXT SIZE", dimmed)
                    FontSize.entries.forEach { size ->
                        ChoiceRow(
                            label = size.name,
                            selected = fontSize == size,
                            onClick = { onFontSizeChange(size) },
                            onBg = onBg, faint = faint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    HintText("TEXT ALSO FOLLOWS YOUR SYSTEM FONT SIZE", faint)
                }

                SettingsPage.HOME -> ScrollPage {
                    SectionLabel("DOCK CORNERS", dimmed)
                    ActionRow(
                        label = "LEFT CORNER",
                        value = labelFor(dockLeftPkg, allApps),
                        onClick = { page = SettingsPage.DOCK_LEFT },
                        onBg = onBg, dimmed = dimmed
                    )
                    ActionRow(
                        label = "RIGHT CORNER",
                        value = labelFor(dockRightPkg, allApps),
                        onClick = { page = SettingsPage.DOCK_RIGHT },
                        onBg = onBg, dimmed = dimmed
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("APP DRAWER", dimmed)
                    SearchBarPosition.entries.forEach { pos ->
                        ChoiceRow(
                            label = if (pos == SearchBarPosition.TOP) "SEARCH BAR AT TOP" else "SEARCH BAR AT BOTTOM",
                            selected = searchBarPosition == pos,
                            onClick = { onSearchBarPositionChange(pos) },
                            onBg = onBg, faint = faint
                        )
                    }
                    ToggleRow(
                        label = "OPEN KEYBOARD",
                        hint = "WHEN THE DRAWER OPENS",
                        checked = autoOpenKeyboard,
                        onToggle = { onAutoOpenKeyboardChange(!autoOpenKeyboard) },
                        onBg = onBg, dimmed = dimmed, faint = faint
                    )
                    ActionRow(
                        label = "HIDDEN APPS",
                        value = if (hiddenPackages.isEmpty()) "NONE" else "${hiddenPackages.size}",
                        onClick = { page = SettingsPage.HIDDEN_APPS },
                        onBg = onBg, dimmed = dimmed
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("FOLDERS", dimmed)
                    NewFolderRow(onCreateFolder = onCreateFolder, onBg = onBg, dimmed = dimmed, faint = faint)
                }

                SettingsPage.FOCUS -> ScrollPage {
                    SectionLabel("MINDFUL PAUSE", dimmed)
                    ActionRow(
                        label = "APPS TO PAUSE",
                        value = if (distractingPackages.isEmpty()) "NONE" else "${distractingPackages.size}",
                        onClick = { page = SettingsPage.FOCUS_APPS },
                        onBg = onBg, dimmed = dimmed
                    )
                    MessageRow(
                        message = mindfulMessage,
                        onMessageChange = onMindfulMessageChange,
                        onBg = onBg, dimmed = dimmed, faint = faint
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("SCREEN TIME", dimmed)
                    val context = LocalContext.current
                    ActionRow(
                        label = if (hasUsageAccess) "USAGE ACCESS GRANTED" else "ALLOW USAGE ACCESS",
                        value = if (hasUsageAccess) "ON" else "OFF",
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        },
                        onBg = onBg, dimmed = dimmed
                    )
                    HintText("SHOWS TODAY'S SCREEN TIME ON THE HOME SCREEN", faint)
                }

                SettingsPage.DATA -> ScrollPage {
                    SectionLabel("BACKUP", dimmed)
                    ActionRow("EXPORT SETTINGS", "JSON FILE", onBackup, onBg, dimmed)
                    ActionRow("IMPORT SETTINGS", "REPLACES ALL", onRestore, onBg, dimmed)
                    if (backupStatusText.isNotEmpty()) HintText(backupStatusText, dimmed)
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("WALLPAPER", dimmed)
                    val context = LocalContext.current
                    ActionRow(
                        label = "SYSTEM WALLPAPER",
                        value = "CHANGE",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "WALLPAPER")
                                )
                            }
                        },
                        onBg = onBg, dimmed = dimmed
                    )
                    HintText("A BLACK WALLPAPER SUITS THIS LAUNCHER BEST", faint)
                }

                // Toggles are atomic per-package operations in the repository, NOT whole-set
                // writes computed here: the prop this page renders lags each write by a full
                // DataStore round-trip, so two quick taps computed from it would both start
                // from the same stale set and the first selection would silently revert.
                SettingsPage.HIDDEN_APPS -> AppPickerPage(
                    apps = allApps,
                    selected = hiddenPackages,
                    onToggle = onToggleHidden,
                    emptyHint = "HIDDEN APPS STAY INSTALLED, JUST OUT OF THE DRAWER",
                    onBg = onBg, dimmed = dimmed, faint = faint
                )

                SettingsPage.FOCUS_APPS -> AppPickerPage(
                    apps = allApps,
                    selected = distractingPackages,
                    onToggle = onToggleDistracting,
                    emptyHint = "THESE APPS ASK YOU TO PAUSE BEFORE THEY OPEN",
                    onBg = onBg, dimmed = dimmed, faint = faint
                )

                SettingsPage.DOCK_LEFT, SettingsPage.DOCK_RIGHT -> {
                    val isLeft = page == SettingsPage.DOCK_LEFT
                    SingleAppPickerPage(
                        apps = allApps,
                        selectedPkg = if (isLeft) dockLeftPkg else dockRightPkg,
                        onPick = { pkg ->
                            if (isLeft) onSetDockLeft(pkg) else onSetDockRight(pkg)
                            page = SettingsPage.HOME
                        },
                        onBg = onBg, faint = faint
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        DottedDatePickerDialog(
            initialDate = birthDate,
            onDateSelected = {
                onBirthDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ═══════════════════════════════════════════
// Root
// ═══════════════════════════════════════════

@Composable
private fun ColumnScope.RootPage(
    onOpen: (SettingsPage) -> Unit,
    onBg: Color,
    dimmed: Color,
    faint: Color
) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        NavRow("LIFE CALENDAR", "BIRTH DATE, EXPECTANCY", { onOpen(SettingsPage.LIFE) }, onBg, dimmed)
        NavRow("APPEARANCE", "CLOCK, BACKGROUND, TEXT", { onOpen(SettingsPage.APPEARANCE) }, onBg, dimmed)
        NavRow("HOME & DRAWER", "DOCK, SEARCH, FOLDERS", { onOpen(SettingsPage.HOME) }, onBg, dimmed)
        NavRow("FOCUS", "MINDFUL PAUSE, SCREEN TIME", { onOpen(SettingsPage.FOCUS) }, onBg, dimmed)
        NavRow("DATA & PRIVACY", "BACKUP, WALLPAPER", { onOpen(SettingsPage.DATA) }, onBg, dimmed)

        Spacer(Modifier.height(32.dp))

        // Quiet footer: the escape hatch, and the two things Play requires be reachable.
        SettingsRow(
            onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                    .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
            },
            label = "CHANGE DEFAULT LAUNCHER"
        ) {
            AutoScaledDotText("CHANGE DEFAULT LAUNCHER", dimmed, baseDotSize = 1.8.dp, baseSpacing = 0.6.dp, alignment = Alignment.Start)
        }
        SettingsRow(
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                }
            },
            label = "PRIVACY POLICY"
        ) {
            AutoScaledDotText("PRIVACY POLICY", dimmed, baseDotSize = 1.8.dp, baseSpacing = 0.6.dp, alignment = Alignment.Start)
        }
        if (version.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                DotText("MEMENTO $version", faint, dotSize = 1.3.dp, spacing = 0.45.dp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════
// Shared row vocabulary
// ═══════════════════════════════════════════

/** Every tappable line in settings: full width, 56dp tall, one spoken label. */
@Composable
private fun SettingsRow(
    onClick: () -> Unit,
    label: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            // clearAndSetSemantics, not semantics: with children that carry their own
            // contentDescriptions (every DotText does), a merging parent's description is
            // DROPPED — TalkBack read the raw label and never the ", on"/", selected" state.
            // Replacing the subtree's semantics makes the row one node with the full sentence.
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) { content() }
        trailing?.invoke()
    }
}

/** A row that opens a sub-page. */
@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit, onBg: Color, dimmed: Color) {
    SettingsRow(onClick = onClick, label = title, trailing = {
        DotText(">", dimmed, dotSize = 1.8.dp, spacing = 0.6.dp)
    }) {
        AutoScaledDotText(title, onBg, baseDotSize = 2.4.dp, baseSpacing = 0.8.dp, alignment = Alignment.Start)
        Spacer(Modifier.height(5.dp))
        AutoScaledDotText(subtitle, dimmed.copy(alpha = 0.5f), baseDotSize = 1.2.dp, baseSpacing = 0.4.dp, alignment = Alignment.Start)
    }
}

/** A row showing a current value that opens something when tapped. */
@Composable
private fun ActionRow(label: String, value: String, onClick: () -> Unit, onBg: Color, dimmed: Color) {
    SettingsRow(onClick = onClick, label = "$label, $value", trailing = {
        Box(Modifier.widthIn(max = 150.dp)) {
            AutoScaledDotText(value, dimmed, baseDotSize = 1.6.dp, baseSpacing = 0.55.dp, alignment = Alignment.End)
        }
    }) {
        AutoScaledDotText(label, onBg, baseDotSize = 2.dp, baseSpacing = 0.7.dp, alignment = Alignment.Start)
    }
}

/**
 * One option in a set. The green dot is the entire selection language of this app — the current
 * week on the calendar, the centre of the icon, and now the current choice.
 */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit, onBg: Color, faint: Color) {
    SettingsRow(
        onClick = onClick,
        label = if (selected) "$label, selected" else label,
        trailing = { Marker(on = selected, faint = faint) }
    ) {
        AutoScaledDotText(label, if (selected) onBg else onBg.copy(alpha = 0.75f), baseDotSize = 2.dp, baseSpacing = 0.7.dp, alignment = Alignment.Start)
    }
}

/** On/off, using the same marker as a choice so there is one visual language, not two. */
@Composable
private fun ToggleRow(
    label: String,
    hint: String?,
    checked: Boolean,
    onToggle: () -> Unit,
    onBg: Color,
    dimmed: Color,
    faint: Color
) {
    SettingsRow(
        onClick = onToggle,
        label = "$label, ${if (checked) "on" else "off"}",
        trailing = { Marker(on = checked, faint = faint) }
    ) {
        AutoScaledDotText(label, onBg, baseDotSize = 2.dp, baseSpacing = 0.7.dp, alignment = Alignment.Start)
        if (hint != null) {
            Spacer(Modifier.height(5.dp))
            AutoScaledDotText(hint, dimmed.copy(alpha = 0.5f), baseDotSize = 1.2.dp, baseSpacing = 0.4.dp, alignment = Alignment.Start)
        }
    }
}

@Composable
private fun Marker(on: Boolean, faint: Color) {
    Box(
        modifier = Modifier
            .size(if (on) 14.dp else 10.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) MARKER else faint)
    )
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onBg: Color,
    dimmed: Color,
    faint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            AutoScaledDotText(label, onBg, baseDotSize = 2.dp, baseSpacing = 0.7.dp, alignment = Alignment.Start)
        }
        StepButton("-", onDecrease, onBg, faint, "Decrease $label")
        Box(Modifier.width(92.dp)) {
            AutoScaledDotText(value, dimmed, baseDotSize = 1.8.dp, baseSpacing = 0.6.dp, alignment = Alignment.CenterHorizontally)
        }
        StepButton("+", onIncrease, onBg, faint, "Increase $label")
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit, onBg: Color, faint: Color, label: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(faint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        DotText(glyph, onBg, dotSize = 2.dp, spacing = 0.7.dp)
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Box(Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)) {
        DotText(text, color.copy(alpha = 0.6f), dotSize = 1.3.dp, spacing = 0.45.dp)
    }
}

@Composable
private fun HintText(text: String, color: Color) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)) {
        AutoScaledDotText(text, color, baseDotSize = 1.3.dp, baseSpacing = 0.45.dp, alignment = Alignment.Start)
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit, onBg: Color, faint: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBack)
                    .clearAndSetSemantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center
            ) {
                DotText("<", onBg, dotSize = 2.6.dp, spacing = 0.9.dp)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                AutoScaledDotText(title, onBg, baseDotSize = 3.2.dp, baseSpacing = 1.dp, alignment = Alignment.Start)
            }
        }
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(faint.copy(alpha = 0.4f)))
    }
}

@Composable
private fun ColumnScope.ScrollPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        content()
        Spacer(Modifier.height(40.dp))
    }
}

// ═══════════════════════════════════════════
// App pickers
// ═══════════════════════════════════════════

/**
 * Full-screen multi-select over the installed apps.
 *
 * A LazyColumn, not the old inline forEach inside a scrolling Column: on a device with several
 * hundred apps that composed every row up front, twice over, inside a settings page the user had
 * usually opened for something else entirely.
 */
@Composable
private fun ColumnScope.AppPickerPage(
    apps: List<AppInfo>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    emptyHint: String,
    onBg: Color,
    dimmed: Color,
    faint: Color
) {
    val sorted = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    Column(Modifier.weight(1f)) {
        HintText(emptyHint, faint)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            items(sorted, key = { it.packageName }) { app ->
                ChoiceRow(
                    label = app.label.uppercase(),
                    selected = selected.contains(app.packageName),
                    onClick = { onToggle(app.packageName) },
                    onBg = onBg, faint = faint
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

/** Single-select, used for the two dock corners. NONE clears the slot. */
@Composable
private fun ColumnScope.SingleAppPickerPage(
    apps: List<AppInfo>,
    selectedPkg: String?,
    onPick: (String?) -> Unit,
    onBg: Color,
    faint: Color
) {
    val sorted = remember(apps) { apps.sortedBy { it.label.lowercase() } }
    LazyColumn(modifier = Modifier.weight(1f).fillMaxSize().padding(horizontal = 24.dp)) {
        item {
            ChoiceRow("NONE", selectedPkg == null, { onPick(null) }, onBg, faint)
        }
        items(sorted, key = { it.packageName }) { app ->
            ChoiceRow(
                label = app.label.uppercase(),
                selected = selectedPkg == app.packageName,
                onClick = { onPick(app.packageName) },
                onBg = onBg, faint = faint
            )
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ═══════════════════════════════════════════
// Text entry rows
// ═══════════════════════════════════════════

/**
 * Free text. Edits show instantly but the DataStore write is debounced: committing on every
 * keystroke would be one disk transaction per character. 400ms after the user pauses typing the
 * value persists — indistinguishable from instant to the user, ~one write per edit for the disk.
 * The local draft is rememberSaveable, so even mid-debounce a configuration change loses nothing.
 */
@Composable
private fun MessageRow(
    message: String,
    onMessageChange: (String) -> Unit,
    onBg: Color,
    dimmed: Color,
    faint: Color
) {
    var draft by rememberSaveable { mutableStateOf(message) }
    LaunchedEffect(draft) {
        if (draft != message) {
            kotlinx.coroutines.delay(400)
            onMessageChange(draft)
        }
    }
    // The debounce alone loses the edit if the user leaves within 400ms of the last keystroke:
    // leaving cancels the LaunchedEffect before it writes. Flush the draft on the way out.
    val latest by rememberUpdatedState(message to onMessageChange)
    DisposableEffect(Unit) {
        onDispose {
            val (committed, commit) = latest
            if (draft != committed) commit(draft)
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
        AutoScaledDotText("PAUSE MESSAGE", onBg, baseDotSize = 2.dp, baseSpacing = 0.7.dp, alignment = Alignment.Start)
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = draft,
            onValueChange = { draft = it },
            textStyle = TextStyle(color = onBg, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(onBg),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(faint.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
        Spacer(Modifier.height(6.dp))
        AutoScaledDotText("SHOWN BEFORE A PAUSED APP OPENS", dimmed.copy(alpha = 0.5f), baseDotSize = 1.2.dp, baseSpacing = 0.4.dp, alignment = Alignment.Start)
    }
}

/** Inline folder creation — type a name, tap CREATE. Blank and duplicate names are rejected. */
@Composable
private fun NewFolderRow(onCreateFolder: (String) -> Unit, onBg: Color, dimmed: Color, faint: Color) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(color = onBg, fontSize = 15.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(onBg),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            DotText("NEW FOLDER NAME", faint, dotSize = 1.4.dp, spacing = 0.5.dp)
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(faint.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = name.isNotBlank()) {
                        onCreateFolder(name.trim())
                        name = ""
                    }
                    .semantics { contentDescription = "Create folder" }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                DotText("CREATE", if (name.isNotBlank()) MARKER else dimmed.copy(alpha = 0.4f), dotSize = 1.6.dp, spacing = 0.55.dp)
            }
        }
    }
}

private fun labelFor(pkg: String?, apps: List<AppInfo>): String =
    pkg?.let { p -> apps.find { it.packageName == p }?.label?.uppercase() } ?: "NONE"
