package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FIX: Removed MODE_WORLD_READABLE. It causes crashes on modern Android.
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
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Ring", "Mini", "Mid", "Max", "Media Mid", "Media Max", "Cube", "Handle", "Gestures", "Dashboard", "Tweaks", "Theme", "Features")

        var w by remember { mutableFloatStateOf(0f) }
        var h by remember { mutableFloatStateOf(0f) }
        var x by remember { mutableFloatStateOf(0f) }
        var y by remember { mutableFloatStateOf(0f) }
        var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }
        var expandUpwards by remember { mutableStateOf(prefs.getBoolean("expand_upwards", false)) }

        val currentPrefix = tabs[selectedTab].lowercase().replace(" ", "_")
        val isSpecialTab = currentPrefix in listOf("handle", "gestures", "dashboard", "tweaks", "theme", "features")

        LaunchedEffect(selectedTab) {
            if (!isSpecialTab) {
                w = prefs.getFloat("${currentPrefix}_w", getDefaultWidth(currentPrefix))
                h = prefs.getFloat("${currentPrefix}_h", getDefaultHeight(currentPrefix))
                x = prefs.getFloat("${currentPrefix}_x", 0f)
                y = prefs.getFloat("${currentPrefix}_y", 48f)
                broadcastUpdate(currentPrefix, w, h, x, y, ringT, expandUpwards)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFF0A0A0A))
                    .wrapContentSize(unbounded = true), 
                contentAlignment = if (expandUpwards) Alignment.BottomCenter else Alignment.TopCenter
            ) {
                if (!isSpecialTab) {
                    Box(
                        modifier = Modifier
                            .offset(x = x.dp, y = y.dp)
                            .width(w.dp)
                            .height(h.dp)
                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(if(currentPrefix.contains("max")) 42.dp else (h/2).dp))
                    )
                } else {
                    Text("Universal Matrix Config", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) { 
                tabs.forEachIndexed { index, title -> 
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) }) 
                } 
            }

            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                when (currentPrefix) {
                    "gestures" -> {
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
                    }
                    "handle" -> {
                        Text(text = "Drag Handle (___)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
                        Text(text = "Customize the floating inner pill.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ThemeSlider("Handle Width", "theme_handle_width", 40f, 10f..150f, prefs)
                        ThemeSlider("Handle Thickness", "theme_handle_height", 5f, 2f..20f, prefs)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Handle Gestures are hardcoded to Expand/Collapse for optimal layout tracking.", fontSize = 12.sp, color = Color.Gray)
                    }
                    "dashboard" -> {
                        Text(text = "Quick Settings Grid", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
                        Text(text = "Select hardware toggles.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        val availableQS = listOf("None", "WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
                        val qsSlots = listOf("QS 1", "QS 2", "QS 3", "QS 4", "QS 5", "QS 6", "QS 7")
                        qsSlots.forEachIndexed { index, slot ->
                            var expanded by remember { mutableStateOf(false) }
                            var selectedQS by remember { mutableStateOf(prefs.getString("qs_tile_$index", availableQS.getOrNull(index + 1) ?: "None") ?: "None") }

                            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                                OutlinedTextField(
                                    value = selectedQS, onValueChange = {}, readOnly = true, label = { Text(slot) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 4.dp)
                                )
                                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    availableQS.forEach { tile ->
                                        DropdownMenuItem(text = { Text(tile) }, onClick = {
                                            selectedQS = tile; prefs.edit().putString("qs_tile_$index", tile).commit(); expanded = false // FIX: Using commit()
                                            broadcastUpdate("dashboard", 0f, 0f, 0f, 0f, 0f, false)
                                        })
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
                        var showAppPicker by remember { mutableStateOf(false) }
                        var activeSlot by remember { mutableIntStateOf(0) }
                        
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
                            val pinnedApps = listOf("App 1", "App 2", "App 3", "App 4", "App 5", "App 6", "App 7", "App 8")
                            pinnedApps.forEachIndexed { index, slot ->
                                val savedPkg = prefs.getString("pinned_app_$index", "") ?: ""
                                val appName = installedApps.find { it.second == savedPkg }?.first ?: if (savedPkg.isEmpty()) "Tap to select" else savedPkg
                                
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { activeSlot = index; showAppPicker = true }
                                ) {
                                    ListItem(
                                        headlineContent = { Text(slot, fontWeight = FontWeight.Bold) },
                                        supportingContent = { Text(appName) }
                                    )
                                }
                            }
                        }

                        if (showAppPicker) {
                            ModalBottomSheet(onDismissRequest = { showAppPicker = false }) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text("Select App for Slot ${activeSlot + 1}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                                        Divider()
                                    }
                                    item {
                                        ListItem(
                                            headlineContent = { Text("None (Clear Slot)", color = Color.Red) },
                                            modifier = Modifier.clickable {
                                                prefs.edit().putString("pinned_app_$activeSlot", "").commit()
                                                broadcastUpdate("dashboard", 0f, 0f, 0f, 0f, 0f, false)
                                                showAppPicker = false
                                            }
                                        )
                                    }
                                    items(installedApps) { app ->
                                        ListItem(
                                            headlineContent = { Text(app.first) },
                                            supportingContent = { Text(app.second, fontSize = 10.sp) },
                                            modifier = Modifier.clickable {
                                                prefs.edit().putString("pinned_app_$activeSlot", app.second).commit()
                                                broadcastUpdate("dashboard", 0f, 0f, 0f, 0f, 0f, false)
                                                showAppPicker = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "tweaks" -> {
                        Text(text = "Physical Adjustments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Adjust the Island without recompiling code.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        var offsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                        Text(text = "Y-Axis Offset (Push down from top): ${offsetY.toInt()}px", color = Color.White)
                        Slider(value = offsetY, onValueChange = { offsetY = it; prefs.edit().putFloat("tweak_offset_y", it).commit(); sendGestureUpdate(prefs, this@ConfigActivity) }, valueRange = 0f..150f)
                    }
                    "theme" -> {
                        Text(text = "UI Customization Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Customize the physical appearance of inner elements.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Interactive Buttons (Max Pill)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00FFCC))
                        
                        ThemeSlider("Button Icon Size (dp)", "theme_button_size", 48f, 20f..80f, prefs)
                        ThemeSlider("Gap Between Buttons (dp)", "theme_button_spacing", 16f, 0f..40f, prefs)
                        ThemeSlider("Button Shape (0=Square, 50=Circle)", "theme_button_radius", 50f, 0f..50f, prefs)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        ThemeSlider("Corner Radius", "theme_corner_radius", 50f, 10f..100f, prefs)
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
                    }
                    "features" -> {
                        Text(text = "Core Engines & Features", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Selectively disable Island behaviors.", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        FeatureSwitch("Enable Media Pill (Music/Spotify)", "enable_media", true, prefs)
                        FeatureSwitch("Enable Charging Cube", "enable_charging", true, prefs)
                        FeatureSwitch("Enable System Alerts (Thermal/Rogue)", "enable_alerts", true, prefs)
                        FeatureSwitch("Enable App Timers (Wellbeing)", "enable_timers", true, prefs)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Typography Engine", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00FFCC))
                        FeatureSwitch("Use System Default Font", "use_system_font", true, prefs)
                        FeatureSwitch("Enable 3D Cube Rotation", "enable_cube_rotation", true, prefs)
                    }
                    else -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Configure ${tabs[selectedTab]}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Button(onClick = { 
                                w = getDefaultWidth(currentPrefix); h = getDefaultHeight(currentPrefix); x = 0f; y = 48f; 
                                prefs.edit().putFloat("pad_t", 0f).putFloat("pad_b", 0f).putFloat("pad_l", 0f).putFloat("pad_r", 0f).commit()
                                saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) 
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.7f))) { 
                                Text("Reset") 
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Expand Upwards (From Bottom)", fontSize = 14.sp)
                            Switch(checked = expandUpwards, onCheckedChange = { expandUpwards = it; prefs.edit().putBoolean("expand_upwards", it).commit(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Outer Dimensions", fontSize = 12.sp, color = Color.Gray)
                        PrecisionSlider("Width", w, 10f..400f, { w = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                        PrecisionSlider("Height", h, 10f..300f, { h = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Offset Coordinates", fontSize = 12.sp, color = Color.Gray)
                        PrecisionSlider("X Offset", x, -200f..200f, { x = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                        PrecisionSlider("Y Offset", y, 0f..200f, { y = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT, expandUpwards) }
                    }
                }
            }
        }
    }

    @Composable
    fun FeatureSwitch(label: String, key: String, default: Boolean, prefs: android.content.SharedPreferences) {
        var checked by remember { mutableStateOf(prefs.getBoolean(key, default)) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Switch(checked = checked, onCheckedChange = { checked = it; prefs.edit().putBoolean(key, it).commit(); sendGestureUpdate(prefs, this@ConfigActivity) })
        }
    }

    @Composable
    fun ThemeSlider(label: String, key: String, default: Float, range: ClosedFloatingPointRange<Float>, prefs: android.content.SharedPreferences) {
        var value by remember { mutableFloatStateOf(prefs.getFloat(key, default)) }
        Text(text = "$label: ${value.toInt()}", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Slider(value = value, onValueChange = { value = it; prefs.edit().putFloat(key, it).commit(); sendGestureUpdate(prefs, this@ConfigActivity) }, valueRange = range)
    }

    @Composable
    fun PrecisionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(text = label, modifier = Modifier.width(60.dp), fontSize = 12.sp)
            IconButton(onClick = { onValueChange((value - 1f).coerceIn(range)); onValueChangeFinished() }) { Icon(Icons.Default.Remove, "Decrease") }
            Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, valueRange = range, modifier = Modifier.weight(1f))
            IconButton(onClick = { onValueChange((value + 1f).coerceIn(range)); onValueChangeFinished() }) { Icon(Icons.Default.Add, "Increase") }
            Text(text = value.toInt().toString(), modifier = Modifier.width(30.dp), fontSize = 12.sp, textAlign = TextAlign.End)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GestureDropdown(label: String, options: Array<IslandAction>, prefs: android.content.SharedPreferences, prefKey: String) {
        var expanded by remember { mutableStateOf(false) }
        var selectedAction by remember { mutableStateOf(IslandAction.valueOf(prefs.getString(prefKey, IslandAction.NONE.name) ?: IslandAction.NONE.name)) }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedAction.name.replace("_", " "), onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).width(180.dp).height(50.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.name.replace("_", " ")) },
                            onClick = {
                                selectedAction = action; prefs.edit().putString(prefKey, action.name).commit(); expanded = false
                                sendGestureUpdate(prefs, this@ConfigActivity)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getDefaultWidth(prefix: String) = when (prefix) { "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "media_mid" -> 320f; "media_max" -> 360f; "cube" -> 85f; else -> 0f }
    private fun getDefaultHeight(prefix: String) = when (prefix) { "ring" -> 45f; "mini" -> 36f; "mid" -> 80f; "max" -> 220f; "media_mid" -> 80f; "media_max" -> 200f; "cube" -> 85f; else -> 0f }

    private fun saveAndBroadcast(prefs: android.content.SharedPreferences, prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, up: Boolean) {
        prefs.edit().putFloat("${prefix}_w", w).putFloat("${prefix}_h", h).putFloat("${prefix}_x", x).putFloat("${prefix}_y", y).putFloat("ring_thickness", ringT).putBoolean("expand_upwards", up).commit()
        broadcastUpdate(prefix, w, h, x, y, ringT, up)
    }

    @SuppressLint("WrongConstant")
    private fun broadcastUpdate(prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, up: Boolean) {
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE) // FIX: SecurityException prevented
        val updateIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            addFlags(0x01000000)
            setPackage("com.android.systemui")
            putExtra("prefix", prefix).putExtra("w", w).putExtra("h", h).putExtra("x", x).putExtra("y", y).putExtra("ring_thickness", ringT).putExtra("expand_upwards", up)
            putExtra("pad_t", prefs.getFloat("pad_t", 0f)).putExtra("pad_b", prefs.getFloat("pad_b", 0f)).putExtra("pad_l", prefs.getFloat("pad_l", 0f)).putExtra("pad_r", prefs.getFloat("pad_r", 0f))
        }
        sendBroadcast(updateIntent)
    }

    @SuppressLint("WrongConstant")
    private fun sendGestureUpdate(prefs: android.content.SharedPreferences, context: Context) {
        val updateIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            addFlags(0x01000000)
            setPackage("com.android.systemui")
        }
        val matrix = JSONObject()
        
        for ((key, value) in prefs.all) {
            if (key.contains("TYPE_") || key.contains("theme_") || key.contains("tweak_") || key.contains("enable_") || key.contains("use_")) {
                matrix.put(key, value.toString()) 
            }
        }
        
        updateIntent.putExtra("gesture_payload", matrix.toString())
        context.sendBroadcast(updateIntent)
    }
}
