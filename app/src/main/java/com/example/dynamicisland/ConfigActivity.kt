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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.io.File
import android.widget.Toast

class ConfigActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Standard Xposed trick: Make the prefs file readable by SystemUI
        prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigUI()
                }
            }
        }
    }



    private fun broadcastUpdate(x: Int, y: Int, w: Int, h: Int) {
        val intent = Intent("com.example.dynamicisland.UPDATE_CONFIG")
        // Target SystemUI explicitly so Android 14+ allows the broadcast
        intent.setPackage("com.android.systemui")
        intent.putExtra("offsetX", x)
        intent.putExtra("offsetY", y)
        intent.putExtra("camWidth", w)
        intent.putExtra("camHeight", h)
        sendBroadcast(intent)
    }

    @Composable
    fun ConfigUI() {
        var offsetX by remember { mutableStateOf(prefs.getInt("offsetX", 0).toFloat()) }
        var offsetY by remember { mutableStateOf(prefs.getInt("offsetY", 48).toFloat()) }
        var camWidth by remember { mutableStateOf(prefs.getInt("camWidth", 24).toFloat()) }
        var camHeight by remember { mutableStateOf(prefs.getInt("camHeight", 24).toFloat()) }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Dynamic Island Configuration", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            ConfigSlider("X Offset (Left/Right)", offsetX, -100f, 100f) {
                offsetX = it; saveAndBroadcast(offsetX, offsetY, camWidth, camHeight)
            }
            ConfigSlider("Y Offset (Up/Down)", offsetY, 0f, 150f) {
                offsetY = it; saveAndBroadcast(offsetX, offsetY, camWidth, camHeight)
            }
            ConfigSlider("Camera Width", camWidth, 10f, 80f) {
                camWidth = it; saveAndBroadcast(offsetX, offsetY, camWidth, camHeight)
            }
            ConfigSlider("Camera Height", camHeight, 10f, 80f) {
                camHeight = it; saveAndBroadcast(offsetX, offsetY, camWidth, camHeight)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    // Send a dummy test notification to show the ring
                    val dummyIntent = Intent("com.example.dynamicisland.TEST_RING")
                    sendBroadcast(dummyIntent)
                    Toast.makeText(this@ConfigActivity, "Progress Ring Test Broadcasted", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Progress Ring")
            }
        }
    }

    private fun saveAndBroadcast(x: Float, y: Float, w: Float, h: Float) {
        prefs.edit()
            .putInt("offsetX", x.toInt())
            .putInt("offsetY", y.toInt())
            .putInt("camWidth", w.toInt())
            .putInt("camHeight", h.toInt())
            .apply()
        broadcastUpdate(x.toInt(), y.toInt(), w.toInt(), h.toInt())
    }

    @Composable
    fun ConfigSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label)
                Text(value.toInt().toString())
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = min..max,
                modifier = Modifier.semantics { contentDescription = "Adjust $label" }
            )
        }
    }
}
