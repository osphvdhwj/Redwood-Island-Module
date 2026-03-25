package com.example.dynamicisland

import android.content.Context
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val haptic = LocalHapticFeedback.current
    val isRinging = model.state == "RINGING"
    
    val ringPulse by rememberInfiniteTransition(label="ring").animateFloat(
        initialValue = 0.4f, targetValue = 1f, 
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label="alpha"
    )

    var elapsedSecs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(model.startTime, isRinging) {
        if (!isRinging) {
            while(isActive) { delay(1000); elapsedSecs = (System.currentTimeMillis() - model.startTime) / 1000 }
        }
    }
    
    val displayString = if (isRinging) "Incoming" else String.format("%d:%02d", elapsedSecs / 60, elapsedSecs % 60)
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
        Text(text = displayString, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DynamicIslandView.CallMid(model: LiveActivityModel.Call) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    
    var isMicMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    var isSpeakerOn by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }
    var elapsedSecs by remember { mutableLongStateOf((System.currentTimeMillis() - model.startTime) / 1000) }
    
    LaunchedEffect(model.startTime) {
        while(isActive) { 
            delay(1000)
            elapsedSecs = (System.currentTimeMillis() - model.startTime) / 1000 
            isMicMuted = audioManager.isMicrophoneMute
            isSpeakerOn = audioManager.isSpeakerphoneOn
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFF00C853).copy(alpha = 0.3f), RoundedCornerShape(40.dp)) 
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 20f) { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenCallUI?.invoke() 
                        } else if (dragAmount.y < -20f) { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            setState(IslandState.TYPE_1_MINI) 
                        }
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pulseScale by rememberInfiniteTransition(label="pulse").animateFloat(initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label="scale")
        Box(modifier = Modifier.size(42.dp).graphicsLayer { scaleX=pulseScale; scaleY=pulseScale }.background(Color(0xFF333333), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(text = model.callerName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            Text(text = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60), color = Color(0xFF00C853), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(isMicMuted) Color.White else Color.White.copy(0.15f)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMicToggle?.invoke(); isMicMuted = !isMicMuted }, contentAlignment = Alignment.Center) {
                Icon(if(isMicMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription=null, tint=if(isMicMuted) Color.Black else Color.White, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(isSpeakerOn) Color.White else Color.White.copy(0.15f)).clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSpeakerToggle?.invoke(); isSpeakerOn = !isSpeakerOn }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VolumeUp, contentDescription=null, tint=if(isSpeakerOn) Color.Black else Color.White, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFF3B30)).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onEndCallClick?.invoke() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CallEnd, contentDescription=null, tint=Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

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
                androidx.compose.foundation.Canvas(modifier = Modifier.size(54.dp)) {
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
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
    val theme = LocalIslandTheme.current
    val isLow = charging.level <= 20 && !charging.isPluggedIn
    val batteryColor = if (charging.isPluggedIn) Color(0xFF00FF55) else if (isLow) Color(0xFFFF0033) else Color.White
    
    val infiniteTransition = rememberInfiniteTransition(label = "charging_glow")
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")
    
    val targetFill = charging.level / 100f
    val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // 🎛️ FIXED: Now uses Config App Battery Icon Size
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(theme.batIconSize + 8.dp)) {
            Box(modifier = Modifier.size(theme.batIconSize).background(batteryColor.copy(alpha = if (charging.isPluggedIn || isLow) glowAlpha else 0.1f), CircleShape).blur(if (charging.isPluggedIn || isLow) 12.dp else 0.dp))
            Icon(imageVector = if (charging.isPluggedIn) Icons.Default.Add else if (isLow) Icons.Default.Warning else Icons.Default.BatteryFull, contentDescription = null, tint = batteryColor, modifier = Modifier.size(theme.batIconSize * 0.65f))
        }
        
        Spacer(Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
             val titleText = if (charging.isPluggedIn) "Charging" else if (isLow) "Low Battery" else "Battery"
             // 🎛️ FIXED: Now uses Config App Battery Text Size
             Text(text = titleText, color = if (isLow) batteryColor else Color.White, fontSize = theme.batTextSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
             Text(text = "${charging.level}% Remaining", color = Color.White.copy(alpha=0.7f), fontSize = theme.batTextSize * 0.85f, maxLines = 1)
        }
        
        Spacer(Modifier.width(14.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(24.dp)
                    .border(2.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(6.dp))
                    .padding(3.dp), 
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedFill)
                        .background(batteryColor, RoundedCornerShape(3.dp))
                )
                if (charging.isPluggedIn) {
                    Icon(Icons.Default.Add, contentDescription=null, tint=Color.Black.copy(alpha=0.5f), modifier = Modifier.align(Alignment.Center).size(16.dp))
                }
            }
            Box(modifier = Modifier.width(4.dp).height(10.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
    val theme = LocalIslandTheme.current
    val color = Color(alert.alertColor)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.size(44.dp).background(color.copy(alpha=0.2f), CircleShape).border(1.dp, color.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
            val icon = when(alert.alertType) { "THERMAL" -> Icons.Default.Warning; "ROGUE" -> Icons.Default.BatteryAlert; else -> Icons.Default.Info }
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             // 🎛️ FIXED: Now uses Config App Alert Text Sizes
             Text(text = alert.title, color = color, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
             Text(text = alert.message, color = color.copy(alpha=0.8f), fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
    val theme = LocalIslandTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), CircleShape).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { (task.progress.toFloat() / task.progressMax.toFloat()).coerceIn(0f, 1f) }, color = Color.Cyan, trackColor = Color.White.copy(alpha=0.2f), strokeWidth = 2.dp)
            Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             // 🎛️ FIXED: Now uses Config App Alert Text Sizes
             Text(text = task.title, color = Color.White, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
             Text(text = task.text, color = Color.White.copy(alpha=0.8f), fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
    var remainingSeconds by remember { mutableIntStateOf(((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }

    LaunchedEffect(model.targetTimeMs) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds = ((model.targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val alertAlpha by pulseTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alertAlpha")

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.size(48.dp).background(Color.Red.copy(alpha = alertAlpha), CircleShape).border(2.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
            if (model.appIcon != null) { Image(bitmap = model.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.size(36.dp).clip(CircleShape)) } else { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             val theme = LocalIslandTheme.current
             Text(text = "Time Limit Reached", color = Color.Red, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
             Text(text = "${model.appName} closing in ${remainingSeconds}s", color = Color.White, fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.UniversalMid(textColor: Color, activity: LiveActivityModel) { 
    val infiniteTransition = rememberInfiniteTransition(label = "pulse");
    val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse");
    val progress = when(activity) { is LiveActivityModel.General -> activity.progress; is LiveActivityModel.Charging -> activity.level / 100f; else -> null };
    val colorInt = when(activity) { is LiveActivityModel.General -> activity.accentColor; is LiveActivityModel.Charging -> android.graphics.Color.GREEN; else -> android.graphics.Color.WHITE };
    val title = when(activity) { is LiveActivityModel.General -> activity.title; is LiveActivityModel.Charging -> if (activity.isPluggedIn) "Charging" else "Disconnected"; else -> "" }; 
    val dataText = when(activity) { is LiveActivityModel.General -> activity.dataText; is LiveActivityModel.Charging -> "${activity.level}%"; else -> "" }; 
    
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) { 
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) { 
            if (progress != null) CircularProgressIndicator(progress = { progress }, color = Color(colorInt), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize());
            val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f;
            Icon(imageVector = getIconForType(activity.type), contentDescription = null, tint = Color(colorInt), modifier = Modifier.size(24.dp).alpha(iconAlpha)) 
        }; 
        Spacer(Modifier.width(16.dp));
        Column(modifier = Modifier.weight(1f)) { 
            val theme = LocalIslandTheme.current
            Text(text = title, color = textColor, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value));
            Text(text = dataText, color = textColor.copy(alpha = 0.7f), fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)) 
        } 
    } 
}

@Composable
fun DynamicIslandView.GeneralMini(general: LiveActivityModel.General) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = getIconForType(general.type), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value)) } }

@Composable
fun DynamicIslandView.HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) { val tempColor = when { hw.cpuTempCelsius > 45f -> Color.Red; hw.cpuTempCelsius > 38f -> Color.Yellow; else -> Color.Green }; Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); androidx.compose.material3.LinearProgressIndicator(progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) }, modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = tempColor, trackColor = Color.White.copy(alpha=0.2f)); Spacer(Modifier.width(8.dp)); Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }

@Composable
fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Timer, contentDescription = "Session Time", tint = Color(0xFF00FF00), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${model.appName} • ${model.sessionMinutes}m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
    }
}

@Composable
fun DynamicIslandView.GeneralMid(general: LiveActivityModel.General) { UniversalMid(Color.White, general) }

// --- SHARED HELPER FUNCTIONS ---

fun DynamicIslandView.setState(newState: IslandState) { islandState.value = newState }
fun DynamicIslandView.setModel(model: LiveActivityModel?) { activeModel.value = model }
fun DynamicIslandView.setSplitModel(model: LiveActivityModel?) { splitModel.value = model }

fun getIconForType(type: ActivityType): ImageVector { return when(type) { ActivityType.CALL -> Icons.Default.Phone; ActivityType.NAVIGATION -> Icons.Default.LocationOn; ActivityType.TIMER -> Icons.Default.Notifications; ActivityType.MESSAGE -> Icons.Default.Email; ActivityType.ALARM -> Icons.Default.Notifications; ActivityType.CHARGING -> Icons.Default.Add; ActivityType.BATTERY_LOW -> Icons.Default.Warning; ActivityType.BLUETOOTH -> Icons.Default.Bluetooth; ActivityType.WIFI -> Icons.Default.Wifi; ActivityType.HARDWARE -> Icons.Default.Info; else -> Icons.Default.Info } }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.safeMarquee(state: IslandState): Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) {
        this.basicMarquee()
    } else {
        this
    }
}
