package com.example.dynamicisland.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinsTilesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHapticManager()
    
    var dynamicQSTiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var installedApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == "com.example.dynamicisland.QS_TILES_REPLY") {
                    val jsonStr = intent.getStringExtra("tiles_json") ?: return
                    try {
                        val array = org.json.JSONArray(jsonStr)
                        val list = mutableListOf<Pair<String, String>>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(Pair(obj.getString("spec"), obj.getString("label")))
                        }
                        dynamicQSTiles = list
                    } catch(e: Exception){}
                }
            }
        }
        context.registerReceiver(receiver, android.content.IntentFilter("com.example.dynamicisland.QS_TILES_REPLY"), Context.RECEIVER_EXPORTED)
        context.sendBroadcast(Intent("com.example.dynamicisland.FETCH_QS_TILES"))
        
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        withContext(Dispatchers.IO) {
            try {
                val apps = pm.getInstalledApplications(0)
                    .filter { appInfo -> try { pm.getLaunchIntentForPackage(appInfo.packageName) != null } catch(e:Throwable){false} }
                    .map { appInfo -> Pair(appInfo.loadLabel(pm).toString(), appInfo.packageName) }
                    .sortedBy { pair -> pair.first }
                installedApps = apps
            } catch(e: Throwable) {}
        }
    }

    PullToRefreshContainer(onRefresh = { 
        haptics.medium()
        context.sendBroadcast(Intent("com.example.dynamicisland.FETCH_QS_TILES")) 
    }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            StaggeredItem(0) { 
                SectionHeader("Pins & Tiles", "Customizing your quick actions", Icons.Default.List, IslandColors.accentCyan) 
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            StaggeredItem(1) {
                SettingsGroup(title = "Quick Settings Grid", icon = Icons.Default.Settings, summary = "7 Interactive Slots") {
                    if (dynamicQSTiles.isEmpty()) {
                         SkeletonLoader(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp).size(200.dp, 160.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                for (i in 0 until 4) { TileSlot(i, prefs, dynamicQSTiles, scope, context, haptics) }
                            }
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                for (i in 4 until 7) { TileSlot(i, prefs, dynamicQSTiles, scope, context, haptics) }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            StaggeredItem(2) { 
                SectionHeader("App Dock", "Pin your favorite applications", Icons.Default.Apps, IslandColors.accentPurple) 
            }
            
            StaggeredItem(3) {
                SettingsGroup(title = "App Dock Pinning", icon = Icons.Default.Star, summary = "8 Dynamic Pins") {
                    if (installedApps.isEmpty()) {
                        SkeletonLoader(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp).size(300.dp, 80.dp))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            items(8) { index -> AppPinSlot(index, prefs, installedApps, scope, context, haptics) }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TileSlot(index: Int, prefs: SharedPreferences, dynamicQSTiles: List<Pair<String, String>>, scope: kotlinx.coroutines.CoroutineScope, context: Context, haptics: HapticManager) {
    var selectedLabel by remember { mutableStateOf(prefs.getString("qs_tile_label_$index", "Empty") ?: "Empty") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .glassmorphicCard(cornerRadius = 16.dp)
            .premiumClickable { 
                haptics.light()
                expanded = true 
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = selectedLabel.take(4).uppercase(),
            color = if (selectedLabel == "Empty") IslandColors.textSecondary else IslandColors.accentCyan,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
        
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Empty") }, onClick = {
                selectedLabel = "Empty"
                expanded = false
                prefs.edit().putString("qs_tile_label_$index", "Empty").putString("qs_tile_spec_$index", "empty").apply()
                NewConfigManager.broadcastUpdateSingle(context, prefs, "dashboard")
            })
            dynamicQSTiles.forEach { tile ->
                DropdownMenuItem(text = { Text(tile.second) }, onClick = {
                    selectedLabel = tile.second
                    expanded = false
                    prefs.edit().putString("qs_tile_label_$index", tile.second).putString("qs_tile_spec_$index", tile.first).apply()
                    NewConfigManager.broadcastUpdateSingle(context, prefs, "dashboard")
                })
            }
        }
    }
}

@Composable
fun AppPinSlot(index: Int, prefs: SharedPreferences, installedApps: List<Pair<String, String>>, scope: kotlinx.coroutines.CoroutineScope, context: Context, haptics: HapticManager) {
    var selectedAppPkg by remember { mutableStateOf(prefs.getString("pinned_app_$index", "") ?: "") }
    val selectedAppName = installedApps.find { it.second == selectedAppPkg }?.first ?: "+"
    var expanded by remember { mutableStateOf(false) }
    val hasApp = selectedAppPkg.isNotEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .glassmorphicCard(cornerRadius = 30.dp)
                .border(2.dp, if (hasApp) IslandColors.accentCyan else IslandColors.border, CircleShape)
                .premiumClickable { 
                    haptics.light()
                    expanded = true 
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasApp) selectedAppName.take(1).uppercase() else "+",
                color = if (hasApp) IslandColors.accentCyan else IslandColors.textSecondary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(selectedAppName.take(8), color = IslandColors.textSecondary, style = MaterialTheme.typography.labelSmall)

        if (expanded) {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("None") }, onClick = {
                    selectedAppPkg = ""; expanded = false
                    NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("pinned_app_$index", "") }) { NewConfigManager.broadcastUpdateSingle(context, prefs, "dashboard") }
                })
                installedApps.forEach { pair ->
                    DropdownMenuItem(text = { Text(pair.first) }, onClick = {
                        selectedAppPkg = pair.second; expanded = false
                        NewConfigManager.commitAndBroadcast(prefs, scope, context, { putString("pinned_app_$index", pair.second) }) { NewConfigManager.broadcastUpdateSingle(context, prefs, "dashboard") }
                    })
                }
            }
        }
    }
}
