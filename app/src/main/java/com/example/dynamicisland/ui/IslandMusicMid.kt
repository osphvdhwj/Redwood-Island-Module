package com.example.dynamicisland.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.R
import com.example.dynamicisland.model.LiveActivityModel

@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val haptic = LocalHapticFeedback.current
    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    
    // Smooth transition between playing/paused states
    val playing = music.isPlaying
    val animatedProgress by animateFloatAsState(
        targetValue = if (playing) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
        label = "playingProgress"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Album Art with shadow and circular clip
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(animatedProgress)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF222222))
        ) {
            if (music.albumArt != null) {
                Image(
                    bitmap = music.albumArt.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Middle: Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = music.artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Right Side: Minimal Controls
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Previous
            Icon(
                painter = painterResource(R.drawable.ic_prev_vector),
                contentDescription = "Prev",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp).clickable { onPrevClick?.invoke() }
            )

            // Play/Pause
            Icon(
                painter = if (playing) painterResource(R.drawable.ic_pause_vector) else painterResource(R.drawable.ic_play_vector),
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onPlayPauseClick?.invoke() }
            )

            // Next
            Icon(
                painter = painterResource(R.drawable.ic_next_vector),
                contentDescription = "Next",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp).clickable { onNextClick?.invoke() }
            )

            // Like/Heart
            Icon(
                imageVector = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Like",
                tint = if (localIsLiked) Color(0xFFFF2A5F) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        localIsLiked = !localIsLiked
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        music.customActions.find { it.action.contains("like", true) || it.action.contains("heart", true) }?.let {
                            onCustomMediaAction?.invoke(it.action)
                        }
                    }
            )
        }
    }
}
