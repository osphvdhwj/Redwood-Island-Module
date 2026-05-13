// File: app/src/main/java/com/example/dynamicisland/ui/MaxView.kt
package com.example.dynamicisland.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.palette.graphics.Palette
import com.example.dynamicisland.manager.IslandController
import com.example.dynamicisland.manager.IslandMediaManager
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.performance.IslandShaderWaveform
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.ui.extractGradientColors

@Composable
fun MaxView(model: LiveActivityModel?, controller: IslandController) {
    // Note: IslandController must have a public `settingsState` property for this to compile.
    // If not already added, insert this in IslandController:
    //     val settingsState = SettingsState()
    val settings = controller.settingsState ?: SettingsState()
    val shape = RoundedCornerShape(settings.pillCornerRadius.dp)

    when (model) {
        is LiveActivityModel.Music -> MusicMax(model, controller.mediaManager, settings)
        is LiveActivityModel.LiveActivity -> LiveActivityMax(model)
        else -> GenericMax(model)
    }
}

@Composable
private fun MusicMax(
    music: LiveActivityModel.Music,
    mediaManager: IslandMediaManager,
    settings: SettingsState
) {
    var gradientColors by remember { mutableStateOf(listOf(Color.DarkGray, Color.Black)) }

    val backgroundBrush = if (settings.dynamicGradient && gradientColors.size >= 2) {
        Brush.verticalGradient(gradientColors)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color.Black))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
    ) {
        if (music.albumArtUri != null) {
            AsyncImage(
                model = music.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Now Playing",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = music.albumArtUri,
                    contentDescription = "Album art",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (settings.dynamicGradient && bitmap != null) {
                            gradientColors = extractGradientColors(bitmap)
                        }
                    }
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        music.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        music.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ✅ Fixed waveform call with correct parameters
            IslandShaderWaveform(
                durationMs = music.durationMs,
                posProvider = { music.positionMs },
                isPlaying = music.isPlaying,
                color = Color.White,
                trackColor = Color.Gray,
                onSeek = { fraction ->
                    val seekPos = (fraction * music.durationMs).toLong()
                    mediaManager.activeMediaController?.transportControls?.seekTo(seekPos)
                }
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
            Spacer(Modifier.height(8.dp))

            // ✅ Media buttons now use sendMediaCommand
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mediaManager.sendMediaCommand("PREV") }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White)
                }
                IconButton(onClick = {
                    mediaManager.sendMediaCommand(if (music.isPlaying) "PAUSE" else "PLAY")
                }) {
                    Icon(
                        if (music.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { mediaManager.sendMediaCommand("NEXT") }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LiveActivityMax(activity: LiveActivityModel.LiveActivity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(16.dp)
    ) {
        Column {
            Text(activity.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(activity.subtitle, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            activity.progress?.let {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = it,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun GenericMax(model: LiveActivityModel?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray.copy(alpha = 0.8f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(model?.toString() ?: "No content", color = Color.White)
    }
}
1