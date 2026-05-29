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
        SettingsCategoryHeader("Automation")
        
        MD3IntelligenceToggle(
            title = "OTP Auto-Capture", 
            description = "Intercept and extract 2FA codes from incoming SMS.", 
            icon = Icons.Default.VpnKey,
            key = SettingKey.OTP_DETECTION, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        
        MD3IntelligenceToggle(
            title = "Navigation HUD", 
            description = "Turn Google Maps notifications into a live instruction bar.", 
            icon = Icons.Default.Navigation,
            key = SettingKey.NAVIGATION, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        MD3IntelligenceToggle(
            title = "Real-time Barcode", 
            description = "Preview detected barcodes for quick scanning.", 
            icon = Icons.Default.QrCodeScanner,
            key = SettingKey.BARCODE, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        SettingsCategoryHeader("AI & Analysis")
        
        MD3IntelligenceToggle(
            title = "Live Translation", 
            description = "Translate intercepted text using ML Kit.", 
            icon = Icons.Default.Translate,
            key = SettingKey.TRANSLATION, 
            prefs = prefs,
            scope = scope,
            context = context
        )
        
        MD3IntelligenceToggle(
            title = "Gaming Telemetry", 
            description = "Monitor FPS and CPU status during heavy gameplay.", 
            icon = Icons.Default.Gamepad,
            key = SettingKey.GAMING_HUD, 
            prefs = prefs,
            scope = scope,
            context = context
        )

        MD3IntelligenceToggle(
            title = "App Predictions", 
            description = "Suggest the next likely app based on usage patterns.", 
            icon = Icons.Default.Psychology,
            key = SettingKey.APP_PREDICTION_SUGGESTION, 
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