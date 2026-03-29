package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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

        val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
             initialValue = if (globalIsCharging.value && !isMedia) 0.3f else 1f,
            targetValue = 1f,
             animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "alpha"
        )
         val progressColor = baseColor.copy(alpha = pulseAlpha)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Remove `.align(Alignment.Center)` from the Canvas modifier
            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp)) {
                val strokeW = ringThickness.value.dp.toPx() 
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                val progressPercent = progress.coerceIn(0f, 1f)

                val sweepGradient = Brush.sweepGradient(0.0f to progressColor.copy(alpha = 0.4f), 0.8f to progressColor, 1.0f to progressColor.copy(alpha = 0.4f))

                drawArc(color = baseColor.copy(alpha=0.20f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))
                drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = 360f * progressPercent, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Round), alpha = 0.95f)

                val markerLength = strokeW * 1.3f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val radius = (size.width - strokeW) / 2
            
                drawLine(color = Color.White, start = androidx.compose.ui.geometry.Offset(center.x, center.y - radius - markerLength/2), end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius + markerLength/2), strokeWidth = 4f)
             
                val angleRad = Math.toRadians((-90f + 360f * progressPercent).toDouble())
                val mStartX = center.x + (radius - markerLength/2) * Math.cos(angleRad).toFloat()
                val mStartY = center.y + (radius - markerLength/2) * Math.sin(angleRad).toFloat()
                val mEndX = center.x + (radius + markerLength/2) * Math.cos(angleRad).toFloat()
                val mEndY = center.y + (radius + markerLength/2) * Math.sin(angleRad).toFloat()
                drawLine(color = Color.White, start = androidx.compose.ui.geometry.Offset(mStartX, mStartY), end = androidx.compose.ui.geometry.Offset(mEndX, mEndY), strokeWidth = 4f)
            }
        }
    }
}
