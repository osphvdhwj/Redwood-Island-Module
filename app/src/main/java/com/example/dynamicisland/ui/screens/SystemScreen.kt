package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.ConfigManager
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val haptics = rememberHapticManager()

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        ConfigManager.broadcastUpdateSingle(context, prefs, "system") 
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StaggeredItem(0) { 
                SectionHeader(
                    title = "System & Advanced", 
                    subtitle = "Performance, security & tools", 
                    icon = Icons.Default.Settings, 
                    accentColor = IslandColors.accentCyan
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(1) {
                SettingsGroup(
                    title = "Interaction", 
                    icon = Icons.Default.Check, 
                    summary = "Tactile Feel"
                ) {
                    SystemToggle("Haptic Feedback", "Vibrate on interactions", SettingKey.HAPTIC_FEEDBACK, prefs, haptics)
                    SystemToggle("One-Hand Mode", "Shift island lower", SettingKey.ONE_HAND_MODE, prefs, haptics)
                    
                    var hapticIntensity by remember { mutableFloatStateOf(prefs.getFloat(SettingKey.HAPTIC_INTENSITY.name, 1f)) }
                    ThemeSlider(
                        label = "Haptic Intensity", 
                        value = hapticIntensity, 
                        valueRange = 0.1f..1.0f,
                        onValueChange = { 
                            hapticIntensity = it
                            prefs.edit().putFloat(SettingKey.HAPTIC_INTENSITY.name, it).apply()
                            haptics.light()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(2) {
                SettingsGroup(
                    title = "Performance", 
                    icon = Icons.Default.Build, 
                    summary = "Efficiency"
                ) {
                    SystemToggle("Battery-Aware Animation", "Disable effects <15% battery", SettingKey.BATTERY_AWARE_ANIMATION, prefs, haptics)
                    SystemToggle("Doze Mode Optimization", "Reduce wakeups in standby", SettingKey.DOZE_MODE_OPTIMISATION, prefs, haptics)
                    SystemToggle("Quick Performance Profile", "Prioritize UI smoothness", SettingKey.QUICK_PERFORMANCE_PROFILE, prefs, haptics)
                    SystemToggle("Data Saver", "Disable blur on mobile data", SettingKey.DATA_SAVER, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(3) {
                SettingsGroup(
                    title = "Privacy & Security", 
                    icon = Icons.Default.Lock, 
                    summary = "Data Protection"
                ) {
                    SystemToggle("Clipboard Cleaner", "Clear sensitive text after timeout", SettingKey.CLIPBOARD_CLEANER, prefs, haptics)
                    SystemToggle("VPN / Tor Indicator", "Show connection status in island", SettingKey.VPN_TOR_INDICATOR, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(4) {
                SettingsGroup(
                    title = "Developer Tools", 
                    icon = Icons.Default.Build, 
                    summary = "Advanced Control"
                ) {
                    SystemToggle("ADB Command Injector", "Control via shell commands", SettingKey.ADB_COMMAND_INJECTOR, prefs, haptics)
                    SystemToggle("Tasker Plugin", "Automate island with Tasker", SettingKey.TASKER_PLUGIN, prefs, haptics)
                    SystemToggle("Open Source SDK", "Enable community components", SettingKey.OPEN_SOURCE_SDK, prefs, haptics)
                    SystemToggle("Log Overlay", "Show debug stats in real-time", SettingKey.LOG_DEBUG_OVERLAY, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            StaggeredItem(5) {
                NeonButton(
                    text = "System Accessibility Settings",
                    onClick = {
                        haptics.medium()
                        context.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun SystemToggle(
    title: String,
    description: String,
    key: SettingKey,
    prefs: SharedPreferences,
    haptics: HapticManager
) {
    var checked by remember { mutableStateOf(prefs.getBoolean(key.name, true)) }
    FeatureSwitch(
        title = title, 
        description = description, 
        checked = checked, 
        onCheckedChange = { 
            if (it) haptics.toggleOn() else haptics.toggleOff()
            checked = it
            prefs.edit().putBoolean(key.name, it).apply()
        },
        accentColor = IslandColors.accentCyan
    )
}
