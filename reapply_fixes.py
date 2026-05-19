import re
import os

# IslandMusicUI
with open('app/src/main/java/com/example/dynamicisland/ui/IslandMusicUI.kt', 'r') as f:
    content = f.read()

new_music_mini = """@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    val dynamicColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color(0xFF00FFCC)

    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(music.isPlaying) {
        if (music.isPlaying) {
            rotationAnim.animateTo(
                targetValue = rotationAnim.value + 3600f,
                animationSpec = infiniteRepeatable(
                    tween(8000, easing = LinearEasing)
                )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = if (music.durationMs > 0) {
                    currentMediaPos.longValue.toFloat() / music.durationMs
                } else 0f

                drawArc(
                    color = dynamicColor.copy(alpha = 0.15f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = dynamicColor.copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (music.albumArt != null) {
                Image(
                    bitmap = music.albumArt.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .rotate(if (isCubeRotationEnabled.value) rotationAnim.value else 0f)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = music.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f).safeMarquee(islandState.value)
        )

        Spacer(Modifier.width(8.dp))

        AnimatedContent(
            targetState = music.isPlaying,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            }, label = "playing_state"
        ) { playing ->
            if (playing) {
                PlayingBarsIndicator(color = dynamicColor)
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = dynamicColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun PlayingBarsIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label="bars")

    val bar1 by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label="b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(500, 150, FastOutSlowInEasing), RepeatMode.Reverse), label="b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        0.2f, 0.9f,
        infiniteRepeatable(tween(700, 75, FastOutSlowInEasing), RepeatMode.Reverse), label="b3"
    )

    Row(
        modifier = modifier.width(16.dp).height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}"""

music_mini_pattern = re.compile(r'@OptIn\(ExperimentalFoundationApi::class\)\s*@Composable\s*fun DynamicIslandView\.MusicMini\(music: LiveActivityModel\.Music\)\s*\{.*?\}\n\n', re.DOTALL)
content = music_mini_pattern.sub(new_music_mini + "\n\n", content)

imports = """import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
"""
content = content.replace('import androidx.compose.animation.core.*', 'import androidx.compose.animation.core.*\n' + imports)

with open('app/src/main/java/com/example/dynamicisland/ui/IslandMusicUI.kt', 'w') as f:
    f.write(content)

# IslandMusicMax
new_music_max = """package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val dynamicTextColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color.White
    val theme = LocalIslandTheme.current
    var audioIcon by remember { mutableStateOf(Icons.Default.Smartphone) }
    var audioLabel by remember { mutableStateOf("Phone") }
    val context = LocalContext.current

    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    var localIsShuffled by remember(music.title, music.isShuffled) { mutableStateOf(music.isShuffled) }
    var localRepeatMode by remember(music.title, music.repeatMode) { mutableIntStateOf(music.repeatMode) }

    LaunchedEffect(music) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val devices = am?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET }
            if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        } catch (e: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (music.blurredAlbumArt != null) {
            Image(
                bitmap = music.blurredAlbumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.1f),
                        0.4f to Color.Black.copy(alpha = 0.3f),
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (music.appIcon != null) {
                    Image(
                        bitmap = music.appIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .alpha(0.8f)
                    )
                } else {
                    Box(Modifier.size(20.dp).background(Color.White.copy(0.2f), RoundedCornerShape(6.dp)))
                }

                Surface(onClick = { onAudioOutputClick?.invoke() }, color = Color.White.copy(alpha=0.15f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (music.albumArt != null) {
                    Image(
                        bitmap = music.albumArt.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .shadow(8.dp, RoundedCornerShape(10.dp))
                    )
                } else {
                     Box(Modifier.size(56.dp).background(Color.White.copy(0.2f), RoundedCornerShape(10.dp)))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.safeMarquee(islandState.value)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = music.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.safeMarquee(islandState.value)
                    )
                }

                music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("like", true) }?.let { action ->
                    InteractiveIconButton(icon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, tint = if (localIsLiked) Color(0xFFFF2A5F) else Color.White.copy(0.8f), baseSize = 36.dp) { localIsLiked = !localIsLiked; onCustomMediaAction?.invoke(action.actionName) }
                }
            }

            Column {
                InteractiveWavyMediaBar(
                    durationMs = music.durationMs,
                    posProvider = { currentMediaPos.longValue },
                    isPlaying = music.isPlaying,
                    color = dynamicTextColor,
                    trackColor = dynamicTextColor.copy(alpha = 0.2f),
                    onSeek = { onSeekTo?.invoke(it) },
                    modifier = Modifier.height(theme.musicSeekerThick * 4)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentMediaPos.longValue),
                        color = Color.White.copy(0.5f),
                        fontSize = 11.sp
                    )
                    Text(
                        formatTime(music.durationMs),
                        color = Color.White.copy(0.5f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                music.customActions.find { it.actionName.contains("shuffle", true) }?.let { action ->
                    InteractiveIconButton(icon = Icons.Default.Shuffle, tint = if (localIsShuffled) dynamicTextColor else Color.White.copy(0.5f), baseSize = 32.dp) { localIsShuffled = !localIsShuffled; onCustomMediaAction?.invoke(action.actionName) }
                } ?: Spacer(Modifier.width(32.dp))

                InteractiveIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = Color.White,
                    baseSize = 44.dp
                ) { onPrevClick?.invoke() }

                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = Color.White, baseSize = 56.dp, bgAlpha = 0.2f) { onPlayPauseClick?.invoke() }

                InteractiveIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    tint = Color.White,
                    baseSize = 44.dp
                ) { onNextClick?.invoke() }

                music.customActions.find { it.actionName.contains("repeat", true) }?.let { action ->
                    InteractiveIconButton(icon = if (localRepeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat, tint = if (localRepeatMode > 0) dynamicTextColor else Color.White.copy(0.5f), baseSize = 32.dp) { localRepeatMode = (localRepeatMode + 1) % 3; onCustomMediaAction?.invoke(action.actionName) }
                } ?: Spacer(Modifier.width(32.dp))
            }
        }
    }
}
"""
with open('app/src/main/java/com/example/dynamicisland/ui/IslandMusicMax.kt', 'w') as f:
    f.write(new_music_max)

# IslandCallUI
with open('app/src/main/java/com/example/dynamicisland/ui/IslandCallUI.kt', 'r') as f:
    content = f.read()
new_call_mini = """@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val haptic = LocalHapticFeedback.current
    val isRinging = model.state == "RINGING"

    val rippleScale by rememberInfiniteTransition(label="ringRipple").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "ripple"
    )

    val bgColor = if (isRinging) {
        androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF00C853), Color(0xFF1DE9B6))
        )
    } else {
        androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF00897B), Color(0xFF00C853))
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = if (isRinging) rippleScale else 1f
                scaleY = if (isRinging) rippleScale else 1f
            }
            .background(bgColor, RoundedCornerShape(50))
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val phoneRotation by rememberInfiniteTransition(label="phoneRot").animateFloat(
            initialValue = -15f,
            targetValue = 15f,
            animationSpec = infiniteRepeatable(
                tween(150, easing = LinearEasing),
                RepeatMode.Reverse
            ), label = "rotation"
        )

        Icon(
            Icons.Default.Phone,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(14.dp)
                .graphicsLayer {
                    rotationZ = if (isRinging) phoneRotation else 0f
                }
        )

        if (isRinging) {
            Text(
                text = model.callerName.ifEmpty { "Incoming Call" },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        } else {
            IsolatedTimerText(
                startTime = model.startTime,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}"""

call_mini_pattern = re.compile(r'@Composable\s*fun DynamicIslandView\.CallMini\(model: LiveActivityModel\.Call\)\s*\{.*?\}\n\n', re.DOTALL)
content = call_mini_pattern.sub(new_call_mini + "\n\n", content)
with open('app/src/main/java/com/example/dynamicisland/ui/IslandCallUI.kt', 'w') as f:
    f.write(content)

# IslandChargingUI
with open('app/src/main/java/com/example/dynamicisland/ui/IslandChargingUI.kt', 'r') as f:
    content = f.read()

new_charging_max = """// 🚀 NEW: The Massive iOS-Style Charging Expansion
@Composable
fun DynamicIslandView.ChargingMax(charging: LiveActivityModel.Charging) {
    val batteryColor = Color(0xFF00FF55)
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(1f, animationSpec = tween(200))
            scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f))
        }
    }

    var displayedLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(charging.level) {
        kotlinx.coroutines.delay(200)
        val target = charging.level
        val duration = 800L
        val startTime = System.currentTimeMillis()
        while (displayedLevel < target) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress).pow(3)
            displayedLevel = (eased * target).toInt()
            kotlinx.coroutines.delay(16)
        }
        displayedLevel = target
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(Brush.radialGradient(listOf(batteryColor.copy(alpha = 0.2f), Color.Transparent)))
        )

        Row(
            modifier = Modifier.fillMaxWidth().scale(scaleAnim.value),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(48.dp).background(batteryColor.copy(alpha = 0.2f), CircleShape).blur(12.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${displayedLevel}%",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alpha(alphaAnim.value)
                )
                Text(
                    text = "Charging",
                    color = batteryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            Box(modifier = Modifier.scale(1.3f)) {
                LiquidBatteryCanvas(level = charging.level, color = batteryColor, isCharging = true)
            }
        }
    }
}

@Composable
fun LiquidBatteryCanvas(level: Int, color: Color, isCharging: Boolean) {
    val targetFill = level / 100f
    val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(46.dp).height(24.dp).border(2.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(3.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val clipPath = androidx.compose.ui.graphics.Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()))) }
                clipPath(clipPath) {
                    drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width * animatedFill, size.height))
                }
            }
            if (isCharging) {
                Icon(Icons.Default.Add, contentDescription=null, tint=Color.Black.copy(alpha=0.6f), modifier = Modifier.align(Alignment.Center).size(16.dp))
            }
        }
        Box(modifier = Modifier.width(4.dp).height(10.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)))
    }
}"""

charging_max_pattern = re.compile(r'// 🚀 NEW: The Massive iOS-Style Charging Expansion\n@Composable\s*fun DynamicIslandView\.ChargingMax\(charging: LiveActivityModel\.Charging\)\s*\{.*?\}\n(?=@Composable|$)', re.DOTALL)
content = charging_max_pattern.sub(new_charging_max + "\n", content)

content = content.replace('import androidx.compose.animation.core.*', 'import androidx.compose.animation.core.*\nimport kotlin.math.pow\nimport androidx.compose.ui.draw.alpha\nimport androidx.compose.ui.graphics.drawscope.clipPath')

with open('app/src/main/java/com/example/dynamicisland/ui/IslandChargingUI.kt', 'w') as f:
    f.write(content)

# IslandRingUI
with open('app/src/main/java/com/example/dynamicisland/ui/IslandRingUI.kt', 'r') as f:
    content = f.read()

new_ring_ui = """package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun DynamicIslandView.RingUI(model: LiveActivityModel?) {
    val musicModel = model as? LiveActivityModel.Music
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = isMedia || globalIsCharging.value || true

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
        val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }

        val batteryLevel = globalBatteryLevel.intValue
        val baseColor = if (isMedia) {
            musicModel?.dominantColor?.let { Color(it) } ?: Color.White
        } else if (globalIsCharging.value) {
            Color(0xFF00FF00)
        } else {
             when {
                batteryLevel <= 5 -> Color(0xFFFF0000)
                batteryLevel <= 10 -> Color(0xFFFF3333)
                batteryLevel <= 40 -> Color(0xFFFFA500)
                batteryLevel <= 60 -> Color(0xFFFFFF00)
                else -> Color(0xFF006400)
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "ring_breath")
        val breathScale by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                tween(3000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "scale"
        )
         val progressColor = baseColor

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp)) {
                val strokeW = ringThickness.value.dp.toPx()
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                val progressPercent = progress.coerceIn(0f, 1f)

                // Subtle track
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f, sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft, size = arcSize,
                    style = Stroke(strokeW)
                )

                // Clean single color progress, no sweep gradient
                if (progressPercent > 0.01f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progressPercent,
                        useCenter = false,
                        topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    // Single dot at progress end - cleaner than tick marks
                    val angleRad = Math.toRadians((-90.0 + 360.0 * progressPercent))
                    val radius = (size.width - strokeW) / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color.White,
                        radius = (strokeW / 2) * 1.2f,
                        center = androidx.compose.ui.geometry.Offset(
                            center.x + radius * kotlin.math.cos(angleRad).toFloat(),
                            center.y + radius * kotlin.math.sin(angleRad).toFloat()
                        )
                    )
                }
            }
        }
    }
}
"""
with open('app/src/main/java/com/example/dynamicisland/ui/IslandRingUI.kt', 'w') as f:
    f.write(new_ring_ui)

# IslandDashboardMax
with open('app/src/main/java/com/example/dynamicisland/ui/IslandDashboardMax.kt', 'r') as f:
    content = f.read()

new_liquid_slider = """@Composable
fun LiquidSlider(icon: ImageVector, value: Float, color: Color, label: String, onValueChange: (Float) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }

    val animatedFill by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "slider_fill"
    )

    GlassCard(modifier = Modifier.width(60.dp).fillMaxHeight()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { change, dragAmount ->
                            val isFast = kotlin.math.abs(dragAmount) > 15f
                            val multiplier = if (isFast) 10f else 1f
                            val delta = (dragAmount * multiplier * -0.1f)
                            onValueChange((value + delta).coerceIn(0f, 100f))
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .background(
                        Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )

            AnimatedContent(
                targetState = isDragging,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                label = "slider_label"
            ) { dragging ->
                if (dragging) {
                    Text(
                        "${value.toInt()}%",
                        color = if (animatedFill > 0.4f) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (animatedFill > 0.4f) Color.Black else Color.White.copy(0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            label,
                            color = if (animatedFill > 0.4f) Color.Black.copy(0.6f) else Color.White.copy(0.4f),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}"""

slider_pattern = re.compile(r'@Composable\s*fun LiquidSlider\(icon: ImageVector, value: Float, color: Color, onValueChange: \(Float\) -> Unit\)\s*\{.*?\}\n\n', re.DOTALL)
content = slider_pattern.sub(new_liquid_slider + "\n\n", content)

content = content.replace('LiquidSlider(\n                            icon = Icons.Default.VolumeUp,\n                            value = localVol,\n                            color = Color.White,\n                            onValueChange = { localVol = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }\n                        )',
                          'LiquidSlider(\n                            icon = Icons.Default.VolumeUp,\n                            value = localVol,\n                            color = Color.White,\n                            label = "VOL",\n                            onValueChange = { localVol = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }\n                        )')
content = content.replace('LiquidSlider(\n                            icon = Icons.Default.LightMode,\n                            value = localBrt,\n                            color = Color(0xFFFFD700),\n                            onValueChange = { localBrt = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }\n                        )',
                          'LiquidSlider(\n                            icon = Icons.Default.LightMode,\n                            value = localBrt,\n                            color = Color(0xFFFFD700),\n                            label = "BRT",\n                            onValueChange = { localBrt = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }\n                        )')

content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.animation.AnimatedContent\nimport androidx.compose.ui.input.pointer.pointerInput')

with open('app/src/main/java/com/example/dynamicisland/ui/IslandDashboardMax.kt', 'w') as f:
    f.write(content)

# IslandMainUI
with open('app/src/main/java/com/example/dynamicisland/ui/IslandMainUI.kt', 'r') as f:
    content = f.read()

old_bg_anim = "val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = \"bgColor\")"
new_bg_anim = """val bgSpec = when {
    state == IslandState.TYPE_3_MAX -> tween<androidx.compose.ui.graphics.Color>(400, easing = FastOutSlowInEasing)
    state == IslandState.TYPE_0_RING -> tween<androidx.compose.ui.graphics.Color>(250, easing = LinearOutSlowInEasing)
    else -> spring<androidx.compose.ui.graphics.Color>(dampingRatio = 0.85f, stiffness = 300f)
}
val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = bgSpec, label = "bgColor")"""
content = content.replace(old_bg_anim, new_bg_anim)

old_transition = """transitionSpec = { (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90))) togetherWith fadeOut(animationSpec = tween(90)) }"""
new_transition = """transitionSpec = {
                            val expanding = targetState.ordinal > initialState.ordinal
                            if (expanding) {
                                (fadeIn(animationSpec = tween(300, delayMillis = 60)) + scaleIn(
                                    initialScale = 0.88f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                                )) togetherWith fadeOut(animationSpec = tween(80))
                            } else {
                                (fadeIn(animationSpec = tween(200)) + scaleIn(
                                    initialScale = 1.04f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
                                )) togetherWith (fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.96f))
                            }
                        }"""
content = content.replace(old_transition, new_transition)

old_graphics_layer = ".graphicsLayer { scaleX = touchScale * islandScale; scaleY = touchScale * islandScale; alpha = islandAlpha; transformOrigin = TransformOrigin(0.5f, 0.5f) }"
new_graphics_layer = ".graphicsLayer { scaleX = islandScale; scaleY = islandScale; alpha = islandAlpha; transformOrigin = TransformOrigin(0.5f, 0.5f) }"
content = content.replace(old_graphics_layer, new_graphics_layer)

old_shadow = ".shadow(elevation = if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp, shape = RoundedCornerShape(animatedRadius), spotColor = Color.Black)"
shadow_logic = """val shadowElevation by animateDpAsState(
        targetValue = if (isSquished) 4.dp else (if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
    )"""
box_idx = content.find("Box(\n            modifier = Modifier")
if box_idx != -1:
    content = content[:box_idx] + shadow_logic + "\n    " + content[box_idx:]
    content = content.replace(old_shadow, ".shadow(elevation = shadowElevation, shape = RoundedCornerShape(animatedRadius), spotColor = Color.Black)")

with open('app/src/main/java/com/example/dynamicisland/ui/IslandMainUI.kt', 'w') as f:
    f.write(content)
