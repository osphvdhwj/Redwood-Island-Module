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
fun IntelligenceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Smart Detection")
        
        MD3IntelligenceToggle(
            title = "OTP Auto-Capture", 
            description = "Instantly captures verification codes from SMS.", 
            icon = Icons.Default.VpnKey,
            key = SettingKey.OTP_DETECTION, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "Link Interceptor", 
            description = "Detects URLs in clipboard or notifications.", 
            icon = Icons.Default.Link,
            key = SettingKey.LINK_INTERCEPT, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "Real-time Barcode", 
            description = "Shows a snippet of detected QR/barcodes.", 
            icon = Icons.Default.QrCodeScanner,
            key = SettingKey.BARCODE, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("AI Assistance")
        
        MD3IntelligenceToggle(
            title = "Live Translation", 
            description = "Translates foreign text on screen in real-time.", 
            icon = Icons.Default.Translate,
            key = SettingKey.TRANSLATION, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "App Predictions", 
            description = "Analyzes usage to suggest the next app.", 
            icon = Icons.Default.Psychology,
            key = SettingKey.APP_PREDICTION_SUGGESTION, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "Contextual Logic", 
            description = "Dynamically changes Island priority based on context.", 
            icon = Icons.Default.DynamicFeed,
            key = SettingKey.CONTEXTUAL_SUGGESTIONS, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("Advanced Labs")
        
        MD3IntelligenceToggle(
            title = "Gaming Telemetry", 
            description = "Displays FPS, CPU temp when a game is detected.", 
            icon = Icons.Default.Gamepad,
            key = SettingKey.GAMING_HUD, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "AR Vision", 
            description = "Makes the Island appear floating.", 
            icon = Icons.Default.Layers,
            key = SettingKey.AR_ISLAND, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        MD3IntelligenceToggle(
            title = "Morse Input", 
            description = "Input text via Morse code.", 
            icon = Icons.Default.Toll,
            key = SettingKey.MORSE_CODE_INPUT, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun MD3IntelligenceToggle(
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
                NewConfigManager.broadcastUpdateSingle(context, prefs, "intelligence")
            }
        },
        icon = icon
    )
}