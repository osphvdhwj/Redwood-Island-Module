package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var glassMode by remember { mutableStateOf(prefs.getBoolean("glass_mode", true)) }
    var blurIntensity by remember { mutableFloatStateOf(prefs.getFloat("blur_intensity", 15f)) }
    var glowEffect by remember { mutableStateOf(prefs.getBoolean("glow_effect", true)) }
    var elasticStretch by remember { mutableStateOf(prefs.getBoolean("elastic_stretch", true)) }
    
    var callStyle by remember { mutableStateOf(com.example.dynamicisland.settings.CallStyle.valueOf(prefs.getString("call_style", "IOS") ?: "IOS")) }
    var chargingStyle by remember { mutableStateOf(com.example.dynamicisland.settings.ChargingStyle.valueOf(prefs.getString("charging_style", "RING") ?: "RING")) }
    var batteryStyle by remember { mutableStateOf(com.example.dynamicisland.settings.BatteryStyle.valueOf(prefs.getString("battery_style", "PILL") ?: "PILL")) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(24.dp)) {
            IslandPreviewCard(modifier = Modifier.glassmorphicCard(cornerRadius = 28.dp))
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategoryHeader("Component Styles")
            
            MD3StyleSelector("Call Style", callStyle.name, listOf("IOS", "MINIMAL", "MODERN")) { newStyle ->
                callStyle = com.example.dynamicisland.settings.CallStyle.valueOf(newStyle)
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("call_style", newStyle) }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                }
            }
            
            MD3StyleSelector("Charging Animation", chargingStyle.name, listOf("RING", "WAVE", "CUBE")) { newStyle ->
                chargingStyle = com.example.dynamicisland.settings.ChargingStyle.valueOf(newStyle)
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("charging_style", newStyle) }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                }
            }

            MD3StyleSelector("Battery Style", batteryStyle.name, listOf("PILL", "GAUGE", "DIGITAL")) { newStyle ->
                batteryStyle = com.example.dynamicisland.settings.BatteryStyle.valueOf(newStyle)
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("battery_style", newStyle) }) {
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                }
            }

            SettingsCategoryHeader("Visual Engine")
            
            SettingsSwitch(
                title = "True Glassmorphism", 
                description = "Enable premium blurred backgrounds & translucent surfaces.", 
                checked = glassMode, 
                onCheckedChange = { 
                    glassMode = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glass_mode", it) }) {
                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                    }
                }
            )

            if (glassMode) {
                SettingsSlider(
                    title = "Blur Intensity", 
                    description = "Strength of the background frost effect.",
                    value = blurIntensity, 
                    valueRange = 5f..40f,
                    onValueChange = { 
                        blurIntensity = it
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("blur_intensity", it) }) {
                            NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                        }
                    }
                )
            }

            SettingsSwitch(
                title = "Glow Effect", 
                description = "Outer neon radiation around the Island.", 
                checked = glowEffect, 
                onCheckedChange = { 
                    glowEffect = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glow_effect", it) }) {
                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                    }
                }
            )

            SettingsSwitch(
                title = "Elastic Stretch", 
                description = "Squishy liquid animations during transitions.", 
                checked = elasticStretch, 
                onCheckedChange = { 
                    elasticStretch = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("elastic_stretch", it) }) {
                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                    }
                }
            )

            SettingsCategoryHeader("Physics Tester")
            
            // Interactive spring tester
            val isPressed = remember { mutableStateOf(false) }
            var stiffness by remember { mutableFloatStateOf(prefs.getFloat("spring_stiffness", 400f)) }
            var damping by remember { mutableFloatStateOf(prefs.getFloat("spring_damping", 0.85f)) }

            val scale by animateFloatAsState(
                targetValue = if (isPressed.value) 0.8f else 1f,
                animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                label = "visualSpring"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { isPressed.value = !isPressed.value },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    "Tap to test animation feel",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }

            SettingsSlider(
                title = "Animation Bounciness", 
                description = "Higher values increase oscillation.",
                value = damping, 
                valueRange = 0.1f..1.0f,
                onValueChange = { 
                    damping = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_damping", it) }) {
                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                    }
                }
            )
            
            SettingsSlider(
                title = "Animation Speed", 
                description = "Stiffness of the animation spring.",
                value = stiffness, 
                valueRange = 50f..1000f,
                onValueChange = { 
                    stiffness = it
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_stiffness", it) }) {
                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                    }
                }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MD3StyleSelector(
    label: String,
    selectedStyle: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { style ->
                FilterChip(
                    selected = selectedStyle.equals(style, ignoreCase = true),
                    onClick = { onSelect(style) },
                    label = { 
                        Text(
                            text = style.lowercase().replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}