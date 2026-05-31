package com.example.dynamicisland.ui.screens

import android.content.Context
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
import com.example.dynamicisland.settings.SettingsViewModel

@Composable
fun AdvancedTriggersScreen(prefs: SharedPreferences, viewModel: SettingsViewModel) {
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

        SettingsCategoryHeader("App Visibility Rules")
        SettingsMenuLink(
            title = "Hide Island Per App",
            description = "Select apps where the module should be completely hidden.",
            icon = Icons.Default.VisibilityOff,
            onClick = {
                // In a real implementation we'd launch AppPicker with 'hide' role
                android.widget.Toast.makeText(context, "Hidden apps picker not implemented.", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
        
        SettingsSwitch(
            title = "Hide on Screenshot",
            description = "Automatically hide the Island when taking a screenshot.",
            checked = prefs.getBoolean("HIDE_ON_SCREENSHOT", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("HIDE_ON_SCREENSHOT", it) } }
        )
        SettingsSwitch(
            title = "Hide on Screen Record",
            description = "Automatically hide the Island during screen recording.",
            checked = prefs.getBoolean("HIDE_ON_SCREEN_RECORD", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("HIDE_ON_SCREEN_RECORD", it) } }
        )

        SettingsCategoryHeader("Floating Windows (Freeform)")
        SettingsSwitch(
            title = "Enable Freeform Launch", 
            description = "Open apps in floating windows (requires Developer Options).", 
            icon = Icons.Default.OpenInNew,
            checked = prefs.getBoolean("freeform_launch_enabled", true),
            onCheckedChange = { value -> NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("freeform_launch_enabled", value) }) }
        )
        if (prefs.getBoolean("freeform_launch_enabled", true)) {
            SettingsSwitch(
                title = "Portal Animation", 
                description = "Show a HyperOS-style portal effect when launching freeform.", 
                checked = prefs.getBoolean("ENABLE_FREEFORM_PORTAL_ANIM", true),
                onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("ENABLE_FREEFORM_PORTAL_ANIM", it) } }
            )
        }

        SettingsCategoryHeader("State Constraint Engine")
        SettingsExpander(title = "Music States", icon = Icons.Default.MusicNote) {
            SettingsSwitch("Allow Mid Pill", "Standard media controls", prefs.getBoolean("allow_music_mid", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_music_mid", it) } 
            }
            SettingsSwitch("Allow Max Pill", "Expanded album art & controls", prefs.getBoolean("allow_music_max", true)) { 
                NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("allow_music_max", it) } 
            }
        }

        SettingsCategoryHeader("Island Neural Core (AI)")
        SettingsMenuLink(
            title = "Clear AI Memory",
            description = "Reset learned behavioral patterns.",
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
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("AI Data", data))
                    android.widget.Toast.makeText(context, "AI Data copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )

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

        SettingsCategoryHeader("Performance")
        SettingsSwitch(
            title = "Low-Latency Mode",
            description = "Disable blur and heavy physics for maximum FPS (Recommended for Redwood).",
            icon = Icons.Default.Speed,
            checked = prefs.getBoolean("ENABLE_LOW_LATENCY_MODE", false),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("ENABLE_LOW_LATENCY_MODE", it) } }
        )

        Spacer(Modifier.height(120.dp))
    }
}
