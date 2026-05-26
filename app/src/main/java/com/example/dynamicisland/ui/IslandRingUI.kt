package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas   // added
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicIslandView.RingUI(model: LiveActivityModel?) {
    val musicModel = model as? LiveActivityModel.Music
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = isMedia || globalIsCharging.value || true

    if (shouldShowRing) {
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f

        val notifPulse by rememberInfiniteTransition(label = "notif_pulse").animateFloat(
            initialValue  = if (hasUnseenNotif.value) 0.4f else 1f,
            targetValue   = 1f,
            animationSpec = if (hasUnseenNotif.value)
                infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            else
                infiniteRepeatable(tween(1, easing = LinearEasing), RepeatMode.Restart),
            label = "notif_alpha"
        )

        val infiniteTransition = rememberInfiniteTransition(label = "ring_breath")
        val breathScale by infiniteTransition.animateFloat(
            initialValue  = 0.97f,
            targetValue   = 1.0f,
            animationSpec = infiniteRepeatable(
                tween(3000, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "scale"
        )

        // 🚀 DEFERRED LAYOUT READS: We use graphicsLayer for scale so it doesn't trigger recomposition.
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
                // 🚀 DRAW PHASE READS: Calculate visual logic here to avoid CPU-heavy recompositions.
                val currentProgress = if (isMedia) {
                    (currentMediaPos.longValue.toFloat() / safeDur)
                } else {
                    globalBatteryLevel.intValue / 100f
                }

                val currentBatteryLevel = globalBatteryLevel.intValue
                val baseColor = when {
                    isMedia -> musicModel?.dominantColor?.let { Color(it) } ?: Color.White
                    globalIsCharging.value -> Color(0xFF00FF00)
                    else -> when {
                        currentBatteryLevel <= 5  -> Color(0xFFFF0000)
                        currentBatteryLevel <= 10 -> Color(0xFFFF3333)
                        currentBatteryLevel <= 40 -> Color(0xFFFFA500)
                        currentBatteryLevel <= 60 -> Color(0xFFFFFF00)
                        else              -> Color(0xFF006400)
                    }
                }

                val currentRingColor = if (hasUnseenNotif.value && !isMedia && !globalIsCharging.value) {
                    Color(pendingNotifColor.intValue)
                } else baseColor

                val strokeW = ringThickness.value.dp.toPx()
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(
                    size.width - strokeW,
                    size.height - strokeW
                )
                val arcTopLeft = Offset(inset, inset)
                val progressPercent = currentProgress.coerceIn(0f, 1f)
                val centerPx = Offset(size.width / 2, size.height / 2)
                val arcRadius = (size.width - strokeW) / 2

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            currentRingColor.copy(alpha = 0.12f * notifPulse),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2 * 1.3f
                )

                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(strokeW)
                )

                if (progressPercent > 0.01f) {
                    drawArc(
                        color = baseColor, // progressColor
                        startAngle = -90f,
                        sweepAngle = 360f * progressPercent,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    val angleRad = Math.toRadians((-90.0 + 360.0 * progressPercent))
                    drawCircle(
                        color = Color.White,
                        radius = (strokeW / 2) * 1.2f,
                        center = Offset(
                            centerPx.x + arcRadius * cos(angleRad).toFloat(),
                            centerPx.y + arcRadius * sin(angleRad).toFloat()
                        )
                    )
                }

                val time = System.currentTimeMillis() * 0.001f
                val particleOrbitRadius = arcRadius * 1.25f
                val particles = List(8) { index ->
                    Triple(
                        (index * 45f),   // offset
                        0.4f + (index % 3) * 0.1f, // speed
                        0.3f + (index % 5) * 0.1f  // alpha
                    )
                }
                particles.forEach { (offset, speed, alpha) ->
                    val angle = (time * speed + offset)
                    val orbitR = particleOrbitRadius + sin(angle * 2f) * 3f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 1.8f,
                        center = Offset(
                            centerPx.x + orbitR * cos(angle),
                            centerPx.y + orbitR * sin(angle)
                        )
                    )
                }
            }
        }
    }
}

private data class ParticleState(
    val offset: Float = (Math.random() * 360).toFloat(),
    val speed: Float = (Math.random() * 0.5f + 0.3f).toFloat(),
    val alpha: Float = (Math.random() * 0.5f + 0.3f).toFloat()
)