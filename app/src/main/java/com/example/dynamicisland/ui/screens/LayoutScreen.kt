package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.ui.components.SettingsSlider
import com.example.dynamicisland.ui.design.IslandPreviewCard
import com.example.dynamicisland.ui.design.glassmorphicCard
import kotlinx.coroutines.launch

@Composable
fun LayoutScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube", "System")

    // State for the current tab
    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var r by remember { mutableFloatStateOf(0f) }
    var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }

    val currentPrefix = tabs[selectedTab].lowercase()

    LaunchedEffect(selectedTab) {
        if (currentPrefix != "system") {
            w = prefs.getFloat("${currentPrefix}_w", NewConfigManager.getDefaultWidth(currentPrefix))
            h = prefs.getFloat("${currentPrefix}_h", NewConfigManager.getDefaultHeight(currentPrefix))
            x = prefs.getFloat("${currentPrefix}_x", 0f)
            y = prefs.getFloat("${currentPrefix}_y", 48f)
            r = prefs.getFloat("${currentPrefix}_r", NewConfigManager.getDefaultRadius(currentPrefix))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 1. Immersive Preview Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            IslandPreviewCard(
                modifier = Modifier.glassmorphicCard(cornerRadius = 28.dp),
                liveWidth = w,
                liveHeight = h,
                liveX = x,
                liveY = y,
                liveRadius = r,
                liveRingT = ringT,
                isLivePreview = currentPrefix != "system",
                previewState = if (currentPrefix == "system") "mini" else currentPrefix
            )
        }
        
        // --- 2. Segmented Navigation ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            divider = {},
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }
        
        // --- 3. Adjustment Controls ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (currentPrefix == "system") {
                SettingsCategoryHeader(title = "System Calibrations")
                
                var globalOffsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                SettingsSlider(
                    title = "Global Vertical Offset", 
                    description = "Shifts the entire UI up or down.",
                    value = globalOffsetY, 
                    valueRange = -100f..300f,
                    onValueChange = {
                        globalOffsetY = it
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("tweak_offset_y", it) }) {
                            NewConfigManager.broadcastUpdateSingle(context, prefs, "theme")
                        }
                    },
                    valueFormatter = { "${it.toInt()} px" }
                )

                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "The Island is injected directly below your camera. Use these sliders to align it perfectly with your hardware cutout.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                SettingsCategoryHeader(title = "${tabs[selectedTab]} State Geometry")
                
                SettingsSlider(
                    title = "Vertical Position", 
                    description = "Fine-tune the height relative to the punch hole.",
                    value = y, 
                    valueRange = -50f..250f, 
                    onValueChange = { 
                        y = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )

                SettingsSlider(
                    title = "Corner Radius", 
                    description = "Curve intensity of the pill/cube edges.",
                    value = r, 
                    valueRange = 0f..200f, 
                    onValueChange = { 
                        r = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )

                SettingsSlider(
                    title = "Width", 
                    value = w, 
                    valueRange = 10f..400f, 
                    onValueChange = { 
                        w = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
                
                SettingsSlider(
                    title = "Height", 
                    value = h, 
                    valueRange = 10f..400f, 
                    onValueChange = { 
                        h = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
                
                SettingsSlider(
                    title = "Horizontal Offset", 
                    value = x, 
                    valueRange = -150f..150f, 
                    onValueChange = { 
                        x = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )

                if (currentPrefix == "ring") {
                    SettingsCategoryHeader(title = "Ring Style")
                    SettingsSlider(
                        title = "Thickness", 
                        value = ringT, 
                        valueRange = 1f..20f, 
                        onValueChange = { 
                            ringT = it
                            NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                        },
                        valueFormatter = { "${it.toInt()} dp" }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}