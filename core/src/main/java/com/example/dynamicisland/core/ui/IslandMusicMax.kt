package com.example.dynamicisland.core.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import com.example.dynamicisland.core.ui.components.text.RollingNumberText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val view = this
    val dynamicTextColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color.White
    val context = LocalContext.current
    
    var isBluetooth by remember { mutableStateOf(false) }
    var audioLabel by remember { mutableStateOf("Phone") }
    
    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    var localIsShuffled by remember(music.title, music.isShuffled) { mutableStateOf(music.isShuffled) }
    var localRepeatMode by remember(music.title, music.repeatMode) { mutableIntStateOf(music.repeatMode) }

    val infiniteTransition = rememberInfiniteTransition(label = "music_max_anim")

    LaunchedEffect(music) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val devices = am?.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
            val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET }
            isBluetooth = hasBt
            audioLabel = if (hasBt) "Bluetooth" else "Phone"
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Layer 1: Cinematic Blurred Background ---
        val art = music.albumArt
        if (art != null) {
            val bgBitmap = music.blurredAlbumArt?.asImageBitmap() ?: art.asImageBitmap()
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.4f)
                    .blur(50.dp)
            )
        }
        
        // --- Layer 2: Ambient Beat Pulse (Ethereal) ---
        val ambientPulse by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "p"
        )
        val dColor = music.dominantColor
        val dominantColor = if (dColor != null) Color(dColor) else Color.Black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(dominantColor.copy(alpha = ambientPulse), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Header: App Icon & Output ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val appIcon = music.appIcon
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .alpha(0.85f)
                        )
                    }
                    Text(
                        text = music.appPackageName.substringAfterLast(".").uppercase(),
                        color = Color.White.copy(0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .squishClickable { view.onAudioOutputClick?.invoke() }
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val logicalIcon = if (isBluetooth) IconProvider.LogicalIcon.BLUETOOTH else IconProvider.LogicalIcon.PHONE
                        Icon(IconProvider.getIcon(logicalIcon, LocalIconPack.current), contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Text(audioLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // --- Main Content: Album Art & Titles ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var artPressed by remember { mutableStateOf(false) }
                val artScale by animateFloatAsState(if(artPressed) 0.9f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label="art")
                
                val albumArt = music.albumArt
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(artScale)
                            .clip(RoundedCornerShape(14.dp))
                            .shadow(16.dp, RoundedCornerShape(14.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(); artPressed = true
                                    waitForUpOrCancellation(); artPressed = false
                                }
                            }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = music.artist,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                music.customActions.find { it.action.contains("heart", true) || it.action.contains("like", true) }?.let { action ->
                    InteractiveIconButton(
                        logicalIcon = IconProvider.LogicalIcon.HEART,
                        tint = if (localIsLiked) Color(0xFFFF2D55) else Color.White.copy(0.6f),
                        baseSize = 44.dp
                    ) { 
                        localIsLiked = !localIsLiked
                        view.onCustomMediaAction?.invoke(action.action) 
                    }
                }
            }

            // --- Progress Section ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InteractiveWavyMediaBar(
                    durationMs = music.durationMs,
                    posProvider = { view.currentMediaPos.longValue },
                    isPlaying = music.isPlaying,
                    color = dominantColor.takeIf { it != Color.Black } ?: Color.White,
                    trackColor = Color.White.copy(alpha = 0.08f),
                    onSeek = { view.onSeekTo?.invoke(it) },
                    modifier = Modifier.height(36.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RollingNumberText(
                        value = formatTime(view.currentMediaPos.longValue),
                        style = TextStyle(color = Color.White.copy(0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = formatTime(music.durationMs),
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // --- Control Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                music.customActions.find { it.action.contains("shuffle", true) }?.let { action ->
                    InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.SHUFFLE, tint = if (localIsShuffled) dominantColor else Color.White.copy(0.3f), baseSize = 40.dp) { localIsShuffled = !localIsShuffled; view.onCustomMediaAction?.invoke("SHUFFLE") }
                } ?: Spacer(Modifier.width(40.dp))

                InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.PREV, tint = Color.White, baseSize = 52.dp) { view.onPrevClick?.invoke() }

                val playIcon = if (music.isPlaying) IconProvider.LogicalIcon.PAUSE else IconProvider.LogicalIcon.PLAY
                Box(contentAlignment = Alignment.Center) {
                    // Aesthetic ring around play button
                    if (music.isPlaying) {
                        val ringPulse by infiniteTransition.animateFloat(0.9f, 1.2f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "ring")
                        Box(modifier = Modifier.size(72.dp).graphicsLayer { scaleX = ringPulse; scaleY = ringPulse }.border(1.dp, Color.White.copy(0.1f), CircleShape))
                    }
                    InteractiveIconButton(logicalIcon = playIcon, tint = Color.White, baseSize = 68.dp, bgAlpha = 0.15f) { view.onPlayPauseClick?.invoke() }
                }

                InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.NEXT, tint = Color.White, baseSize = 52.dp) { view.onNextClick?.invoke() }

                music.customActions.find { it.action.contains("repeat", true) }?.let { action ->
                    InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.SYNC, tint = if (localRepeatMode > 0) dominantColor else Color.White.copy(0.3f), baseSize = 40.dp) { localRepeatMode = (localRepeatMode + 1) % 3; view.onCustomMediaAction?.invoke("REPEAT") }
                } ?: Spacer(Modifier.width(40.dp))
            }
        }
    }
}
