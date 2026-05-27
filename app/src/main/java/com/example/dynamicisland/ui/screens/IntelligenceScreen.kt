package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
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
fun IntelligenceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val haptics = rememberHapticManager()

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        NewConfigManager.broadcastUpdateSingle(context, prefs, "intelligence") 
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                StaggeredItem(0) { 
                    SectionHeader(
                        title = "Redwood Intelligence", 
                        subtitle = "Context-aware island behavior", 
                        icon = Icons.Default.AutoAwesome, 
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
                StaggeredItem(1) {
                    SettingsGroup(
                        title = "Smart Detection", 
                        icon = Icons.Default.Search, 
                        summary = "Active System Hooks"
                    ) {
                        DetectionToggle(
                            title = "OTP Auto-Capture", 
                            description = "Instantly captures verification codes from SMS and displays them in the Island for one-tap copying.", 
                            icon = Icons.Default.VpnKey,
                            key = SettingKey.OTP_DETECTION, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "Link Interceptor", 
                            description = "Detects URLs in clipboard or notifications and provides quick 'Open' or 'Share' actions directly in the pill.", 
                            icon = Icons.Default.Link,
                            key = SettingKey.LINK_INTERCEPT, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "Real-time Barcode", 
                            description = "When the camera is active, the Island will automatically show a snippet of any detected QR codes or barcodes.", 
                            icon = Icons.Default.QrCodeScanner,
                            key = SettingKey.BARCODE, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredItem(2) {
                    SettingsGroup(
                        title = "AI Assistance", 
                        icon = Icons.Default.Memory, 
                        summary = "Predictive Overlays"
                    ) {
                        DetectionToggle(
                            title = "Live Translation", 
                            description = "Translates foreign text on screen in real-time and flows the result into a 'Mid' state pill.", 
                            icon = Icons.Default.Translate,
                            key = SettingKey.TRANSLATION, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "App Predictions", 
                            description = "Analyzes your usage patterns to suggest the most likely next app in the Island Dock.", 
                            icon = Icons.Default.Psychology,
                            key = SettingKey.APP_PREDICTION_SUGGESTION, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "Contextual Logic", 
                            description = "Dynamically changes Island priority based on what you're doing (e.g., prioritization of maps during navigation).", 
                            icon = Icons.Default.DynamicFeed,
                            key = SettingKey.CONTEXTUAL_SUGGESTIONS, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredItem(3) {
                    SettingsGroup(
                        title = "Advanced Labs", 
                        icon = Icons.Default.Science, 
                        summary = "Experimental Triggers"
                    ) {
                        DetectionToggle(
                            title = "Gaming Telemetry", 
                            description = "Displays FPS, CPU temperature, and network latency when a full-screen game is detected.", 
                            icon = Icons.Default.Gamepad,
                            key = SettingKey.GAMING_HUD, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "AR Vision", 
                            description = "Uses depth data to make the Island appear as if it's floating above the screen content.", 
                            icon = Icons.Default.Layers,
                            key = SettingKey.AR_ISLAND, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                        DetectionToggle(
                            title = "Morse Input", 
                            description = "Experimental long-press gesture to input text via Morse code directly into the Island.", 
                            icon = Icons.Default.Toll,
                            key = SettingKey.MORSE_CODE_INPUT, 
                            prefs = prefs, 
                            haptics = haptics
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun DetectionToggle(
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
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "intelligence")
                }
            },
            accentColor = IslandColors.accentCyan,
            icon = icon
        )
    }
}
