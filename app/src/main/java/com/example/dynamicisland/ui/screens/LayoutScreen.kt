package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.ui.components.SettingsSlider
import com.example.dynamicisland.ui.components.SettingsSwitch
import com.example.dynamicisland.ui.design.IslandPreviewCard
import com.example.dynamicisland.ui.design.glassmorphicCard
import kotlinx.coroutines.launch

@Composable
fun LayoutScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
            w = prefs.getFloat("${currentPrefix}_w", NewConfigManager.getDefaultWidth(currentPrefix))
            h = prefs.getFloat("${currentPrefix}_h", NewConfigManager.getDefaultHeight(currentPrefix))
            x = prefs.getFloat("${currentPrefix}_x", 0f)
            y = prefs.getFloat("${currentPrefix}_y", 48f)
            NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(24.dp)) {
            IslandPreviewCard(
                modifier = Modifier.glassmorphicCard(cornerRadius = 28.dp),
                liveWidth = w,
                liveHeight = h,
                liveX = x,
                liveY = y,
                liveRingT = ringT,
                isLivePreview = currentPrefix != "tweaks",
                previewState = currentPrefix,
                expandUpwards = expandUpwards
            )
        }
        
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (currentPrefix == "tweaks") {
                SettingsCategoryHeader(title = "Physical Adjustments")
                var offsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                SettingsSlider(
                    title = "Y-Axis Offset", 
                    value = offsetY, 
                    valueRange = 0f..150f,
                    onValueChange = {
                        offsetY = it
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("tweak_offset_y", it) }) {
                            NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                        }
                    },
                    valueFormatter = { "${it.toInt()} px" }
                )
            } else {
                SettingsCategoryHeader(title = "${tabs[selectedTab]} Geometry")
                
                SettingsSwitch(
                    title = "Expand Upwards", 
                    description = "Flip expansion direction (for bottom islands)", 
                    checked = expandUpwards, 
                    onCheckedChange = { 
                        expandUpwards = it
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("expand_upwards", it) }) {
                            NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                        }
                    }
                )
                
                SettingsSlider(
                    title = "Width", 
                    value = w, 
                    valueRange = 10f..400f, 
                    onValueChange = { 
                        w = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
                
                SettingsSlider(
                    title = "Height", 
                    value = h, 
                    valueRange = 10f..400f, 
                    onValueChange = { 
                        h = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
                
                SettingsSlider(
                    title = "X Position", 
                    value = x, 
                    valueRange = -200f..200f, 
                    onValueChange = { 
                        x = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
                
                SettingsSlider(
                    title = "Y Position", 
                    value = y, 
                    valueRange = -100f..200f, 
                    onValueChange = { 
                        y = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )

                if (currentPrefix == "ring") {
                    SettingsSlider(
                        title = "Ring Thickness", 
                        value = ringT, 
                        valueRange = 1f..20f, 
                        onValueChange = { 
                            ringT = it
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("ring_thickness", ringT) }) {
                                NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
                            }
                        },
                        valueFormatter = { "${it.toInt()} dp" }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}