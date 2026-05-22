package com.example.dynamicisland.ui

import com.example.dynamicisland.model.LocalIslandTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation

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
    val dominantColor = music.dominantColor?.let { Color(it) } ?: Color.White
    val theme = LocalIslandTheme.current
    
    var artPressed by remember { mutableStateOf(false) }
    val artScale by animateFloatAsState(if(artPressed) 0.90f else 1f, IslandPhysics.springFloat, label="art")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Left: Album Art with Shadow ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(artScale)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(); artPressed = true
                        waitForUpOrCancellation(); artPressed = false
                    }
                }
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

        Spacer(Modifier.width(14.dp))

        // --- Middle: Scrolling Titles ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.safeMarquee(islandState.value)
            )
            Text(
                text = music.artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.safeMarquee(islandState.value)
            )
        }

        Spacer(Modifier.width(10.dp))

        // --- Right: Fluid Controls ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_prev_vector),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp).squishClickable { onPrevClick?.invoke() }
            )

            Icon(
                painter = if (music.isPlaying) painterResource(R.drawable.ic_pause_vector) else painterResource(R.drawable.ic_play_vector),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp).squishClickable { onPlayPauseClick?.invoke() }
            )

            Icon(
                painter = painterResource(R.drawable.ic_next_vector),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp).squishClickable { onNextClick?.invoke() }
            )
        }
    }
}
