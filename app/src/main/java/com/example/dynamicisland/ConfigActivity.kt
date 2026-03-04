package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class ConfigActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup XSharedPreferences bridging safely
        prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        makePrefsWorldReadable()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                ConfigScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigScreen() {
        // Read existing prefs, fallback to sensible defaults
        var offsetX by remember { mutableStateOf(prefs.getInt("offsetX", 0).toFloat()) }
        var offsetY by remember { mutableStateOf(prefs.getInt("offsetY", 48).toFloat()) }
        var width by remember { mutableStateOf(prefs.getInt("camWidth", 24).toFloat()) }
        var height by remember { mutableStateOf(prefs.getInt("camHeight", 24).toFloat()) }
        var pinnedApps by remember { mutableStateOf(prefs.getString("pinnedApps", "com.whatsapp,com.instagram.android") ?: "") }

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Dynamic Island Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Poco X5 Pro Fine-Tuning", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // 1. POSITION CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Position", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("X Offset: ${offsetX.toInt()}", color = Color.Gray)
                    Slider(value = offsetX, onValueChange = { offsetX = it; saveAndBroadcast(offsetX, offsetY, width, height, pinnedApps) }, valueRange = -200f..200f)
                    Text("Y Offset: ${offsetY.toInt()}", color = Color.Gray)
                    Slider(value = offsetY, onValueChange = { offsetY = it; saveAndBroadcast(offsetX, offsetY, width, height, pinnedApps) }, valueRange = -100f..200f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. DIMENSIONS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Punch Hole Size", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Width: ${width.toInt()}", color = Color.Gray)
                    Slider(value = width, onValueChange = { width = it; saveAndBroadcast(offsetX, offsetY, width, height, pinnedApps) }, valueRange = 10f..150f)
                    Text("Height: ${height.toInt()}", color = Color.Gray)
                    Slider(value = height, onValueChange = { height = it; saveAndBroadcast(offsetX, offsetY, width, height, pinnedApps) }, valueRange = 10f..150f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. PINNED APPS ENGINE
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dashboard Pinned Apps", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter package names separated by commas.", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinnedApps,
                        onValueChange = { pinnedApps = it; saveAndBroadcast(offsetX, offsetY, width, height, pinnedApps) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. LIVE PREVIEW ENGINE
            Text("Live Preview Overrides", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { sendPreviewIntent("TYPE_1_MINI") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("Mini") }
                Button(
                    onClick = { sendPreviewIntent("TYPE_2_MID") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("Mid") }
                Button(
                    onClick = { sendPreviewIntent("TYPE_3_MAX") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) { Text("Max") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { sendBroadcast(Intent("com.example.dynamicisland.TEST_RING").setPackage("com.android.systemui")) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC))
            ) {
                Text("Trigger Test Notification")
            }

            Spacer(modifier = Modifier.height(32.dp)) // Bottom padding
        }
    }

    private fun saveAndBroadcast(x: Float, y: Float, w: Float, h: Float, apps: String) {
        prefs.edit()
            .putInt("offsetX", x.toInt())
            .putInt("offsetY", y.toInt())
            .putInt("camWidth", w.toInt())
            .putInt("camHeight", h.toInt())
            .putString("pinnedApps", apps)
            .apply()

        makePrefsWorldReadable()

        // Broadcast to SystemUI to live-update the layout
        val intent = Intent("com.example.dynamicisland.UPDATE_CONFIG").apply {
            putExtra("offsetX", x.toInt())
            putExtra("offsetY", y.toInt())
            putExtra("camWidth", w.toInt())
            putExtra("camHeight", h.toInt())
            putExtra("pinnedApps", apps)
            setPackage("com.android.systemui")
        }
        sendBroadcast(intent)
    }

    private fun sendPreviewIntent(state: String) {
        val intent = Intent("com.example.dynamicisland.LIVE_PREVIEW").apply {
            putExtra("preview_state", state)
            setPackage("com.android.systemui")
        }
        sendBroadcast(intent)
    }

    private fun makePrefsWorldReadable() {
        try {
            val file = File(applicationInfo.dataDir, "shared_prefs/island_prefs.xml")
            if (file.exists()) {
                file.setReadable(true, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
