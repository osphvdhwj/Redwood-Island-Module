// File: app/src/main/java/com/example/dynamicisland/ui/ConfigScreens.kt
package com.example.dynamicisland.ui

import androidx.compose.material.icons.filled.Settings
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.gesture.IslandGesture   // ← ADD THIS

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── The rest of the file stays EXACTLY as you posted ─────────────────────
// … all the @Composable functions LayoutScreen, ThemeScreen, DashboardScreen, FeaturesScreen, GesturesScreen …
// (I'm not duplicating them because they are already correct.)

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
            w = prefs.getFloat("${currentPrefix}_w", ConfigManager.getDefaultWidth(currentPrefix))
            h = prefs.getFloat("${currentPrefix}_h", ConfigManager.getDefaultHeight(currentPrefix))
            x = prefs.getFloat("${currentPrefix}_x", 0f)
            y = prefs.getFloat("${currentPrefix}_y", 48f)
            // Initial sync
            ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = if (expandUpwards) Alignment.BottomCenter else Alignment.TopCenter) {
            if (currentPrefix != "tweaks") {
                val cornerRadius = when (currentPrefix) { "max" -> 42.dp; "mid" -> 16.dp; "cube" -> 24.dp; else -> (h / 2).dp }
                if (currentPrefix == "ring") {
                    Box(modifier = Modifier.offset(x = x.dp, y = y.dp).size(w.dp, h.dp).border(ringT.dp, Color.White.copy(alpha = 0.6f), CircleShape))
                } else {
                    Box(modifier = Modifier.offset(x = x.dp, y = y.dp).width(w.dp).height(h.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius)))
                }
            } else {
                Text("Physical Tweaks Active", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) { tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) }) } }

        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            if (currentPrefix == "tweaks") {
                Text(text = "Physical Adjustments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "Adjust the Island without recompiling code.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))

                var offsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                Text(text = "Y-Axis Offset (Push down from top): ${offsetY.toInt()}px", color = Color.White)
                Slider(value = offsetY, onValueChange = { 
                    offsetY = it
                    ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("tweak_offset_y", it) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                }, valueRange = 0f..150f)

                var baseWidth by remember { mutableFloatStateOf(prefs.getFloat("tweak_base_width", 100f)) }
                Text(text = "Mini Pill Width: ${baseWidth.toInt()}dp", color = Color.White)
                Slider(value = baseWidth, onValueChange = { 
                    baseWidth = it
                    ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("tweak_base_width", it) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                }, valueRange = 50f..200f)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Configure ${tabs[selectedTab]}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = { 
                        w = ConfigManager.getDefaultWidth(currentPrefix); h = ConfigManager.getDefaultHeight(currentPrefix); x = 0f; y = 48f
                        ConfigManager.commitAndBroadcast(prefs, scope, context, {
                            putFloat("pad_t", 0f).putFloat("pad_b", 0f).putFloat("pad_l", 0f).putFloat("pad_r", 0f)
                        }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.7f))) { Text("Reset") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Expand Upwards (From Bottom)", fontSize = 14.sp)
                    Switch(checked = expandUpwards, onCheckedChange = { 
                        expandUpwards = it
                        ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, it)
                    })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Outer Dimensions", fontSize = 12.sp, color = Color.Gray)
                PrecisionSlider("Width", w, 10f..400f, { w = it }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                PrecisionSlider("Height", h, 10f..400f, { h = it }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                PrecisionSlider("X Pos", x, -200f..200f, { x = it }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                PrecisionSlider("Y Pos", y, -100f..200f, { y = it }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                
                if (currentPrefix == "ring") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ring Properties", fontSize = 12.sp, color = Color.Gray)
                    PrecisionSlider("Thickness", ringT, 1f..20f, { ringT = it }) { 
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("ring_thickness", ringT) }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Inner Compression (Padding)", fontSize = 12.sp, color = Color.Gray)
                var padT by remember { mutableFloatStateOf(prefs.getFloat("pad_t", 0f)) }; var padB by remember { mutableFloatStateOf(prefs.getFloat("pad_b", 0f)) }; var padL by remember { mutableFloatStateOf(prefs.getFloat("pad_l", 0f)) }; var padR by remember { mutableFloatStateOf(prefs.getFloat("pad_r", 0f)) }
                PrecisionSlider("Top", padT, 0f..100f, { padT = it }) { ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("pad_t", padT) }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) } }
                PrecisionSlider("Bottom", padB, 0f..100f, { padB = it }) { ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("pad_b", padB) }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) } }
                PrecisionSlider("Left", padL, 0f..100f, { padL = it }) { ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("pad_l", padL) }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) } }
                PrecisionSlider("Right", padR, 0f..100f, { padR = it }) { ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("pad_r", padR) }) { ConfigManager.saveAndBroadcast(prefs, scope, context, currentPrefix, w, h, x, y, ringT, expandUpwards) } }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "UI Customization Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "Customize the physical appearance of inner elements.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Physics & Animation Tuning", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Yellow)
        
        var glassMode by remember { mutableStateOf(prefs.getBoolean("glass_mode", true)) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("True Glassmorphism", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = glassMode, onCheckedChange = { 
                glassMode = it
                ConfigManager.commitAndBroadcast(prefs, scope, context, { putBoolean("glass_mode", it) }) { ConfigManager.sendGestureUpdate(context, prefs) }
            })
        }

        var damping by remember { mutableFloatStateOf(prefs.getFloat("spring_damping", 0.85f) * 100f) }
        Text(text = "Animation Bounciness (Lower = Wobble): ${damping.toInt()}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Slider(value = damping, onValueChange = { damping = it }, onValueChangeFinished = {
            ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_damping", damping / 100f) }) { ConfigManager.sendGestureUpdate(context, prefs) }
        }, valueRange = 10f..100f)

        var stiffness by remember { mutableFloatStateOf(prefs.getFloat("spring_stiffness", 400f)) }
        Text(text = "Animation Speed/Stiffness: ${stiffness.toInt()}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Slider(value = stiffness, onValueChange = { stiffness = it }, onValueChangeFinished = {
            ConfigManager.commitAndBroadcast(prefs, scope, context, { putFloat("spring_stiffness", stiffness) }) { ConfigManager.sendGestureUpdate(context, prefs) }
        }, valueRange = 50f..1000f)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Icon Pack", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Cyan)
        var selectedIconPack by remember { mutableStateOf(prefs.getString("icon_pack", "AMOLED_CYBERPUNK") ?: "AMOLED_CYBERPUNK") }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("AMOLED_CYBERPUNK" to "Cyberpunk", "MATERIAL_YOU" to "Material", "CUPERTINO_GLASS" to "Glass").forEach { (pack, label) ->
                FilterChip(
                    selected = selectedIconPack == pack,
                    onClick = {
                        selectedIconPack = pack
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("icon_pack", pack) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00FFFF).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF00FFFF),
                        selectedLeadingIconColor = Color(0xFF00FFFF)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedIconPack == pack,
                        borderColor = if (selectedIconPack == pack) Color(0xFF00FFFF) else Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Premium Effects", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Magenta)
        ThemeSlider("Haptic Strength (0=Off, 3=Heavy)", "haptic_strength", 1f, 0f..3f, prefs, scope, context)
        ThemeSlider("Background Blur Quality (0=Off)", "blur_intensity", 16f, 0f..24f, prefs, scope, context)
        
        var chargingExpanded by remember { mutableStateOf(false) }
        var selectedCharging by remember { mutableStateOf(prefs.getString("charging_style", "CUBE") ?: "CUBE") }
        ExposedDropdownMenuBox(expanded = chargingExpanded, onExpandedChange = { chargingExpanded = !chargingExpanded }) {
            OutlinedTextField(value = selectedCharging, onValueChange = {}, readOnly = true, label = { Text("Charging Animation Style") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chargingExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 8.dp))
            ExposedDropdownMenu(expanded = chargingExpanded, onDismissRequest = { chargingExpanded = false }) {
                listOf("CUBE", "APPLE", "HYPEROS").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { 
                        selectedCharging = opt; chargingExpanded = false
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("charging_style", opt) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Interactive Buttons (Max Pill)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00FFCC))
        
        var animExpanded by remember { mutableStateOf(false) }
        var selectedAnim by remember { mutableStateOf(prefs.getString("theme_anim_type", "BOUNCE") ?: "BOUNCE") }
        ExposedDropdownMenuBox(expanded = animExpanded, onExpandedChange = { animExpanded = !animExpanded }) {
            OutlinedTextField(value = selectedAnim, onValueChange = {}, readOnly = true, label = { Text("Click Animation Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = animExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 8.dp))
            ExposedDropdownMenu(expanded = animExpanded, onDismissRequest = { animExpanded = false }) {
                listOf("CHECKMARK", "BOUNCE", "PULSE", "NONE").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = { 
                        selectedAnim = opt; animExpanded = false
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("theme_anim_type", opt) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    })
                }
            }
        }

        ThemeSlider("Button Icon Size (dp)", "theme_button_size", 48f, 20f..80f, prefs, scope, context)
        ThemeSlider("Gap Between Buttons (dp)", "theme_button_spacing", 16f, 0f..40f, prefs, scope, context)
        ThemeSlider("Button Shape (0=Square, 50=Circle)", "theme_button_radius", 50f, 0f..50f, prefs, scope, context)
        
        Spacer(modifier = Modifier.height(16.dp))
        ThemeSlider("Corner Radius", "theme_corner_radius", 50f, 10f..100f, prefs, scope, context)
        ThemeSlider("Global Text Size (sp)", "theme_text_primary", 16f, 10f..30f, prefs, scope, context)
        ThemeSlider("Global Subtext Size (sp)", "theme_text_secondary", 14f, 8f..24f, prefs, scope, context)
        ThemeSlider("Global Progress Thickness", "theme_progress_thick", 4f, 1f..15f, prefs, scope, context)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Music Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Cyan)
        ThemeSlider("Title Text Size", "theme_music_title", 16f, 10f..30f, prefs, scope, context)
        ThemeSlider("Artist Text Size", "theme_music_artist", 14f, 8f..24f, prefs, scope, context)
        ThemeSlider("Seeker Thickness", "theme_music_seeker", 4f, 1f..15f, prefs, scope, context)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Battery Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Green)
        ThemeSlider("Cube Text Size", "theme_bat_text", 16f, 10f..30f, prefs, scope, context)
        ThemeSlider("Cube Icon Size", "theme_bat_icon", 36f, 16f..72f, prefs, scope, context)
        ThemeSlider("Ring Thickness", "theme_bat_ring", 12f, 2f..25f, prefs, scope, context)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Notification Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
        ThemeSlider("Alert Title Size", "theme_alert_title", 16f, 10f..30f, prefs, scope, context)
        ThemeSlider("Alert Message Size", "theme_alert_msg", 14f, 8f..24f, prefs, scope, context)
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dynamicQSTiles by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    // 🚀 FIXED: Use DisposableEffect to unregister the receiver and prevent memory leaks
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

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Quick Settings Grid", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
        Text(text = "Select 7 hardware toggles.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        if (dynamicQSTiles.isEmpty()) {
             CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
             Text("Fetching native tiles from SystemUI...", color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Column {
                for (row in 0..3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            if (index < 7) {
                                var expanded by remember { mutableStateOf(false) }
                                var selectedSpec by remember { mutableStateOf(prefs.getString("qs_tile_spec_$index", "") ?: "") }
                                var selectedLabel by remember { mutableStateOf(prefs.getString("qs_tile_label_$index", "Select Tile") ?: "Select Tile") }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                        OutlinedTextField(
                                            value = selectedLabel, onValueChange = {}, readOnly = true, label = { Text("QS ${index + 1}") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 4.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                            maxLines = 1
                                        )
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            dynamicQSTiles.forEach { (spec, label) ->
                                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                                    selectedSpec = spec; selectedLabel = label; expanded = false
                                                    ConfigManager.commitAndBroadcast(prefs, scope, context, { 
                                                        putString("qs_tile_spec_$index", spec)
                                                        putString("qs_tile_label_$index", label) 
                                                    }) { ConfigManager.broadcastUpdateSingle(context, prefs, "dashboard") }
                                                })
                                            }
                                        }
                                    }
                                }
                            } else { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "App Dock Pinning", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
        Text(text = "Select 8 apps to pin.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        val pm = context.packageManager
        var installedApps by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
        
        LaunchedEffect(Unit) {
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

        if (installedApps.isEmpty()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        } else {
            Column {
                for (row in 0..3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            var expanded by remember { mutableStateOf(false) }
                            var selectedAppPkg by remember { mutableStateOf(prefs.getString("pinned_app_$index", "") ?: "") }
                            val selectedAppName = installedApps.find { it.second == selectedAppPkg }?.first ?: "None"

                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                    OutlinedTextField(
                                        value = selectedAppName, onValueChange = {}, readOnly = true, label = { Text("App ${index + 1}") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 4.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                                        maxLines = 1
                                    )
                                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        DropdownMenuItem(text = { Text("None") }, onClick = {
                                            selectedAppPkg = ""; expanded = false
                                            ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("pinned_app_$index", "") }) { ConfigManager.broadcastUpdateSingle(context, prefs, "dashboard") }
                                        })
                                        installedApps.forEach { pair ->
                                            DropdownMenuItem(text = { Text(pair.first) }, onClick = {
                                                selectedAppPkg = pair.second; expanded = false
                                                ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("pinned_app_$index", pair.second) }) { ConfigManager.broadcastUpdateSingle(context, prefs, "dashboard") }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun FeaturesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(text = "Active Modules", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "Selectively disable Island behaviors.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        FeatureSwitch("Auto-Hide in Landscape (Gaming/Video)", "hide_landscape", false, prefs, scope, context)
        FeatureSwitch("Enable Media Pill (Music/Spotify)", "enable_media", true, prefs, scope, context)
        FeatureSwitch("Enable Charging Cube", "enable_charging", true, prefs, scope, context)
        FeatureSwitch("Enable System Alerts (Battery/Temp)", "enable_alerts", true, prefs, scope, context)
        FeatureSwitch("Enable App Timers (Wellbeing)", "enable_timers", true, prefs, scope, context)
        FeatureSwitch("Spin Album Art in Mini Pill", "rotate_cube", true, prefs, scope, context)

        // New toggles and button
        Spacer(modifier = Modifier.height(16.dp))
        Text("Batch 6 — Defining Features", fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD54F))

        FeatureSwitch("On-Device Translation Overlay",
            "enable_translation", true, prefs, scope, context)

        FeatureSwitch("Continuity Camera (QR / Barcode)",
            "enable_continuity_camera", false, prefs, scope, context)

        FeatureSwitch("Gaming HUD (FPS · Temp · Freq)",
            "enable_gaming_hud", true, prefs, scope, context)

        // Button to open Accessibility Settings
        Button(
            onClick = {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.10f)
            )
        ) {
            Icon(Icons.Default.Settings, null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Open Accessibility Settings", color = Color.White)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        
        // 🚀 ADDED: Smart Gesture Fallbacks Section
        Text(text = "Smart Gesture Fallbacks", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
        Text(text = "When no media is playing or active.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        // Idle Swipe Action Dropdown
        var swipeExpanded by remember { mutableStateOf(false) }
        var selectedSwipe by remember { mutableStateOf(prefs.getString("idle_swipe_action", "BRIGHTNESS") ?: "BRIGHTNESS") }
        ExposedDropdownMenuBox(expanded = swipeExpanded, onExpandedChange = { swipeExpanded = !swipeExpanded }) {
            OutlinedTextField(
                value = selectedSwipe, onValueChange = {}, readOnly = true, 
                label = { Text("Idle Horizontal Swipe") }, 
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = swipeExpanded) }, 
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 8.dp)
            )
            ExposedDropdownMenu(expanded = swipeExpanded, onDismissRequest = { swipeExpanded = false }) {
                listOf("BRIGHTNESS", "VOLUME", "PREV_APP", "NONE").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        selectedSwipe = opt; swipeExpanded = false
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("idle_swipe_action", opt) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    })
                }
            }
        }

        // Long Press Action Dropdown
        var lpExpanded by remember { mutableStateOf(false) }
        var selectedLp by remember { mutableStateOf(prefs.getString("long_press_action", "SCREENSHOT") ?: "SCREENSHOT") }
        ExposedDropdownMenuBox(expanded = lpExpanded, onExpandedChange = { lpExpanded = !lpExpanded }) {
            OutlinedTextField(
                value = selectedLp, onValueChange = {}, readOnly = true, 
                label = { Text("Default Long Press") }, 
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lpExpanded) }, 
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 8.dp)
            )
            ExposedDropdownMenu(expanded = lpExpanded, onDismissRequest = { lpExpanded = false }) {
                listOf("SCREENSHOT", "OPEN_QUICK_TOGGLES", "LAUNCH_CAMERA").forEach { opt ->
                    DropdownMenuItem(text = { Text(opt) }, onClick = {
                        selectedLp = opt; lpExpanded = false
                        ConfigManager.commitAndBroadcast(prefs, scope, context, { putString("long_press_action", opt) }) { ConfigManager.sendGestureUpdate(context, prefs) }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Existing Action Matrix
        Text(text = "Action Matrix", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        val states = listOf("Ring" to "TYPE_0_RING", "Mini Pill" to "TYPE_1_MINI", "Mid Pill" to "TYPE_2_MID", "Max Pill" to "TYPE_3_MAX")
        val gestures = IslandGesture.values()
        val actionOptions = IslandAction.values()

        states.forEach { (label, stateKey) ->
            var expanded by remember { mutableStateOf(false) }
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                        exit = shrinkVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                    ) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            gestures.forEach { gesture ->
                                val prefKey = "${stateKey}_${gesture.name}"
                                GestureDropdown(label = gesture.name.replace("_", " "), options = actionOptions, prefs = prefs, prefKey = prefKey, scope = scope, context = context)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}
