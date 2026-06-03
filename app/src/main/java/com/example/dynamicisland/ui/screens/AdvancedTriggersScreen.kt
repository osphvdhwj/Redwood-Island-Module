package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
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
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ring_media_visible", value) }) 
            }
        )
        SettingsSwitch(
            title = "Battery Ring", 
            description = "Persistent battery level indicator.", 
            icon = Icons.Default.BatteryChargingFull,
            checked = prefs.getBoolean("ring_battery_visible", true),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ring_battery_visible", value) }) 
            }
        )

        SettingsCategoryHeader("DeGoogled Bridge (Pillar 6)")
        SettingsSwitch(
            title = "Assistant Interceptor",
            description = "Route 'Assist' gesture to Brave Browser via Island Aura.",
            icon = Icons.Default.Assistant,
            checked = prefs.getBoolean("assistBridgeEnabled", false),
            onCheckedChange = { value ->
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("assistBridgeEnabled", value) })
            }
        )
        if (prefs.getBoolean("assistBridgeEnabled", false)) {
            SettingsMenuLink(
                title = "Assistant Target",
                description = prefs.getString("assistBridgeTarget", "com.brave.browser") ?: "com.brave.browser",
                icon = Icons.Default.Link,
                onClick = {
                    Toast.makeText(context, "App selection not implemented yet.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        SettingsCategoryHeader("App Visibility Rules")
        SettingsMenuLink(
            title = "Hide Island Per App",
            description = "Select apps where the module should be completely hidden.",
            icon = Icons.Default.VisibilityOff,
            onClick = {
                Toast.makeText(context, "Hidden apps picker not implemented.", Toast.LENGTH_SHORT).show()
            }
        )
        
        SettingsSwitch(
            title = "Hide on Screenshot",
            description = "Automatically hide the Island when taking a screenshot.",
            checked = prefs.getBoolean("HIDE_ON_SCREENSHOT", true),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("HIDE_ON_SCREENSHOT", value) }) 
            }
        )
        SettingsSwitch(
            title = "Hide on Screen Record",
            description = "Automatically hide the Island during screen recording.",
            checked = prefs.getBoolean("HIDE_ON_SCREEN_RECORD", true),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("HIDE_ON_SCREEN_RECORD", value) }) 
            }
        )

        SettingsCategoryHeader("Floating Windows (Freeform)")
        SettingsSwitch(
            title = "Enable Freeform Launch", 
            description = "Open apps in floating windows (requires Developer Options).", 
            icon = Icons.Default.OpenInNew,
            checked = prefs.getBoolean("freeform_launch_enabled", true),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("freeform_launch_enabled", value) }) 
            }
        )
        if (prefs.getBoolean("freeform_launch_enabled", true)) {
            SettingsSwitch(
                title = "Portal Animation", 
                description = "Show a HyperOS-style portal effect when launching freeform.", 
                checked = prefs.getBoolean("ENABLE_FREEFORM_PORTAL_ANIM", true),
                onCheckedChange = { value -> 
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ENABLE_FREEFORM_PORTAL_ANIM", value) }) 
                }
            )
        }

        SettingsCategoryHeader("State Constraint Engine")
        SettingsExpander(title = "Music States", icon = Icons.Default.MusicNote) {
            SettingsSwitch(
                title = "Allow Mid Pill", 
                description = "Standard media controls", 
                checked = prefs.getBoolean("allow_music_mid", true),
                onCheckedChange = { value ->
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("allow_music_mid", value) })
                }
            )
            SettingsSwitch(
                title = "Allow Max Pill", 
                description = "Expanded album art & controls", 
                checked = prefs.getBoolean("allow_music_max", true),
                onCheckedChange = { value ->
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("allow_music_max", value) })
                }
            )
        }

        SettingsCategoryHeader("Island Neural Core (AI)")
        SettingsMenuLink(
            title = "Clear AI Memory",
            description = "Reset learned behavioral patterns.",
            icon = Icons.Default.DeleteSweep,
            onClick = {
                val ok = viewModel.clearAiMemory()
                if (ok) Toast.makeText(context, "AI Memory Cleared", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "AI Data copied to clipboard!", Toast.LENGTH_SHORT).show()
                }
            }
        )

        SettingsCategoryHeader("OLED Protection")
        SettingsSwitch(
            title = "Anti Burn-In Shifter", 
            checked = prefs.getBoolean("anti_burn_in_enabled", true),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("anti_burn_in_enabled", value) }) 
            }
        )
        if (prefs.getBoolean("anti_burn_in_enabled", true)) {
            SettingsSlider(
                title = "Shift Intensity", 
                value = prefs.getFloat("anti_burn_in_intensity", 1.5f), 
                defaultValue = 1.5f,
                valueRange = 1f..3f,
                onValueChange = { value -> 
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putFloat("anti_burn_in_intensity", value) }) 
                }
            )
        }

        SettingsCategoryHeader("Performance")
        SettingsMenuLink(
            title = "Optimize Battery Usage",
            description = "Ensure the engine stays active in the background.",
            icon = Icons.Default.BatteryChargingFull,
            onClick = {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )
        SettingsSwitch(
            title = "Low-Latency Mode",
            description = "Disable blur and heavy physics for maximum FPS.",
            icon = Icons.Default.Speed,
            checked = prefs.getBoolean("ENABLE_LOW_LATENCY_MODE", false),
            onCheckedChange = { value -> 
                NewConfigManager.commitAndBroadcast(prefs, scope, context, editBlock = { putBoolean("ENABLE_LOW_LATENCY_MODE", value) }) 
            }
        )

        Spacer(Modifier.height(120.dp))
    }
}
