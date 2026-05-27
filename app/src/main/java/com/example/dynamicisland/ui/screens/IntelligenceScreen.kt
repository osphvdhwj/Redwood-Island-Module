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
                        title = "Intelligence Hub", 
                        subtitle = "Smart detection & AI behavior", 
                        icon = Icons.Default.Star, 
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
                    title = "Detection Engines", 
                    icon = Icons.Default.Notifications, 
                    summary = "Contextual triggers"
                ) {
                    DetectionToggle("OTP Detection", "Auto-copy one-time passwords", SettingKey.OTP_DETECTION, prefs, haptics)
                    DetectionToggle("Link Intercept", "Inline URL actions", SettingKey.LINK_INTERCEPT, prefs, haptics)
                    DetectionToggle("Barcode Scanner", "Identify codes in view", SettingKey.BARCODE, prefs, haptics)
                    DetectionToggle("Navigation", "Turn-by-turn guidance", SettingKey.NAVIGATION, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(2) {
                SettingsGroup(
                    title = "Smart Overlays", 
                    icon = Icons.Default.Star, 
                    summary = "Real-time data"
                ) {
                    DetectionToggle("Translation", "Live text translation", SettingKey.TRANSLATION, prefs, haptics)
                    DetectionToggle("Gaming HUD", "FPS & Temperature metrics", SettingKey.GAMING_HUD, prefs, haptics)
                    DetectionToggle("App Prediction", "Suggest actions based on usage", SettingKey.APP_PREDICTION_SUGGESTION, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(3) {
                SettingsGroup(
                    title = "Prediction & AI", 
                    icon = Icons.Default.Info, 
                    summary = "Adaptive learning"
                ) {
                    DetectionToggle("Predictive Actions", "Pre-load likely apps", SettingKey.PREDICTIVE_ACTIONS, prefs, haptics)
                    DetectionToggle("Contextual Suggestions", "Smart shortcuts", SettingKey.CONTEXTUAL_SUGGESTIONS, prefs, haptics)
                    DetectionToggle("Gesture Learning", "Refines on your usage", SettingKey.GESTURE_LEARNING, prefs, haptics)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(4) {
                SettingsGroup(
                    title = "Experimental Labs", 
                    icon = Icons.Default.Build, 
                    summary = "Cutting-edge features"
                ) {
                    DetectionToggle("AR Island", "Visual depth effects", SettingKey.AR_ISLAND, prefs, haptics)
                    DetectionToggle("Morse Code Input", "Type with long-press", SettingKey.MORSE_CODE_INPUT, prefs, haptics)
                    DetectionToggle("Crypto Ticker", "Live price scrolling", SettingKey.CRYPTO_STOCK_TICKER, prefs, haptics)
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
    key: SettingKey,
    prefs: SharedPreferences,
    haptics: HapticManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checked by remember { mutableStateOf(prefs.getBoolean(key.name, true)) }
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
        accentColor = IslandColors.accentCyan
    )
}
