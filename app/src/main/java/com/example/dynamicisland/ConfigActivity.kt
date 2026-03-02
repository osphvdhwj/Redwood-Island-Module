package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

class ConfigActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Standard Xposed trick: Make the prefs file readable by SystemUI
        prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        makePrefsWorldReadable()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigUI()
                }
            }
        }
    }

    private fun makePrefsWorldReadable() {
        val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "island_prefs.xml")
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false)
            prefsDir.setExecutable(true, false)
        }
    }



    @Composable
    fun ConfigUI() {
        var offsetX by remember { mutableStateOf(prefs.getInt("offsetX", 0).toFloat()) }
        var offsetY by remember { mutableStateOf(prefs.getInt("offsetY", 48).toFloat()) }
        var ringOffsetY by remember { mutableStateOf(prefs.getInt("ringOffsetY", 48).toFloat()) }
        var camWidth by remember { mutableStateOf(prefs.getInt("camWidth", 24).toFloat()) }
        var camHeight by remember { mutableStateOf(prefs.getInt("camHeight", 24).toFloat()) }
        var pillScaleX by remember { mutableStateOf(prefs.getFloat("pillScaleX", 1f)) }
        var pillScaleY by remember { mutableStateOf(prefs.getFloat("pillScaleY", 1f)) }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Dynamic Island Configuration", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            ConfigSlider("Pill X Offset (Left/Right)", offsetX, -100f, 100f) {
                offsetX = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }
            ConfigSlider("Pill Y Offset (Up/Down)", offsetY, -100f, 200f) {
                offsetY = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }
            ConfigSlider("Ring Y Offset", ringOffsetY, -100f, 200f) {
                ringOffsetY = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY, isRing = true)
            }
            ConfigSlider("Camera Width", camWidth, 10f, 80f) {
                camWidth = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }
            ConfigSlider("Camera Height", camHeight, 10f, 80f) {
                camHeight = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }
            ConfigSlider("Pill Width Scale", pillScaleX, 0.5f, 2.0f) {
                pillScaleX = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }
            ConfigSlider("Pill Height Scale", pillScaleY, 0.5f, 2.0f) {
                pillScaleY = it; saveAndBroadcast(offsetX, offsetY, ringOffsetY, camWidth, camHeight, pillScaleX, pillScaleY)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { sendBroadcast(Intent("com.example.dynamicisland.TOGGLE_PREVIEW")) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("Toggle Visual Preview")
            }

            Button(
                onClick = {
                    // Send a dummy test notification to show the ring
                    val dummyIntent = Intent("com.example.dynamicisland.TEST_RING")
                    sendBroadcast(dummyIntent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Progress Ring")
            }
        }
    }

    private fun saveAndBroadcast(x: Float, y: Float, ringY: Float, w: Float, h: Float, scaleX: Float, scaleY: Float, isRing: Boolean = false) {
        prefs.edit()
            .putInt("offsetX", x.toInt())
            .putInt("offsetY", y.toInt())
            .putInt("ringOffsetY", ringY.toInt())
            .putInt("camWidth", w.toInt())
            .putInt("camHeight", h.toInt())
            .putFloat("pillScaleX", scaleX)
            .putFloat("pillScaleY", scaleY)
            .apply()
        makePrefsWorldReadable()

        val intent = Intent("com.example.dynamicisland.UPDATE_CONFIG")
        intent.putExtra("offsetX", x.toInt())
        intent.putExtra("offsetY", y.toInt())
        intent.putExtra("ringOffsetY", ringY.toInt())
        intent.putExtra("camWidth", w.toInt())
        intent.putExtra("camHeight", h.toInt())
        intent.putExtra("pillScaleX", scaleX)
        intent.putExtra("pillScaleY", scaleY)
        sendBroadcast(intent)

        if (isRing) {
            sendBroadcast(Intent("com.example.dynamicisland.SHOW_RING_PREVIEW"))
        } else {
            sendBroadcast(Intent("com.example.dynamicisland.TOGGLE_PREVIEW"))
        }
    }

    @Composable
    fun ConfigSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label)
                Text(value.toInt().toString())
            }
            Slider(value = value, onValueChange = onValueChange, valueRange = min..max)
        }
    }
}
