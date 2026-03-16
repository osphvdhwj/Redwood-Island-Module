package com.example.dynamicisland

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// 🚀 NEW: The Standalone "Pill within a Pill" Drag Handle
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
            .height(handleHeight * 2) // Extra height for easier touch targeting
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

// 🚀 NEW: Universal Aura Background for Media
@Composable
fun MediaAuraBackground(albumArt: android.graphics.Bitmap?) {
    if (albumArt != null) {
        Image(
            bitmap = albumArt.asImageBitmap(),
            contentDescription = "Aura",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.35f) // Reduced transparency so it doesn't wash out text
                .blur(40.dp) // Massive blur for the "Aura" effect
        )
        // Dark gradient overlay to ensure text is always readable
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.8f)))))
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212)))
    }
}

// 🚀 REBUILT: Small Pill (TYPE_1_MINI) - Left Aligned, Clean
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    Box(modifier = Modifier.fillMaxSize()) {
        MediaAuraBackground(music.albumArt)

        // Far-Left Aligned Content
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
            
            if (music.albumArt != null) {
                Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape).graphicsLayer { rotationZ = if (music.isPlaying) rotation else 0f })
            } else {
                Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))
            }
            
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${music.title} • ${music.artist}", 
                color = Color.White, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Medium, 
                maxLines = 1, 
                modifier = Modifier.weight(1f).safeMarquee(islandState.value) // Max scroll room
            )
        }

        // Floating Handle & Timestamps at the exact bottom center
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = Color.White.copy(alpha=0.6f), fontSize = 10.sp)
            FloatingDragHandle(
                onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) }
            )
            Text(text = formatTime(music.durationMs), color = Color.White.copy(alpha=0.6f), fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        }
    }
}

// 🚀 REBUILT: Mid Pill (TYPE_2_MID) - Perfect Spacing
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val dynamicTextColor = Color.White // Forced white over the dark aura for premium contrast
    Box(modifier = Modifier.fillMaxSize()) {
        MediaAuraBackground(music.albumArt)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            
            // Top Row: Art & Text
            Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    IsolatedCircularProgressIndicator(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha = 0.2f), strokeWidth = 2.5.dp, modifier = Modifier.fillMaxSize())
                    if (music.albumArt != null) {
                        Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(48.dp).clip(CircleShape)) 
                    } else {
                        Box(Modifier.size(48.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = dynamicTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                    Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                }
                
                // Mini Play/Pause directly in the Mid Pill
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = 40.dp, bgAlpha = 0.15f) { onPlayPauseClick?.invoke() }
            }

            // Bottom Row: Floating Handle & Timestamps
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor.copy(alpha=0.6f), fontSize = 11.sp)
                FloatingDragHandle(
                    onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                    onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) }
                )
                Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// 🚀 REBUILT: Max Pill (TYPE_3_MAX) - The Ultimate Music Dashboard
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val dynamicTextColor = Color.White
    var audioIcon by remember { mutableStateOf(Icons.Default.Smartphone) }
    var audioLabel by remember { mutableStateOf("Phone") }

    LaunchedEffect(music) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val devices = am?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            if (devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } 
            else if (devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES }) { audioIcon = Icons.Default.Headset; audioLabel = "Headphones" }
        } catch (e: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MediaAuraBackground(music.albumArt)

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            
            // Top Row: App Icon & Output Switcher
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (music.appIcon != null) Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App", modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))) 
                else Box(Modifier.size(32.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(8.dp)))
                
                Row(modifier = Modifier.background(Color.White.copy(alpha=0.15f), RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).clickable { onAudioOutputClick?.invoke() }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Huge Album Art inside Max Pill
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).shadow(8.dp))
            }

            Spacer(modifier = Modifier.weight(0.5f))
            
            // Text Block
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = music.title, color = dynamicTextColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Timestamps & Slider
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp)
                Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
            }
            IsolatedMediaSlider(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, dynamicTextColor = dynamicTextColor, onSeek = { onSeekTo?.invoke(it) })

            Spacer(modifier = Modifier.height(16.dp))
            
            // Media Controls
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                InteractiveIconButton(icon = Icons.Default.FavoriteBorder, tint = dynamicTextColor, baseSize = 48.dp, bgAlpha = 0f) {} 
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = 48.dp, bgAlpha = 0f) { onPrevClick?.invoke() }
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = Color.Black, baseSize = 64.dp, bgAlpha = 1f) { onPlayPauseClick?.invoke() } // Solid white button, black icon for premium feel
                
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = 48.dp, bgAlpha = 0f) { onNextClick?.invoke() }
                InteractiveIconButton(icon = Icons.Default.Refresh, tint = dynamicTextColor, baseSize = 48.dp, bgAlpha = 0f) {} 
            }

            Spacer(modifier = Modifier.weight(1f))

            // Floating Handle at very bottom
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                FloatingDragHandle(
                    onSwipeDown = { onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) },
                    onSwipeUp = { onGestureEvent?.invoke(IslandGesture.SWIPE_UP) }
                )
            }
        }
    }
}

// ==========================================
// UTILITY COMPONENTS (Do not touch)
// ==========================================

@Composable
fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    var isClicked by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isClicked) { if (isClicked) { delay(200); isClicked = false } }
    val scale by animateFloatAsState(if (isClicked) 1.15f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label="scale")

    val bgColor = if (bgAlpha == 1f) Color.White else tint.copy(alpha = bgAlpha) // Exception for solid play button

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); isClicked = true; onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(baseSize * 0.5f).graphicsLayer { scaleX = scale; scaleY = scale })
    }
}

fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

@Composable
fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp, modifier: Modifier = Modifier) {
    val timeStr by remember(durationMs) { derivedStateOf { formatTime(posProvider()) } }
    Text(text = timeStr, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedMediaSlider(durationMs: Long, posProvider: () -> Long, dynamicTextColor: Color, onSeek: (Long) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    var localPosition by remember { mutableFloatStateOf(0f) }
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val safePosition = if (isDragged) localPosition else posProvider().toFloat().coerceAtLeast(0f)

    Slider(
        value = (safePosition / safeDuration).coerceIn(0f, 1f),
        onValueChange = { localPosition = it * safeDuration; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        onValueChangeFinished = { onSeek(localPosition.toLong()); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
        interactionSource = interactionSource,
        colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.25f), thumbColor = dynamicTextColor),
        modifier = Modifier.fillMaxWidth().height(24.dp)
    )
}

@Composable
fun IsolatedLinearProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    androidx.compose.foundation.Canvas(modifier = modifier.height(4.dp)) {
        val currentPosition = posProvider().toFloat().coerceAtLeast(0f)
        val targetProgress = (currentPosition / safeDuration).coerceIn(0f, 1f)
        drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, size.height / 2), end = androidx.compose.ui.geometry.Offset(size.width * targetProgress, size.height / 2), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun IsolatedCircularProgressIndicator(durationMs: Long, posProvider: () -> Long, color: Color, trackColor: Color, strokeWidth: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
    CircularProgressIndicator(progress = { (posProvider().toFloat() / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = trackColor, strokeWidth = strokeWidth, modifier = modifier)
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun androidx.compose.ui.Modifier.safeMarquee(state: IslandState): androidx.compose.ui.Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) this.basicMarquee() else this
}
