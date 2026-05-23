package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.ConfigManager
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*
import kotlinx.coroutines.launch

@Composable
fun LayoutScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticManager()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube", "Tweaks")

    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }
    var expandUpwards by remember { mutableStateOf(prefs.getBoolean("expand_upwards", false)) }

    val currentPrefix = tabs[selectedTab].lowercase()

    LaunchedEffect(selectedTab) {
        if (currentPrefix != "tweaks") {
            w = prefs.getFloat("${currentPrefix}_w", ConfigManager.getDefaultWidth(currentPrefix))
            h = prefs.getFloat("${currentPrefix}_h", ConfigManager.getDefaultHeight(currentPrefix))
            x = prefs.getFloat("${currentPrefix}_x", 0f)
            y = prefs.getFloat("${currentPrefix}_y", 48f)
            ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
        }
    }

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        ConfigManager.broadcastUpdateSingle(context, prefs, "layout") 
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
                StaggeredItem(1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(tabs) { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) IslandColors.accentCyan else IslandColors.surface)
                                    .premiumClickable { 
                                        haptics.light()
                                        selectedTab = index 
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.Black else IslandColors.textSecondary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                StaggeredItem(2) {
                    if (currentPrefix == "tweaks") {
                        SettingsGroup(
                            title = "Physical Adjustments", 
                            icon = Icons.Default.Build, 
                            summary = "Advanced tuning"
                        ) {
                            var offsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                            ThemeSlider(
                                label = "Y-Axis Offset", 
                                value = offsetY, 
                                valueRange = 0f..150f,
                                onValueChange = {
                                    offsetY = it
                                    ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("tweak_offset_y", it) }) {
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                }
                            )
                        }
                    } else {
                        SettingsGroup(
                            title = "${tabs[selectedTab]} Geometry", 
                            icon = Icons.Default.Build, 
                            summary = "Size & Position"
                        ) {
                            FeatureSwitch(
                                title = "Expand Upwards", 
                                description = "Flip expansion direction", 
                                checked = expandUpwards, 
                                onCheckedChange = { 
                                    haptics.toggleOn()
                                    expandUpwards = it
                                    ConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("expand_upwards", it) }) {
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                }, 
                                accentColor = IslandColors.accentCyan
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PrecisionSlider(
                                    label = "Width", 
                                    value = w, 
                                    valueRange = 10f..400f, 
                                    onValueChange = { 
                                        w = it
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                )
                                PrecisionSlider(
                                    label = "Height", 
                                    value = h, 
                                    valueRange = 10f..400f, 
                                    onValueChange = { 
                                        h = it
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                )
                                PrecisionSlider(
                                    label = "X Position", 
                                    value = x, 
                                    valueRange = -200f..200f, 
                                    onValueChange = { 
                                        x = it
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                )
                                PrecisionSlider(
                                    label = "Y Position", 
                                    value = y, 
                                    valueRange = -100f..200f, 
                                    onValueChange = { 
                                        y = it
                                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                    }
                                )
                            }

                            if (currentPrefix == "ring") {
                                Spacer(modifier = Modifier.height(16.dp))
                                PrecisionSlider(
                                    label = "Ring Thickness", 
                                    value = ringT, 
                                    valueRange = 1f..20f, 
                                    onValueChange = { 
                                        ringT = it
                                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("ring_thickness", ringT) }) {
                                            ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
