package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PremiumControlRow(
    type: String, // "VOLUME" or "BRIGHTNESS"
    currentPercent: Int,
    activeColor: Color,
    isTrailingActive: Boolean,
    onValueChanged: (Int) -> Unit,
    onTrailingClick: () -> Unit
) {
    var dragPercent by remember { mutableFloatStateOf(-1f) } 
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "squish")
    val animatedPercent by animateFloatAsState(targetValue = if (dragPercent >= 0f) dragPercent else currentPercent.toFloat(), animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f), label = "fill")

    val mainIcon = when(type) {
        "VOLUME" -> when {
            animatedPercent == 0f -> Icons.Rounded.VolumeOff
            animatedPercent < 40f -> Icons.Rounded.VolumeDown
            else -> Icons.Rounded.VolumeUp
        }
        else -> when {
            animatedPercent < 30f -> Icons.Rounded.BrightnessLow
            animatedPercent < 70f -> Icons.Rounded.BrightnessMedium
            else -> Icons.Rounded.BrightnessHigh
        }
    }

    val trailingIcon = if (type == "VOLUME") {
        when (isTrailingActive) {
            true -> Icons.Rounded.Vibration
            false -> Icons.Rounded.NotificationsActive
        }
    } else {
        if (isTrailingActive) Icons.Rounded.AutoMode else Icons.Rounded.SettingsBrightness
    }

    Row(modifier = Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxHeight().graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.15f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                        onTap = { offset -> val pct = (offset.x / size.width) * 100f; onValueChanged(pct.coerceIn(0f, 100f).toInt()) }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> isPressed = true; dragPercent = (offset.x / size.width) * 100f },
                        onDragEnd = { isPressed = false; dragPercent = -1f },
                        onDragCancel = { isPressed = false; dragPercent = -1f }
                    ) { change, dragAmount ->
                        change.consume()
                        dragPercent = (dragPercent + (dragAmount.x / size.width) * 100f).coerceIn(0f, 100f)
                        onValueChanged(dragPercent.toInt())
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = (animatedPercent / 100f).coerceIn(0f, 1f)).background(activeColor))
            AnimatedContent(targetState = mainIcon, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp), transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) }, label = "icon_anim") { icon ->
                Icon(imageVector = icon, contentDescription = null, tint = if (animatedPercent > 15f) Color.Black else Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(if (isTrailingActive) activeColor else Color.White.copy(alpha = 0.15f)).clickable { onTrailingClick() }, contentAlignment = Alignment.Center) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = if (isTrailingActive) Color.Black else Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

@Suppress("UNUSED_PARAMETER", "DEPRECATION")
@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
    val context = LocalContext.current
    val theme = LocalIslandTheme.current
    val safeQsTiles: List<String> = this.qsTiles.toList()
    val safePinnedApps: List<String> = this.pinnedApps.toList()

    val wifiManager = remember { try { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager } catch(e: Throwable) { null } }
    val btAdapter = remember { try { (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter } catch(e: Throwable) { null } }
    val cameraManager = remember { try { context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager } catch(e: Throwable) { null } }
    val cameraId = remember { try { cameraManager?.cameraIdList?.firstOrNull() } catch(e: Throwable) { null } }
    
    var isWifiOn by remember { mutableStateOf(try { wifiManager?.isWifiEnabled == true } catch(e: Throwable) { false }) }
    var isBtOn by remember { mutableStateOf(try { btAdapter?.isEnabled == true } catch(e: Throwable) { false }) }
    var isTorchOn by remember { mutableStateOf(false) }
    
    // 🎛️ FIXED: Collapse to Ring instead of completely hiding
    val launchAndCollapse = { intent: Intent -> 
        try { 
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent) 
            setState(IslandState.TYPE_0_RING) // Changed from HIDDEN to TYPE_0_RING
        } catch(e: Throwable) {} 
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
            val activeQS = safeQsTiles.filter { it.isNotEmpty() && it != "None" }
            if (activeQS.isEmpty()) {
                DashboardQuickToggle(Icons.Default.Settings, true, "Settings") { launchAndCollapse(Intent(android.provider.Settings.ACTION_SETTINGS)) }
            } else {
                activeQS.forEach { tileStr ->
                    when (tileStr) {
                        "WiFi" -> DashboardQuickToggle(Icons.Default.Wifi, isWifiOn, "Wi-Fi") { try { val newState = !isWifiOn; wifiManager?.isWifiEnabled = newState; isWifiOn = newState } catch(e: Throwable) { launchAndCollapse(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) } }
                        "Bluetooth" -> DashboardQuickToggle(Icons.Default.Bluetooth, isBtOn, "Bluetooth") { try { val newState = !isBtOn; @SuppressLint("MissingPermission") if (newState) btAdapter?.enable() else btAdapter?.disable(); isBtOn = newState } catch(e: Throwable) { launchAndCollapse(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) } }
                        "Torch" -> DashboardQuickToggle(Icons.Default.FlashlightOn, isTorchOn, "Torch") { try { isTorchOn = !isTorchOn; cameraId?.let { cameraManager?.setTorchMode(it, isTorchOn) } } catch(e: Throwable) {} }
                        "Location" -> DashboardQuickToggle(Icons.Default.LocationOn, true, "Location") { launchAndCollapse(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                        "Airplane" -> DashboardQuickToggle(Icons.Default.AirplanemodeActive, false, "Airplane") { launchAndCollapse(Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)) }
                        "DND" -> DashboardQuickToggle(Icons.Default.DoNotDisturbOn, false, "DND") { launchAndCollapse(Intent(android.provider.Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)) }
                        "Settings" -> DashboardQuickToggle(Icons.Default.Settings, true, "Settings") { launchAndCollapse(Intent(android.provider.Settings.ACTION_SETTINGS)) }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val pm = context.packageManager
        Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val validApps = safePinnedApps.filter { it.isNotEmpty() }
            if (validApps.isEmpty()) {
                Box(Modifier.size(36.dp).background(Color.White.copy(0.05f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)) }
            } else {
                validApps.forEach { pkgStr ->
                    val iconBmp = remember(pkgStr) { 
                        try { 
                            val drawable = pm.getApplicationIcon(pkgStr)
                            val bmp = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            bmp.asImageBitmap()
                        } catch(e: Throwable) { null } 
                    }
                    if (iconBmp != null) {
                        Image(bitmap = iconBmp, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).clickable { pm.getLaunchIntentForPackage(pkgStr)?.let { launchAndCollapse(it) } })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val isVibrateOrSilent = hardwareRingerMode.intValue != android.media.AudioManager.RINGER_MODE_NORMAL
        PremiumControlRow(
            type = "VOLUME",
            currentPercent = hardwareVolume.intValue,
            activeColor = Color.White,
            isTrailingActive = isVibrateOrSilent,
            onValueChanged = { newPct -> onVolumeDrag?.invoke(newPct) },
            onTrailingClick = { onRingerToggle?.invoke() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PremiumControlRow(
            type = "BRIGHTNESS",
            currentPercent = hardwareBrightness.intValue,
            activeColor = Color(0xFFFFD700),
            isTrailingActive = hardwareAutoBrightness.value,
            onValueChanged = { newPct -> onBrightnessDrag?.invoke(newPct) },
            onTrailingClick = { onAutoBrightnessToggle?.invoke() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Suppress("UNUSED_PARAMETER") 
@Composable
fun DynamicIslandView.DashboardMid(model: LiveActivityModel.Dashboard) {
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        DashboardQuickToggle(Icons.Default.Wifi, true)
        DashboardQuickToggle(Icons.Default.Bluetooth, false)
        DashboardQuickToggle(Icons.Default.Build, false)
        DashboardQuickToggle(Icons.Default.NotificationsActive, true)
    }
}

@Composable
fun DynamicIslandView.DashboardQuickToggle(icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, label: String? = null, onClick: () -> Unit = {}) {
    val theme = LocalIslandTheme.current
    val tint = if (isActive) Color.Black else Color.White
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        InteractiveIconButton(icon = icon, tint = tint, baseSize = theme.buttonSize, bgAlpha = if (isActive) 1f else 0.1f) { onClick() }
        if (label != null) { Spacer(modifier = Modifier.height(6.dp)); Text(label, color = Color.White.copy(alpha=0.9f), fontSize = 10.sp, fontWeight = FontWeight.Medium) }
    }
}
