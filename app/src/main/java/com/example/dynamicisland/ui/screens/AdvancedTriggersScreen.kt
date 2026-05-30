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
import com.example.dynamicisland.settings.SettingsManager.SettingKey

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
        SettingsSwitch(
            title = "Network Ring", 
            description = "Speed & data usage ring animations.", 
            icon = Icons.Default.Wifi,
            checked = prefs.getBoolean("ring_data_visible", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("ring_data_visible", it) }) }
        )

        SettingsCategoryHeader("Precision Touch & Interaction")
        SettingsSwitch(
            title = "Touch Pass-Through", 
            description = "Allow tapping behind the Island when it's idle.", 
            icon = Icons.Default.TouchApp,
            checked = prefs.getBoolean("invisible_ring_touch_passthrough", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("invisible_ring_touch_passthrough", it) }) }
        )

        SettingsCategoryHeader("OLED Protection")
        SettingsSwitch(
            title = "Anti Burn-In Shifter", 
            description = "Randomly shifts pixels every 60s to save your screen.", 
            icon = Icons.Default.Shield,
            checked = prefs.getBoolean("anti_burn_in_enabled", true),
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("anti_burn_in_enabled", it) }) }
        )
        
        if (prefs.getBoolean("anti_burn_in_enabled", true)) {
            SettingsSlider(
                title = "Shift Intensity", 
                description = "Maximum pixel offset (1-3px recommended).",
                value = prefs.getFloat("anti_burn_in_intensity", 1.5f), 
                defaultValue = 1.5f,
                valueRange = 1f..3f,
                onValueChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("anti_burn_in_intensity", it) }) }
            )
        }

        SettingsCategoryHeader("Connection Alerts")
        SettingsSlider(
            title = "Hotspot Alert Duration", 
            description = "Display time for device connections.",
            value = prefs.getInt("hotspot_alert_duration", 5).toFloat(), 
            defaultValue = 5f,
            valueRange = 1f..10f,
            onValueChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putInt("hotspot_alert_duration", it.toInt()) }) }
        )
        SettingsSlider(
            title = "WiFi/BT Duration", 
            description = "Pill visibility for standard connectivity.",
            value = prefs.getInt("wifi_alert_duration", 3).toFloat(), 
            defaultValue = 3f,
            valueRange = 1f..10f,
            onValueChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putInt("wifi_alert_duration", it.toInt()) }) }
        )

        Spacer(Modifier.height(120.dp))
    }
}
