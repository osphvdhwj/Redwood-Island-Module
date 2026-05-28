package com.example.dynamicisland.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.NewConfigManager
import com.example.dynamicisland.ui.components.SettingsCategoryHeader
import kotlinx.coroutines.launch

@Composable
fun GesturesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ring", "Mini", "Mid", "Max")
    val stateKeys = listOf("TYPE_0_RING", "TYPE_1_MINI", "TYPE_2_MID", "TYPE_3_MAX")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Interaction Matrix")
        
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
        
        val stateKey = stateKeys[selectedTab]
        
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

        SettingsCategoryHeader("${tabs[selectedTab]} State Actions")
        
        MD3GestureSelector("Single Tap", "${stateKey}_single_tap", prefs, currentAssignments)
        MD3GestureSelector("Double Tap", "${stateKey}_double_tap", prefs, currentAssignments)
        MD3GestureSelector("Long Press", "${stateKey}_long_press", prefs, currentAssignments)
        MD3GestureSelector("Swipe Left", "${stateKey}_swipe_left", prefs, currentAssignments)
        MD3GestureSelector("Swipe Right", "${stateKey}_swipe_right", prefs, currentAssignments)
        MD3GestureSelector("Swipe Up", "${stateKey}_swipe_up", prefs, currentAssignments)
        MD3GestureSelector("Swipe Down", "${stateKey}_swipe_down", prefs, currentAssignments)
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun MD3GestureSelector(
    label: String,
    prefsKey: String,
    prefs: SharedPreferences,
    inUseActions: List<String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedAction by remember { mutableStateOf(prefs.getString(prefsKey, "none") ?: "none") }
    var pendingOverrideAction by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    val allActions = listOf("none", "dismiss", "expand", "next_track", "previous_track", "toggle_play_pause")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodyLarge, 
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedAction.replace("_", " ").capitalize(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allActions.forEach { action ->
                val inUse = action in inUseActions && action != selectedAction && action != "none"
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = action.replace("_", " ").capitalize(),
                            color = if (inUse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        ) 
                    },
                    onClick = {
                        expanded = false
                        if (inUse) {
                            pendingOverrideAction = action
                        } else {
                            selectedAction = action
                            NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString(prefsKey, action) }) {
                                NewConfigManager.sendGestureUpdate(context, prefs)
                            }
                        }
                    }
                )
            }
        }
    }

    if (pendingOverrideAction != null) {
        AlertDialog(
            onDismissRequest = { pendingOverrideAction = null },
            title = { Text("Override Action?") },
            text = { 
                Text("The action '${pendingOverrideAction?.replace("_", " ")?.capitalize()}' is already assigned to another gesture. Reassign it here?") 
            },
            confirmButton = {
                TextButton(onClick = {
                    val actionToOverride = pendingOverrideAction!!
                    
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
                }) {
                    Text("Reassign")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOverrideAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }