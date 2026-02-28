package com.optimistswe.mementolauncher.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.optimistswe.mementolauncher.ui.components.AutoScaledDotText
import com.optimistswe.mementolauncher.ui.components.DotText

/**
 * Full-screen prompt asking the user to set Memento as their default home screen.
 *
 * Shown every time the app opens when it isn't the default launcher.
 * Uses RoleManager.ROLE_HOME on Android 10+, falls back to HOME settings on older.
 */
@Composable
fun DefaultLauncherScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(false) }
    val onBg = MaterialTheme.colorScheme.onBackground
    val bg = MaterialTheme.colorScheme.background
    val dimmed = onBg.copy(alpha = 0.4f)

    val roleRequestLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Re-check after returning from the role picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(android.app.role.RoleManager::class.java)
            val nowDefault = rm?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
            isDefault = nowDefault
            if (nowDefault) onDismiss()
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
                                onDismiss()
                            }
                        } else {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            context.startActivity(intent)
                        }
                    } catch (_: Exception) { }
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
    }
}
