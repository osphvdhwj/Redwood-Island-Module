package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube")

    // State for the current tab
    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    var x by remember { mutableFloatStateOf(0f) }
    var y by remember { mutableFloatStateOf(0f) }
    var r by remember { mutableFloatStateOf(0f) }
    var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }

    val currentPrefix = tabs[selectedTab].lowercase()

    LaunchedEffect(selectedTab) {
        w = prefs.getFloat("${currentPrefix}_w", NewConfigManager.getDefaultWidth(currentPrefix))
        h = prefs.getFloat("${currentPrefix}_h", NewConfigManager.getDefaultHeight(currentPrefix))
        x = prefs.getFloat("${currentPrefix}_x", 0f)
        y = prefs.getFloat("${currentPrefix}_y", 48f)
        r = prefs.getFloat("${currentPrefix}_r", NewConfigManager.getDefaultRadius(currentPrefix))
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                liveRadius = if (currentPrefix == "ring") h / 2 else r,
                liveRingT = ringT,
                isLivePreview = true,
                previewState = currentPrefix
            )
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
            SettingsCategoryHeader(title = "${tabs[selectedTab]} Geometry")
            
            // Dynamic value ranges based on state
            val widthRange = if (currentPrefix == "ring") 20f..48f else if (currentPrefix == "cube") 20f..150f else 48f..420f
            val heightRange = if (currentPrefix == "ring") 20f..48f else if (currentPrefix == "cube") 20f..150f else 20f..300f
            val radiusRange = 4f..60f

            SettingsSlider(
                title = "Width", 
                description = "Standardize the horizontal span.",
                value = w, 
                defaultValue = NewConfigManager.getDefaultWidth(currentPrefix),
                valueRange = widthRange, 
                onValueChange = { 
                    w = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                },
                valueFormatter = { "${it.toInt()} dp" }
            )
            
            SettingsSlider(
                title = "Height", 
                description = "Adjust the vertical thickness.",
                value = h, 
                defaultValue = NewConfigManager.getDefaultHeight(currentPrefix),
                valueRange = heightRange, 
                onValueChange = { 
                    h = it
                    if (currentPrefix == "ring") {
                         // Force Ring to stay a circle
                         w = it
                    }
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                },
                valueFormatter = { "${it.toInt()} dp" }
            )
            
            SettingsSlider(
                title = "Horizontal Offset", 
                description = "X-axis alignment (0 is centered).",
                value = x, 
                defaultValue = 0f,
                valueRange = -150f..150f, 
                onValueChange = { 
                    x = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                },
                valueFormatter = { "${it.toInt()} dp" }
            )

            SettingsSlider(
                title = "Vertical Position", 
                description = "Distance from the top (align with camera).",
                value = y, 
                defaultValue = 48f,
                valueRange = -50f..400f, 
                onValueChange = { 
                    y = it
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                },
                valueFormatter = { "${it.toInt()} dp" }
            )

            if (currentPrefix != "ring") {
                SettingsSlider(
                    title = "Corner Radius", 
                    description = "Curve of the edges (capped at 60dp for stability).",
                    value = r, 
                    defaultValue = NewConfigManager.getDefaultRadius(currentPrefix),
                    valueRange = 4f..60f, 
                    onValueChange = { 
                        r = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
            }

            if (currentPrefix == "ring") {
                SettingsCategoryHeader(title = "Ring Specifics")
                SettingsSlider(
                    title = "Thickness", 
                    description = "Stroke width of the notification circle.",
                    value = ringT, 
                    defaultValue = 6f,
                    valueRange = 1f..10f, 
                    onValueChange = { 
                        ringT = it
                        NewConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, r, ringT)
                    },
                    valueFormatter = { "${it.toInt()} dp" }
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}