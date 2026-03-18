package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay

// 🚀 RESTORED: Standalone Handle Freedom (Broadcasts direct swipes to Master Matrix)
@Composable
fun FloatingDragHandle(
    modifier: Modifier = Modifier,
    handleWidth: Dp = 40.dp,
    handleHeight: Dp = 5.dp,
    handleColor: Color = Color.White.copy(alpha = 0.4f),
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 1.2f else 1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f), label = "handleScale")

    Box(
        modifier = modifier
            .width(handleWidth)
            .height(handleHeight * 2) 
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isPressed = true },
                    onDragEnd = { isPressed = false; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    onDragCancel = { isPressed = false }
                ) { change, dragAmount ->
                    change.consume()
                    if (dragAmount.y > 15f) onSwipeDown()
                    else if (dragAmount.y < -15f) onSwipeUp()
                    else if (dragAmount.x > 15f) onSwipeRight()
                    else if (dragAmount.x < -15f) onSwipeLeft()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onLongPress = { onLongClick(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                    onTap = { onClick(); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(handleWidth)
                .height(handleHeight)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(handleColor)
        )
    }
}

// 🚀 RESTORED: "Clean Slate" Margins (weight(1f) to prevent overlapping)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween 
    ) {
        if (music.albumArt != null) {
            Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${music.title} • ${music.artist}", 
            color = Color.White, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, 
            maxLines = 1, 
            modifier = Modifier.weight(1f).safeMarquee(islandState.value) 
        )
        if (music.isPlaying) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(music.dominantColor ?: android.graphics.Color.WHITE), modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val dynamicColor = Color.White
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.weight(0.7f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (music.albumArt != null) {
                Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = music.title, color = dynamicColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = dynamicColor.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
            val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
            InteractiveIconButton(icon = playIcon, tint = dynamicColor, baseSize = 40.dp, bgAlpha = 0.15f) { onPlayPauseClick?.invoke() }
        }
        Row(modifier = Modifier.weight(0.3f).fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicColor.copy(alpha=0.6f), fontSize = 11.sp)
            
            // 🚀 RESTORED: Handle routing direct to Master Gesture Matrix
            FloatingDragHandle(
                onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) },
                onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                onSwipeLeft = { onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT) },
                onSwipeRight = { onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) },
                onClick = { onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                onLongClick = { onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
            )
            Text(text = formatTime(music.durationMs), color = dynamicColor.copy(alpha=0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.weight(0.15f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            if (music.appIcon != null) Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App", modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))) 
            else Box(Modifier.size(24.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(6.dp)))
            Icon(Icons.Default.Smartphone, contentDescription = "Output", tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
        }
        Row(modifier = Modifier.weight(0.4f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = music.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = Color.White.copy(alpha=0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
        }
        Column(modifier = Modifier.weight(0.45f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
            IsolatedMediaSlider(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, dynamicTextColor = Color.White, onSeek = { onSeekTo?.invoke(it) })
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, baseSize = 48.dp, bgAlpha = 0f) { onPrevClick?.invoke() }
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = Color.Black, baseSize = 64.dp, bgAlpha = 1f) { onPlayPauseClick?.invoke() } 
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = Color.White, baseSize = 48.dp, bgAlpha = 0f) { onNextClick?.invoke() }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FloatingDragHandle(
                    onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) },
                    onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                    onSwipeLeft = { onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT) },
                    onSwipeRight = { onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) },
                    onClick = { onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                    onLongClick = { onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
    val context = LocalContext.current
    val wifiManager = remember { try { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager } catch(e: Throwable) { null } }
    val btAdapter = remember { try { (context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter } catch(e: Throwable) { null } }
    
    var isWifiOn by remember { mutableStateOf(try { wifiManager?.isWifiEnabled == true } catch(e: Throwable) { false }) }
    var isBtOn by remember { mutableStateOf(try { btAdapter?.isEnabled == true } catch(e: Throwable) { false }) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val pm = context.packageManager
            val validApps = pinnedApps.toList().filter { it.isNotEmpty() }
            if (validApps.isEmpty()) {
                Box(Modifier.size(36.dp).background(Color.White.copy(0.05f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(16.dp)) }
            } else {
                validApps.forEach { pkg ->
                    val iconBmp = remember(pkg) { 
                        try { 
                            val drawable = pm.getApplicationIcon(pkg)
                            val bmp = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp); drawable.setBounds(0, 0, canvas.width, canvas.height); drawable.draw(canvas)
                            bmp.asImageBitmap()
                        } catch(e: Throwable) { null } 
                    }
                    if (iconBmp != null) {
                        Image(bitmap = iconBmp, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape).clickable {
                            try { val intent = pm.getLaunchIntentForPackage(pkg); if (intent != null) { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) } } catch(e: Throwable) {}
                        })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            DashboardToggle(Icons.Default.Wifi, isWifiOn) { try { val newState = !isWifiOn; wifiManager?.isWifiEnabled = newState; isWifiOn = newState } catch(e: Throwable) { try { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(ex: Throwable) {} } }
            DashboardToggle(Icons.Default.Bluetooth, isBtOn) { try { val newState = !isBtOn; if (newState) btAdapter?.enable() else btAdapter?.disable(); isBtOn = newState } catch(e: Throwable) { try { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(ex: Throwable) {} } }
            DashboardToggle(Icons.Default.LocationOn, true) { try { context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
            DashboardToggle(Icons.Default.Settings, true) { try { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch(e: Throwable) {} }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FloatingDragHandle(
                onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) },
                onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                onSwipeLeft = { onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT) },
                onSwipeRight = { onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) },
                onClick = { onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                onLongClick = { onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
            )
        }
    }
}

@Composable
fun DashboardToggle(icon: ImageVector, active: Boolean, onClick: () -> Unit = {}) {
    InteractiveIconButton(icon = icon, tint = if(active) Color.Black else Color.White, baseSize = 56.dp, bgAlpha = if(active) 1f else 0.15f) { onClick() }
}

@Composable
fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    var isClicked by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(isClicked) { if (isClicked) { delay(200); isClicked = false } }
    val scale by animateFloatAsState(if (isClicked) 1.15f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label="scale")
    val bgColor = if (bgAlpha == 1f) Color.White else tint.copy(alpha = bgAlpha) 
    Box(
        modifier = Modifier.size(baseSize).clip(CircleShape).background(bgColor).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); isClicked = true; onClick() },
        contentAlignment = Alignment.Center
    ) { Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(baseSize * 0.5f).graphicsLayer { scaleX = scale; scaleY = scale }) }
}

@Composable
fun DynamicIslandView.OtpMid(model: LiveActivityModel.Otp) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(Color(0xFF4285F4).copy(alpha=0.2f), CircleShape).border(1.dp, Color(0xFF4285F4).copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(text = "Code from ${model.sourceApp}", color = Color.Gray, fontSize = 12.sp)
             Text(text = model.code, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
        }
        Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).clickable { 
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("OTP", model.code))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onGestureEvent?.invoke(IslandGesture.SWIPE_UP) 
            } catch(e: Exception){}
        }, contentAlignment = Alignment.Center) {
             Icon(Icons.Default.Add, contentDescription="Copy", tint=Color.Cyan, modifier = Modifier.size(24.dp))
        }
    }
}

// 🚀 RESTORED: Config-driven 3D Cube Rotation
@Composable
fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
    val infiniteTransition = rememberInfiniteTransition(label = "cubeRotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, 
        targetValue = if (isCubeRotationEnabled.value) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "rotation"
    )

    val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White
    Column(
        modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = rotation }, 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "${model.level}%", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Green.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.Green) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Charging", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${charging.level}%", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color(alert.alertColor).copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, null, tint = Color(alert.alertColor)) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = alert.title, color = Color(alert.alertColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = alert.message, color = Color(alert.alertColor).copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Cyan.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = Color.Cyan) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = task.text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Red.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, null, tint = Color.Red) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Time Limit Reached", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = model.appName, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${model.appName} • ${model.sessionMinutes}m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DynamicIslandView.HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedMediaSlider(durationMs: Long, posProvider: () -> Long, dynamicTextColor: Color, onSeek: (Long) -> Unit) {
    var localPosition by remember { mutableFloatStateOf(0f) }
    Slider(
        value = (posProvider().toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
        onValueChange = { localPosition = it * durationMs; onSeek(localPosition.toLong()) },
        colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.25f), thumbColor = dynamicTextColor),
        modifier = Modifier.fillMaxWidth().height(24.dp)
    )
}

fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

@Composable
fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp) {
    val timeStr by remember(durationMs) { derivedStateOf { formatTime(posProvider()) } }
    Text(text = timeStr, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun androidx.compose.ui.Modifier.safeMarquee(state: IslandState): androidx.compose.ui.Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) this.basicMarquee() else this
}

fun DynamicIslandView.setState(newState: IslandState) { islandState.value = newState }
fun DynamicIslandView.setModel(model: LiveActivityModel?) { activeModel.value = model }
fun DynamicIslandView.setSplitModel(model: LiveActivityModel?) { splitModel.value = model }
