package com.example.dynamicisland.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dynamicisland.ui.components.LiveVisualAid
import com.example.dynamicisland.ui.design.IslandColors
import com.example.dynamicisland.ui.design.RedwoodTheme
import com.example.dynamicisland.ui.design.glassmorphicCard
import com.example.dynamicisland.ui.design.premiumClickable
import com.example.dynamicisland.ui.screens.*
import com.example.dynamicisland.ui.settings.SettingsScreen
import com.example.dynamicisland.settings.SettingsViewModel
import com.example.dynamicisland.settings.SettingsManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@AndroidEntryPoint
class ConfigActivity : ComponentActivity() {

    @Inject lateinit var settingsManager: SettingsManager
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsViewModel = SettingsViewModel(settingsManager)
        val prefs = getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        
        val hasCompletedSetup = prefs.getBoolean("has_completed_setup", false)
        if (!hasCompletedSetup) {
            startActivity(Intent(this, com.example.dynamicisland.ui.setup.SetupActivity::class.java))
            finish()
            return
        }

        val composeView = ComposeView(this).apply {
            setContent {
                RedwoodTheme {
                    PermissionGuard {
                        ConfigScreenNav(prefs, settingsViewModel)
                    }
                }
            }
        }
        setContentView(composeView)
    }
}

@Composable
fun PermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var isOverlayMissing by remember { mutableStateOf(!Settings.canDrawOverlays(context)) }
    var isNotificationMissing by remember { mutableStateOf(!isNotificationListenerEnabled(context)) }
    var isAccessibilityMissing by remember { mutableStateOf(!isAccessibilityServiceEnabled(context)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isOverlayMissing = !Settings.canDrawOverlays(context)
                isNotificationMissing = !isNotificationListenerEnabled(context)
                isAccessibilityMissing = !isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val missingPermission = when {
        isOverlayMissing -> "Overlay"
        isNotificationMissing -> "Notification Access"
        isAccessibilityMissing -> "Accessibility"
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (missingPermission != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .premiumClickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .glassmorphicCard(cornerRadius = 24.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.GppMaybe, 
                        null, 
                        tint = Color.Red, 
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Permission Shield", 
                        color = Color.White, 
                        fontSize = 22.sp, 
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The Island cannot function without $missingPermission. Tap below to fix this specific requirement.",
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            when {
                                isOverlayMissing -> {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                    context.startActivity(intent)
                                }
                                isNotificationMissing -> {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                }
                                isAccessibilityMissing -> {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslandColors.accentCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Grant $missingPermission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
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

data class NavItemData(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun ConfigScreenNav(prefs: android.content.SharedPreferences, settingsViewModel: SettingsViewModel) {
    var selectedNav by remember { mutableIntStateOf(0) }
    
    val navItems = listOf(
        NavItemData("Layout", Icons.Default.Build),
        NavItemData("Appearance", Icons.Default.Palette),
        NavItemData("Shortcuts", Icons.Default.Apps),
        NavItemData("System", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedNav == index,
                        onClick = { selectedNav = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent<Int>(
                        targetState = selectedNav,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }) + fadeOut())
                            } else {
                                (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) + fadeIn()) togetherWith
                                (slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "TabTransition"
                    ) { navIndex ->
                        when (navIndex) {
                            0 -> LayoutScreen(prefs)
                            1 -> AppearanceScreen(prefs)
                            2 -> InteractionsTab(prefs)
                            3 -> SettingsScreen(settingsViewModel)
                        }
                    }
                }
            }
            
            // Floating Visual Aid overlay
            LiveVisualAid()
        }
    }
}

@Composable
fun InteractionsTab(prefs: android.content.SharedPreferences) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    Column {
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            divider = {},
            edgePadding = 16.dp
        ) {
            Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("App Roles") })
            Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Pins & Tiles") })
            Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Gestures") })
        }
        when (selectedSubTab) {
            0 -> AppRolesScreen(prefs)
            1 -> PinsTilesScreen(prefs)
            2 -> GesturesScreen(prefs)
        }
    }
}
