package com.example.dynamicisland.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.Image
import com.example.dynamicisland.core.ui.components.text.RollingNumberText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.shared.model.LiveActivityModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    val dynamicColor = Color(music.titleTextColor).takeIf { it != Color.Transparent && it != Color.Black } ?: Color(0xFF00FFCC)

    val rotationAnim = remember { Animatable(0f) }
    LaunchedEffect(music.isPlaying) {
        if (music.isPlaying) {
            rotationAnim.animateTo(
                targetValue = rotationAnim.value + 3600f,
                animationSpec = infiniteRepeatable(
                    tween(8000, easing = LinearEasing)
                )
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val progress = if (music.durationMs > 0) {
                    currentMediaPos.longValue.toFloat() / music.durationMs
                } else 0f

                drawArc(
                    color = dynamicColor.copy(alpha = 0.15f),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = dynamicColor.copy(alpha = 0.9f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            val art = music.albumArt
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .rotate(if (isCubeRotationEnabled.value) rotationAnim.value else 0f)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = music.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.weight(1f).safeMarquee(islandState.value)
        )

        Spacer(Modifier.width(8.dp))

        AnimatedContent(
            targetState = music.isPlaying,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            }, label = "playing_state"
        ) { playing ->
            if (playing) {
                PlayingBarsIndicator(color = dynamicColor)
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = dynamicColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun PlayingBarsIndicator(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label="bars")

    val bar1 by infiniteTransition.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label="b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(500, 150, FastOutSlowInEasing), RepeatMode.Reverse), label="b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        0.2f, 0.9f,
        infiniteRepeatable(tween(700, 75, FastOutSlowInEasing), RepeatMode.Reverse), label="b3"
    )

    Row(
        modifier = modifier.width(16.dp).height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(bar1, bar2, bar3).forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(color)
            )
        }
    }
}
