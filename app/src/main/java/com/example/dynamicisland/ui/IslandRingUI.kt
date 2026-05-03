package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

@Composable
fun DynamicIslandView.RingUI(model: LiveActivityModel?) {
    val musicModel = model as? LiveActivityModel.Music
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = isMedia || globalIsCharging.value || true

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
        val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }
        
        val batteryLevel = globalBatteryLevel.intValue
        val baseColor = if (isMedia) {
            musicModel?.dominantColor?.let { Color(it) } ?: Color.White
        } else if (globalIsCharging.value) {
            Color(0xFF00FF00) 
        } else {
             when {
                batteryLevel <= 5 -> Color(0xFFFF0000) 
                batteryLevel <= 10 -> Color(0xFFFF3333) 
                batteryLevel <= 40 -> Color(0xFFFFA500) 
                batteryLevel <= 60 -> Color(0xFFFFFF00) 
                else -> Color(0xFF006400) 
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "ring_breath")
        val breathScale by infiniteTransition.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                tween(3000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "scale"
        )
         val progressColor = baseColor

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
                val strokeW = ringThickness.value.dp.toPx() 
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                val progressPercent = progress.coerceIn(0f, 1f)

                // Subtle track
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f, sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft, size = arcSize,
                    style = Stroke(strokeW)
                )

                // Clean single color progress, no sweep gradient
                if (progressPercent > 0.01f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progressPercent,
                        useCenter = false,
                        topLeft = arcTopLeft, size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    // Single dot at progress end - cleaner than tick marks
                    val angleRad = Math.toRadians((-90.0 + 360.0 * progressPercent))
                    val radius = (size.width - strokeW) / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color.White,
                        radius = (strokeW / 2) * 1.2f,
                        center = androidx.compose.ui.geometry.Offset(
                            center.x + radius * kotlin.math.cos(angleRad).toFloat(),
                            center.y + radius * kotlin.math.sin(angleRad).toFloat()
                        )
                    )
                }
            }
        }
    }
}
