package com.example.dynamicisland.ui
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

                music.customActions.find { it.action.contains("heart", true) || it.action.contains("like", true) }?.let { action ->
                    InteractiveIconButton(icon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, tint = if (localIsLiked) Color(0xFFFF2A5F) else Color.White.copy(0.8f), baseSize = 36.dp) { localIsLiked = !localIsLiked; onCustomMediaAction?.invoke(action.action) }
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
                music.customActions.find { it.action.contains("shuffle", true) }?.let { action ->
                    InteractiveIconButton(icon = Icons.Default.Shuffle, tint = if (localIsShuffled) dynamicTextColor else Color.White.copy(0.5f), baseSize = 32.dp) { localIsShuffled = !localIsShuffled; onCustomMediaAction?.invoke(action.action) }
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

                music.customActions.find { it.action.contains("repeat", true) }?.let { action ->
                    InteractiveIconButton(icon = if (localRepeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat, tint = if (localRepeatMode > 0) dynamicTextColor else Color.White.copy(0.5f), baseSize = 32.dp) { localRepeatMode = (localRepeatMode + 1) % 3; onCustomMediaAction?.invoke(action.action) }
                } ?: Spacer(Modifier.width(32.dp))
            }
        }
    }
}
