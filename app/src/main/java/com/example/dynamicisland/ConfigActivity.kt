package com.example.dynamicisland

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.io.File

class ConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Surface(modifier = Modifier.fillMaxSize()) { ConfigScreen(prefs) } } }
    }

    @Composable
    fun ConfigScreen(prefs: android.content.SharedPreferences) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube", "Gestures", "Pinning", "Tweaks", "Theme")

        var w by remember { mutableFloatStateOf(0f) }
        var h by remember { mutableFloatStateOf(0f) }
        var x by remember { mutableFloatStateOf(0f) }
        var y by remember { mutableFloatStateOf(0f) }
        var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }
        var expandUpwards by remember { mutableStateOf(prefs.getBoolean("expand_upwards", false)) }

        val currentPrefix = tabs[selectedTab].lowercase()

        LaunchedEffect(selectedTab) {
            if (currentPrefix != "gestures") {
                w = prefs.getFloat("${currentPrefix}_w", getDefaultWidth(currentPrefix))
                h = prefs.getFloat("${currentPrefix}_h", getDefaultHeight(currentPrefix))
                x = prefs.getFloat("${currentPrefix}_x", 0f)
                y = prefs.getFloat("${currentPrefix}_y", 48f)
                broadcastUpdate(currentPrefix, w, h, x, y, ringT, expandUpwards)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = if (expandUpwards) Alignment.BottomCenter else Alignment.TopCenter) {
                if (currentPrefix != "gestures") {
                    Box(modifier = Modifier.offset(x = x.dp, y = y.dp).width(w.dp).height(h.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(if(currentPrefix == "max") 42.dp else (h/2).dp)))
                } else Text("Universal Matrix Config", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) { tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) }) } }

            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                if (currentPrefix == "gestures") {
                    // 🚀 THE NEW ACCORDION GESTURE MATRIX
                    Text(text = "Action Matrix", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val states = listOf("Ring" to "TYPE_0_RING", "Mini Pill" to "TYPE_1_MINI", "Mid Pill" to "TYPE_2_MID", "Max Pill" to "TYPE_3_MAX")
                    val gestures = IslandGesture.values()

                    // Creates a clean, readable list: "None", "Play Pause", "Volume Up", etc.
                    val actionOptions = IslandAction.values().map { it.name.replace("_", " ") }

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
                } else if (currentPrefix == "pinning") {
                    Text(text = "Control Center Shortcuts", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Select 4 apps to pin to your Max Dashboard.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 🚀 THE FIX: Live Native App Fetcher
                    val pm = LocalContext.current.packageManager
                    val installedApps = remember {
                        pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                            .filter { appInfo -> pm.getLaunchIntentForPackage(appInfo.packageName) != null }
                            .map { appInfo -> Pair(appInfo.loadLabel(pm).toString(), appInfo.packageName) }
                            .sortedBy { pair -> pair.first }
                    }

                    val pinnedApps = listOf("Slot 1", "Slot 2", "Slot 3", "Slot 4")
                    pinnedApps.forEachIndexed { index, slot ->
                        var expanded by remember { mutableStateOf(false) }
                        var selectedAppPkg by remember { mutableStateOf(prefs.getString("pinned_app_$index", "") ?: "") }
                        val selectedAppName = installedApps.find { it.second == selectedAppPkg }?.first ?: "None"

                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                            OutlinedTextField(
                                value = selectedAppName, onValueChange = {}, readOnly = true, label = { Text(slot) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().padding(vertical = 4.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = {
                                    selectedAppPkg = ""; prefs.edit().putString("pinned_app_$index", "").apply(); expanded = false
                                    saveAndBroadcast(prefs, "pinning", 0f, 0f, 0f, 0f, 0f, false)
                                })
                                installedApps.forEach { pair ->
                                    DropdownMenuItem(text = { Text(pair.first) }, onClick = {
                                        selectedAppPkg = pair.second; prefs.edit().putString("pinned_app_$index", pair.second).apply(); expanded = false
                                        saveAndBroadcast(prefs, "pinning", 0f, 0f, 0f, 0f, 0f, false)
                                    })
                                }
                            }
                        }
                    }
                } else if (currentPrefix == "tweaks") {
                    Text(text = "Physical Adjustments", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Adjust the Island without recompiling code.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Y-Offset Slider
                    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("tweak_offset_y", 0f)) }
                    Text(text = "Y-Axis Offset (Push down from top): ${offsetY.toInt()}px", color = Color.White)
                    Slider(value = offsetY, onValueChange = { offsetY = it; prefs.edit().putFloat("tweak_offset_y", it).apply(); sendGestureUpdate(prefs) }, valueRange = 0f..150f)

                    // Base Width Slider
                    var baseWidth by remember { mutableFloatStateOf(prefs.getFloat("tweak_base_width", 100f)) }
                    Text(text = "Mini Pill Width: ${baseWidth.toInt()}dp", color = Color.White)
                    Slider(value = baseWidth, onValueChange = { baseWidth = it; prefs.edit().putFloat("tweak_base_width", it).apply(); sendGestureUpdate(prefs) }, valueRange = 50f..200f)
                } else if (currentPrefix == "theme") {
                    Text(text = "UI Customization Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Customize the physical appearance of inner elements.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Helper function to create sliders easily
                    @Composable
                    fun ThemeSlider(label: String, key: String, default: Float, range: ClosedFloatingPointRange<Float>) {
                        var value by remember { mutableFloatStateOf(prefs.getFloat(key, default)) }
                        Text(text = "$label: ${value.toInt()}", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                        Slider(value = value, onValueChange = {
                            value = it; prefs.edit().putFloat(key, it).apply(); sendGestureUpdate(prefs)
                        }, valueRange = range)
                    }

                    ThemeSlider("Corner Radius", "theme_corner_radius", 50f, 10f..100f)
                    ThemeSlider("Primary Text Size (sp)", "theme_text_primary", 16f, 10f..30f)
                    ThemeSlider("Secondary Text Size (sp)", "theme_text_secondary", 14f, 8f..24f)
                    ThemeSlider("Progress Bar Thickness", "theme_progress_thick", 4f, 1f..15f)
                    ThemeSlider("Ring Thickness", "theme_ring_thick", 12f, 2f..25f)
                    ThemeSlider("Button Tap Size", "theme_button_size", 48f, 24f..72f)
                    ThemeSlider("Element Gap (Spacing)", "theme_element_gap", 8f, 0f..32f)
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
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GestureDropdown(label: String, options: List<String>, prefs: android.content.SharedPreferences, prefKey: String) {
        var expanded by remember { mutableStateOf(false) }
        var selectedOption by remember { mutableStateOf(prefs.getString(prefKey, IslandAction.NONE.name) ?: IslandAction.NONE.name) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(value = selectedOption, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { selectedOption = option; prefs.edit().putString(prefKey, option).apply(); expanded = false; sendGestureUpdate(prefs) }) }
            }
        }
    }

    @Composable
    fun PrecisionSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.width(60.dp), fontSize = 14.sp)
            IconButton(onClick = { onValueChange((value - 1f).coerceIn(range)); onValueChangeFinished() }) { Icon(Icons.Default.Remove, contentDescription = "-") }
            Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, modifier = Modifier.weight(1f), valueRange = range)
            IconButton(onClick = { onValueChange((value + 1f).coerceIn(range)); onValueChangeFinished() }) { Icon(Icons.Default.Add, contentDescription = "+") }
            Text(String.format("%.0f", value), modifier = Modifier.width(40.dp), fontSize = 14.sp)
        }
    }

    private fun saveAndBroadcast(prefs: android.content.SharedPreferences, prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean) {
        prefs.edit().putFloat("${prefix}_w", w).putFloat("${prefix}_h", h).putFloat("${prefix}_x", x).putFloat("${prefix}_y", y).apply()
        makePrefsWorldReadable()
        broadcastUpdate(prefix, w, h, x, y, ringT, expandUp)
    }

    private fun broadcastUpdate(prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean) {
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").addFlags(0x01000000) 
        intent.putExtra("prefix", prefix).putExtra("w", w).putExtra("h", h).putExtra("x", x).putExtra("y", y).putExtra("ring_thickness", ringT).putExtra("expand_upwards", expandUp)
        intent.putExtra("pad_t", prefs.getFloat("pad_t", 0f)).putExtra("pad_b", prefs.getFloat("pad_b", 0f)).putExtra("pad_l", prefs.getFloat("pad_l", 0f)).putExtra("pad_r", prefs.getFloat("pad_r", 0f))
        
        // Serialize JSON Matrix
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        sendBroadcast(intent)
    }

    private fun sendGestureUpdate(prefs: android.content.SharedPreferences) { 
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").addFlags(0x01000000)
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        sendBroadcast(intent) 
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
