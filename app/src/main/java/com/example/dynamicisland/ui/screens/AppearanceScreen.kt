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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var glassMode by remember { mutableStateOf(prefs.getBoolean("glass_mode", true)) }
    var blurIntensity by remember { mutableFloatStateOf(prefs.getFloat("blur_intensity", 15f)) }
    var glowEffect by remember { mutableStateOf(prefs.getBoolean("glow_effect", true)) }
    var elasticStretch by remember { mutableStateOf(prefs.getBoolean("elastic_stretch", true)) }
    
    var callStyle by remember { mutableStateOf(prefs.getString("call_style", "IOS") ?: "IOS") }
    var chargingStyle by remember { mutableStateOf(prefs.getString("charging_style", "RING") ?: "RING") }
    var batteryStyle by remember { mutableStateOf(prefs.getString("battery_style", "PILL") ?: "PILL") }

    var springStiffness by remember { mutableFloatStateOf(prefs.getFloat("spring_stiffness", 400f)) }
    var springDamping by remember { mutableFloatStateOf(prefs.getFloat("spring_damping", 0.85f)) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        
        // --- PRO GRADE STYLE PLAYGROUND ---
        SettingsCategoryHeader("Live Style Playground")
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TRIGGER REAL-TIME TESTS", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaygroundChip("Charging", Icons.Default.BatteryChargingFull) {
                        context.sendBroadcast(Intent("com.crdroid.batterywellbeing.SYSTEM_OVERRIDE").apply { putExtra("action", "SMART_CHARGE_LIMIT"); putExtra("level", 85) })
                    }
                    PlaygroundChip("Music", Icons.Default.MusicNote) {
                        // Mock music update via native pipe logic (internal)
                        context.sendBroadcast(Intent("com.example.dynamicisland.MEDIA_STATE_CHANGED"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlaygroundChip("Incoming Call", Icons.Default.Call) {
                        // Trigger a mock system alert that looks like a call
                        context.sendBroadcast(Intent("com.example.dynamicisland.HARDWARE_TOGGLE").apply { putExtra("type", "RINGER"); putExtra("state", 2) })
                    }
                    PlaygroundChip("Privacy", Icons.Default.Visibility) {
                        context.sendBroadcast(Intent("com.example.dynamicisland.hook.FutureFrameworkA15Hooks.ACTION_FUTURE_PRIVACY_INDICATOR").apply { putExtra("op", "CAMERA"); putExtra("pkg", "System UI") })
                    }
                }
            }
        }

        SettingsCategoryHeader("Visual Surfaces")
        SettingsSwitch(
            title = "True Glassmorphism", 
            description = "Premium frosted glass effect with background sampling.", 
            checked = glassMode, 
            icon = Icons.Default.BlurOn,
            onCheckedChange = { 
                glassMode = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glass_mode", it) })
            }
        )

        if (glassMode) {
            SettingsSlider(
                title = "Frosted Intensity", 
                description = "Sampling radius for background blur.",
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
            title = "Neon Radiation", 
            description = "Subtle outer glow based on content color.", 
            checked = glowEffect, 
            icon = Icons.Default.WbIridescent,
            onCheckedChange = { 
                glowEffect = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glow_effect", it) })
            }
        )

        SettingsCategoryHeader("Component Studio")
        SettingsChoiceChip("Call Style", callStyle, listOf("IOS", "MINIMAL", "MODERN")) {
            callStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("call_style", it) })
        }
        SettingsChoiceChip("Charging UI", chargingStyle, listOf("RING", "WAVE", "CUBE")) {
            chargingStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("charging_style", it) })
        }
        SettingsChoiceChip("Battery Style", batteryStyle, listOf("PILL", "GAUGE", "DIGITAL")) {
            batteryStyle = it
            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("battery_style", it) })
        }

        SettingsCategoryHeader("Motion & Physics")
        SettingsSwitch(
            title = "Liquid Elasticity", 
            description = "Squishy metaball-inspired transitions.", 
            checked = elasticStretch, 
            icon = Icons.Default.AutoFixHigh,
            onCheckedChange = { 
                elasticStretch = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("elastic_stretch", it) })
            }
        )

        SettingsSlider(
            title = "Animation Stiffness", 
            description = "How fast the Island snaps into position.",
            value = springStiffness, 
            defaultValue = 400f,
            valueRange = 100f..1000f,
            onValueChange = { 
                springStiffness = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_stiffness", it) })
            }
        )

        SettingsSlider(
            title = "Bounciness", 
            description = "Oscillation level of the springs.",
            value = springDamping, 
            defaultValue = 0.85f,
            valueRange = 0.2f..1.0f,
            onValueChange = { 
                springDamping = it
                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_damping", it) })
            }
        )

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun RowScope.PlaygroundChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        modifier = Modifier.weight(1f),
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    )
}
