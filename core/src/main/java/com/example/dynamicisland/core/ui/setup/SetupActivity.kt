package com.example.dynamicisland.core.ui.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.ConfigActivity
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.glassmorphicCard
import com.example.dynamicisland.core.util.IslandProcessUtils
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.launch

/**
 * Top-Grade Setup Flow
 * Automatically skips unnecessary permissions when LSPosed is detected.
 */
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedwoodTheme {
                SetupScreen()
            }
        }
    }
}

@Composable
fun SetupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var hasRoot by remember { mutableStateOf(false) }
    
    val isHyperOS = remember { 
        val brand = android.os.Build.BRAND.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        brand.contains("xiaomi") || manufacturer.contains("xiaomi") || brand.contains("poco") || brand.contains("redmi")
    }

    val pagerState = rememberPagerState(pageCount = { if (isHyperOS) 5 else 4 })

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            hasRoot = IslandProcessUtils.isRootAvailable()
        }
    }

    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasBattery = isIgnoringBatteryOptimizations(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage { scope.launch { pagerState.animateScrollToPage(1) } }
                1 -> RootPage(
                    isGranted = hasRoot,
                    onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
                2 -> PermissionPage(
                    title = "System Stability",
                    description = "To keep the Island Engine running smoothly in the background, please disable battery optimizations.",
                    icon = Icons.Default.BatteryChargingFull,
                    isGranted = hasBattery,
                    onGrant = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                            batteryLauncher.launch(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            batteryLauncher.launch(intent)
                        }
                    },
                    onNext = { scope.launch { pagerState.animateScrollToPage(3) } }
                )
                3 -> {
                    if (isHyperOS) {
                        MIUIPage { scope.launch { pagerState.animateScrollToPage(4) } }
                    } else {
                        CompletionPage { finishSetup(context) }
                    }
                }
                4 -> CompletionPage { finishSetup(context) }
            }
        }
        
        Row(
            Modifier.height(50.dp).fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center
        ) {
            val count = if (isHyperOS) 5 else 4
            repeat(count) { iteration ->
                val color = if (pagerState.currentPage == iteration) IslandColors.accentCyan else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(2.dp)).background(color).size(width = 24.dp, height = 4.dp)
                )
            }
        }
    }
}

private fun finishSetup(context: Context) {
    val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("has_completed_setup", true).apply()
    context.startActivity(Intent(context, ConfigActivity::class.java))
    (context as Activity).finish()
}

@Composable
fun RootPage(isGranted: Boolean, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).glassmorphicCard(cornerRadius = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isGranted) Icons.Default.Terminal else Icons.Default.Shield, 
                null, 
                tint = if (isGranted) Color.Green else IslandColors.accentCyan, 
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(Modifier.height(32.dp))
        Text("Environment Check", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Text(
            if (isGranted) "Superuser access detected. Root-level stability features enabled." 
            else "Root not detected via 'su'. Some advanced hardware triggers may require manual ADB setup.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = if (isGranted) Color.Green.copy(alpha = 0.2f) else IslandColors.accentCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (isGranted) "Proceed" else "Continue Anyway", color = if (isGranted) Color.Green else Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MIUIPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("HyperOS detected", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "MIUI/HyperOS requires 'Autostart' and 'No restrictions' in App Info for the Island to stay active during deep sleep.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.accentCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("I Understand", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = IslandColors.accentCyan, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("Redwood Engine", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Text("Top-Grade System Module", color = IslandColors.accentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Text(
            "The module is ready to inject. Since you are using LSPosed, no complex permissions are required.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.accentCyan),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Begin Setup", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun PermissionPage(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrant: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp).glassmorphicCard(cornerRadius = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (isGranted) Color.Green else IslandColors.accentCyan, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        Text(
            description,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(64.dp))
        
        if (!isGranted) {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, IslandColors.accentCyan),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Optimize Battery", color = IslandColors.accentCyan, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = Color.Green)
                Spacer(Modifier.width(8.dp))
                Text("Continue", color = Color.Green, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CompletionPage(onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Celebration, null, tint = IslandColors.accentPurple, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(32.dp))
        Text("You're All Set!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(
            "Redwood is now a native part of your system. Customize your experience in the dashboard.",
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(64.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = IslandColors.accentPurple),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Enter Dashboard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
