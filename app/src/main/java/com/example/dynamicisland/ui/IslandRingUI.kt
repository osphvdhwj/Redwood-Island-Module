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
fun DynamicIslandView.RingUI(model: LiveActivityModel?) {
    val musicModel = model as? LiveActivityModel.Music
    val weatherModel = model as? LiveActivityModel.WeatherMood
    val settings = controller?.settingsState ?: com.example.dynamicisland.settings.SettingsState()
    
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = isMedia || globalIsCharging.value || weatherModel != null || true

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f

        val pulseStyle = settings.ringPulseStyle
        
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
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
                    val breath by infiniteTransition.animateFloat(
                        initialValue = ringThickness.value.dp.toPx() * 0.8f,
                        targetValue = ringThickness.value.dp.toPx() * 1.2f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "breath"
                    )
                    breath
                } else ringThickness.value.dp.toPx()

                if (settings.navIslandMode) {
                    // 🌓 NAV ISLAND PILLAR: Hooked to Android Nav Pill
                    val fillProgress = currentProgress.coerceIn(0f, 1f)
                    
                    val barHeight = if (settings.isNavIslandFloating) 8.dp.toPx() else 4.dp.toPx()
                    val barWidth = if (settings.isNavIslandFloating) size.width else (size.width * 0.35f).coerceAtLeast(64.dp.toPx())
                    val cornerR = barHeight / 2
                    
                    val xOffset = (size.width - barWidth) / 2
                    val yOffset = size.height - barHeight - (if (settings.isNavIslandFloating) 0f else 8.dp.toPx())

                    // Background Bar (Ethereal Glass)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset(xOffset, yOffset),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR)
                    )
                    
                    // Outer Glow (for low or charging)
                    if (currentBatteryLevel <= 20 || globalIsCharging.value) {
                         drawRoundRect(
                             color = baseColor.copy(alpha = 0.3f),
                             topLeft = Offset(xOffset - 2.dp.toPx(), yOffset - 2.dp.toPx()),
                             size = androidx.compose.ui.geometry.Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                             cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR + 2.dp.toPx(), cornerR + 2.dp.toPx())
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
                } else {
                    // Legacy Ring Mode
                    val inset = strokeW / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                    val arcTopLeft = Offset(inset, inset)
                    val progressPercent = currentProgress.coerceIn(0f, 1f)

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
