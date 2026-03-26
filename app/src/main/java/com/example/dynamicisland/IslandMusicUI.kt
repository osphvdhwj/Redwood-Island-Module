package com.example.dynamicisland

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Global flag to prevent local position from fighting the drag
var isDraggingMedia by mutableStateOf(false)

@Composable
fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    val theme = LocalIslandTheme.current
    var isClicked by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(if(theme.actionAnimType == "CHECKMARK") 1000 else 300)
            isClicked = false
        }
    }

    val scale by animateFloatAsState(if (isClicked && theme.actionAnimType == "BOUNCE") 1.3f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label="scale")
    val alpha by animateFloatAsState(if (isClicked && theme.actionAnimType == "PULSE") 0.3f else 1f, tween(150), label="alpha")
    val currentIcon = if (isClicked && theme.actionAnimType == "CHECKMARK") Icons.Default.Check else icon
    val currentTint = if (isClicked && theme.actionAnimType == "CHECKMARK") Color.Green else tint

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(RoundedCornerShape(theme.buttonCornerRadius))
            .background(currentTint.copy(alpha = bgAlpha))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isClicked = true
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = currentIcon,
            contentDescription = null,
            tint = currentTint.copy(alpha = alpha),
            modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    val theme = LocalIslandTheme.current
    val dynamicTextColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color(0xFF00FFCC) 
    val safeDuration = if (music.durationMs <= 0L) 1f else music.durationMs.toFloat()
    val currentPosition = currentMediaPos.longValue.toFloat().coerceAtLeast(0f)
    val targetProgress = (currentPosition / safeDuration).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(1000, easing = LinearEasing), label = "bottom_progress")

    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label="spin")
            val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "spin")
            val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp)) {
                if (music.albumArt != null) {
                    Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.fillMaxSize().clip(CircleShape).rotate(currentRotation))
                } else {
                    Box(Modifier.fillMaxSize().background(Color.White.copy(0.2f), CircleShape))
                }
            }
            
            Spacer(Modifier.width(8.dp))
            Text(text = music.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f).safeMarquee(islandState.value))
            Spacer(Modifier.width(8.dp))
            Text(text = "${formatTime(currentMediaPos.longValue)} / ${formatTime(music.durationMs)}", color = Color.White.copy(alpha=0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        
        Canvas(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp)) {
            val activeWidth = size.width * animatedProgress
            drawLine(color = dynamicTextColor.copy(alpha=0.2f), start = androidx.compose.ui.geometry.Offset(activeWidth, size.height/2), end = androidx.compose.ui.geometry.Offset(size.width, size.height/2), strokeWidth = size.height, cap = StrokeCap.Round)
            drawLine(color = dynamicTextColor, start = androidx.compose.ui.geometry.Offset(0f, size.height/2), end = androidx.compose.ui.geometry.Offset(activeWidth, size.height/2), strokeWidth = size.height, cap = StrokeCap.Round)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(activeWidth, size.height/2))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val theme = LocalIslandTheme.current
    val dynamicTextColor = Color(music.titleTextColor)
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "spin")
    val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    var localIsShuffled by remember(music.title, music.isShuffled) { mutableStateOf(music.isShuffled) }
    var localRepeatMode by remember(music.title, music.repeatMode) { mutableIntStateOf(music.repeatMode) }

    Row(modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            IsolatedCircularProgress(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor)
            if (music.albumArt != null) { 
                Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(50.dp).clip(CircleShape).rotate(currentRotation)) 
            } else {
                Box(Modifier.size(50.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.musicTitleSize, fontFamily = theme.titleFont, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = theme.musicArtistSize, fontFamily = theme.titleFont, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            
            Spacer(Modifier.height(theme.elementGap))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = formatTime(currentMediaPos.longValue), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, isPlaying = music.isPlaying, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.weight(1f).height(theme.musicSeekerThick * 3))
                Spacer(Modifier.width(6.dp))
                Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = 26.dp, bgAlpha = 0f) { onPrevClick?.invoke() }
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = 34.dp, bgAlpha = 0.15f) { onPlayPauseClick?.invoke() }
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = 26.dp, bgAlpha = 0f) { onNextClick?.invoke() }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) || it.actionName.contains("like", true) }
                if (favoriteAction != null) {
                    val heartIcon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                    val heartTint = if (localIsLiked) Color(0xFFFF2A5F) else dynamicTextColor.copy(alpha=0.8f)
                    InteractiveIconButton(icon = heartIcon, tint = heartTint, baseSize = 22.dp, bgAlpha = 0f) { 
                        localIsLiked = !localIsLiked;
                        onCustomMediaAction?.invoke(favoriteAction.actionName) 
                    }
                }

                val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) || it.actionName.contains("shuffle", true) }
                if (repeatAction != null) {
                    val isShuffleAct = repeatAction.actionName.contains("shuffle", true)
                    if (isShuffleAct) {
                        val shuffleTint = if (localIsShuffled) Color(0xFF00FFCC) else dynamicTextColor.copy(alpha=0.8f)
                        InteractiveIconButton(icon = Icons.Default.Shuffle, tint = shuffleTint, baseSize = 22.dp, bgAlpha = 0f) { 
                            localIsShuffled = !localIsShuffled;
                            onCustomMediaAction?.invoke(repeatAction.actionName) 
                        }
                    } else {
                        val loopIcon = if (localRepeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat
                        val loopTint = if (localRepeatMode > 0) Color(0xFF00FFCC) else dynamicTextColor.copy(alpha=0.8f)
                        InteractiveIconButton(icon = loopIcon, tint = loopTint, baseSize = 22.dp, bgAlpha = 0f) { 
                            localRepeatMode = if (localRepeatMode == 0) 1 else if (localRepeatMode == 1) 2 else 0
                            onCustomMediaAction?.invoke(repeatAction.actionName) 
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val dynamicTextColor = Color.White
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
            val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            val hasHeadphone = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET }
            if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else if (hasHeadphone) { audioIcon = Icons.Default.Headset; audioLabel = "Headphones" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        } catch (e: Exception) { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (music.appIcon != null) { Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Logo", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) } else Box(Modifier.size(36.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(10.dp)))
            
            Surface(
                onClick = { onAudioOutputClick?.invoke() },
                color = Color.White.copy(alpha=0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.5f))
        
        Text(text = music.title, color = dynamicTextColor, fontSize = theme.musicTitleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
        Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = theme.musicArtistSize * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
        
        Spacer(modifier = Modifier.weight(0.5f))

        IsolatedTimeRow(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor)
        InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, isPlaying = music.isPlaying, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.padding(vertical = theme.elementGap).height(theme.musicSeekerThick * 3))
        Spacer(modifier = Modifier.weight(1f))
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
            
            val favoriteAction = music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("favorite", true) || it.actionName.contains("thumb", true) || it.actionName.contains("like", true) }
            if (favoriteAction != null) {
                val heartIcon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                val heartTint = if (localIsLiked) Color(0xFFFF2A5F) else dynamicTextColor.copy(alpha=0.8f)
                InteractiveIconButton(icon = heartIcon, tint = heartTint, baseSize = theme.buttonSize, bgAlpha = 0f) { 
                     localIsLiked = !localIsLiked;
                     onCustomMediaAction?.invoke(favoriteAction.actionName) 
                }
            } else {
                Spacer(Modifier.width(theme.buttonSize))
            }
            
            InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onPrevClick?.invoke() }
            
            val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
            InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0.2f) { onPlayPauseClick?.invoke() }
            
            InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = theme.buttonSize, bgAlpha = 0f) { onNextClick?.invoke() }

            val repeatAction = music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("loop", true) || it.actionName.contains("shuffle", true) }
            if (repeatAction != null) {
                val isShuffleAct = repeatAction.actionName.contains("shuffle", true)
                if (isShuffleAct) {
                    val shuffleTint = if (localIsShuffled) Color(0xFF00FFCC) else dynamicTextColor.copy(alpha=0.8f)
                    InteractiveIconButton(icon = Icons.Default.Shuffle, tint = shuffleTint, baseSize = theme.buttonSize, bgAlpha = 0f) { 
                        localIsShuffled = !localIsShuffled;
                        onCustomMediaAction?.invoke(repeatAction.actionName) 
                    }
                } else {
                    val loopIcon = if (localRepeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val loopTint = if (localRepeatMode > 0) Color(0xFF00FFCC) else dynamicTextColor.copy(alpha=0.8f)
                    InteractiveIconButton(icon = loopIcon, tint = loopTint, baseSize = theme.buttonSize, bgAlpha = 0f) { 
                        localRepeatMode = if (localRepeatMode == 0) 1 else if (localRepeatMode == 1) 2 else 0
                        onCustomMediaAction?.invoke(repeatAction.actionName) 
                    }
                }
            } else {
                Spacer(Modifier.width(theme.buttonSize))
            }
        }
    }
}

fun formatTime(ms: Long): String { 
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return String.format("%d:%02d", s / 60, s % 60) 
}

@Composable
fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, modifier: Modifier = Modifier) {
    Text(text = "${formatTime(posProvider())} / ${formatTime(durationMs)}", color = textColor, fontSize = 12.sp, modifier = modifier)
}

@Composable
fun IsolatedTimeRow(durationMs: Long, posProvider: () -> Long, textColor: Color) {
    var localPos by remember { mutableLongStateOf(posProvider()) }
    LaunchedEffect(Unit) { while(isActive) { delay(100); localPos = posProvider() } }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = formatTime(localPos), color = textColor.copy(alpha=0.7f), fontSize = 12.sp)
        Text(text = formatTime(durationMs), color = textColor.copy(alpha=0.7f), fontSize = 12.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedMediaSlider(durationMs: Long, posProvider: () -> Long, dynamicTextColor: Color, onSeek: (Long) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
    var localPosition by remember(isDragged) { mutableFloatStateOf(currentPosition) }
    val safePosition = if (isDragged) localPosition else currentPosition

    Slider(
        value = (safePosition / safeDuration).coerceIn(0f, 1f),
        onValueChange = {
            localPosition = it * safeDuration
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        onValueChangeFinished = {
            onSeek(localPosition.toLong())
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.3f), thumbColor = dynamicTextColor),
        modifier = Modifier.fillMaxWidth().height(24.dp)
    )
}

@Composable
fun IsolatedLinearProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    val theme = LocalIslandTheme.current 
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
    val targetProgress = (currentPosition / safeDuration).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(durationMillis = 1000, easing = LinearEasing), label = "liquid_progress")

    Canvas(modifier = modifier.height(theme.mediaBarThickness)) {
        drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = theme.mediaBarThickness.toPx(), cap = theme.mediaBarCap)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width * animatedProgress, size.height / 2), strokeWidth = theme.mediaBarThickness.toPx(), cap = theme.mediaBarCap)
    }
}

@Composable
fun IsolatedCircularProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, strokeWidth: Dp, modifier: Modifier = Modifier) {
    val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
    CircularProgressIndicator(progress = { (posProvider().toFloat() / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = trackColor, strokeWidth = strokeWidth, modifier = modifier)
}

@Composable
fun IsolatedCircularProgress(durationMs: Long, posProvider: () -> Long, color: Color) {
    var localPos by remember { mutableLongStateOf(posProvider()) }
    LaunchedEffect(Unit) { while(isActive) { delay(100); localPos = posProvider() } }

    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val currentPosition = localPos.toFloat().coerceAtLeast(0f)
    CircularProgressIndicator(progress = { (currentPosition / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = color.copy(alpha = 0.2f), strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
}

@Composable
fun InteractiveWavyMediaBar(durationMs: Long, posProvider: () -> Long, isPlaying: Boolean, color: Color, trackColor: Color, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var localPos by remember { mutableLongStateOf(posProvider()) }
    LaunchedEffect(Unit) { while(isActive) { delay(50); if (!isDraggingMedia) localPos = posProvider() } }
    
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val currentProgress = (localPos / safeDuration).coerceIn(0f, 1f)
    val displayProgress = if (isDraggingMedia) dragProgress else currentProgress

    val targetAmplitude = if (isDraggingMedia) 6f else if (isPlaying) 2.5f else 0f
    
    val amplitude by animateFloatAsState(targetValue = targetAmplitude, animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f), label = "amp")
    val phaseShift by rememberInfiniteTransition(label = "wave").animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "phase")

    val wavePath = remember { androidx.compose.ui.graphics.Path() }

    Canvas(
        modifier = modifier.fillMaxWidth().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset -> isDraggingMedia = true; dragProgress = (offset.x / size.width).coerceIn(0f, 1f); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                onDragEnd = { isDraggingMedia = false; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSeek((dragProgress * safeDuration).toLong()) },
                onDragCancel = { isDraggingMedia = false }
            ) { change, _ -> change.consume(); dragProgress = (change.position.x / size.width).coerceIn(0f, 1f) }
        }.pointerInput(Unit) { detectTapGestures(onTap = { offset -> onSeek(((offset.x / size.width).coerceIn(0f, 1f) * safeDuration).toLong()) }) }
    ) {
        val midY = size.height / 2
        val activeWidth = size.width * displayProgress
        drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(activeWidth, midY), end = androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = size.height, cap = StrokeCap.Round)

        wavePath.rewind() 
        wavePath.moveTo(0f, midY)
        val frequency = 0.08f
        
        for (x in 0..activeWidth.toInt() step 4) {
            val tension = if (isDraggingMedia) (1f - (kotlin.math.abs(x - activeWidth) / size.width)).coerceAtLeast(0.2f) else 1f
            val y = midY + kotlin.math.sin((x * frequency) + phaseShift) * (amplitude.dp.toPx() * tension)
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(activeWidth, midY)

        drawIntoCanvas { 
            drawPath(path = wavePath, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height, cap = StrokeCap.Round)) 
        }
        drawCircle(color = Color.White, radius = if(isDraggingMedia) 6.dp.toPx() else 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(activeWidth, midY))
    }
}
