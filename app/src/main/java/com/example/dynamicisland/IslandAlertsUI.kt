package com.example.dynamicisland

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ============================================================================
// 📱 MINI STATES (The Chips)
// ============================================================================

@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val haptic = LocalHapticFeedback.current
    val isRinging = model.state == "RINGING"
    val ringPulse by rememberInfiniteTransition(label="ring").animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label="alpha")
    val pillAlpha = if (isRinging) ringPulse else 1f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00C853).copy(alpha = pillAlpha), RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isRinging) onOpenCallUI?.invoke() else setState(IslandState.TYPE_2_MID) 
                    },
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenCallUI?.invoke() 
                    }
                )
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        if (isRinging) {
            Text(text = "Incoming", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        } else {
            IsolatedTimerText(startTime = model.startTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ============================================================================
// 📱 MID STATES (Powered by the Unified Slot API)
// ============================================================================

@Composable
fun DynamicIslandView.CallMid(model: LiveActivityModel.Call) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val theme = LocalIslandTheme.current
    
    var isMicMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    var isSpeakerOn by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }

    AlertMidSlot(
        islandState = islandState.value,
        swipeAction = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenCallUI?.invoke()
        },
        iconContent = {
            val pulseScale by rememberInfiniteTransition(label="pulse").animateFloat(initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label="scale")
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX=pulseScale; scaleY=pulseScale }.background(Color(0xFF333333), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(theme.batIconSize * 0.65f))
            }
        },
        title = model.callerName,
        titleColor = Color.White,
        subtitleContent = {
            IsolatedTimerText(startTime = model.startTime, color = Color(0xFF00C853), fontSize = theme.alertMsgSize, fontWeight = FontWeight.SemiBold)
        },
        rightContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickCircleBtn(icon = if(isMicMuted) Icons.Default.MicOff else Icons.Default.Mic, isActive = isMicMuted, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMicToggle?.invoke(); isMicMuted = !isMicMuted 
                }
                QuickCircleBtn(icon = Icons.Default.VolumeUp, isActive = isSpeakerOn, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSpeakerToggle?.invoke(); isSpeakerOn = !isSpeakerOn 
                }
                QuickCircleBtn(icon = Icons.Default.CallEnd, isActive = true, activeColor = Color(0xFFFF3B30), inactiveColor = Color(0xFFFF3B30)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); onEndCallClick?.invoke() 
                }
            }
        }
    )
}

@Composable
fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
    val theme = LocalIslandTheme.current
    val isLow = charging.level <= 20 && !charging.isPluggedIn
    val batteryColor = if (charging.isPluggedIn) Color(0xFF00FF55) else if (isLow) Color(0xFFFF0033) else Color.White
    
    val infiniteTransition = rememberInfiniteTransition(label = "charging_glow")
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")

    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(batteryColor.copy(alpha = if (charging.isPluggedIn || isLow) glowAlpha else 0.1f), CircleShape).blur(if (charging.isPluggedIn || isLow) 12.dp else 0.dp))
            Icon(imageVector = if (charging.isPluggedIn) Icons.Default.Add else if (isLow) Icons.Default.Warning else Icons.Default.BatteryFull, contentDescription = null, tint = batteryColor, modifier = Modifier.size(theme.batIconSize * 0.65f))
        },
        title = if (charging.isPluggedIn) "Charging" else if (isLow) "Low Battery" else "Battery",
        titleColor = if (isLow) batteryColor else Color.White,
        subtitle = "${charging.level}% Remaining",
        subtitleColor = Color.White.copy(alpha=0.7f),
        rightContent = { LiquidBatteryCanvas(level = charging.level, color = batteryColor, isCharging = charging.isPluggedIn) }
    )
}

@Composable
fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
    val color = Color(alert.alertColor)
    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(color.copy(alpha=0.2f), CircleShape).border(1.dp, color.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                val icon = when(alert.alertType) { "THERMAL" -> Icons.Default.Warning; "ROGUE" -> Icons.Default.BatteryAlert; else -> Icons.Default.Info }
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        },
        title = alert.title,
        titleColor = color,
        subtitle = alert.message,
        subtitleColor = color.copy(alpha=0.8f)
    )
}

@Composable
fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha=0.2f), CircleShape).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { (task.progress.toFloat() / task.progressMax.toFloat()).coerceIn(0f, 1f) }, color = Color.Cyan, trackColor = Color.White.copy(alpha=0.2f), strokeWidth = 2.dp)
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        },
        title = task.title,
        titleColor = Color.White,
        subtitle = task.text,
        subtitleColor = Color.White.copy(alpha=0.8f)
    )
}

@Composable
fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val alertAlpha by pulseTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alertAlpha")

    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = alertAlpha), CircleShape).border(2.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                if (model.appIcon != null) { Image(bitmap = model.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.fillMaxSize(0.7f).clip(CircleShape)) } 
                else { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
        },
        title = "Time Limit Reached",
        titleColor = Color.Red,
        subtitleContent = {
            IsolatedCountdownText(targetTimeMs = model.targetTimeMs, prefix = "${model.appName} closing in ", suffix = "s", color = Color.White)
        }
    )
}


// ============================================================================
// 📱 ANIMATIONS & CUBES
// ============================================================================

@Composable
fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
    val theme = LocalIslandTheme.current
    val isLow = model.level <= 20 && !model.isPluggedIn
    val color = if (model.isPluggedIn) Color(0xFF00FF55) else if (isLow) Color(0xFFFF0033) else Color.White
    val infiniteTransition = rememberInfiniteTransition(label = "anim")
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (theme.chargingStyle) {
            "APPLE" -> {
                val fillProgress by animateFloatAsState(targetValue = model.level / 100f, animationSpec = tween(1500), label = "fill")
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawArc(color = Color.White.copy(alpha=0.1f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(10f))
                    drawArc(color = color, startAngle = -90f, sweepAngle = 360f * fillProgress, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(10f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
            "HYPEROS" -> {
                val glowAlpha by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 0.6f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "glow")
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = glowAlpha)))))
            }
            else -> {
                val pulseScale by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scale")
                val spinAngle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "spin")
                if (model.isPluggedIn || isLow) { 
                    Box(modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = spinAngle; scaleX = pulseScale; scaleY = pulseScale }) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val brush = Brush.sweepGradient(0f to Color.Transparent, 0.5f to color.copy(alpha=0.5f), 1f to Color.Transparent)
                            drawCircle(brush = brush, radius = size.width/2, style = androidx.compose.ui.graphics.drawscope.Stroke(12f))
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val icon = if (model.isPluggedIn) Icons.Default.Add else if (isLow) Icons.Default.Warning else Icons.Default.BatteryFull
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = "${model.level}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}


// ============================================================================
// 📱 UNIFIED SLOT API & HIGH-PERFORMANCE COMPONENTS
// ============================================================================

/**
 * 🎛️ NEW: The Unified Alert Slot. 
 * Forces all mid-alerts to use perfectly standardized padding and typography hierarchy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlertMidSlot(
    islandState: IslandState,
    iconContent: @Composable BoxScope.() -> Unit,
    title: String,
    titleColor: Color,
    subtitle: String? = null,
    subtitleColor: Color = Color.White.copy(alpha = 0.7f),
    subtitleContent: (@Composable () -> Unit)? = null,
    swipeAction: (() -> Unit)? = null,
    rightContent: (@Composable RowScope.() -> Unit)? = null
) {
    val theme = LocalIslandTheme.current
    var modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    
    // Add gesture layer if swipeAction is provided
    if (swipeAction != null) {
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                if (dragAmount.y > 20f) swipeAction()
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(theme.batIconSize + 8.dp)) { iconContent() }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(text = title, color = titleColor, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState))
             if (subtitleContent != null) {
                 subtitleContent()
             } else if (subtitle != null) {
                 Text(text = subtitle, color = subtitleColor, fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState))
             }
        }
        if (rightContent != null) {
            Spacer(Modifier.width(14.dp))
            rightContent()
        }
    }
}

/**
 * 🎛️ NEW: Recomposition-Safe Timer. 
 * Polls the system clock inside its own isolated scope so the parent UI never redraws.
 */
@Composable
fun IsolatedTimerText(startTime: Long, color: Color, fontSize: TextUnit, fontWeight: FontWeight) {
    var elapsedSecs by remember { mutableLongStateOf((System.currentTimeMillis() - startTime) / 1000) }
    LaunchedEffect(startTime) {
        while(isActive) { delay(1000); elapsedSecs = (System.currentTimeMillis() - startTime) / 1000 }
    }
    Text(text = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60), color = color, fontSize = fontSize, fontWeight = fontWeight)
}

@Composable
fun IsolatedCountdownText(targetTimeMs: Long, prefix: String, suffix: String, color: Color) {
    val theme = LocalIslandTheme.current
    var remaining by remember { mutableIntStateOf(((targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }
    LaunchedEffect(targetTimeMs) {
        while (remaining > 0) { delay(1000); remaining = ((targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0) }
    }
    Text(text = "$prefix$remaining$suffix", color = color, fontSize = theme.alertMsgSize, maxLines = 1)
}

/**
 * 🎛️ NEW: Mathematically Perfect Liquid Battery.
 * Uses clip paths to ensure the progress bar geometry never breaks, even at 1%.
 */
@Composable
fun LiquidBatteryCanvas(level: Int, color: Color, isCharging: Boolean) {
    val targetFill = level / 100f
    val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(46.dp).height(24.dp).border(2.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(3.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val clipPath = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))) }
                clipPath(clipPath) {
                    drawRect(color = color, topLeft = Offset.Zero, size = Size(size.width * animatedFill, size.height))
                }
            }
            if (isCharging) {
                Icon(Icons.Default.Add, contentDescription=null, tint=Color.Black.copy(alpha=0.6f), modifier = Modifier.align(Alignment.Center).size(16.dp))
            }
        }
        Box(modifier = Modifier.width(4.dp).height(10.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)))
    }
}

@Composable
fun QuickCircleBtn(icon: ImageVector, isActive: Boolean, activeColor: Color, inactiveColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isActive) activeColor else inactiveColor).clickable { onClick() }, 
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription=null, tint = if (isActive && activeColor == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
}

fun getIconForType(type: ActivityType): ImageVector { return when(type) { ActivityType.CALL -> Icons.Default.Phone; ActivityType.NAVIGATION -> Icons.Default.LocationOn; ActivityType.TIMER -> Icons.Default.Notifications; ActivityType.MESSAGE -> Icons.Default.Email; ActivityType.ALARM -> Icons.Default.Notifications; ActivityType.CHARGING -> Icons.Default.Add; ActivityType.BATTERY_LOW -> Icons.Default.Warning; ActivityType.BLUETOOTH -> Icons.Default.Bluetooth; ActivityType.WIFI -> Icons.Default.Wifi; ActivityType.HARDWARE -> Icons.Default.Info; else -> Icons.Default.Info } }
