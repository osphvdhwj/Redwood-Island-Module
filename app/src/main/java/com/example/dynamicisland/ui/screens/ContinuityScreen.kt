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
import com.example.dynamicisland.manager.ConfigManager
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuityScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val haptics = rememberHapticManager()

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        ConfigManager.broadcastUpdateSingle(context, prefs, "continuity") 
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StaggeredItem(0) { 
                SectionHeader(
                    title = "Continuity & Ambient", 
                    subtitle = "Ecosystem & background experiences", 
                    icon = Icons.Default.Home, 
                    accentColor = IslandColors.accentCyan
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(1) {
                SettingsGroup(
                    title = "Cross-Device", 
                    icon = Icons.Default.Home, 
                    summary = "Clipboard & Accessories"
                ) {
                    ContinuityToggle("Clipboard Sync", "Sync text between devices", SettingKey.CLIPBOARD_SYNC, prefs, haptics)
                    ContinuityToggle("AirPods Pop-up", "Apple-style connection card", SettingKey.AIRPODS_POPUP, prefs, haptics)
                    ContinuityToggle("Wear OS Remote", "Control Island from watch", SettingKey.WEAR_OS_REMOTE, prefs, haptics)
                    ContinuityToggle("Universal Control", "Mouse & Keyboard sharing", SettingKey.UNIVERSAL_CONTROL, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(2) {
                SettingsGroup(
                    title = "iOS-Inspired", 
                    icon = Icons.Default.Phone, 
                    summary = "Classic interactions"
                ) {
                    ContinuityToggle("Live Activities API", "Enable third-party tracking", SettingKey.LIVE_ACTIVITIES_API, prefs, haptics)
                    ContinuityToggle("Face ID Padlock", "Unlock animation in island", SettingKey.FACE_ID_PADLOCK, prefs, haptics)
                    ContinuityToggle("MagSafe Animation", "Ring charging effect", SettingKey.MAGSAFE_CHARGING_ANIMATION, prefs, haptics)
                    ContinuityToggle("Always-On Companion", "Minimal island on AOD", SettingKey.ALWAYS_ON_DISPLAY_COMPANION, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(3) {
                SettingsGroup(
                    title = "Media & Audio", 
                    icon = Icons.Default.PlayArrow, 
                    summary = "Ambient listening"
                ) {
                    ContinuityToggle("Artwork Blur", "Pill background follows music", SettingKey.MEDIA_ARTWORK_BLUR, prefs, haptics)
                    ContinuityToggle("Now Playing", "Ambient song recognition", SettingKey.NOW_PLAYING, prefs, haptics)
                    ContinuityToggle("Waveform Seeker", "Visualized seeker bar", SettingKey.WAVEFORM_ENABLED, prefs, haptics)
                    ContinuityToggle("Live Caption", "Speech-to-text overlay", SettingKey.LIVE_CAPTION, prefs, haptics)
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
