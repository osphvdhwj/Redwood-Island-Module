package com.example.dynamicisland.ui

import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val view = this
    val dynamicTextColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color.White
    val theme = LocalIslandTheme.current
    val context = LocalContext.current
    
    var isBluetooth by remember { mutableStateOf(false) }
    var audioLabel by remember { mutableStateOf("Phone") }
    
    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    var localIsShuffled by remember(music.title, music.isShuffled) { mutableStateOf(music.isShuffled) }
    var localRepeatMode by remember(music.title, music.repeatMode) { mutableIntStateOf(music.repeatMode) }

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
        if (music.albumArt != null) {
            val bgBitmap = music.blurredAlbumArt?.asImageBitmap() ?: music.albumArt.asImageBitmap()
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.35f)
                    .blur(40.dp)
            )
        }
        
        // --- Layer 2: Adaptive Gradient Overlay ---
        val dominantColor = music.dominantColor?.let { Color(it) } ?: Color.Black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(dominantColor.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(0f, 0f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Header: App Icon & Output ---
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
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .alpha(0.9f)
                    )
                } else {
                    Box(Modifier.size(22.dp).background(Color.White.copy(0.2f), RoundedCornerShape(6.dp)))
                }

                Box(
                    modifier = Modifier
                        .squishClickable { view.onAudioOutputClick?.invoke() }
                        .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val logicalIcon = if (isBluetooth) IconProvider.LogicalIcon.BLUETOOTH else IconProvider.LogicalIcon.PHONE
                        Icon(IconProvider.getIcon(logicalIcon, LocalIconPack.current), contentDescription = null, tint = dynamicTextColor, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // --- Main Content: Album Art & Titles ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                var artPressed by remember { mutableStateOf(false) }
                val artScale by animateFloatAsState(if(artPressed) 0.92f else 1f, IslandPhysics.springFloat, label="art")
                
                if (music.albumArt != null) {
                    Image(
                        bitmap = music.albumArt.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .scale(artScale)
                            .clip(RoundedCornerShape(12.dp))
                            .shadow(12.dp, RoundedCornerShape(12.dp))
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
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier.safeMarquee(islandState.value)
                    )
                    Text(
                        text = music.artist,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.safeMarquee(islandState.value)
                    )
                }

                music.customActions.find { it.action.contains("heart", true) || it.action.contains("like", true) }?.let { action ->
                    InteractiveIconButton(
                        logicalIcon = IconProvider.LogicalIcon.HEART,
                        tint = if (localIsLiked) Color(0xFFFF2D55) else Color.White.copy(0.7f),
                        baseSize = 40.dp
                    ) { 
                        localIsLiked = !localIsLiked
                        view.onCustomMediaAction?.invoke(action.action) 
                    }
                }
            }

            // --- Progress Section ---
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InteractiveWavyMediaBar(
                    durationMs = music.durationMs,
                    posProvider = { view.currentMediaPos.longValue },
                    isPlaying = music.isPlaying,
                    color = dynamicTextColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    onSeek = { view.onSeekTo?.invoke(it) },
                    modifier = Modifier.height(32.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(view.currentMediaPos.longValue), color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(music.durationMs), color = Color.White.copy(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- Control Bar ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                music.customActions.find { it.action.contains("shuffle", true) }?.let { action ->
                    InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.SHUFFLE, tint = if (localIsShuffled) dominantColor else Color.White.copy(0.4f), baseSize = 36.dp) { localIsShuffled = !localIsShuffled; view.onCustomMediaAction?.invoke(action.action) }
                } ?: Spacer(Modifier.width(36.dp))

                InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.PREV, tint = Color.White, baseSize = 48.dp) { view.onPrevClick?.invoke() }

                val playIcon = if (music.isPlaying) IconProvider.LogicalIcon.PAUSE else IconProvider.LogicalIcon.PLAY
                InteractiveIconButton(logicalIcon = playIcon, tint = Color.White, baseSize = 64.dp, bgAlpha = 0.15f) { view.onPlayPauseClick?.invoke() }

                InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.NEXT, tint = Color.White, baseSize = 48.dp) { view.onNextClick?.invoke() }

                music.customActions.find { it.action.contains("repeat", true) }?.let { action ->
                    InteractiveIconButton(logicalIcon = IconProvider.LogicalIcon.SYNC, tint = if (localRepeatMode > 0) dominantColor else Color.White.copy(0.4f), baseSize = 36.dp) { localRepeatMode = (localRepeatMode + 1) % 3; view.onCustomMediaAction?.invoke(action.action) }
                } ?: Spacer(Modifier.width(36.dp))
            }
        }
    }
}
