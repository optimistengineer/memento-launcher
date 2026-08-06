package com.betteruniverse.mementolauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Search bar for the app drawer with a monospace text input and a settings icon button.
 *
 * Shows a dot-matrix "SEARCH APPS" placeholder when the query is empty.
 */
@Composable
fun AppDrawerSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    focusRequester: FocusRequester,
    focusManager: FocusManager
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val dimmed = onBg.copy(alpha = 0.4f)
    val faint = onBg.copy(alpha = 0.15f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Input Box
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                textStyle = TextStyle(
                    color = onBg,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                cursorBrush = SolidColor(onBg),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            DotText(
                                text = "SEARCH APPS",
                                color = faint,
                                dotSize = 2.dp,
                                spacing = 0.7.dp
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )

            // Clearing meant holding backspace or selecting the whole query by hand, which is
            // the one thing people do constantly while searching — type, miss, retype.
            // Only rendered when there is something to clear, so the empty field stays quiet.
            if (searchQuery.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onClearSearch()
                            // Keep focus and the keyboard up: clearing is a step in the middle
                            // of searching, not the end of it.
                            focusRequester.requestFocus()
                        }
                        .semantics { contentDescription = "Clear search" },
                    contentAlignment = Alignment.Center
                ) {
                    DotIcon(
                        type = DotIconType.CLOSE,
                        color = dimmed,
                        dotSize = 1.3.dp,
                        spacing = 0.4.dp
                    )
                }
            }
        }

        // Settings Icon Button
        IconButton(onClick = onOpenSettings) {
            DotIcon(
                type = DotIconType.SETTINGS,
                color = dimmed,
                dotSize = 1.3.dp,
                spacing = 0.4.dp
            )
        }
    }
}
