package com.example.dynamicisland.core.ui

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.manager.IslandHardwareManager
import com.example.dynamicisland.core.util.ComposeLifecycleOwner
import com.google.accompanist.drawablepainter.rememberDrawablePainter

class SidebarView(context: Context, private val hardwareManager: IslandHardwareManager) : FrameLayout(context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val composeView = ComposeView(context)
    private var isExpanded = mutableStateOf(false)

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.END or Gravity.CENTER_VERTICAL
    }

    init {
        val lifecycleOwner = ComposeLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.attachToView(this)
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        addView(composeView)
        setupContent()
    }

    private fun setupContent() {
        composeView.setContent {
            val expanded by isExpanded
            val width by animateDpAsState(
                targetValue = if (expanded) 280.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                label = "sidebarWidth"
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(width)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(if (expanded) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                    .premiumClickable { isExpanded.value = !expanded }
            ) {
                if (expanded) {
                    SidebarContent()
                } else {
                    // Small "grabber" handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(4.dp)
                            .height(60.dp)
                            .background(IslandColors.accentCyan.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }

    @Composable
    private fun SidebarContent() {
        var activePanel by remember { mutableStateOf("apps") }

        Row(modifier = Modifier.fillMaxSize()) {
            // --- Left Navigation Rail ---
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SidebarTabItem(Icons.Default.Tune, activePanel == "controls") { activePanel = "controls" }
                SidebarTabItem(Icons.Default.Apps, activePanel == "apps") { activePanel = "apps" }
                SidebarTabItem(Icons.Default.Assessment, activePanel == "stats") { activePanel = "stats" }
                SidebarTabItem(Icons.Default.Assignment, activePanel == "clipboard") { activePanel = "clipboard" }
                SidebarTabItem(Icons.Default.AcUnit, activePanel == "freeze") { activePanel = "freeze" }
            }

            // --- Panel Content ---
            Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(16.dp)) {
                AnimatedContent(targetState = activePanel, label = "panelTransition") { panel ->
                    when (panel) {
                        "controls" -> SidebarHardwareControls()
                        "apps" -> SidebarAppLauncher()
                        "stats" -> SidebarSystemStats()
                        "clipboard" -> SidebarClipboard()
                        "freeze" -> SidebarFreezeMode()
                    }
                }
            }
        }
    }

    @Composable
    private fun SidebarTabItem(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isSelected) IslandColors.accentCyan.copy(alpha = 0.1f) else Color.Transparent)
        ) {
            Icon(icon, null, tint = if (isSelected) IslandColors.accentCyan else Color.White.copy(alpha = 0.4f))
        }
    }

    @Composable
    private fun SidebarHardwareControls() {
        val am = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var ringerMode by remember { mutableIntStateOf(am.ringerMode) }
        var isAutoBrightness by remember { mutableStateOf(hardwareManager.isAutoBrightnessEnabled) }

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text("Hardware", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)

            // Brightness Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, null, tint = IslandColors.accentCyan, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .glassmorphicCard(cornerRadius = 12.dp)
                            .premiumClickable { 
                                hardwareManager.toggleAutoBrightness(null)
                                isAutoBrightness = hardwareManager.isAutoBrightnessEnabled
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (isAutoBrightness) "AUTO" else "MANUAL", color = IslandColors.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                VerticalLiquidSlider(
                    value = 50f, // Ideally fetch real value from hardwareManager
                    onValueChange = { hardwareManager.setSystemBrightness(it.toInt(), null) },
                    activeColor = IslandColors.accentCyan,
                    iconRes = android.R.drawable.ic_menu_compass // Placeholder
                )
            }

            // Volume Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, null, tint = IslandColors.accentPurple, modifier = Modifier.size(20.dp))
                    Box(
                        modifier = Modifier
                            .glassmorphicCard(cornerRadius = 12.dp)
                            .premiumClickable { 
                                hardwareManager.toggleRingerMode(null)
                                ringerMode = am.ringerMode
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        val modeText = when(ringerMode) {
                            AudioManager.RINGER_MODE_SILENT -> "SILENT"
                            AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                            else -> "RING"
                        }
                        Text(modeText, color = IslandColors.accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                VerticalLiquidSlider(
                    value = 50f, // Ideally fetch real value
                    onValueChange = { hardwareManager.setSystemVolume(it.toInt(), null) },
                    activeColor = IslandColors.accentPurple,
                    iconRes = android.R.drawable.ic_lock_silent_mode_off // Placeholder
                )
            }
        }
    }

    @Composable
    private fun SidebarAppLauncher() {
        Text("Quick Launcher", color = Color.White, fontWeight = FontWeight.Black)
    }

    @Composable
    private fun SidebarSystemStats() {
        Text("System Vitals", color = Color.White, fontWeight = FontWeight.Black)
    }

    @Composable
    private fun SidebarClipboard() {
        Text("Clipboard History", color = Color.White, fontWeight = FontWeight.Black)
    }

    @Composable
    private fun SidebarFreezeMode() {
        val context = LocalContext.current
        var frozenApps by remember { mutableStateOf(setOf<String>()) }
        val pm = context.packageManager
        
        // This would ideally be loaded from a shared prefs or Hail database
        val targetApps = listOf("com.android.chrome", "com.google.android.youtube", "com.facebook.katana")

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Freeze Mode", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("Powered by Hail Engine", color = IslandColors.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(targetApps) { pkg ->
                    val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (e: Exception) { null }
                    if (appInfo != null) {
                        FreezeItem(appInfo, frozenApps.contains(pkg)) {
                            // Toggle Freeze Logic (Placeholder for real su command)
                            if (frozenApps.contains(pkg)) {
                                frozenApps = frozenApps - pkg
                                // su -c pm enable pkg
                            } else {
                                frozenApps = frozenApps + pkg
                                // su -c pm disable-user pkg
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun FreezeItem(appInfo: android.content.pm.ApplicationInfo, isFrozen: Boolean, onToggle: () -> Unit) {
        val pm = LocalContext.current.packageManager
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphicCard(cornerRadius = 12.dp)
                .premiumClickable { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberDrawablePainter(appInfo.loadIcon(pm)),
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape).then(if (isFrozen) Modifier.blur(2.dp) else Modifier)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                appInfo.loadLabel(pm).toString(),
                color = if (isFrozen) Color.White.copy(alpha = 0.3f) else Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isFrozen) Icons.Default.Lock else Icons.Default.LockOpen,
                null,
                tint = if (isFrozen) Color.Red else Color.Green,
                modifier = Modifier.size(16.dp)
            )
        }
    }

    fun show() {
        try { windowManager.addView(this, params) } catch (e: Exception) {}
    }
}
