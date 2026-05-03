package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.manager.*

import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            
            if (music.albumArt != null) {
                Image(
                    bitmap = music.albumArt.asImageBitmap(),
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

            if (music.albumArt != null) {
                Image(
                    bitmap = music.albumArt.asImageBitmap(),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val theme = LocalIslandTheme.current
    val dynamicTextColor = Color(music.titleTextColor)
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "spin")
    val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }
    var localIsShuffled by remember(music.title, music.isShuffled) { mutableStateOf(music.isShuffled) }
    var localRepeatMode by remember(music.title, music.repeatMode) { mutableIntStateOf(music.repeatMode) }

    Row(modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            IsolatedCircularProgress(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, color = dynamicTextColor)
            if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(50.dp).clip(CircleShape).rotate(currentRotation))
            else Box(Modifier.size(50.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(text = music.title, color = dynamicTextColor, fontSize = theme.musicTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = theme.musicArtistSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            Spacer(Modifier.height(theme.elementGap))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = formatTime(currentMediaPos.longValue), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                InteractiveWavyMediaBar(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, isPlaying = music.isPlaying, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha=0.2f), onSeek = { onSeekTo?.invoke(it) }, modifier = Modifier.weight(1f).height(theme.musicSeekerThick * 3))
                Spacer(Modifier.width(6.dp))
                Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 10.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, tint = dynamicTextColor, baseSize = 26.dp) { onPrevClick?.invoke() }
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                InteractiveIconButton(icon = playIcon, tint = dynamicTextColor, baseSize = 34.dp, bgAlpha = 0.15f) { onPlayPauseClick?.invoke() }
                InteractiveIconButton(icon = Icons.AutoMirrored.Filled.ArrowForward, tint = dynamicTextColor, baseSize = 26.dp) { onNextClick?.invoke() }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                music.customActions.find { it.actionName.contains("heart", true) || it.actionName.contains("like", true) }?.let { action ->
                    InteractiveIconButton(icon = if (localIsLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, tint = if (localIsLiked) Color(0xFFFF2A5F) else dynamicTextColor.copy(alpha=0.8f), baseSize = 22.dp) { localIsLiked = !localIsLiked; onCustomMediaAction?.invoke(action.actionName) }
                }
                music.customActions.find { it.actionName.contains("repeat", true) || it.actionName.contains("shuffle", true) }?.let { action ->
                    val isShuffle = action.actionName.contains("shuffle", true)
                    InteractiveIconButton(icon = if(isShuffle) Icons.Default.Shuffle else (if(localRepeatMode==1) Icons.Default.RepeatOne else Icons.Default.Repeat), tint = if(localIsShuffled || localRepeatMode > 0) Color(0xFF00FFCC) else dynamicTextColor.copy(alpha=0.8f), baseSize = 22.dp) { if(isShuffle) localIsShuffled=!localIsShuffled else localRepeatMode = (localRepeatMode+1)%3; onCustomMediaAction?.invoke(action.actionName) }
                }
            }
        }
    }
}
