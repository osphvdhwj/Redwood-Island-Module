package com.example.dynamicisland.ui

import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium ring composable.
 *
 * Preserved original features:
 * - Battery level ring with colour thresholds
 * - Media playback progress ring
 * - Notification pulse glow
 * - Breath animation
 *
 * New premium additions:
 * - BPM‑synchronised pulse (layered on breath)
 * - Weather mood colour override
 * - Soft inner radial glow
 * - Floating ambient particles
 */
@Composable
fun DynamicIslandView.RingUI(model: LiveActivityModel?) {
    val musicModel = model as? LiveActivityModel.Music
    val weatherModel = model as? LiveActivityModel.WeatherMood
    val isMedia = musicModel != null && musicModel.isPlaying
    val shouldShowRing = isMedia || globalIsCharging.value || true

    if (shouldShowRing) {
        // ── Progress calculation ──
        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
        val progress = if (isMedia) {
            (currentMediaPos.longValue.toFloat() / safeDur)
        } else {
            globalBatteryLevel.intValue / 100f
        }

        // ── Base colour logic ──
        val batteryLevel = globalBatteryLevel.intValue
        val baseColor = when {
            // Weather mood takes priority if model is present
            weatherModel != null -> weatherModel.color
            isMedia -> musicModel?.dominantColor?.let { Color(it) } ?: Color.White
            globalIsCharging.value -> Color(0xFF00FF00)
            else -> when {
                batteryLevel <= 5  -> Color(0xFFFF0000)
                batteryLevel <= 10 -> Color(0xFFFF3333)
                batteryLevel <= 40 -> Color(0xFFFFA500)
                batteryLevel <= 60 -> Color(0xFFFFFF00)
                else              -> Color(0xFF006400)
            }
        }

        val ringColor = when {
            hasUnseenNotif.value && !isMedia && !globalIsCharging.value ->
                Color(pendingNotifColor.intValue)
            else -> baseColor
        }

        val progressColor = baseColor

        // ── Notification pulse ──
        val notifPulse by rememberInfiniteTransition(label = "notif_pulse").animateFloat(
            initialValue  = if (hasUnseenNotif.value) 0.4f else 1f,
            targetValue   = 1f,
            animationSpec = if (hasUnseenNotif.value)
                infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
            else tween(0),
            label = "notif_alpha"
        )

        // ── Breath animation ──
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

        // ── 🆕 BPM pulse (layered on top of breath) ──
        val bpmPulseScale = remember { Animatable(1f) }
        val bpm = AudioReactiveAnalyzer.bpm.collectAsState().value

        LaunchedEffect(bpm, isMedia) {
            if (isMedia && bpm > 0f) {
                val intervalMs = (60_000L / bpm).toLong()
                while (true) {
                    bpmPulseScale.animateTo(1.06f, tween(intervalMs.toInt() / 2))
                    bpmPulseScale.animateTo(0.94f, tween(intervalMs.toInt() / 2))
                }
            } else {
                bpmPulseScale.snapTo(1f)
            }
        }

        // ── 🆕 Floating particles ──
        val particles = remember { List(8) { ParticleState() } }

        // ── Combined scale = breath × BPM ──
        val combinedScale = breathScale * bpmPulseScale.value

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = combinedScale
                    scaleY = combinedScale
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp)) {
                val strokeW = ringThickness.value.dp.toPx()
                val inset = strokeW / 2
                val arcSize = androidx.compose.ui.geometry.Size(
                    size.width - strokeW,
                    size.height - strokeW
                )
                val arcTopLeft = Offset(inset, inset)
                val progressPercent = progress.coerceIn(0f, 1f)
                val centerPx = Offset(size.width / 2, size.height / 2)
                val arcRadius = (size.width - strokeW) / 2

                // ── 🆕 Soft inner radial glow ──
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2 * 1.3f
                )

                // ── Subtle track ──
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(strokeW)
                )

                // ── Clean progress arc ──
                if (progressPercent > 0.01f) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progressPercent,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )

                    // ── Progress dot ──
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

                // ── 🆕 Floating ambient particles ──
                val time = System.currentTimeMillis() * 0.001f
                val particleOrbitRadius = arcRadius * 1.25f
                particles.forEach { p ->
                    val angle = (time * p.speed + p.offset).toFloat()
                    val orbitR = particleOrbitRadius + sin(angle * 2f) * 3f
                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
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

/**
 * Lightweight state for a single floating particle.
 */
private data class ParticleState(
    val offset: Float = (Math.random() * 360).toFloat(),
    val speed: Float = (Math.random() * 0.5f + 0.3f).toFloat(),
    val alpha: Float = (Math.random() * 0.5f + 0.3f).toFloat()
)