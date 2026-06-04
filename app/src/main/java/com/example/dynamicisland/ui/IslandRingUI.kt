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
import com.example.dynamicisland.settings.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * ELITE RING RENDERER
 * Supports multiple visual dialects (Standard, Circular, Sam-style, Futuristic).
 */
@Composable
fun DynamicIslandView.RingUI(isPulsing: Boolean) {
    val model = activeModel.value
    val musicModel = model as? LiveActivityModel.Music
    val weatherModel = model as? LiveActivityModel.WeatherMood
    val settings = controller?.settingsState ?: SettingsState()
    val pack = settings.iconPack
    
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = (isMedia || globalIsCharging.value || weatherModel != null || settings.showRingIdle) && settings.islandEnabled

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
        val pulseStyle = settings.ringPulseStyle
        
        // --- Dialect-Aware Physics ---
        val pulseScale by animateFloatAsState(
            targetValue = if (isPulsing) 1.15f else 1.0f,
            animationSpec = if (pack is IconPack.iOS) spring(dampingRatio = 0.4f) else tween(200),
            label = "pulse_scale"
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
                    else -> when {
                        currentBatteryLevel <= 10 -> Color(0xFFFF3B30)
                        currentBatteryLevel <= 25 -> Color(0xFFFF9500)
                        else -> Color(0xFF32D74B)
                    }
                }

                // --- Dialect Rendering Logic ---
                when (pack) {
                    is IconPack.Futuristic -> {
                        // Cyber-HUD Hexagonal segments
                        val segments = 12
                        val sweep = 360f / segments
                        for (i in 0 until segments) {
                            val active = (currentProgress * segments).toInt() > i
                            drawArc(
                                color = if (active) baseColor else baseColor.copy(alpha = 0.1f),
                                startAngle = i * sweep - 90f + 2f,
                                sweepAngle = sweep - 4f,
                                useCenter = false,
                                style = Stroke(4.dp.toPx(), cap = StrokeCap.Butt)
                            )
                        }
                    }
                    is IconPack.Pixel -> {
                        // Perfect Circular dots
                        val dots = 8
                        for (i in 0 until dots) {
                            val angle = (i * (360f / dots)) - 90f
                            val rad = Math.toRadians(angle.toDouble())
                            val x = size.width / 2 + (size.width / 2) * Math.cos(rad).toFloat()
                            val y = size.height / 2 + (size.height / 2) * Math.sin(rad).toFloat()
                            val active = (currentProgress * dots).toInt() > i
                            drawCircle(
                                color = if (active) baseColor else baseColor.copy(alpha = 0.15f),
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                    else -> {
                        // Standard Fluid Arc
                        val strokeW = ringThickness.value.dp.toPx()
                        drawArc(
                            color = baseColor.copy(alpha = 0.1f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(strokeW)
                        )
                        drawArc(
                            color = baseColor,
                            startAngle = -90f, sweepAngle = 360f * currentProgress.coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(strokeW, cap = if (pack is IconPack.Outline) StrokeCap.Butt else StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
