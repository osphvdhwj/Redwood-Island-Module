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
        SettingsCategoryHeader("Apple Ecosystem")
        
        MD3ContinuityToggle(
            title = "AirPods Pop-up", 
            description = "Simulates the Apple AirPods connection card.", 
            icon = Icons.Default.Headset,
            key = SettingKey.AIRPODS_POPUP, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Face ID Padlock", 
            description = "Shows a smooth iOS-style padlock animation.", 
            icon = Icons.Default.Lock,
            key = SettingKey.FACE_ID_PADLOCK, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "MagSafe Charging", 
            description = "Displays the circular MagSafe charging ring.", 
            icon = Icons.Default.ChargingStation,
            key = SettingKey.MAGSAFE_CHARGING_ANIMATION, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("Network Continuity")
        
        MD3ContinuityToggle(
            title = "Clipboard Sync", 
            description = "Syncs copied text across devices.", 
            icon = Icons.Default.Assignment,
            key = SettingKey.CLIPBOARD_SYNC, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Wear OS Remote", 
            description = "Allows smartwatch to trigger Island actions.", 
            icon = Icons.Default.Watch,
            key = SettingKey.WEAR_OS_REMOTE, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("Ambient Media")
        
        MD3ContinuityToggle(
            title = "Artwork Backdrop", 
            description = "Blurs background to match album art.", 
            icon = Icons.Default.Brush,
            key = SettingKey.MEDIA_ARTWORK_BLUR, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Now Playing AI", 
            description = "Listens for ambient music.", 
            icon = Icons.Default.Hearing,
            key = SettingKey.NOW_PLAYING, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3ContinuityToggle(
            title = "Waveform Visualizer", 
            description = "Adds an audio-reactive wavy seeker bar.", 
            icon = Icons.Default.Waves,
            key = SettingKey.WAVEFORM_ENABLED, 
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