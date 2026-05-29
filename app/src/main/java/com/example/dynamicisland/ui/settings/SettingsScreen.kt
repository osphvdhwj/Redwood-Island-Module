package com.example.dynamicisland.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.dynamicisland.manager.ConfigBackupManager
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.settings.SettingsViewModel
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.ui.components.SettingsSwitch
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("island_prefs", android.content.Context.MODE_PRIVATE)

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = ConfigBackupManager.exportConfig(context, prefs, uri)
                Toast.makeText(context, if (success) "Configuration Exported" else "Export Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val success = ConfigBackupManager.importConfig(context, prefs, uri)
                Toast.makeText(context, if (success) "Configuration Imported! Restarting Engine..." else "Import Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Core Engine")
        
        SettingsSwitch(
            title = "Engine Master", 
            description = "Enable or disable all Dynamic Island features.", 
            icon = Icons.Default.PowerSettingsNew,
            checked = state.islandEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.ISLAND_ENABLED, it) }
        )
        SettingsSwitch(
            title = "Lockscreen Visibility", 
            description = "Keep the Island active on the Always-On Display.", 
            icon = Icons.Default.Lock,
            checked = state.islandOnLockscreen, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.ISLAND_ON_LOCKSCREEN, it) }
        )
        SettingsSwitch(
            title = "Multitasking (Split)", 
            description = "Allow two activities to share the Island simultaneously.", 
            icon = Icons.Default.VerticalSplit,
            checked = state.splitPillEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.SPLIT_PILL_ENABLED, it) }
        )
        SettingsSwitch(
            title = "Haptic Feedback", 
            description = "Vibrate on transitions and interactions.", 
            icon = Icons.Default.Vibration,
            checked = state.hapticFeedback, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.HAPTIC_FEEDBACK, it) }
        )

        SettingsCategoryHeader("Automation & Continuity")
        
        SettingsSwitch(
            title = "OTP Auto-Capture", 
            description = "Intercept 2FA codes from incoming messages.", 
            icon = Icons.Default.VpnKey,
            checked = state.otpDetection, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.OTP_DETECTION, it) }
        )
        SettingsSwitch(
            title = "AirPods Bridge", 
            description = "iOS-style connection card for AirPods and Beats.", 
            icon = Icons.Default.Headset,
            checked = state.airpodsPopup, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.AIRPODS_POPUP, it) }
        )
        SettingsSwitch(
            title = "Navigation HUD", 
            description = "Native Google Maps instruction ticker.", 
            icon = Icons.Default.Navigation,
            checked = state.navigation, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.NAVIGATION, it) }
        )
        SettingsSwitch(
            title = "Face ID Engine", 
            description = "Unlock animation synced with biometric auth.", 
            icon = Icons.Default.LockOpen,
            checked = state.faceIDPadlock, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.FACE_ID_PADLOCK, it) }
        )
        SettingsSwitch(
            title = "MagSafe Charging", 
            description = "Circular charging ring with haptic pulse.", 
            icon = Icons.Default.ChargingStation,
            checked = state.magsafeChargingAnimation, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.MAGSAFE_CHARGING_ANIMATION, it) }
        )

        SettingsCategoryHeader("Media Hub")
        
        SettingsSwitch(
            title = "Waveform Seeker", 
            description = "Visualized playback bar for active music.", 
            icon = Icons.Default.GraphicEq,
            checked = state.waveformEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.WAVEFORM_ENABLED, it) }
        )
        SettingsSwitch(
            title = "Artwork Canvas", 
            description = "Immersive background blur from album art.", 
            icon = Icons.Default.Album,
            checked = state.mediaArtworkBlur, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.MEDIA_ARTWORK_BLUR, it) }
        )
        SettingsSwitch(
            title = "Beat Pulse", 
            description = "Sync Island expansion with audio tempo.", 
            icon = Icons.Default.Hearing,
            checked = state.bpmPulse, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.BPM_PULSE, it) }
        )

        SettingsCategoryHeader("Maintenance")

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = { exportLauncher.launch("redwood_config.json") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Export Config")
                }
                FilledTonalButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Import Config")
                }
            }

            Button(
                onClick = { viewModel.resetAll() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text("Reset All Settings")
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}