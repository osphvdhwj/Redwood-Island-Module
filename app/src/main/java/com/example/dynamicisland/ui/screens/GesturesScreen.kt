package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
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
import com.example.dynamicisland.ui.settings.GestureActionChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val haptics = rememberHapticManager()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ring", "Mini", "Mid", "Max")
    val stateKeys = listOf("TYPE_0_RING", "TYPE_1_MINI", "TYPE_2_MID", "TYPE_3_MAX")

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        ConfigManager.sendGestureUpdate(context, prefs) 
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StaggeredItem(0) { 
                SectionHeader("Interaction Matrix", "Assign actions to island gestures", Icons.Default.TouchApp, IslandColors.accentCyan) 
            }
            
            Spacer(modifier = Modifier.height(24.dp))

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
                val stateKey = stateKeys[selectedTab]
                SettingsGroup(
                    title = "${tabs[selectedTab]} State Actions", 
                    icon = Icons.Default.TouchApp, 
                    summary = "Swipes & Taps"
                ) {
                    GestureSelector("Swipe Left", "${stateKey}_swipe_left", prefs, haptics)
                    GestureSelector("Swipe Right", "${stateKey}_swipe_right", prefs, haptics)
                    GestureSelector("Long Press", "${stateKey}_long_press", prefs, haptics)
                    GestureSelector("Double Tap", "${stateKey}_double_tap", prefs, haptics)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun GestureSelector(
    label: String,
    prefsKey: String,
    prefs: SharedPreferences,
    haptics: HapticManager
) {
    val actions = listOf("dismiss", "next_track", "previous_track", "toggle_play_pause", "expand", "none")
    var selectedAction by remember { mutableStateOf(prefs.getString(prefsKey, "none") ?: "none") }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label, 
            style = MaterialTheme.typography.titleSmall, 
            color = IslandColors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GestureActionChips(
            selectedAction = selectedAction,
            onSelect = { action ->
                haptics.light()
                selectedAction = action
                prefs.edit().putString(prefsKey, action).apply()
            }
        )
    }
}
