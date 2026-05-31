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
fun AdvancedTriggersScreen(prefs: SharedPreferences, viewModel: com.example.dynamicisland.settings.SettingsViewModel) {
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
            onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("ring_media_visible", value) }) }
        )
        SettingsSwitch(
            title = "Battery Ring", 
            description = "Persistent battery level indicator.", 
            icon = Icons.Default.BatteryChargingFull,
            checked = prefs.getBoolean("ring_battery_visible", true),
            onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("ring_battery_visible", value) }) }
        )

        SettingsCategoryHeader("Floating Windows (Freeform)")
        SettingsSwitch(
            title = "Enable Freeform Launch", 
            description = "Open apps in floating windows (requires Developer Options -> Freeform Windows).", 
            icon = Icons.Default.OpenInNew,
            checked = prefs.getBoolean("freeform_launch_enabled", true),
            onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("freeform_launch_enabled", value) }) }
        )
        if (prefs.getBoolean("freeform_launch_enabled", true)) {
            SettingsSwitch(
                title = "Smart Gesture Launch", 
                description = "Swipe down on a notification/music pill to open in freeform.", 
                checked = prefs.getBoolean("freeform_smart_gesture", true),
                onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("freeform_smart_gesture", value) }) }
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
            SettingsSwitch("Allow Mid Pill", "Standard media controls", prefs.getBoolean("allow_music_mid", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_music_mid", v) })
            })
            SettingsSwitch("Allow Max Pill", "Expanded album art & controls", prefs.getBoolean("allow_music_max", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_music_max", v) })
            })
        }

        SettingsExpander(title = "Charging States", icon = Icons.Default.BatteryChargingFull) {
            SettingsSwitch("Allow Mini Pill", "Small battery percentage", prefs.getBoolean("allow_charging_mini", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_charging_mini", v) })
            })
            SettingsSwitch("Allow Mid Pill", "Standard charging info", prefs.getBoolean("allow_charging_mid", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_charging_mid", v) })
            })
        }

        SettingsExpander(title = "Notification States", icon = Icons.Default.Notifications) {
            SettingsSwitch("Allow Mini Pill", "App icon only", prefs.getBoolean("allow_notif_mini", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_notif_mini", v) })
            })
            SettingsSwitch("Allow Mid Pill", "Icon + Content text", prefs.getBoolean("allow_notif_mid", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_notif_mid", v) })
            })
            SettingsSwitch("Allow Max Pill", "Full notification stack", prefs.getBoolean("allow_notif_max", true), { v ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("allow_notif_max", v) })
            })
        }

        SettingsCategoryHeader("OLED Protection")
        SettingsSwitch(
            title = "Anti Burn-In Shifter", 
            checked = prefs.getBoolean("anti_burn_in_enabled", true),
            onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("anti_burn_in_enabled", value) }) }
        )
        if (prefs.getBoolean("anti_burn_in_enabled", true)) {
            SettingsSlider(
                title = "Shift Intensity", 
                value = prefs.getFloat("anti_burn_in_intensity", 1.5f), 
                defaultValue = 1.5f,
                valueRange = 1f..3f,
                onValueChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("anti_burn_in_intensity", value) }) }
            )
        }

        SettingsCategoryHeader("Island Neural Core (AI)")
        Text(
            text = "The AI learns your habits over time to predict gestures and optimize performance. You can reset its memory or export the learned data below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        var showExportDialog by remember { mutableStateOf(false) }
        var exportContent by remember { mutableStateOf("") }

        SettingsMenuLink(
            title = "Clear AI Memory",
            description = "Reset all learned behavioral patterns.",
            icon = Icons.Default.DeleteSweep,
            onClick = {
                val ok = viewModel.clearAiMemory()
                if (ok) android.widget.Toast.makeText(context, "AI Memory Cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        )

        SettingsMenuLink(
            title = "Export AI Data",
            description = "Save learned weights as a JSON string.",
            icon = Icons.Default.Share,
            onClick = {
                val data = viewModel.exportAiData()
                if (data != null) {
                    exportContent = data
                    showExportDialog = true
                }
            }
        )

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Exported AI Data") },
                text = {
                    Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        Text(exportContent, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI Data", exportContent))
                        showExportDialog = false
                    }) { Text("Copy to Clipboard") }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) { Text("Close") }
                }
            )
        }

        Spacer(Modifier.height(120.dp))
    }
}
