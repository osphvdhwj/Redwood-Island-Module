package com.example.dynamicisland.core.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.manager.ConfigBackupManager
import com.example.dynamicisland.core.settings.SettingsManager.SettingKey
import com.example.dynamicisland.core.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.core.ui.components.SettingsSwitch
import kotlinx.coroutines.launch
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Core Engine")
        
        SettingsSwitch(
            title = "Island Master Switch", 
            description = "Enable or disable all Dynamic Island features.", 
            icon = Icons.Default.PowerSettingsNew,
            checked = state.islandEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.ISLAND_ENABLED, it) }
        )
            title = "AOD Persistence", 
            description = "Keep the Island visible on the Always-On Display.", 
            icon = Icons.Default.BrightnessLow,
            checked = state.islandOnLockscreen, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.ISLAND_ON_LOCKSCREEN, it) }
            title = "Simultaneous Multitasking", 
            description = "Allow two activities to share the Island (Split mode).", 
            icon = Icons.Default.CompareArrows,
            checked = state.splitPillEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.SPLIT_PILL_ENABLED, it) }
            title = "Tactile Response", 
            description = "Haptic feedback on transitions and interactions.", 
            icon = Icons.Default.Vibration,
            checked = state.hapticFeedback, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.HAPTIC_FEEDBACK, it) }
        SettingsCategoryHeader("Intelligence & Automation")
            title = "Secure OTP Detection", 
            description = "Intercept 2FA codes from messages and show them.", 
            icon = Icons.Default.VpnKey,
            checked = state.otpDetection, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.OTP_DETECTION, it) }
            title = "Navigation Hijacker", 
            description = "Show real-time Google Maps ticker in the Island.", 
            icon = Icons.Default.Explore,
            checked = state.navigation, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.NAVIGATION, it) }
            title = "Auto-Translation", 
            description = "On-the-fly translation of notification text.", 
            icon = Icons.Default.Translate,
            checked = state.translation, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.TRANSLATION, it) }
        SettingsCategoryHeader("Media Intelligence")
            title = "Adaptive Waveform", 
            description = "Visualized playback progress for active music.", 
            icon = Icons.Default.GraphicEq,
            checked = state.waveformEnabled, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.WAVEFORM_ENABLED, it) }
            title = "Context-Aware Blur", 
            description = "Sample album art colors for Island background.", 
            icon = Icons.Default.Palette,
            checked = state.mediaArtworkBlur, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.MEDIA_ARTWORK_BLUR, it) }
            title = "Beat Synchronization", 
            description = "Island pulse intensity based on audio tempo.", 
            icon = Icons.Default.MusicNote,
            checked = state.bpmPulse, 
            onCheckedChange = { viewModel.updateSetting(SettingKey.BPM_PULSE, it) }
        SettingsCategoryHeader("System Maintenance")
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { viewModel.resetAll() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Factory Reset Settings")
            }
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}
