package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuityScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val haptics = rememberHapticManager()

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        NewConfigManager.broadcastUpdateSingle(context, prefs, "continuity") 
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StaggeredItem(0) { 
                SectionHeader(
                    title = "Ecosystem Continuity", 
                    subtitle = "Cross-device & background services", 
                    icon = Icons.Default.Hub, 
                    accentColor = IslandColors.accentCyan
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(1) {
                SettingsGroup(
                    title = "Apple Ecosystem", 
                    icon = Icons.Default.Devices, 
                    summary = "AirPods & iOS logic"
                ) {
                    ContinuityToggle(
                        "AirPods Pop-up", 
                        "Simulates the Apple AirPods connection card in the Dynamic Island when headphones are detected.", 
                        Icons.Default.Headset,
                        SettingKey.AIRPODS_POPUP, prefs, haptics
                    )
                    ContinuityToggle(
                        "Face ID Padlock", 
                        "Shows a smoothiOS-style padlock animation in the pill when biometric authentication is triggered.", 
                        Icons.Default.Lock,
                        SettingKey.FACE_ID_PADLOCK, prefs, haptics
                    )
                    ContinuityToggle(
                        "MagSafe Charging", 
                        "Displays the circular MagSafe charging ring and percentage when a power source is connected.", 
                        Icons.Default.ChargingStation,
                        SettingKey.MAGSAFE_CHARGING_ANIMATION, prefs, haptics
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(2) {
                SettingsGroup(
                    title = "Network Continuity", 
                    icon = Icons.Default.NetworkCheck, 
                    summary = "Remote & Sync"
                ) {
                    ContinuityToggle(
                        "Clipboard Sync", 
                        "Instantly syncs copied text to the Island of your other logged-in devices.", 
                        Icons.Default.Assignment,
                        SettingKey.CLIPBOARD_SYNC, prefs, haptics
                    )
                    ContinuityToggle(
                        "Wear OS Remote", 
                        "Allows your smartwatch to trigger Island actions like 'Next Track' or 'Expand'.", 
                        Icons.Default.Watch,
                        SettingKey.WEAR_OS_REMOTE, prefs, haptics
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(3) {
                SettingsGroup(
                    title = "Ambient Media", 
                    icon = Icons.Default.MusicNote, 
                    summary = "Background Listening"
                ) {
                    ContinuityToggle(
                        "Artwork Backdrop", 
                        "The Island's background color dynamically blurs to match the current music's album art.", 
                        Icons.Default.Brush,
                        SettingKey.MEDIA_ARTWORK_BLUR, prefs, haptics
                    )
                    ContinuityToggle(
                        "Now Playing AI", 
                        "Continuously listens for ambient music and shows the track title in a small 'Ring' state.", 
                        Icons.Default.Hearing,
                        SettingKey.NOW_PLAYING, prefs, haptics
                    )
                    ContinuityToggle(
                        "Waveform Visualizer", 
                        "Adds an audio-reactive wavy seeker bar inside the 'Mid' and 'Max' expanded states.", 
                        Icons.Default.Waves,
                        SettingKey.WAVEFORM_ENABLED, prefs, haptics
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ContinuityToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    key: SettingKey,
    prefs: SharedPreferences,
    haptics: HapticManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(prefs.getBoolean(key.name, true)) }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        FeatureSwitch(
            title = title, 
            description = description, 
            checked = checked, 
            onCheckedChange = { 
                if (it) haptics.toggleOn() else haptics.toggleOff()
                checked = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean(key.name, it) }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "continuity")
                }
            },
            accentColor = IslandColors.accentCyan,
            icon = icon
        )
    }
}
