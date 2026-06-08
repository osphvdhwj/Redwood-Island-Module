package com.example.dynamicisland.core.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.core.ui.components.SettingsSwitch
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.settings.*
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
        SettingsCategoryHeader("Smart AI Gestures")
        
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                SettingsSwitch(
                    title = "Enable Smart Engine", 
                    description = "AI predicts intent and overrides defaults.", 
                    checked = prefs.getBoolean("smart_gestures_enabled", true),
                    onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("smart_gestures_enabled", it) }) }
                )
                
                if (prefs.getBoolean("smart_gestures_enabled", true)) {
                    Divider(modifier = Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitch(
                        title = "Smart Media Controls", 
                        description = "Swipe social media apps to change songs.", 
                        checked = prefs.getBoolean("smart_media_override", true),
                        onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("smart_media_override", it) }) }
                    )
                        title = "Smart Gaming Mode", 
                        description = "Auto-Dashboard & Dismiss during games.", 
                        checked = prefs.getBoolean("smart_gaming_override", true),
                        onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("smart_gaming_override", it) }) }
                        title = "Smart Call Handling", 
                        description = "Tap to mute, long press to hang up.", 
                        checked = prefs.getBoolean("smart_call_override", true),
                        onCheckedChange = { NewConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("smart_call_override", it) }) }
                }
            }
        }
        SettingsCategoryHeader("Manual Fallback Actions")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 16.dp,
            divider = {},
            containerColor = androidx.compose.ui.graphics.Color.Transparent
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
        val stateKey = stateKeys[selectedTab]
        val gestureSuffixes = listOf("single_tap", "double_tap", "long_press", "swipe_left", "swipe_right", "swipe_up", "swipe_down")
        // Re-read current assignments to show occupied status
        val currentAssignments = remember(selectedTab, prefs.all) {
            gestureSuffixes.associateWith { suffix -> prefs.getString("${stateKey}_$suffix", "NONE") ?: "NONE" }
        SettingsCategoryHeader("${tabs[selectedTab]} Contextual Actions")
        gestureSuffixes.forEach { suffix ->
            val label = suffix.replace("_", " ").capitalize()
            val prefsKey = "${stateKey}_$suffix"
            MD3GestureSelector(label, prefsKey, prefs, currentAssignments)
        Spacer(modifier = Modifier.height(100.dp))
    }
}
private fun MD3GestureSelector(
    label: String,
    prefsKey: String,
    prefs: SharedPreferences,
    assignments: Map<String, String>
) {
    val actualSelectedAction = (assignments[prefsKey] ?: "NONE").uppercase()
    var expanded by remember { mutableStateOf(false) }
    val allActions = listOf(
        "NONE", "COLLAPSE", "EXPAND", "NEXT_TRACK", "PREV_TRACK", "PLAY_PAUSE", 
        "OPEN_SOURCE_APP", "TOGGLE_TORCH", "MUTE_TOGGLE", "FORCE_DISMISS", "VOLUME_UP", "VOLUME_DOWN", "SCREENSHOT"
    )
    Row(
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label, 
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.onSurface
            )
        Row(verticalAlignment = Alignment.CenterVertically) {
                text = actualSelectedAction.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = if (actualSelectedAction == "NONE") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allActions.forEach { action ->
                val isOccupied = assignments.values.count { it.uppercase() == action && action != "NONE" } > 0
                val isCurrentlyThis = actualSelectedAction == action
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = action.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                color = if (isCurrentlyThis) MaterialTheme.colorScheme.primary 
                                        else if (isOccupied) MaterialTheme.colorScheme.error 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            if (isOccupied && !isCurrentlyThis) {
                                Spacer(Modifier.width(8.dp))
                                Text("(In Use)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, {
                            if (isOccupied && action != "NONE") {
                                val statePrefix = prefsKey.substringBeforeLast("_")
                                assignments.filter { it.value.uppercase() == action }.keys.forEach { suffix ->
                                    putString("${statePrefix}_$suffix", "NONE")
                                }
                            putString(prefsKey, action)
                        }) {
                            NewConfigManager.sendGestureUpdate(context, prefs)
                    }
private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
