package com.example.dynamicisland.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.settings.AestheticStyle
import com.example.dynamicisland.settings.PhysicsStyle
import com.example.dynamicisland.settings.ContentTransitionStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var blurIntensity by remember { mutableFloatStateOf(prefs.getFloat("blur_intensity", 15f)) }
    var aestheticStyle by remember { mutableStateOf(prefs.getString("AESTHETIC_STYLE", "GLASS") ?: "GLASS") }
    var physicsStyle by remember { mutableStateOf(prefs.getString("PHYSICS_STYLE", "APPLE") ?: "APPLE") }
    var transitionStyle by remember { mutableStateOf(prefs.getString("CONTENT_TRANSITION_STYLE", "SLIDE") ?: "SLIDE") }
    
    var callStyle by remember { mutableStateOf(prefs.getString("call_style", "IOS") ?: "IOS") }
    var chargingStyle by remember { mutableStateOf(prefs.getString("charging_style", "RING") ?: "RING") }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        
        // --- LIVE SANDBOX PREVIEW (Pillar 5) ---
        SettingsCategoryHeader("Live Sandbox Preview")
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                // Mini Island Mock
                Box(
                    modifier = Modifier
                        .size(width = 180.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(if (aestheticStyle == "VOID_BLACK") Color.Black else Color.White.copy(0.1f))
                        .border(0.5.dp, Color.White.copy(0.2f), CircleShape)
                ) {
                    Text("Live Preview", color = Color.White, modifier = Modifier.align(Alignment.Center), fontSize = 12.sp)
                }
            }
        }

        SettingsCategoryHeader("Visual Surfaces")
        SettingsChoiceChip("Aesthetic Mode", aestheticStyle, listOf("GLASS", "VOID_BLACK")) {
            aestheticStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("AESTHETIC_STYLE", it) })
        }

        if (aestheticStyle == "GLASS") {
            SettingsSlider(
                title = "Frosted Intensity", 
                value = blurIntensity, 
                defaultValue = 15f,
                valueRange = 5f..40f,
                onValueChange = { 
                    blurIntensity = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("blur_intensity", it) })
                }
            )
        }

        SettingsSwitch(
            title = "Monochrome Icons", 
            description = "Force app icons to adopt a consistent chalk style.", 
            checked = prefs.getBoolean("MONOCHROME_ICONS", false),
            icon = Icons.Default.InvertColors,
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("MONOCHROME_ICONS", it) } }
        )

        SettingsCategoryHeader("Icon Engine (Pillar 5)")
        val currentIconPack = prefs.getString("ICON_PACK", "MATERIAL_YOU") ?: "MATERIAL_YOU"
        SettingsChoiceChip("Icon Set", currentIconPack, listOf("MATERIAL_YOU", "IOS", "OXYGEN_OS", "ONE_UI", "AMOLED_CYBERPUNK", "CUPERTINO_GLASS")) {
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("ICON_PACK", it) })
        }

        SettingsCategoryHeader("Motion & Physics (Pillar 4)")
        SettingsChoiceChip("Physics Profile", physicsStyle, listOf("APPLE", "OXYGEN_OS")) {
            physicsStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("PHYSICS_STYLE", it) })
        }
        
        SettingsChoiceChip("Content Transition", transitionStyle, listOf("SLIDE", "FADE_SCALE", "FLIP")) {
            transitionStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("CONTENT_TRANSITION_STYLE", it) })
        }

        SettingsSwitch(
            title = "Metaball Tear", 
            description = "Liquid drop effect when the Island splits.", 
            checked = prefs.getBoolean("ENABLE_METABALL_TEAR", true),
            icon = Icons.Default.Waves,
            onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context) { putBoolean("ENABLE_METABALL_TEAR", it) } }
        )

        SettingsCategoryHeader("Component Studio")
        SettingsChoiceChip("Call UI", callStyle, listOf("IOS", "MINIMAL", "MODERN")) {
            callStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("call_style", it) })
        }
        SettingsChoiceChip("Charging UI", chargingStyle, listOf("RING", "WAVE", "CUBE")) {
            chargingStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("charging_style", it) })
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}
