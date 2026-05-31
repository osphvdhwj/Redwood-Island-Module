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
fun AdvancedTriggersScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Dynamic Ring Visibility")
        SettingsSwitch(
            title = "Media Ring", 
            description = "Circular progress for active music/video.", 
            icon = Icons.Default.MusicNote,
            checked = prefs.getBoolean("ring_media_visible", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("ring_media_visible", it) }) }
        )
        SettingsSwitch(
            title = "Battery Ring", 
            description = "Persistent battery level indicator.", 
            icon = Icons.Default.BatteryChargingFull,
            checked = prefs.getBoolean("ring_battery_visible", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("ring_battery_visible", it) }) }
        )

        SettingsCategoryHeader("Floating Windows (Freeform)")
        SettingsSwitch(
            title = "Enable Freeform Launch", 
            description = "Open apps in floating windows (requires Developer Options -> Freeform Windows).", 
            icon = Icons.Default.OpenInNew,
            checked = prefs.getBoolean("freeform_launch_enabled", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("freeform_launch_enabled", it) }) }
        )
        if (prefs.getBoolean("freeform_launch_enabled", true)) {
            SettingsSwitch(
                title = "Smart Gesture Launch", 
                description = "Swipe down on a notification/music pill to open in freeform.", 
                checked = prefs.getBoolean("freeform_smart_gesture", true),
                onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("freeform_smart_gesture", it) }) }
            )
        }

        SettingsCategoryHeader("State Constraint Engine")
        Text(
            text = "Select which Island sizes are allowed for each event type. Island will automatically downgrade to the next safe size if disabled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        SettingsExpander(title = "Music States", icon = Icons.Default.MusicNote) {
            SettingsSwitch("Allow Mid Pill", "Standard media controls", prefs.getBoolean("allow_music_mid", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_music_mid", it) } 
            }
            SettingsSwitch("Allow Max Pill", "Expanded album art & controls", prefs.getBoolean("allow_music_max", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_music_max", it) } 
            }
        }

        SettingsExpander(title = "Charging States", icon = Icons.Default.BatteryChargingFull) {
            SettingsSwitch("Allow Mini Pill", "Small battery percentage", prefs.getBoolean("allow_charging_mini", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_charging_mini", it) } 
            }
            SettingsSwitch("Allow Mid Pill", "Standard charging info", prefs.getBoolean("allow_charging_mid", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_charging_mid", it) } 
            }
        }

        SettingsExpander(title = "Notification States", icon = Icons.Default.Notifications) {
            SettingsSwitch("Allow Mini Pill", "App icon only", prefs.getBoolean("allow_notif_mini", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_notif_mini", it) } 
            }
            SettingsSwitch("Allow Mid Pill", "Icon + Content text", prefs.getBoolean("allow_notif_mid", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_notif_mid", it) } 
            }
            SettingsSwitch("Allow Max Pill", "Full notification stack", prefs.getBoolean("allow_notif_max", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_notif_max", it) } 
            }
        }

        SettingsCategoryHeader("OLED Protection")
        SettingsSwitch(
            title = "Anti Burn-In Shifter", 
            checked = prefs.getBoolean("anti_burn_in_enabled", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("anti_burn_in_enabled", it) }) }
        )
        if (prefs.getBoolean("anti_burn_in_enabled", true)) {
            SettingsSlider(
                title = "Shift Intensity", 
                value = prefs.getFloat("anti_burn_in_intensity", 1.5f), 
                defaultValue = 1.5f,
                valueRange = 1f..3f,
                onValueChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("anti_burn_in_intensity", it) }) }
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}
