package com.example.dynamicisland.ui.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.dynamicisland.ui.ConfigActivity
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.RedwoodDesignSystem
import com.example.dynamicisland.ui.design.RedwoodTheme
import com.example.dynamicisland.ui.design.glassmorphicCard
import com.example.dynamicisland.ui.design.premiumClickable
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(pageCount = { 6 })
    
    // Permission States
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotification by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var hasAccessibility by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var hasBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasOverlay = Settings.canDrawOverlays(context)
    }
    
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasNotification = isNotificationListenerEnabled(context)
    }

    val accessibilityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasAccessibility = isAccessibilityServiceEnabled(context)
    }

    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasBattery = isIgnoringBatteryOptimizations(context)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false // Force user to use buttons
        ) { page ->
            when (page) {
                0 -> WelcomePage { scope.launch { pagerState.animateScrollToPage(1) } }
                1 -> PermissionPage(
                    title = "System Overlay",
                    description = "Redwood Engine needs to draw over other apps to show the Dynamic Island.",
                    icon = Icons.Default.Layers,
                    isGranted = hasOverlay,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        overlayLauncher.launch(intent)
                    },
                    onNext = { scope.launch { pagerState.animateScrollToPage(2) } }
                )
                2 -> PermissionPage(
                    title = "Notification Access",
                    description = "Required for the Smart Notification Engine to capture and group messages.",
                    icon = Icons.Default.Notifications,
                    isGranted = hasNotification,
                    onGrant = {
                        notificationLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onNext = { scope.launch { pagerState.animateScrollToPage(3) } }
                )
                3 -> PermissionPage(
                    title = "Accessibility",
                    description = "Used to detect the current foreground app and enable global gestures.",
                    icon = Icons.Default.Accessibility,
                    isGranted = hasAccessibility,
                    onGrant = {
                        accessibilityLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onNext = { scope.launch { pagerState.animateScrollToPage(4) } }
                )
                4 -> PermissionPage(
                    title = "Battery",
                    description = "Disable optimizations to ensure the engine isn't killed by the system.",
                    icon = Icons.Default.BatteryChargingFull,
                    isGranted = hasBattery,
                    onGrant = {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                        batteryLauncher.launch(intent)
                    },
                    onNext = { scope.launch { pagerState.animateScrollToPage(5) } }
                )
                5 -> CompletionPage {
                    val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("has_completed_setup", true).apply()
                    context.startActivity(Intent(context, ConfigActivity::class.java))
                    (context as Activity).finish()
                }
            }
        }
        
        // Pager Indicators
        Row(
            Modifier.height(50.dp).fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(6) { iteration ->
                val color = if (pagerState.currentPage == iteration) IslandColors.accentCyan else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier.padding(4.dp).clip(RoundedCornerShape(2.dp)).background(color).size(width = 24.dp, height = 4.dp)
                )
            }
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
        Text("Next-Gen Dynamic Island", color = IslandColors.accentCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        Text(
            "Welcome to the ultimate Android enhancement suite. Let's get you set up with a few required permissions.",
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
            Text("Get Started", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                Text("Grant Permission", color = IslandColors.accentCyan, fontWeight = FontWeight.Bold)
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
            "The engine is ready. You can now customize your island and sidebar in the main app.",
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
            Text("Enter Redwood", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedId = "${context.packageName}/com.example.dynamicisland.accessibility.IslandAccessibilityService"
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return enabledServices?.contains(expectedId) == true
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
