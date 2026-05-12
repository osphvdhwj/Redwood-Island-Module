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
import com.example.dynamicisland.manager.IslandMediaManager
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.performance.IslandShaderWaveform
import com.example.dynamicisland.settings.SettingsState
import androidx.compose.foundation.text.BasicMarquee

/**
 * MusicMid – Medium expanded music view.
 * Shows blurred album art, marquee title/artist, waveform seeker, and inline controls.
 */
@Composable
fun MusicMid(
    music: LiveActivityModel.Music,
    mediaManager: IslandMediaManager,
    settings: SettingsState
) {
    // Extract colors from album art if dynamic gradient is enabled
    var gradientColors by remember { mutableStateOf(listOf(Color.DarkGray, Color.Black)) }

    // Determine background brush
    val backgroundBrush = if (settings.dynamicGradient && gradientColors.size >= 2) {
        Brush.verticalGradient(gradientColors)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color.Black))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)   // compact mid size
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundBrush)
    ) {
        // Blurred album art as background
        if (music.albumArtUri != null) {
            AsyncImage(
                model = music.albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Foreground content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Track info with marquee
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small album art thumbnail
                AsyncImage(
                    model = music.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        val bitmap = state.result.image.bitmap
                        if (settings.dynamicGradient && bitmap != null) {
                            gradientColors = extractGradientColors(bitmap)
                        }
                    }
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Marquee
                    )
                    Text(
                        text = music.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Marquee
                    )
                }
            }

            // Waveform seeker using existing IslandShaderWaveform
            IslandShaderWaveform(
                progress = music.progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { mediaManager.skipToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White)
                }
                IconButton(onClick = { mediaManager.playPause() }) {
                    Icon(
                        if (music.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { mediaManager.skipToNext() }) {
                    Icon(Icons.Default.SkipNext, "Next", tint = Color.White)
                }
                if (mediaManager.isLiked()) {
                    IconButton(onClick = { mediaManager.toggleLike() }) {
                        Icon(Icons.Default.Favorite, "Like", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Helper to extract two prominent colors from album art bitmap.
 */
private fun extractGradientColors(bitmap: Bitmap): List<Color> {
    val palette = Palette.from(bitmap).generate()
    val colors = mutableListOf<Color>()
    palette.dominantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.vibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    if (colors.isEmpty()) {
        colors.add(Color.DarkGray)
        colors.add(Color.Black)
    }
    return colors.distinct().take(2)
}