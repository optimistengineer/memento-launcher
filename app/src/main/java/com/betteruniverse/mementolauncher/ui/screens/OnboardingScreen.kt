package com.betteruniverse.mementolauncher.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.betteruniverse.mementolauncher.domain.LifeCalendarCalculator
import com.betteruniverse.mementolauncher.ui.components.AutoScaledDotText
import com.betteruniverse.mementolauncher.ui.components.DotText
import com.betteruniverse.mementolauncher.ui.components.DottedDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * First-run setup.
 *
 * Three screens, in descending order of how much they matter:
 *
 *  0. Become the home app. A launcher that does not hold the HOME role does nothing at all,
 *     so this is the only step that is genuinely load-bearing and it comes first.
 *  1. Birth date. The single input the life calendar needs.
 *  2. Life calendar opt-in. The signature feature, but a grid counting down the weeks you have
 *     left is not for everyone, so it is offered rather than assumed.
 *
 * Every step can be skipped individually, and every step also offers "SKIP SETUP", which
 * finishes immediately using defaults for everything that has not been answered yet. Nothing
 * here is irreversible — all of it is editable in settings afterwards.
 *
 * @param onComplete Invoked once, with whatever the user chose (or the defaults).
 */
@Composable
fun OnboardingScreen(
    onComplete: (birthDate: LocalDate?, lifeExpectancy: Int, showLifeCalendar: Boolean) -> Unit
) {
    val context = LocalContext.current
    val onBg = Color.White
    val dimmed = onBg.copy(alpha = 0.55f)
    val faint = onBg.copy(alpha = 0.30f)

    // rememberSaveable, not remember: MainActivity declares no configChanges, so it is recreated
    // on rotation, a foldable unfold, a system font-size change or a light/dark switch. With plain
    // remember, a user who had reached the last step — or picked a birth date — was thrown back to
    // WELCOME with the date silently gone. That cost nothing before this flow collected input;
    // now it does. LocalDate is not Saveable, so the epoch day is stored instead.
    // Start past the welcome step when this app already holds the HOME role: that step asks one
    // question ("make me your home app?") which is then already answered. This matters most on
    // the path that exposed the skipped-onboarding bug — the user grants the role, the system
    // bounces them to the launcher, the launcher sends them back here — where re-asking would
    // make them tap "SET AS HOME APP" a second time to get past a step with nothing left to do.
    val startStep = remember {
        val alreadyHome = runCatching {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.getSystemService(android.app.role.RoleManager::class.java)
                    ?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
        }.getOrDefault(false)
        if (alreadyHome) 1 else 0
    }
    var step by rememberSaveable { mutableIntStateOf(startStep) }
    var birthDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val birthDate: LocalDate? = birthDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val defaults = { onComplete(birthDate, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY, true) }

    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Whether or not the role was granted, the user has answered this step.
        step = 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            when (step) {
                0 -> WelcomeStep(
                    onBg = onBg, dimmed = dimmed,
                    onSetDefault = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val rm = context.getSystemService(android.app.role.RoleManager::class.java)
                                if (rm != null && !rm.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)) {
                                    roleRequestLauncher.launch(
                                        rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
                                    )
                                } else {
                                    step = 1
                                }
                            } else {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                                )
                                step = 1
                            }
                        } catch (_: Exception) {
                            // No role picker on this device — nothing to do but move on.
                            step = 1
                        }
                    },
                    onSkip = { step = 1 }
                )

                1 -> BirthDateStep(
                    onBg = onBg, dimmed = dimmed, faint = faint,
                    birthDate = birthDate,
                    onPickDate = { showDatePicker = true },
                    onContinue = { step = 2 },
                    onSkip = { step = 2 }
                )

                else -> LifeCalendarStep(
                    onBg = onBg, dimmed = dimmed, faint = faint,
                    onChoose = { show ->
                        onComplete(birthDate, LifeCalendarCalculator.DEFAULT_LIFE_EXPECTANCY, show)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            StepIndicator(current = step, total = 3, active = onBg, inactive = faint)

            Spacer(modifier = Modifier.height(20.dp))

            // Present on every step: finish now, defaults for anything unanswered.
            if (step < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable { defaults() },
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = "SKIP SETUP",
                        color = faint,
                        dotSize = 1.5.dp,
                        spacing = 0.5.dp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(44.dp))
            }
        }
    }

    if (showDatePicker) {
        DottedDatePickerDialog(
            initialDate = birthDate,
            onDateSelected = {
                birthDateEpochDay = it.toEpochDay()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ═══════════════════════════════════════════
// STEP 0 — become the home app
// ═══════════════════════════════════════════

@Composable
private fun WelcomeStep(
    onBg: Color,
    dimmed: Color,
    onSetDefault: () -> Unit,
    onSkip: () -> Unit
) {
    DotDiamond(color = onBg, markerColor = Color(0xFF228B22), size = 96.dp)

    Spacer(modifier = Modifier.height(36.dp))

    AutoScaledDotText(
        text = "MEMENTO",
        color = onBg,
        baseDotSize = 6.dp,
        baseSpacing = 1.5.dp,
        alignment = Alignment.CenterHorizontally
    )

    Spacer(modifier = Modifier.height(20.dp))

    CenteredLine("A QUIETER HOME SCREEN", dimmed, 1.6.dp)
    Spacer(modifier = Modifier.height(8.dp))
    CenteredLine("BUILT AROUND THE TIME YOU HAVE", dimmed, 1.6.dp)

    Spacer(modifier = Modifier.height(44.dp))

    PrimaryButton(text = "SET AS HOME APP", onBg = onBg, onClick = onSetDefault)
    Spacer(modifier = Modifier.height(12.dp))
    SecondaryButton(text = "NOT NOW", color = dimmed, onClick = onSkip)
}

// ═══════════════════════════════════════════
// STEP 1 — birth date
// ═══════════════════════════════════════════

@Composable
private fun BirthDateStep(
    onBg: Color,
    dimmed: Color,
    faint: Color,
    birthDate: LocalDate?,
    onPickDate: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    AutoScaledDotText(
        text = "WHEN WERE YOU BORN?",
        color = onBg,
        baseDotSize = 3.4.dp,
        baseSpacing = 1.dp,
        alignment = Alignment.CenterHorizontally
    )

    Spacer(modifier = Modifier.height(16.dp))

    CenteredLine("SO MEMENTO CAN COUNT YOUR WEEKS", dimmed, 1.6.dp)

    Spacer(modifier = Modifier.height(36.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(onBg.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onPickDate() },
        contentAlignment = Alignment.Center
    ) {
        DotText(
            text = birthDate?.format(formatter)?.uppercase() ?: "TAP TO CHOOSE",
            color = if (birthDate != null) onBg else dimmed,
            dotSize = 2.6.dp,
            spacing = 0.9.dp
        )
    }

    Spacer(modifier = Modifier.height(36.dp))

    if (birthDate != null) {
        PrimaryButton(text = "CONTINUE", onBg = onBg, onClick = onContinue)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "CHANGE DATE", color = dimmed, onClick = onPickDate)
    } else {
        SecondaryButton(text = "SET IT LATER", color = dimmed, onClick = onSkip)
        Spacer(modifier = Modifier.height(12.dp))
        CenteredLine("YOU CAN ADD IT ANY TIME IN SETTINGS", faint, 1.2.dp)
    }
}

// ═══════════════════════════════════════════
// STEP 2 — life calendar opt-in
// ═══════════════════════════════════════════

@Composable
private fun LifeCalendarStep(
    onBg: Color,
    dimmed: Color,
    faint: Color,
    onChoose: (Boolean) -> Unit
) {
    MiniLifeGrid(filled = onBg, empty = faint, marker = Color(0xFF228B22))

    Spacer(modifier = Modifier.height(32.dp))

    AutoScaledDotText(
        text = "SHOW THE LIFE CALENDAR?",
        color = onBg,
        baseDotSize = 3.2.dp,
        baseSpacing = 1.dp,
        alignment = Alignment.CenterHorizontally
    )

    Spacer(modifier = Modifier.height(16.dp))

    CenteredLine("ONE DOT FOR EVERY WEEK OF YOUR LIFE", dimmed, 1.6.dp)
    Spacer(modifier = Modifier.height(6.dp))
    CenteredLine("FILLED FOR THE WEEKS YOU HAVE LIVED", dimmed, 1.6.dp)

    Spacer(modifier = Modifier.height(40.dp))

    PrimaryButton(text = "SHOW IT", onBg = onBg, onClick = { onChoose(true) })
    Spacer(modifier = Modifier.height(12.dp))
    SecondaryButton(text = "NO THANKS", color = dimmed, onClick = { onChoose(false) })

    Spacer(modifier = Modifier.height(16.dp))
    CenteredLine("YOU CAN CHANGE THIS IN SETTINGS", faint, 1.2.dp)
}

// ═══════════════════════════════════════════
// Shared pieces
// ═══════════════════════════════════════════

@Composable
private fun CenteredLine(text: String, color: Color, dotSize: Dp) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AutoScaledDotText(
            text = text,
            color = color,
            baseDotSize = dotSize,
            baseSpacing = dotSize * 0.34f,
            alignment = Alignment.CenterHorizontally
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onBg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(onBg, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        DotText(text = text, color = Color.Black, dotSize = 2.4.dp, spacing = 0.8.dp)
    }
}

@Composable
private fun SecondaryButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        DotText(text = text, color = color, dotSize = 2.dp, spacing = 0.7.dp)
    }
}

/** Three dots showing progress through setup. */
@Composable
private fun StepIndicator(current: Int, total: Int, active: Color, inactive: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "Step ${current + 1} of $total" }
    ) {
        repeat(total) { i ->
            Canvas(modifier = Modifier.size(7.dp)) {
                drawCircle(color = if (i == current) active else inactive)
            }
        }
    }
}

/**
 * The app mark: a diamond lattice of dots with the current-week marker at its centre.
 * Same construction as the launcher icon, so first-run and home screen agree.
 */
@Composable
private fun DotDiamond(color: Color, markerColor: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        // Must stay the SAME mark as the launcher icon (res/drawable/ic_launcher_foreground.xml):
        // the Current Week (13) diamond — every cell within taxicab distance 2 of the centre,
        // 1+3+5+3+1 = 13 dots, with the current week marked in green. This drew n = 3 (25 dots)
        // while the app icon was the 13-dot mark, so the first thing a new user saw during
        // onboarding was a different logo from the one they had just tapped to get there.
        // Ratios below are taken from the icon: dot radius 0.34 x pitch, marker 1.10 x that.
        val n = 2
        val pitch = this.size.minDimension / (2 * n + 2)
        val r = pitch * 0.34f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        for (k in -n..n) {
            val width = 2 * (n - kotlin.math.abs(k)) + 1
            val start = -(width - 1) / 2f
            for (i in 0 until width) {
                val gx = start + i
                val isCentre = gx == 0f && k == 0
                drawCircle(
                    color = if (isCentre) markerColor else color,
                    radius = if (isCentre) r * 1.10f else r,
                    center = Offset(cx + gx * pitch, cy + k * pitch)
                )
            }
        }
    }
}

/** A small, honest preview of what the life calendar page looks like. */
@Composable
private fun MiniLifeGrid(filled: Color, empty: Color, marker: Color) {
    val columns = 26
    val rows = 12
    val livedFraction = 0.42f
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val cell = size.width / columns
        val radius = cell * 0.26f
        val livedCells = (rows * columns * livedFraction).toInt()
        var index = 0
        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val cx = col * cell + cell / 2f
                val cy = row * (size.height / rows) + (size.height / rows) / 2f
                when {
                    index == livedCells -> drawCircle(marker, radius, Offset(cx, cy))
                    index < livedCells -> drawCircle(filled, radius, Offset(cx, cy))
                    else -> drawCircle(empty, radius * 0.62f, Offset(cx, cy))
                }
                index++
            }
        }
    }
}
