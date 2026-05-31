package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.*

@Composable
fun FocusModeScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Focus Engine")
        
        SettingsSwitch(
            title = "Enable Focus Mode",
            description = "Intelligently filter notifications when using productive apps.",
            icon = Icons.Default.FilterCenterFocus,
            checked = prefs.getBoolean("ENABLE_FOCUS_MODE", false),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("ENABLE_FOCUS_MODE", it) } }
        )

        if (prefs.getBoolean("ENABLE_FOCUS_MODE", false)) {
            SettingsCategoryHeader("Productivity Workspace")
            Text(
                text = "When any of these apps are in the foreground, the Island will suppress non-essential social/media alerts to keep you focused.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            // Re-using AppPicker logic if available, or just a simple multi-select
            SettingsMenuLink(
                title = "Select Productive Apps",
                description = "Define your work/study apps.",
                icon = Icons.Default.AppRegistration,
                onClick = {
                    // Trigger AppPickerActivity with Focus role
                    val intent = android.content.Intent(context, AppPickerActivity::class.java).apply {
                        putExtra("role", "focus")
                    }
                    context.startActivity(intent)
                }
            )
            
            SettingsCategoryHeader("Focus Rules")
            SettingsSwitch(
                title = "Allow Calls",
                description = "Always show incoming calls even in focus mode.",
                checked = true, // Force on for safety for now
                onCheckedChange = {}
            )
            SettingsSwitch(
                title = "Allow Critical Alerts",
                description = "Show low battery and OTP detections.",
                checked = true,
                onCheckedChange = {}
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}
