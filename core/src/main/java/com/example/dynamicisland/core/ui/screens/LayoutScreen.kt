package com.example.dynamicisland.core.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.core.ui.components.SettingsSlider
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.launch

@Composable
fun LayoutScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube")
    val currentPrefix = tabs[selectedTab].lowercase()

    // State for the current tab
    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var r by remember { mutableFloatStateOf(0f) }
    var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }

    LaunchedEffect(selectedTab) {
        w = prefs.getFloat("${currentPrefix}_w", NewConfigManager.getDefaultWidth(currentPrefix))
        h = prefs.getFloat("${currentPrefix}_h", NewConfigManager.getDefaultHeight(currentPrefix))
        x = prefs.getFloat("${currentPrefix}_x", 0f)
        y = prefs.getFloat("${currentPrefix}_y", 48f)
        r = prefs.getFloat("${currentPrefix}_r", NewConfigManager.getDefaultRadius(currentPrefix))
        
        // Notify service of active calibration target
        NewConfigManager.setCalibrationMode(context, true, currentPrefix)
    }

    DisposableEffect(Unit) {
        onDispose {
            NewConfigManager.setCalibrationMode(context, false, "")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // High-End Calibration Info Header
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Direct Calibration Mode", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "The actual Island on your status bar is highlighted. Move the sliders to align it with your camera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
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
            SettingsCategoryHeader(title = "${tabs[selectedTab]} Precision Controls")
            
            // Reordered & Optimized Sliders: Width -> Height -> X -> Y -> Radius
            
            val widthRange = if (currentPrefix == "ring") 20f..64f else if (currentPrefix == "cube") 40f..120f else 80f..420f
            val heightRange = if (currentPrefix == "ring") 20f..64f else if (currentPrefix == "cube") 40f..120f else 20f..250f

            SettingsSlider(
                title = "Width", 
                description = "Horizontal span (tap value for manual input).",
                value = w, 
                defaultValue = NewConfigManager.getDefaultWidth(currentPrefix),
                valueRange = widthRange, 
                onValueChange = { 
                    w = it
                    if (currentPrefix == "ring") h = it // Ring stays circular
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                }
            )
            
            SettingsSlider(
                title = "Height", 
                description = "Vertical span.",
                value = h, 
                defaultValue = NewConfigManager.getDefaultHeight(currentPrefix),
                valueRange = heightRange, 
                onValueChange = { 
                    h = it
                    if (currentPrefix == "ring") w = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                }
            )
            
            SettingsSlider(
                title = "Horizontal Alignment", 
                description = "X-axis offset (0 is perfectly centered).",
                value = x, 
                defaultValue = 0f,
                valueRange = -200f..200f, 
                onValueChange = { 
                    x = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                }
            )

            SettingsSlider(
                title = "Vertical Position", 
                description = "Distance from screen top.",
                value = y, 
                defaultValue = 48f,
                valueRange = -40f..300f, 
                onValueChange = { 
                    y = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                }
            )

            if (currentPrefix != "ring") {
                SettingsSlider(
                    title = "Corner Radius", 
                    description = "Curve intensity (4 to 60 dp).",
                    value = r, 
                    defaultValue = NewConfigManager.getDefaultRadius(currentPrefix),
                    valueRange = 4f..60f, 
                    onValueChange = { 
                        r = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    }
                )
            }

            if (currentPrefix == "ring") {
                SettingsCategoryHeader(title = "Ring Decoration")
                SettingsSlider(
                    title = "Glow Thickness", 
                    description = "Stroke width of the accent border.",
                    value = ringT, 
                    defaultValue = 6f,
                    valueRange = 1f..12f, 
                    onValueChange = { 
                        ringT = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
