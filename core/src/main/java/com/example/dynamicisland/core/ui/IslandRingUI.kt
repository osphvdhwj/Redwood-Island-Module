package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import com.example.dynamicisland.shared.settings.SettingsState
import com.example.dynamicisland.shared.model.LiveActivityModel

/**
 * 🚀 ELITE RING RENDERER
 * 
 * Optimized for locked 120FPS. Uses deferred state reads and layer-based scaling.
 */
@Composable
fun DynamicIslandView.RingUI(isPulsing: Boolean) {
    val model = activeModel.value
    val musicModel = model as? LiveActivityModel.Music
    val weatherModel = model as? LiveActivityModel.WeatherMood
    val settings = controller?.settingsState ?: com.example.dynamicisland.shared.settings.SettingsState()
    val pack = settings.iconPack
    
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = (isMedia || globalIsCharging.value || weatherModel != null || settings.showRingIdle) && settings.islandEnabled

    if (shouldShowRing) {
        val pulseStyle = settings.ringPulseStyle
        
        // 🛡️ Optimization 1: Use spring for high-fidelity physical interaction
        val pulseScale by animateFloatAsState(
            targetValue = if (isPulsing) 1.15f else 1.0f,
            animationSpec = if (pack is com.example.dynamicisland.shared.settings.IconPack.iOS) spring(dampingRatio = 0.4f) else tween(200),
            label = "pulse_scale"
        )
        
        // 🛡️ Optimization 2: Infinite breathing on the UI thread layer
        val breathScale by rememberInfiniteTransition(label = "ring_pulse").animateFloat(
            initialValue  = if (pulseStyle == com.example.dynamicisland.shared.settings.RingPulseStyle.BREATH) 0.96f else 1.0f,
            targetValue   = 1.0f,
            animationSpec = if (pulseStyle == com.example.dynamicisland.shared.settings.RingPulseStyle.BREATH) 
                infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            else 
                infiniteRepeatable(tween(1000)),
            label = "scale"
        )

        // 🛡️ Optimization 3: Compute current metrics using derivedStateOf to prevent redundant layout
        val progressMetrics by remember(isMedia, musicModel, globalBatteryLevel.intValue, globalIsCharging.value) {
            derivedStateOf {
                val currentProgress = if (isMedia && musicModel != null && musicModel.durationMs > 0) {
                    (currentMediaPos.longValue.toFloat() / musicModel.durationMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    globalBatteryLevel.intValue / 100f
                }

                val batteryLevel = globalBatteryLevel.intValue
                val color = when {
                    isMedia -> musicModel?.dominantColor?.let { Color(it) } ?: Color.White
                    globalIsCharging.value -> Color(0xFF00FF00)
                    else -> when {
                        batteryLevel <= 10 -> Color(0xFFFF3B30)
                        batteryLevel <= 25 -> Color(0xFFFF9500)
                        else -> Color(0xFF32D74B)
                    }
                }
                Pair(currentProgress, color)
            }
        }

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
                val (currentProgress, baseColor) = progressMetrics
                
                when (pack) {
                    com.example.dynamicisland.shared.settings.IconPack.Futuristic -> {
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
                    com.example.dynamicisland.shared.settings.IconPack.Pixel -> {
                        val dots = 8
                        for (i in 0 until dots) {
                            val angle = (i * (360f / dots)) - 90f
                            val rad = Math.toRadians(angle.toDouble())
                            val x = size.width / 2 + (size.width / 2) * cos(rad).toFloat()
                            val y = size.height / 2 + (size.height / 2) * sin(rad).toFloat()
                            val active = (currentProgress * dots).toInt() > i
                            drawCircle(
                                color = if (active) baseColor else baseColor.copy(alpha = 0.15f),
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                    else -> {
                        val strokeW = ringThickness.value.dp.toPx()
                        drawArc(
                            color = baseColor.copy(alpha = 0.1f),
                            startAngle = 0f, sweepAngle = 360f, useCenter = false,
                            style = Stroke(strokeW)
                        )
                        drawArc(
                            color = baseColor,
                            startAngle = -90f, sweepAngle = 360f * currentProgress,
                            useCenter = false,
                            style = Stroke(strokeW, cap = if (pack == com.example.dynamicisland.shared.settings.IconPack.Outline) StrokeCap.Butt else StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
