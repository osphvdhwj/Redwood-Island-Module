package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val tabs = listOf("Ring", "Mini", "Mid", "Max", "Cube", "Gestures")

        var w by remember { mutableFloatStateOf(0f) }
        var h by remember { mutableFloatStateOf(0f) }
        var x by remember { mutableFloatStateOf(0f) }
        var y by remember { mutableFloatStateOf(0f) }
        var ringT by remember { mutableFloatStateOf(prefs.getFloat("ring_thickness", 6f)) }

        val currentPrefix = tabs[selectedTab].lowercase()

        LaunchedEffect(selectedTab) {
            if (currentPrefix != "gestures") {
                w = prefs.getFloat("${currentPrefix}_w", getDefaultWidth(currentPrefix))
                h = prefs.getFloat("${currentPrefix}_h", getDefaultHeight(currentPrefix))
                x = prefs.getFloat("${currentPrefix}_x", 0f)
                y = prefs.getFloat("${currentPrefix}_y", 48f)
                broadcastUpdate(currentPrefix, w, h, x, y, ringT)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.Black), contentAlignment = Alignment.TopCenter) {
                if (currentPrefix != "gestures") {
                    Box(modifier = Modifier.offset(x = x.dp, y = y.dp).width(w.dp).height(h.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(if(currentPrefix == "max") 42.dp else (h/2).dp)))
                } else Text("Gesture Customizer Coming Soon", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }

            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 8.dp) { 
                tabs.forEachIndexed { index, title -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) }) } 
            }

            if (currentPrefix != "gestures") {
                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Configure ${tabs[selectedTab]}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            w = getDefaultWidth(currentPrefix); h = getDefaultHeight(currentPrefix); x = 0f; y = 48f
                            prefs.edit().putFloat("pad_t", 0f).putFloat("pad_b", 0f).putFloat("pad_l", 0f).putFloat("pad_r", 0f).apply()
                            saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT)
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.7f))) { Text("Reset") }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Outer Dimensions", fontSize = 12.sp, color = Color.Gray)
                    
                    // 🚀 FIXED: IPC Broadcast Flood - Only saves when finger is lifted!
                    PrecisionSlider("Width", w, 10f..400f, { w = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("Height", h, 10f..400f, { h = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("X Pos", x, -200f..200f, { x = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("Y Pos", y, -100f..200f, { y = it }) { saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    
                    if (currentPrefix == "ring") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Ring Properties", fontSize = 12.sp, color = Color.Gray)
                        PrecisionSlider("Thickness", ringT, 1f..20f, { ringT = it }) { prefs.edit().putFloat("ring_thickness", ringT).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Inner Compression (Padding)", fontSize = 12.sp, color = Color.Gray)
                    var padT by remember { mutableFloatStateOf(prefs.getFloat("pad_t", 0f)) }
                    var padB by remember { mutableFloatStateOf(prefs.getFloat("pad_b", 0f)) }
                    var padL by remember { mutableFloatStateOf(prefs.getFloat("pad_l", 0f)) }
                    var padR by remember { mutableFloatStateOf(prefs.getFloat("pad_r", 0f)) }
                    
                    PrecisionSlider("Top", padT, 0f..100f, { padT = it }) { prefs.edit().putFloat("pad_t", padT).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("Bottom", padB, 0f..100f, { padB = it }) { prefs.edit().putFloat("pad_b", padB).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("Left", padL, 0f..100f, { padL = it }) { prefs.edit().putFloat("pad_l", padL).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    PrecisionSlider("Right", padR, 0f..100f, { padR = it }) { prefs.edit().putFloat("pad_r", padR).apply(); saveAndBroadcast(prefs, currentPrefix, w, h, x, y, ringT) }
                    
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    // 🚀 FIXED: Separated real-time state change from the Broadcast intent dispatch
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

    private fun saveAndBroadcast(prefs: android.content.SharedPreferences, prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float) {
        prefs.edit().putFloat("${prefix}_w", w).putFloat("${prefix}_h", h).putFloat("${prefix}_x", x).putFloat("${prefix}_y", y).apply()
        makePrefsWorldReadable()
        broadcastUpdate(prefix, w, h, x, y, ringT)
    }

    private fun broadcastUpdate(prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float) {
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS")
        intent.addFlags(0x01000000) 
        intent.putExtra("prefix", prefix).putExtra("w", w).putExtra("h", h).putExtra("x", x).putExtra("y", y).putExtra("ring_thickness", ringT)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        intent.putExtra("pad_t", prefs.getFloat("pad_t", 0f)).putExtra("pad_b", prefs.getFloat("pad_b", 0f)).putExtra("pad_l", prefs.getFloat("pad_l", 0f)).putExtra("pad_r", prefs.getFloat("pad_r", 0f))
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
