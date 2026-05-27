package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
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
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.settings.DesignLanguage
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticManager()

    // Using mutableState for UI, but in a real app these would be in a ViewModel
    var designLanguage by remember { mutableStateOf(DesignLanguage.valueOf(prefs.getString("design_language", "APPLE_LIQUID_GLASS") ?: "APPLE_LIQUID_GLASS")) }
    var glassMode by remember { mutableStateOf(prefs.getBoolean("glass_mode", true)) }
    var dynamicColors by remember { mutableStateOf(prefs.getBoolean("dynamic_colors", true)) }
    var blurIntensity by remember { mutableFloatStateOf(prefs.getFloat("blur_intensity", 15f)) }
    var cornerRadius by remember { mutableFloatStateOf(prefs.getFloat("pill_corner_radius", 100f)) }
    var pillShape by remember { mutableStateOf(prefs.getString("pill_shape", "pill") ?: "pill") }
    var stiffness by remember { mutableFloatStateOf(prefs.getFloat("spring_stiffness", 400f)) }
    var damping by remember { mutableFloatStateOf(prefs.getFloat("spring_damping", 0.85f)) }
    var glowEffect by remember { mutableStateOf(prefs.getBoolean("glow_effect", true)) }
    var elasticStretch by remember { mutableStateOf(prefs.getBoolean("elastic_stretch", true)) }
    
    var callStyle by remember { mutableStateOf(com.example.dynamicisland.settings.CallStyle.valueOf(prefs.getString("call_style", "IOS") ?: "IOS")) }
    var chargingStyle by remember { mutableStateOf(com.example.dynamicisland.settings.ChargingStyle.valueOf(prefs.getString("charging_style", "RING") ?: "RING")) }
    var batteryStyle by remember { mutableStateOf(com.example.dynamicisland.settings.BatteryStyle.valueOf(prefs.getString("battery_style", "PILL") ?: "PILL")) }

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme") 
    }) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(16.dp)) {
                StaggeredItem(0) { 
                    IslandPreviewCard(modifier = Modifier.glassmorphicCard(cornerRadius = 28.dp))
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
                        title = "Component Styles",
                        icon = Icons.Default.Build,
                        summary = "Individual app UIs"
                    ) {
                        StyleSelector("Call Style", callStyle.name, listOf("IOS", "MINIMAL", "MODERN")) { newStyle ->
                            haptics.light()
                            callStyle = com.example.dynamicisland.settings.CallStyle.valueOf(newStyle)
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("call_style", newStyle) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                        
                        StyleSelector("Charging Animation", chargingStyle.name, listOf("RING", "WAVE", "CUBE")) { newStyle ->
                            haptics.light()
                            chargingStyle = com.example.dynamicisland.settings.ChargingStyle.valueOf(newStyle)
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("charging_style", newStyle) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }

                        StyleSelector("Battery Vital", batteryStyle.name, listOf("PILL", "GAUGE", "DIGITAL")) { newStyle ->
                            haptics.light()
                            batteryStyle = com.example.dynamicisland.settings.BatteryStyle.valueOf(newStyle)
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("battery_style", newStyle) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                StaggeredItem(2) {
                SettingsGroup(
                    title = "Design Language", 
                    icon = Icons.Default.Palette, 
                    summary = designLanguage.name.replace("_", " ")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DesignChip(
                            label = "Material You", 
                            selected = designLanguage == DesignLanguage.MATERIAL_YOU,
                            onClick = { 
                                haptics.light()
                                designLanguage = DesignLanguage.MATERIAL_YOU
                                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("design_language", "MATERIAL_YOU") }) {
                                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DesignChip(
                            label = "Liquid Glass", 
                            selected = designLanguage == DesignLanguage.APPLE_LIQUID_GLASS,
                            onClick = { 
                                haptics.light()
                                designLanguage = DesignLanguage.APPLE_LIQUID_GLASS
                                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("design_language", "APPLE_LIQUID_GLASS") }) {
                                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (designLanguage == DesignLanguage.MATERIAL_YOU) {
                        FeatureSwitch(
                            title = "Dynamic Colors", 
                            description = "Use system accent colors", 
                            checked = dynamicColors, 
                            onCheckedChange = { 
                                haptics.toggleOn()
                                dynamicColors = it
                                NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("dynamic_colors", it) }) {
                                    NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                                }
                            }, 
                            accentColor = IslandColors.accentCyan
                        )
                    }

                    FeatureSwitch(
                        title = "True Glassmorphism", 
                        description = "Enable premium blurred backgrounds", 
                        checked = glassMode, 
                        onCheckedChange = { 
                            haptics.toggleOn()
                            glassMode = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glass_mode", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }, 
                        accentColor = IslandColors.accentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(2) {
                SettingsGroup(
                    title = "Visual Effects", 
                    icon = Icons.Default.Create, 
                    summary = "Refine the Look"
                ) {
                    ThemeSlider(
                        label = "Blur Intensity", 
                        value = blurIntensity, 
                        valueRange = 5f..40f,
                        onValueChange = { 
                            blurIntensity = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("blur_intensity", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                    )

                    FeatureSwitch(
                        title = "Glow Effect", 
                        description = "Outer neon radiation", 
                        checked = glowEffect, 
                        onCheckedChange = { 
                            haptics.toggleOn()
                            glowEffect = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glow_effect", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }, 
                        accentColor = IslandColors.accentPurple
                    )

                    FeatureSwitch(
                        title = "Elastic Stretch", 
                        description = "Squishy liquid animations", 
                        checked = elasticStretch, 
                        onCheckedChange = { 
                            haptics.toggleOn()
                            elasticStretch = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("elastic_stretch", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }, 
                        accentColor = IslandColors.accentPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(3) {
                SettingsGroup(
                    title = "Shape & Geometry", 
                    icon = Icons.Default.Build, 
                    summary = pillShape.replaceFirstChar { it.uppercase() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("pill", "capsule", "squircle").forEach { shape ->
                            DesignChip(
                                label = shape.replaceFirstChar { it.uppercase() }, 
                                selected = pillShape == shape,
                                onClick = { 
                                    haptics.light()
                                    pillShape = shape
                                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("pill_shape", shape) }) {
                                        NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    PrecisionSlider(
                        label = "Corner Radius", 
                        value = cornerRadius, 
                        valueRange = 8f..200f,
                        onValueChange = { 
                            cornerRadius = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("pill_corner_radius", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(4) {
                SettingsGroup(
                    title = "Physics & Animations", 
                    icon = Icons.Default.Refresh, 
                    summary = "Bounciness & Speed"
                ) {
                    val isPressed = remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed.value) 0.8f else 1f,
                        animationSpec = spring(dampingRatio = damping, stiffness = stiffness),
                        label = "visualSpring"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .glassmorphicCard(cornerRadius = 16.dp)
                            .premiumClickable { 
                                haptics.light()
                                isPressed.value = !isPressed.value 
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                                .clip(CircleShape)
                                .background(IslandColors.accentCyan)
                        )
                        Text(
                            "Tap to test spring",
                            color = IslandColors.textPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ThemeSlider(
                        label = "Animation Bounciness", 
                        value = damping, 
                        valueRange = 0.1f..1.0f,
                        onValueChange = { 
                            damping = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_damping", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                    )
                    
                    ThemeSlider(
                        label = "Animation Stiffness", 
                        value = stiffness, 
                        valueRange = 50f..1000f,
                        onValueChange = { 
                            stiffness = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_stiffness", it) }) {
                                NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
}

@Composable
fun DesignChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) IslandColors.accentCyan else IslandColors.surface)
            .premiumClickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else IslandColors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StyleSelector(
    label: String,
    selectedStyle: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = label,
            color = IslandColors.textPrimary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { style ->
                DesignChip(
                    label = style.lowercase().replaceFirstChar { it.uppercase() },
                    selected = selectedStyle == style,
                    onClick = { onSelect(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
