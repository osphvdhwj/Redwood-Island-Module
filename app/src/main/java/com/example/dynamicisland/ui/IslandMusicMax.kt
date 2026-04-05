package com.example.dynamicisland.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET }
            if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        } catch (e: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (music.appIcon != null) Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))) else Box(Modifier.size(36.dp).background(Color.White.copy(0.2f), RoundedCornerShape(10.dp)))
            Surface(onClick = { onAudioOutputClick?.invoke() }, color = Color.White.copy(alpha=0.15f), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.weight(0.5f))
        Text(text = music.title, color = dynamicTextColor, fontSize = theme.musicTitleSize * 1.25f, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
        Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = theme.musicArtistSize * 1.15f, maxLines = 1, modifier = Modifier.fillMaxWidth().safeMarquee(islandState.value))
        Spacer(modifier = Modifier.weight(0.5f))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatTime(currentMediaPos.longValue), color = Color.White.copy(0.7f), fontSize = 12.sp)
            Text(text = formatTime(music.durationMs), color = Color.White.copy(0.7f), fontSize = 12.sp)
        }
        InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, isPlaying = music.isPlaying, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.padding(vertical = theme.elementGap).height(theme.musicSeekerThick * 3))
        
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(theme.buttonSpacing, Alignment.CenterHorizontally)) {
            music.customActions.find { it.actionName.contains("heart", true) }?.let { action ->
                InteractiveIconButton(icon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, tint = if (localIsLiked) Color(0xFFFF2A5F) else Color.White.copy(0.8f), baseSize = theme.buttonSize) { localIsLiked = !localIsLiked; onCustomMediaAction?.invoke(action.actionName) }
            }
            InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = Color.White, baseSize = theme.buttonSize) { onPrevClick?.invoke() }
            val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
            InteractiveIconButton(icon = playIcon, tint = Color.White, baseSize = theme.buttonSize, bgAlpha = 0.2f) { onPlayPauseClick?.invoke() }
            InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = Color.White, baseSize = theme.buttonSize) { onNextClick?.invoke() }
        }
    }
}
