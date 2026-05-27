package com.example.dynamicisland.ui.settings

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
import com.example.dynamicisland.settings.SettingsManager.SettingKey
import com.example.dynamicisland.settings.SettingsViewModel
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dynamicisland.manager.ConfigBackupManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.state
    val context = LocalContext.current
    val haptics = rememberHapticManager()
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

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        // No-op for now, could reload state
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                StaggeredItem(0) { 
                    SectionHeader(
                        title = "System Configuration", 
                        subtitle = "Global island controls", 
                        icon = Icons.Default.Settings, 
                        accentColor = IslandColors.accentCyan
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                StaggeredItem(1) {
                    SettingsGroup(
                        title = "Core Engine", 
                        icon = Icons.Default.FlashOn, 
                        summary = "Master toggles"
                    ) {
                        PremiumSettingToggle(
                            title = "Engine Master", 
                            desc = "Enable/Disable Dynamic Island", 
                            checked = state.islandEnabled, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.ISLAND_ENABLED, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "AOD Visibility", 
                            desc = "Show on Lockscreen", 
                            checked = state.islandOnLockscreen, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.ISLAND_ON_LOCKSCREEN, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Multitasking", 
                            desc = "Split Pill Mode support", 
                            checked = state.splitPillEnabled, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.SPLIT_PILL_ENABLED, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Tactile Feedback", 
                            desc = "System-wide haptics", 
                            checked = state.hapticFeedback, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.HAPTIC_FEEDBACK, it) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredItem(2) {
                    SettingsGroup(
                        title = "Media Hub", 
                        icon = Icons.Default.MusicNote, 
                        summary = "Audio reactivity"
                    ) {
                        PremiumSettingToggle(
                            title = "Waveform Seeker", 
                            desc = "Visualized playback bar", 
                            checked = state.waveformEnabled, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.WAVEFORM_ENABLED, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Artwork Blur", 
                            desc = "Immersive media backdrop", 
                            checked = state.mediaArtworkBlur, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.MEDIA_ARTWORK_BLUR, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Ambient Ring", 
                            desc = "Audio-reactive border", 
                            checked = state.ambientReactiveRing, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.AMBIENT_REACTIVE, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Beat Pulse", 
                            desc = "Sync island with BPM", 
                            checked = state.bpmPulse, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.BPM_PULSE, it) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredItem(3) {
                    SettingsGroup(
                        title = "Advanced Labs", 
                        icon = Icons.Default.Science, 
                        summary = "Beta features"
                    ) {
                        PremiumSettingToggle(
                            title = "Gaming HUD", 
                            desc = "FPS & Thermal monitoring", 
                            checked = state.gamingHud, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.GAMING_HUD, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "iOS Padlock", 
                            desc = "Face ID style unlock", 
                            checked = state.faceIDPadlock, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.FACE_ID_PADLOCK, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "Continuity Cam", 
                            desc = "Pro camera integration", 
                            checked = state.continuityCameraActions, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.CONTINUITY_CAMERA_ACTIONS, it) 
                            }
                        )
                        PremiumSettingToggle(
                            title = "MagSafe Anim", 
                            desc = "Premium charging visuals", 
                            checked = state.magsafeChargingAnimation, 
                            onCheckedChange = { 
                                if (it) haptics.toggleOn() else haptics.toggleOff()
                                viewModel.updateSetting(SettingKey.MAGSAFE_CHARGING_ANIMATION, it) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredItem(4) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            NeonButton(
                                text = "Export Config",
                                onClick = {
                                    haptics.medium()
                                    exportLauncher.launch("redwood_config.json")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            NeonButton(
                                text = "Import Config",
                                onClick = {
                                    haptics.medium()
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        NeonButton(
                            text = "Reset All Settings",
                            onClick = {
                                haptics.heavy()
                                viewModel.resetAll()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun PremiumSettingToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Re-use FeatureSwitch to match the aesthetic of the rest of the app
    FeatureSwitch(
        title = title,
        description = desc,
        checked = checked,
        onCheckedChange = onCheckedChange,
        accentColor = IslandColors.accentCyan
    )
}
