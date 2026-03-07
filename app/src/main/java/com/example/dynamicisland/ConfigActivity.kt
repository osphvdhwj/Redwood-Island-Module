package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

class ConfigActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        makePrefsWorldReadable()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color(0xFF121212))) {
                ConfigScreen()
            }
        }
    }

    @Composable
    fun ConfigScreen() {
        val scrollState = rememberScrollState()
        val tabs = listOf("Ring", "Mini Pill", "Mid Pill", "Max Pill")
        var selectedTab by remember { mutableStateOf(0) }

        // State identifiers for SharedPreferences keys
        val statePrefixes = listOf("ring", "mini", "mid", "max")
        val currentPrefix = statePrefixes[selectedTab]

        // Default values based on your original logic
        val defaultW = listOf(45f, 180f, 320f, 360f)
        val defaultH = listOf(45f, 36f, 80f, 220f)

        // The 4 variables for the CURRENTLY selected tab
        var currentW by remember(selectedTab) { mutableStateOf(prefs.getFloat("${currentPrefix}_w", defaultW[selectedTab])) }
        var currentH by remember(selectedTab) { mutableStateOf(prefs.getFloat("${currentPrefix}_h", defaultH[selectedTab])) }
        var currentX by remember(selectedTab) { mutableStateOf(prefs.getFloat("${currentPrefix}_x", 0f)) }
        var currentY by remember(selectedTab) { mutableStateOf(prefs.getFloat("${currentPrefix}_y", 48f)) }

        // Live Preview Animation states
        val animSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
        val previewW by animateDpAsState(currentW.dp, animSpec, label = "mock_w")
        val previewH by animateDpAsState(currentH.dp, animSpec, label = "mock_h")
        // Squircle math: 50% for ring/small pills, 15% for massive dashboard
        val previewRad by animateDpAsState(if (selectedTab == 3) 42.dp else (currentH / 2).dp, animSpec, label = "mock_r")

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            // --- 1. THE IN-APP LIVE PREVIEW (No Hook Required) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .background(Color(0xFF0A0A0A)), // Slightly darker to simulate screen top
                contentAlignment = Alignment.TopCenter
            ) {
                // Center guide line
                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Color.DarkGray.copy(alpha = 0.3f)))

                // The Mock Island
                Box(
                    modifier = Modifier
                        .padding(top = currentY.dp)
                        .offset(x = currentX.dp)
                        .width(previewW)
                        .height(previewH)
                        .clip(RoundedCornerShape(previewRad))
                        .background(Color.Black)
                ) {
                    // Just a visual indicator of the state inside the mock
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(tabs[selectedTab], color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.DarkGray))

            // --- 2. THE EDITOR ---
            Column(modifier = Modifier.padding(20.dp).verticalScroll(scrollState)) {
                Text("Island Studio", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Independent State Configuration", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dimensions Card
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Size", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Width: ${currentW.toInt()} dp", color = Color.Gray, fontSize = 12.sp)
                        Slider(value = currentW, onValueChange = { currentW = it; saveState(currentPrefix, currentW, currentH, currentX, currentY) }, valueRange = 20f..400f)

                        Text("Height: ${currentH.toInt()} dp", color = Color.Gray, fontSize = 12.sp)
                        Slider(value = currentH, onValueChange = { currentH = it; saveState(currentPrefix, currentW, currentH, currentX, currentY) }, valueRange = 20f..350f)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Position Card
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Position", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("X Offset (Left/Right): ${currentX.toInt()}", color = Color.Gray, fontSize = 12.sp)
                        Slider(value = currentX, onValueChange = { currentX = it; saveState(currentPrefix, currentW, currentH, currentX, currentY) }, valueRange = -200f..200f)

                        Text("Y Offset (Up/Down): ${currentY.toInt()}", color = Color.Gray, fontSize = 12.sp)
                        Slider(value = currentY, onValueChange = { currentY = it; saveState(currentPrefix, currentW, currentH, currentX, currentY) }, valueRange = -50f..200f)
                    }
                }
            }
        }
    }

    private fun saveState(prefix: String, w: Float, h: Float, x: Float, y: Float) {
        prefs.edit()
            .putFloat("${prefix}_w", w)
            .putFloat("${prefix}_h", h)
            .putFloat("${prefix}_x", x)
            .putFloat("${prefix}_y", y)
            .apply()

        makePrefsWorldReadable()

        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS")
        intent.addFlags(0x01000000) // 🚀 Brilliant Hex Fix!
        intent.putExtra("prefix", prefix)
        intent.putExtra("w", w)
        intent.putExtra("h", h)
        intent.putExtra("x", x)
        intent.putExtra("y", y)
        sendBroadcast(intent)
    }

    private fun makePrefsWorldReadable() {
        try {
            val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
            val prefsFile = File(prefsDir, "island_prefs.xml")
            
            // 🚀 FIX: Directory MUST be executable (+x) for SELinux to allow SystemUI inside
            if (prefsDir.exists()) {
                prefsDir.setExecutable(true, false)
                prefsDir.setReadable(true, false)
            }
            if (prefsFile.exists()) prefsFile.setReadable(true, false)
        } catch (e: Exception) { e.printStackTrace() }
    }
