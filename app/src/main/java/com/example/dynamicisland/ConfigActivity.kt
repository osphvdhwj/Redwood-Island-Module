package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.TouchApp
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
import org.json.JSONObject
import java.io.File

class ConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        setContent { 
            MaterialTheme(colorScheme = darkColorScheme()) { 
                Surface(modifier = Modifier.fillMaxSize()) { 
                    ConfigScreen(prefs) 
                } 
            } 
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigScreen(prefs: android.content.SharedPreferences) {
        var selectedNav by remember { mutableIntStateOf(0) }
        
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedNav == 0, onClick = { selectedNav = 0 }, icon = { Icon(Icons.Default.AspectRatio, null) }, label = { Text("Layout") })
                    NavigationBarItem(selected = selectedNav == 1, onClick = { selectedNav = 1 }, icon = { Icon(Icons.Default.Palette, null) }, label = { Text("Theme") })
                    NavigationBarItem(selected = selectedNav == 2, onClick = { selectedNav = 2 }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Dashboard") })
                    NavigationBarItem(selected = selectedNav == 3, onClick = { selectedNav = 3 }, icon = { Icon(Icons.Default.SettingsSystemDaydream, null) }, label = { Text("Features") })
                    NavigationBarItem(selected = selectedNav == 4, onClick = { selectedNav = 4 }, icon = { Icon(Icons.Default.TouchApp, null) }, label = { Text("Gestures") })
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                when (selectedNav) {
                    0 -> LayoutScreen(prefs)
                    1 -> ThemeScreen(prefs)
                    2 -> DashboardScreen(prefs)
                    3 -> FeaturesScreen(prefs)
                    4 -> GesturesScreen(prefs)
                }
            }
        }
    }

    @Composable
    fun LayoutScreen(prefs: android.content.SharedPreferences) {
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
                w = prefs.getFloat("${currentPrefix}_w", getDefaultWidth(currentPrefix))
                h = prefs.getFloat("${currentPrefix}_h", getDefaultHeight(currentPrefix))
                x = prefs.getFloat("${currentPrefix}_x", 0f)
                y = prefs.getFloat("${currentPrefix}_y", 48f)
                broadcastUpdate(currentPrefix, w, h, x, y, ringT, expandUpwards)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = if (expandUpwards) Alignment.BottomCenter else Alignment.TopCenter) {
                if (currentPrefix != "tweaks") {
                    val cornerRadius = when (currentPrefix) {
                        "max" -> 42.dp
                        "mid" -> 16.dp
                        "cube" -> 24.dp
                        else -> (h / 2).dp
                    }

                    if (currentPrefix == "ring") {
                        Box(
                            modifier = Modifier
                                .offset(x = x.dp, y = y.dp)
                                .size(w.dp, h.dp)
                                .border(ringT.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .offset(x = x.dp, y = y.dp)
                                .width(w.dp)
                                .height(h.dp)
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(cornerRadius))
                        )
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
                    Slider(value = offsetY, onValueChange = { offsetY = it; prefs.edit().putFloat("tweak_offset_y", it).apply(); sendGestureUpdate(prefs, this@ConfigActivity) }, valueRange = 0f..150f)

                    var baseWidth by remember { mutableFloatStateOf(prefs.getFloat("tweak_base_width", 100f)) }
                    Text(text = "Mini Pill Width: ${baseWidth.toInt()}dp", color = Color.White)
                    Slider(value = baseWidth, onValueChange = { baseWidth = it; prefs.edit().putFloat("tweak_base_width", it).apply(); sendGestureUpdate(prefs, this@ConfigActivity) }, valueRange = 50f..200f)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Configure ${tabs[selectedTab]}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = { w = getDefaultWidth(currentPrefix); h = getDefaultHeight(currentPrefix); x = 0f; y = 48f; prefs.edit().putFloat("pad_t", 0f).putFloat("pad_b", 0f).putFloat("pad_l", 0f).putFloat("pad_r", 0f).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.7f))) { Text("Reset") }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Expand Upwards (From Bottom)", fontSize = 14.sp)
                        Switch(checked = expandUpwards, onCheckedChange = { expandUpwards = it; prefs.edit().putBoolean("expand_upwards", it).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Outer Dimensions", fontSize = 12.sp, color = Color.Gray)
                    PrecisionSlider("Width", w, 10f..400f, { w = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("Height", h, 10f..400f, { h = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("X Pos", x, -200f..200f, { x = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("Y Pos", y, -100f..200f, { y = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    if (currentPrefix == "ring") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ring Properties", fontSize = 12.sp, color = Color.Gray)
                        PrecisionSlider("Thickness", ringT, 1f..20f, { ringT = it }) { prefs.edit().putFloat("ring_thickness", ringT).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Inner Compression (Padding)", fontSize = 12.sp, color = Color.Gray)
                    var padT by remember { mutableFloatStateOf(prefs.getFloat("pad_t", 0f)) }; var padB by remember { mutableFloatStateOf(prefs.getFloat("pad_b", 0f)) }; var padL by remember { mutableFloatStateOf(prefs.getFloat("pad_l", 0f)) }; var padR by remember { mutableFloatStateOf(prefs.getFloat("pad_r", 0f)) }
                    PrecisionSlider("Top", padT, 0f..100f, { padT = it }) { prefs.edit().putFloat("pad_t", padT).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("Bottom", padB, 0f..100f, { padB = it }) { prefs.edit().putFloat("pad_b", padB).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("Left", padL, 0f..100f, { padL = it }) { prefs.edit().putFloat("pad_l", padL).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    PrecisionSlider("Right", padR, 0f..100f, { padR = it }) { prefs.edit().putFloat("pad_r", padR).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ThemeScreen(prefs: android.content.SharedPreferences) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(text = "UI Customization Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Customize the physical appearance of inner elements.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Interactive Buttons (Max Pill)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00FFCC))
            
            var animExpanded by remember { mutableStateOf(false) }
            var selectedAnim by remember { mutableStateOf(prefs.getString("theme_anim_type", "BOUNCE") ?: "BOUNCE") }
            ExposedDropdownMenuBox(expanded = animExpanded, onExpandedChange = { animExpanded = !animExpanded }) {
                OutlinedTextField(value = selectedAnim, onValueChange = {}, readOnly = true, label = { Text("Click Animation Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = animExpanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 8.dp))
                ExposedDropdownMenu(expanded = animExpanded, onDismissRequest = { animExpanded = false }) {
                    listOf("CHECKMARK", "BOUNCE", "PULSE", "NONE").forEach { opt ->
                        DropdownMenuItem(text = { Text(opt) }, onClick = { selectedAnim = opt; prefs.edit().putString("theme_anim_type", opt).apply(); animExpanded = false; sendGestureUpdate(prefs, this@ConfigActivity) })
                    }
                }
            }

            ThemeSlider("Button Icon Size (dp)", "theme_button_size", 48f, 20f..80f, prefs)
            ThemeSlider("Gap Between Buttons (dp)", "theme_button_spacing", 16f, 0f..40f, prefs)
            ThemeSlider("Button Shape (0=Square, 50=Circle)", "theme_button_radius", 50f, 0f..50f, prefs)
            
            Spacer(modifier = Modifier.height(16.dp))

            ThemeSlider("Corner Radius", "theme_corner_radius", 50f, 10f..100f, prefs)
            ThemeSlider("Global Text Size (sp)", "theme_text_primary", 16f, 10f..30f, prefs)
            ThemeSlider("Global Subtext Size (sp)", "theme_text_secondary", 14f, 8f..24f, prefs)
            ThemeSlider("Global Progress Thickness", "theme_progress_thick", 4f, 1f..15f, prefs)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Music Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Cyan)
            ThemeSlider("Title Text Size", "theme_music_title", 16f, 10f..30f, prefs)
            ThemeSlider("Artist Text Size", "theme_music_artist", 14f, 8f..24f, prefs)
            ThemeSlider("Seeker Thickness", "theme_music_seeker", 4f, 1f..15f, prefs)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Battery Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Green)
            ThemeSlider("Cube Text Size", "theme_bat_text", 16f, 10f..30f, prefs)
            ThemeSlider("Cube Icon Size", "theme_bat_icon", 36f, 16f..72f, prefs)
            ThemeSlider("Ring Thickness", "theme_bat_ring", 12f, 2f..25f, prefs)

            Spacer(modifier = Modifier.height(16.dp))
            Text("Notification Customizations", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
            ThemeSlider("Alert Title Size", "theme_alert_title", 16f, 10f..30f, prefs)
            ThemeSlider("Alert Message Size", "theme_alert_msg", 14f, 8f..24f, prefs)
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(prefs: android.content.SharedPreferences) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(text = "Quick Settings Grid", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
            Text(text = "Select 7 hardware toggles.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            val availableQS = listOf("None", "WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
            
            // 🎛️ FIXED: Compact 2-Column Grid for QS Tiles
            Column {
                for (row in 0..3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            if (index < 7) {
                                var expanded by remember { mutableStateOf(false) }
                                var selectedQS by remember { mutableStateOf(prefs.getString("qs_tile_$index", availableQS[index + 1]) ?: availableQS[index + 1]) }
                                
                                Box(modifier = Modifier.weight(1f)) {
                                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                        OutlinedTextField(
                                            value = selectedQS, onValueChange = {}, readOnly = true, label = { Text("QS ${index + 1}") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 4.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                        )
                                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                            availableQS.forEach { tile ->
                                                DropdownMenuItem(text = { Text(tile) }, onClick = {
                                                    selectedQS = tile; prefs.edit().putString("qs_tile_$index", tile).apply(); expanded = false
                                                    broadcastUpdateSingle("dashboard", prefs)
                                                })
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f)) // Empty slot for the 8th item
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "App Dock Pinning", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
            Text(text = "Select 8 apps to pin.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            val pm = LocalContext.current.packageManager
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
                // 🎛️ FIXED: Compact 2-Column Grid for Pinned Apps
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
                                                selectedAppPkg = ""; prefs.edit().putString("pinned_app_$index", "").apply(); expanded = false
                                                broadcastUpdateSingle("dashboard", prefs)
                                            })
                                            installedApps.forEach { pair ->
                                                DropdownMenuItem(text = { Text(pair.first) }, onClick = {
                                                    selectedAppPkg = pair.second; prefs.edit().putString("pinned_app_$index", pair.second).apply(); expanded = false
                                                    broadcastUpdateSingle("dashboard", prefs)
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
    fun FeaturesScreen(prefs: android.content.SharedPreferences) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(text = "Active Modules", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Selectively disable Island behaviors.", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            FeatureSwitch("Enable Media Pill (Music/Spotify)", "enable_media", true, prefs)
            FeatureSwitch("Enable Charging Cube", "enable_charging", true, prefs)
            FeatureSwitch("Enable System Alerts (Battery/Temp)", "enable_alerts", true, prefs)
            FeatureSwitch("Enable App Timers (Wellbeing)", "enable_timers", true, prefs)
            
            // 🎛️ FIXED: Added missing toggle for Mini Pill Album Art Rotation
            FeatureSwitch("Spin Album Art in Mini Pill", "rotate_cube", true, prefs)
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    @Composable
    fun FeatureSwitch(label: String, key: String, default: Boolean, prefs: android.content.SharedPreferences) {
        var checked by remember { mutableStateOf(prefs.getBoolean(key, default)) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = { checked = it; prefs.edit().putBoolean(key, it).apply(); sendGestureUpdate(prefs, this@ConfigActivity) })
        }
    }

    @Composable
    fun GesturesScreen(prefs: android.content.SharedPreferences) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
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
                        AnimatedVisibility(expanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                gestures.forEach { gesture ->
                                    val prefKey = "${stateKey}_${gesture.name}"
                                    GestureDropdown(label = gesture.name.replace("_", " "), options = actionOptions, prefs = prefs, prefKey = prefKey)
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

    @Composable
    fun ThemeSlider(label: String, key: String, default: Float, range: ClosedFloatingPointRange<Float>, prefs: android.content.SharedPreferences) {
        var localValue by remember { mutableFloatStateOf(prefs.getFloat(key, default)) }
        Text(text = "$label: ${localValue.toInt()}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
        Slider(
            value = localValue, 
            onValueChange = { localValue = it }, 
            onValueChangeFinished = { 
                prefs.edit().putFloat(key, localValue).apply() 
                sendGestureUpdate(prefs, this@ConfigActivity) 
            }, 
            valueRange = range
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GestureDropdown(label: String, options: Array<IslandAction>, prefs: android.content.SharedPreferences, prefKey: String) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(prefs.getString(prefKey, IslandAction.NONE.name) ?: IslandAction.NONE.name) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedOption.replace("_", " "), onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.replace("_", " ")) },
                        onClick = {
                            selectedOption = option.name
                            prefs.edit().putString(prefKey, option.name).apply()
                            expanded = false
                            sendGestureUpdate(prefs, this@ConfigActivity)
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun PrecisionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
        var localValue by remember(value) { mutableFloatStateOf(value) } 
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.width(60.dp), fontSize = 14.sp)
            IconButton(onClick = { 
                localValue = (localValue - 1f).coerceIn(range)
                onValueChange(localValue)
                onValueChangeFinished() 
            }) { Icon(Icons.Default.Remove, contentDescription = "-") }
            
            Slider(
                value = localValue, 
                onValueChange = { localValue = it }, 
                onValueChangeFinished = { 
                    onValueChange(localValue) 
                    onValueChangeFinished()   
                }, 
                modifier = Modifier.weight(1f), 
                valueRange = range
            )
            
            IconButton(onClick = { 
                localValue = (localValue + 1f).coerceIn(range)
                onValueChange(localValue)
                onValueChangeFinished() 
            }) { Icon(Icons.Default.Add, contentDescription = "+") }
            Text(String.format("%.0f", localValue), modifier = Modifier.width(40.dp), fontSize = 14.sp)
        }
    }

    private fun saveAndBroadcast(prefs: android.content.SharedPreferences, prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean) {
        prefs.edit().putFloat("${prefix}_w", w).putFloat("${prefix}_h", h).putFloat("${prefix}_x", x).putFloat("${prefix}_y", y).apply()
        makePrefsWorldReadable()
        broadcastUpdate(prefix, w, h, x, y, ringT, expandUp)
    }

    private fun broadcastUpdateSingle(prefix: String, prefs: android.content.SharedPreferences) {
        makePrefsWorldReadable()
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            @Suppress("WrongConstant")
            addFlags(android.content.Intent.FLAG_RECEIVER_FOREGROUND or 0x01000000)
            setPackage("com.android.systemui") 
            putExtra("prefix", prefix)
        }
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean) {
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        @Suppress("WrongConstant")
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").addFlags(0x01000000).apply {
            setPackage("com.android.systemui") 
        } 
        intent.putExtra("prefix", prefix).putExtra("w", w).putExtra("h", h).putExtra("x", x).putExtra("y", y).putExtra("ring_thickness", ringT).putExtra("expand_upwards", expandUp)
        intent.putExtra("pad_t", prefs.getFloat("pad_t", 0f)).putExtra("pad_b", prefs.getFloat("pad_b", 0f)).putExtra("pad_l", prefs.getFloat("pad_l", 0f)).putExtra("pad_r", prefs.getFloat("pad_r", 0f))
        
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        sendBroadcast(intent)
    }

    private fun sendGestureUpdate(prefs: android.content.SharedPreferences, context: android.content.Context) {
        val intent = android.content.Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            @Suppress("WrongConstant")
            addFlags(android.content.Intent.FLAG_RECEIVER_FOREGROUND or 0x01000000)
            setPackage("com.android.systemui") 
            
            putExtra("tweak_offset_y", prefs.getFloat("tweak_offset_y", 0f))
            putExtra("tweak_base_width", prefs.getFloat("tweak_base_width", 100f))
            
            putExtra("theme_button_size", prefs.getFloat("theme_button_size", 48f))
            putExtra("theme_button_spacing", prefs.getFloat("theme_button_spacing", 16f))
            putExtra("theme_button_radius", prefs.getFloat("theme_button_radius", 50f))
            putExtra("theme_anim_type", prefs.getString("theme_anim_type", "BOUNCE"))

            putExtra("theme_corner_radius", prefs.getFloat("theme_corner_radius", 50f))
            putExtra("theme_text_primary", prefs.getFloat("theme_text_primary", 16f))
            putExtra("theme_text_secondary", prefs.getFloat("theme_text_secondary", 14f))
            putExtra("theme_progress_thick", prefs.getFloat("theme_progress_thick", 4f))
            putExtra("theme_ring_thick", prefs.getFloat("theme_ring_thick", 12f))
            putExtra("theme_element_gap", prefs.getFloat("theme_element_gap", 8f))
            putExtra("theme_music_title", prefs.getFloat("theme_music_title", 16f))
            putExtra("theme_music_artist", prefs.getFloat("theme_music_artist", 14f))
            putExtra("theme_music_seeker", prefs.getFloat("theme_music_seeker", 4f))
            putExtra("theme_bat_text", prefs.getFloat("theme_bat_text", 16f))
            putExtra("theme_bat_icon", prefs.getFloat("theme_bat_icon", 36f))
            putExtra("theme_bat_ring", prefs.getFloat("theme_bat_ring", 12f))
            putExtra("theme_alert_title", prefs.getFloat("theme_alert_title", 16f))
            putExtra("theme_alert_msg", prefs.getFloat("theme_alert_msg", 14f))

            putExtra("enable_media", prefs.getBoolean("enable_media", true))
            putExtra("enable_charging", prefs.getBoolean("enable_charging", true))
            putExtra("enable_alerts", prefs.getBoolean("enable_alerts", true))
            putExtra("enable_timers", prefs.getBoolean("enable_timers", true))
            putExtra("rotate_cube", prefs.getBoolean("rotate_cube", true))
        }
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        context.sendBroadcast(intent)
    }

    private fun getDefaultWidth(prefix: String): Float = when(prefix) { "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "cube" -> 85f; else -> 0f }
    private fun getDefaultHeight(prefix: String): Float = when(prefix) { "ring" -> 45f; "mini" -> 36f; "mid" -> 80f; "max" -> 220f; "cube" -> 85f; else -> 0f }

    private fun makePrefsWorldReadable() {
        try {
            val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "island_prefs.xml")
            if (prefsDir.exists()) { prefsDir.setExecutable(true, false); prefsDir.setReadable(true, false) }
            if (prefsFile.exists()) prefsFile.setReadable(true, false)
        } catch (e: Exception) {}
    }
}
