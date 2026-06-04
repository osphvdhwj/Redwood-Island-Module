package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.RingPulseStyle
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicIslandView.RingUI(isPulsing: Boolean) {
    val model = activeModel.value
    val musicModel = model as? LiveActivityModel.Music
    val weatherModel = model as? LiveActivityModel.WeatherMood
    val settings = controller?.settingsState ?: com.example.dynamicisland.settings.SettingsState()
    
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = (isMedia || globalIsCharging.value || weatherModel != null || settings.showRingIdle) && settings.islandEnabled

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f

        val pulseStyle = settings.ringPulseStyle
        
        // --- 💓 SYNERGY PULSE ANIMATION ---
        val pulseScale by animateFloatAsState(
            targetValue = if (isPulsing) 1.15f else 1.0f,
            animationSpec = tween(200),
            label = "pulse_scale"
        )
        
        val pulseAlpha by animateFloatAsState(
            targetValue = if (isPulsing) 1.0f else 0.0f,
            animationSpec = tween(500),
            label = "pulse_alpha"
        )

        val breathScale by rememberInfiniteTransition(label = "ring_pulse").animateFloat(
            initialValue  = if (pulseStyle == RingPulseStyle.BREATH) 0.96f else 1.0f,
            targetValue   = 1.0f,
            animationSpec = if (pulseStyle == RingPulseStyle.BREATH) 
                infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            else 
                infiniteRepeatable(tween(1000), RepeatMode.Restart),
            label = "scale"
        )

        val laserRotation by rememberInfiniteTransition(label = "laser").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
            label = "rotation"
        )
        
        val breathAnimation = rememberInfiniteTransition(label = "breath_anim")
        val animatedBreath by breathAnimation.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val baseScale = if (isPulsing) pulseScale else breathScale
                    scaleX = baseScale
                    scaleY = baseScale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp)) {
                val currentProgress = if (isMedia) (currentMediaPos.longValue.toFloat() / safeDur) else globalBatteryLevel.intValue / 100f
                val currentBatteryLevel = globalBatteryLevel.intValue
                
                val baseColor = when {
                    isMedia -> musicModel?.dominantColor?.let { Color(it) } ?: Color.White
                    globalIsCharging.value -> Color(0xFF00FF00)
                    weatherModel != null -> Color(weatherModel.color)
                    else -> when {
                        currentBatteryLevel <= 10 -> Color(0xFFFF3B30) // Apple Critical Red
                        currentBatteryLevel <= 25 -> Color(0xFFFF9500) // Apple Low Orange
                        currentBatteryLevel <= 45 -> Color(0xFFFFD60A) // Apple Caution Yellow
                        currentBatteryLevel <= 75 -> Color(0xFF32D74B) // Apple Mid Green
                        else -> Color(0xFF30D158) // Apple Full Green
                    }
                }

                val strokeW = if (pulseStyle == RingPulseStyle.BREATH) {
                    ringThickness.value.dp.toPx() * animatedBreath
                } else ringThickness.value.dp.toPx()

                if (settings.navIslandMode) {
                    // 🌓 NAV ISLAND PILLAR: Hooked to Android Nav Pill
                    val isNavMusic = musicModel != null && settings.navIslandMusicBarMorph
                    val fillProgress = currentProgress.coerceIn(0f, 1f)
                    
                    val targetBarHeight = if (isNavMusic) 64.dp.toPx() else (if (settings.isNavIslandFloating) 8.dp.toPx() else 4.dp.toPx())
                    val targetBarWidth = if (isNavMusic) size.width - 32.dp.toPx() else (if (settings.isNavIslandFloating) size.width else (size.width * 0.35f).coerceAtLeast(64.dp.toPx()))
                    
                    val barHeight = targetBarHeight
                    val barWidth = targetBarWidth
                    val cornerR = if (isNavMusic) 24.dp.toPx() else barHeight / 2
                    
                    val xOffset = (size.width - barWidth) / 2
                    val yOffset = size.height - barHeight - (if (isNavMusic) 12.dp.toPx() else (if (settings.isNavIslandFloating) 0f else 8.dp.toPx()))

                    // Background Bar (Dull Color for Visibility)
                    val bgAlpha = if (settings.navIslandDullBackground) 0.3f else 0.15f
                    drawRoundRect(
                        color = Color.White.copy(alpha = bgAlpha),
                        topLeft = Offset(xOffset, yOffset),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR)
                    )
                    
                    // Synergy Pulse Glow
                    if (isPulsing) {
                        drawRoundRect(
                            color = baseColor.copy(alpha = pulseAlpha * 0.4f),
                            topLeft = Offset(xOffset - 4.dp.toPx(), yOffset - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(barWidth + 8.dp.toPx(), barHeight + 8.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR + 4.dp.toPx(), cornerR + 4.dp.toPx())
                        )
                    }

                    // Foreground Battery Fill (Liquid Gradient)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(baseColor, baseColor.copy(alpha = 0.6f)),
                            startX = xOffset,
                            endX = xOffset + barWidth
                        ),
                        topLeft = Offset(xOffset, yOffset),
                        size = androidx.compose.ui.geometry.Size(barWidth * fillProgress, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR)
                    )

                    // 🏷️ PIPE INDICATOR (|)
                    if (settings.navIslandShowPipeIndicator) {
                        val pipeX = xOffset + (barWidth * fillProgress)
                        drawLine(
                            color = Color.White,
                            start = Offset(pipeX, yOffset - 2.dp.toPx()),
                            end = Offset(pipeX, yOffset + barHeight + 2.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else if (settings.redwoodEnabled) {
                    // Redwood Ring Mode
                    val inset = strokeW / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                    val arcTopLeft = Offset(inset, inset)
                    val progressPercent = currentProgress.coerceIn(0f, 1f)

                    // Synergy Pulse Glow for Ring
                    if (isPulsing) {
                        drawArc(
                            color = baseColor.copy(alpha = pulseAlpha * 0.5f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(arcTopLeft.x - 4.dp.toPx(), arcTopLeft.y - 4.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(arcSize.width + 8.dp.toPx(), arcSize.height + 8.dp.toPx()),
                            style = Stroke(strokeW + 4.dp.toPx())
                        )
                    }

                    // Background Ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.1f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(strokeW)
                    )

                    // Progress Arc
                    if (progressPercent > 0.01f) {
                        drawArc(
                            color = baseColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progressPercent,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }

                    // Laser Style Border
                    if (pulseStyle == RingPulseStyle.LASER) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(baseColor, Color.Transparent, baseColor),
                                center = center
                            ),
                            startAngle = laserRotation,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = arcTopLeft,
                            size = arcSize,
                            style = Stroke(strokeW * 1.5f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
