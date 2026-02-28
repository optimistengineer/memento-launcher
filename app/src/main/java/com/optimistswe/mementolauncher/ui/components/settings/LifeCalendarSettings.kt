package com.optimistswe.mementolauncher.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.domain.LifeCalendarCalculator
import com.optimistswe.mementolauncher.ui.components.DotText
import com.optimistswe.mementolauncher.ui.components.DottedDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Settings section for life calendar configuration.
 *
 * Allows the user to update their birth date and life expectancy,
 * which are the core inputs for the life calendar wallpaper.
 */
@Composable
fun LifeCalendarSettings(
    birthDate: LocalDate?,
    lifeExpectancy: Int,
    initialExpanded: Boolean = true,
    onBirthDateChange: (LocalDate) -> Unit,
    onLifeExpectancyChange: (Int) -> Unit,
    onBg: Color,
    bg: Color,
    dimmed: Color,
    faint: Color,
    cardBg: Color
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    var showDatePicker by remember { mutableStateOf(false) }
    var localLifeExpectancy by remember(lifeExpectancy) { mutableIntStateOf(lifeExpectancy) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d yyyy") }

    Column {
        SettingsSectionHeader(
            title = "LIFE CALENDAR",
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Birth date row
                DotText(
                    text = "BIRTH DATE",
                    color = onBg,
                    dotSize = 1.5.dp,
                    spacing = 0.5.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(onBg.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DotText(
                        text = birthDate?.format(dateFormatter)?.uppercase() ?: "TAP TO SET",
                        color = if (birthDate != null) onBg else dimmed,
                        dotSize = 2.5.dp,
                        spacing = 0.8.dp
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(faint))

                // Life expectancy row
                DotText(
                    text = "LIFE EXPECTANCY",
                    color = onBg,
                    dotSize = 1.5.dp,
                    spacing = 0.5.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(onBg.copy(alpha = 0.1f))
                            .clickable {
                                if (localLifeExpectancy > LifeCalendarCalculator.MIN_LIFE_EXPECTANCY) {
                                    localLifeExpectancy--
                                    onLifeExpectancyChange(localLifeExpectancy)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(text = "-", color = onBg, dotSize = 2.dp, spacing = 1.dp)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    DotText(
                        text = "$localLifeExpectancy YRS",
                        color = onBg,
                        dotSize = 3.dp,
                        spacing = 1.dp
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(onBg.copy(alpha = 0.1f))
                            .clickable {
                                if (localLifeExpectancy < LifeCalendarCalculator.MAX_LIFE_EXPECTANCY) {
                                    localLifeExpectancy++
                                    onLifeExpectancyChange(localLifeExpectancy)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        DotText(text = "+", color = onBg, dotSize = 2.dp, spacing = 1.dp)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DottedDatePickerDialog(
            initialDate = birthDate,
            onDateSelected = { date ->
                onBirthDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
