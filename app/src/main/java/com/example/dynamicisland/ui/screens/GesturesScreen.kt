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
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.*
import com.example.dynamicisland.ui.design.*

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
        NewConfigManager.sendGestureUpdate(context, prefs) 
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
                
                // Calculate which actions are already assigned to *other* gestures in this state
                val allGestureKeys = listOf(
                    "${stateKey}_single_tap",
                    "${stateKey}_double_tap",
                    "${stateKey}_long_press",
                    "${stateKey}_swipe_left",
                    "${stateKey}_swipe_right",
                    "${stateKey}_swipe_up",
                    "${stateKey}_swipe_down"
                )
                
                val currentAssignments = remember(selectedTab, prefs.all) {
                    allGestureKeys.mapNotNull { key -> 
                        prefs.getString(key, "none")?.takeIf { it != "none" } 
                    }
                }

                SettingsGroup(
                    title = "${tabs[selectedTab]} State Actions", 
                    icon = Icons.Default.TouchApp, 
                    summary = "Swipes & Taps"
                ) {
                    GestureSelector("Single Tap", "${stateKey}_single_tap", prefs, haptics, currentAssignments)
                    GestureSelector("Double Tap", "${stateKey}_double_tap", prefs, haptics, currentAssignments)
                    GestureSelector("Long Press", "${stateKey}_long_press", prefs, haptics, currentAssignments)
                    GestureSelector("Swipe Left", "${stateKey}_swipe_left", prefs, haptics, currentAssignments)
                    GestureSelector("Swipe Right", "${stateKey}_swipe_right", prefs, haptics, currentAssignments)
                    GestureSelector("Swipe Up", "${stateKey}_swipe_up", prefs, haptics, currentAssignments)
                    GestureSelector("Swipe Down", "${stateKey}_swipe_down", prefs, haptics, currentAssignments)
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
    haptics: HapticManager,
    inUseActions: List<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedAction by remember { mutableStateOf(prefs.getString(prefsKey, "none") ?: "none") }
    var pendingOverrideAction by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label, 
            style = MaterialTheme.typography.titleSmall, 
            color = IslandColors.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        GestureActionChips(
            selectedAction = selectedAction,
            inUseActions = inUseActions,
            onSelect = { action ->
                haptics.light()
                if (action in inUseActions && action != selectedAction && action != "none") {
                    // Action is already in use by another gesture, prompt for override
                    pendingOverrideAction = action
                    haptics.heavy()
                } else {
                    selectedAction = action
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString(prefsKey, action) }) {
                        NewConfigManager.sendGestureUpdate(context, prefs)
                    }
                }
            }
        )
    }

    if (pendingOverrideAction != null) {
        AlertDialog(
            onDismissRequest = { pendingOverrideAction = null },
            title = { Text("Override Action?", color = IslandColors.textPrimary) },
            text = { 
                Text(
                    "The action '${pendingOverrideAction?.replace("_", " ")?.capitalize()}' is already assigned to another gesture in this state. Do you want to reassign it here?",
                    color = IslandColors.textSecondary
                ) 
            },
            containerColor = IslandColors.surface,
            confirmButton = {
                TextButton(onClick = {
                    val actionToOverride = pendingOverrideAction!!
                    
                    // Find the previous gesture that had this action and set it to 'none'
                    val allGestureKeys = listOf(
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_single_tap",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_double_tap",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_long_press",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_swipe_left",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_swipe_right",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_swipe_up",
                        "${prefsKey.substringBefore("_swipe").substringBefore("_long").substringBefore("_double").substringBefore("_single")}_swipe_down"
                    )
                    
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, {
                        allGestureKeys.forEach { key ->
                            if (key != prefsKey && prefs.getString(key, "none") == actionToOverride) {
                                putString(key, "none")
                            }
                        }
                        putString(prefsKey, actionToOverride)
                    }) {
                        NewConfigManager.sendGestureUpdate(context, prefs)
                    }

                    selectedAction = actionToOverride
                    pendingOverrideAction = null
                    haptics.medium()
                }) {
                    Text("Reassign", color = IslandColors.accentCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverrideAction = null }) {
                    Text("Cancel", color = IslandColors.textSecondary)
                }
            }
        )
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
