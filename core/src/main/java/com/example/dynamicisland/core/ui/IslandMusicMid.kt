package com.example.dynamicisland.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.clickable
import com.example.dynamicisland.core.ui.components.text.RollingNumberText
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.*

@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val view = this
    val dColor = music.dominantColor
    val dominantColor = if (dColor != null) Color(dColor) else Color.White
    val theme = LocalIslandTheme.current
    
    var artPressed by remember { mutableStateOf(false) }
    val artScale by animateFloatAsState(if(artPressed) 0.90f else 1f, spring(dampingRatio = 0.6f, stiffness = 400f), label="art")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Left: Album Art with Shadow ---
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(artScale)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF121212))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(); artPressed = true
                        waitForUpOrCancellation(); artPressed = false
                    }
                }
        ) {
            val art = music.albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // --- Middle: Scrolling Titles & Rolling Time ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RollingNumberText(
                    value = formatTime(view.currentMediaPos.longValue),
                    style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = " • ${music.artist}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // --- Right: Fluid Controls ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = { view.onPrevClick?.invoke() }, modifier = Modifier.size(28.dp)) {
                Icon(painterResource(R.drawable.ic_prev_vector), null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = { view.onPlayPauseClick?.invoke() },
                modifier = Modifier.size(36.dp).background(Color.White.copy(0.1f), CircleShape)
            ) {
                Icon(
                    painter = if (music.isPlaying) painterResource(R.drawable.ic_pause_vector) else painterResource(R.drawable.ic_play_vector),
                    null, tint = Color.White, modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = { view.onNextClick?.invoke() }, modifier = Modifier.size(28.dp)) {
                Icon(painterResource(R.drawable.ic_next_vector), null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
