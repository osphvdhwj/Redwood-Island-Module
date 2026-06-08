package com.example.dynamicisland.core.ui.screens

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
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.manager.IslandBackupManager
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.AppPickerActivity
import com.example.dynamicisland.core.ui.components.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

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
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ENABLE_FOCUS_MODE", value) }) 
            }
        )

        if (prefs.getBoolean("ENABLE_FOCUS_MODE", false)) {
            SettingsCategoryHeader("Productivity Workspace")
            Text(
                text = "When any of these apps are in the foreground, the Island will suppress non-essential social/media alerts to keep you focused.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            SettingsMenuLink(
                title = "Select Productive Apps",
                description = "Define your work/study apps.",
                icon = Icons.Default.AppRegistration,
                onClick = {
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
                checked = true, 
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
