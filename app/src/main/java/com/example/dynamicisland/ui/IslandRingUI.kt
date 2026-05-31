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
                        currentBatteryLevel <= 10 -> Color(0xFFFF3B30)
                        currentBatteryLevel <= 30 -> Color(0xFFFF9500)
                        else -> Color(0xFF34C759)
                    }
                }

                val strokeW = ringThickness.value.dp.toPx()
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                val arcTopLeft = Offset(inset, inset)
                val progressPercent = currentProgress.coerceIn(0f, 1f)

                // Background Ring
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 0f,
                    sweepAngle = 360f,
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
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(strokeW * 1.5f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
