package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.ui.components.SettingsSwitch
import kotlinx.coroutines.launch

@Composable
fun ContinuityScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Continuity Experience")
        
        MD3ContinuityToggle(
            title = "AirPods Bridge", 
            description = "iOS-style connection card for AirPods and Beats.", 
            icon = Icons.Default.Headset,
            key = SettingKey.AIRPODS_POPUP, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Face ID Engine", 
            description = "Unlock animation synced with biometric auth.", 
            icon = Icons.Default.Lock,
            key = SettingKey.FACE_ID_PADLOCK, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "MagSafe Charging", 
            description = "Circular charging ring with haptic pulse.", 
            icon = Icons.Default.ChargingStation,
            key = SettingKey.MAGSAFE_CHARGING_ANIMATION, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("Immersive Media")
        
        MD3ContinuityToggle(
            title = "Artwork Canvas", 
            description = "Blur the Island backdrop with media album art colors.", 
            icon = Icons.Default.Brush,
            key = SettingKey.MEDIA_ARTWORK_BLUR, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Waveform Visualizer", 
            description = "Adaptive wavy seeker for active playback.", 
            icon = Icons.Default.Waves,
            key = SettingKey.WAVEFORM_ENABLED, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Beat Pulse", 
            description = "Sync the entire Island with audio tempo.", 
            icon = Icons.Default.Hearing,
            key = SettingKey.BPM_PULSE, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun MD3ContinuityToggle(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    key: SettingKey,
    prefs: SharedPreferences,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context
) {
    var checked by remember { mutableStateOf(prefs.getBoolean(key.name, true)) }
    
    SettingsSwitch(
        title = title, 
        description = description, 
        checked = checked, 
        onCheckedChange = { 
            checked = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean(key.name, it) }) {
                NewConfigManager.broadcastUpdateSingle(context, prefs, "continuity")
            }
        },
        icon = icon
    )
}