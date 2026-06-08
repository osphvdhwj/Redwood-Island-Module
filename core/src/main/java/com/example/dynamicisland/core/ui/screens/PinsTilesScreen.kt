package com.example.dynamicisland.core.ui.screens

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.manager.IslandBackupManager
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.components.SettingsCategoryHeader
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PinsTilesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SettingsCategoryHeader("Quick Settings Grid (7 Slots)")
        
        if (dynamicQSTiles.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp), 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until 4) { MD3TileSlot(i, prefs, dynamicQSTiles, context) }
                }
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    for (i in 4 until 7) { MD3TileSlot(i, prefs, dynamicQSTiles, context) }
                }
            }
        }
        
        SettingsCategoryHeader("App Dock (8 Pins)")
        
        if (installedApps.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(32.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp), 
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(8) { index -> MD3AppPinSlot(index, prefs, installedApps, scope, context) }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun MD3TileSlot(index: Int, prefs: SharedPreferences, dynamicQSTiles: List<Pair<String, String>>, context: Context) {
    var selectedLabel by remember { mutableStateOf(prefs.getString("qs_tile_label_$index", "Empty") ?: "Empty") }
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { expanded = true },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = selectedLabel.take(4).uppercase(),
            color = if (selectedLabel == "Empty") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
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
fun MD3AppPinSlot(index: Int, prefs: SharedPreferences, installedApps: List<Pair<String, String>>, scope: kotlinx.coroutines.CoroutineScope, context: Context) {
    var selectedAppPkg by remember { mutableStateOf(prefs.getString("pinned_app_$index", "") ?: "") }
    val selectedAppName = installedApps.find { it.second == selectedAppPkg }?.first ?: "+"
    var expanded by remember { mutableStateOf(false) }
    val hasApp = selectedAppPkg.isNotEmpty()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (hasApp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasApp) selectedAppName.take(1).uppercase() else "+",
                color = if (hasApp) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(selectedAppName.take(8), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall)

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
