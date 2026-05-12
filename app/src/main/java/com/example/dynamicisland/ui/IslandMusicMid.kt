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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Mid-state music card.
 *
 * Layout (horizontal, fills the TYPE_2_MID pill):
 *
 *   [Album art 52dp] | Title / Artist (scrolling) | ◀  ▶  ▶|  like
 *                    | Seeker bar (full width)     |
 *
 * The seeker uses InteractiveWavyMediaBar so it is reactive to playback.
 * Album art has a soft drop-shadow and rounded corners.
 * Background tints toward the dominant album colour at 20% opacity.
 */
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val haptic = LocalHapticFeedback.current

    // Hard-coded dimensions (previously from LocalIslandTheme)
    val alertTitleSize = 14.sp
    val alertMsgSize = 12.sp
    val musicSeekerThick = 4.dp   // base thickness, multiplied later

    // Resolve text / accent colour from album art palette
    val accentColor = music.dominantColor?.let { Color(it) } ?: Color.White
    val textColor   = Color(music.titleTextColor)
        .takeIf { it != Color.Transparent && it != Color.Black } ?: Color.White

    // Local liked state (mirrors remote until next metadata update)
    var localIsLiked by remember(music.title, music.isLiked) { mutableStateOf(music.isLiked) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Coloured background wash ──────────────────────────────────────
        if (music.dominantColor != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(music.dominantColor).copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Album art ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .shadow(8.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.DarkGray)
            ) {
                if (music.albumArt != null) {
                    Image(
                        bitmap          = music.albumArt.asImageBitmap(),
                        contentDescription = null,
                        contentScale    = ContentScale.Crop,
                        modifier        = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // ── Centre column: title / artist + seeker ────────────────────
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text       = music.title,
                    color      = textColor,
                    fontSize   = alertTitleSize,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.safeMarquee(islandState.value)
                )
                // Artist
                Text(
                    text     = music.artist,
                    color    = textColor.copy(alpha = 0.65f),
                    fontSize = alertMsgSize,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.safeMarquee(islandState.value)
                )
                // Seeker (3x base thickness)
                InteractiveWavyMediaBar(
                    durationMs  = music.durationMs,
                    posProvider = { currentMediaPos.longValue },
                    isPlaying   = music.isPlaying,
                    color       = accentColor,
                    trackColor  = accentColor.copy(alpha = 0.20f),
                    onSeek      = { onSeekTo?.invoke(it) },
                    modifier    = Modifier.height(musicSeekerThick * 3)
                )
            }

            Spacer(Modifier.width(8.dp))

            // ── Right controls ─────────────────────────────────────────────
            Row(
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Previous
                SmallMediaBtn(
                    icon    = Icons.Default.SkipPrevious,
                    tint    = textColor.copy(alpha = 0.75f),
                    size    = 28.dp,
                    haptic  = haptic
                ) { onPrevClick?.invoke() }

                // Play / Pause
                AnimatedContent(
                    targetState  = music.isPlaying,
                    transitionSpec = {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    },
                    label = "pp_mid"
                ) { playing ->
                    SmallMediaBtn(
                        icon   = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        tint   = textColor,
                        size   = 34.dp,
                        haptic = haptic
                    ) { onPlayPauseClick?.invoke() }
                }

                // Next
                SmallMediaBtn(
                    icon    = Icons.Default.SkipNext,
                    tint    = textColor.copy(alpha = 0.75f),
                    size    = 28.dp,
                    haptic  = haptic
                ) { onNextClick?.invoke() }

                // Like (only if the session exposes a like action)
                val hasLike = music.customActions.any {
                    it.actionName.contains("heart", true) || it.actionName.contains("like", true)
                }
                if (hasLike) {
                    Spacer(Modifier.width(2.dp))
                    val likeScale by animateFloatAsState(
                        targetValue   = if (localIsLiked) 1.25f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
                        label         = "like_scale"
                    )
                    Icon(
                        imageVector = if (localIsLiked) Icons.Default.Favorite
                                      else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint        = if (localIsLiked) Color(0xFFFF2A5F)
                                      else textColor.copy(alpha = 0.60f),
                        modifier    = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                localIsLiked = !localIsLiked
                                music.customActions
                                    .firstOrNull {
                                        it.actionName.contains("heart", true) ||
                                        it.actionName.contains("like", true)
                                    }
                                    ?.let { onCustomMediaAction?.invoke(it.actionName) }
                            }
                    )
                }
            }
        }
    }
}

// ── Tiny reusable icon button used only inside MusicMid ───────────────────

@Composable
private fun SmallMediaBtn(
    icon:   androidx.compose.ui.graphics.vector.ImageVector,
    tint:   Color,
    size:   androidx.compose.ui.unit.Dp,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(
        targetValue   = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label         = "btn_scale"
    )
    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(120); pressed = false }
    }
    Icon(
        imageVector        = icon,
        contentDescription = null,
        tint               = tint,
        modifier           = Modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pressed = true
                onClick()
            }
    )
}