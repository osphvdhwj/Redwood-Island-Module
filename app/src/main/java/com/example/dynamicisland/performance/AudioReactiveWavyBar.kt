package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.dynamicisland.performance.AudioReactiveAnalyzer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/**
 * BATCH 2: Audio-Reactive Waveform Bar
 *
 * Replaces the sin-function simulation in InteractiveWavyMediaBar with a
 * waveform driven by real FFT frequency data from AudioReactiveAnalyzer.
 *
 * The seeker bar is divided into BAND_COUNT (8) equal segments.
 * Each segment's peak amplitude is driven by one frequency band.
 * Between segment boundaries we smoothly interpolate using cubic Hermite
 * splines so the waveform looks organic rather than stepped.
 *
 * Visual grammar:
 *   - Played portion (left of scrubber): full color, full amplitude
 *   - Unplayed portion (right): track color, 40% amplitude
 *   - Scrubber head: white circle, expands on drag
 *   - During drag: all bands animate to max amplitude (visual feedback)
 *   - During silence: bands decay to a thin flat line (elegant idle state)
 */
@Composable
fun AudioReactiveWavyBar(
    durationMs: Long,
    posProvider: () -> Long,
    isPlaying: Boolean,
    color: Color,
    trackColor: Color,
    onSeek: (Long) -> Unit,
    analyzer: AudioReactiveAnalyzer?,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var localPos by remember { mutableLongStateOf(posProvider()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    // Collect real frequency bands from the analyzer
    val frequencyBandsState = (analyzer?.frequencyBands
        ?: kotlinx.coroutines.flow.MutableStateFlow(FloatArray(AudioReactiveAnalyzer.BAND_COUNT) { 0f }))
        .collectAsState()

    // Position ticker — updates localPos once per second when not dragging
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(50)
            if (!isDragging) localPos = posProvider()
        }
    }

    // When dragging, boost all bands to max so the waveform reacts visually
    // to the scrubbing gesture. Animate smoothly between normal and drag states.
    val dragBoostState = animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "drag_boost"
    )

    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val wavePath = remember { Path() }
    val splineBuffer = remember { FloatArray(AudioReactiveAnalyzer.BAND_COUNT) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSeek((dragProgress * safeDuration).toLong())
                    },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    onSeek(((offset.x / size.width).coerceIn(0f, 1f) * safeDuration).toLong())
                })
            }
    ) {
        val midY = size.height / 2f
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 🚀 DEFERRED STATE READS: Value reads happen here in the Draw Scope.
        val currentFrequencyBands = frequencyBandsState.value
        val currentDragBoost = dragBoostState.value

        // Maximum amplitude in pixels — half the canvas height minus a small margin
        val maxAmplitudePx = (canvasHeight / 2f) * 0.88f

        // Current playback progress (0..1)
        val rawProgress = (localPos.toFloat() / safeDuration).coerceIn(0f, 1f)
        val displayProgress = if (isDragging) dragProgress else rawProgress
        val activeWidth = canvasWidth * displayProgress

        // ── Draw unplayed track (right side) ──────────────────────────────────

        // First pass: draw the track region as a thin flat line behind the waveform
        drawLine(
            color = trackColor,
            start = Offset(activeWidth, midY),
            end = Offset(canvasWidth, midY),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        // ── Blend band amplitudes with drag boost ─────────────────────────────

        for (i in 0 until AudioReactiveAnalyzer.BAND_COUNT) {
            val realBand = currentFrequencyBands.getOrElse(i) { 0f }
            // During drag: lerp toward 1.0; at rest: use real band value
            splineBuffer[i] = lerp(realBand, 1f, currentDragBoost)
        }

        // ── Draw played waveform (left of scrubber) ────────────────────────────

        val bandCount = AudioReactiveAnalyzer.BAND_COUNT
        val segmentWidth = canvasWidth / bandCount.toFloat()

        wavePath.rewind()
        wavePath.moveTo(0f, midY)

        // Build the waveform using cubic Hermite interpolation between band peaks.
        // Each band spans [startX .. endX] of the canvas.
        // We sample the path at 4-pixel intervals for smooth rendering.
        val step = 4f
        var x = 0f
        while (x <= activeWidth) {
            // Which band does this x fall into?
            val bandIndex = floor(x / segmentWidth).toInt().coerceIn(0, bandCount - 1)

            // Cubic Hermite blend within the segment (0..1 within this segment)
            val segmentProgress = (x - bandIndex * segmentWidth) / segmentWidth
            val t = smoothstep(segmentProgress)

            // Amplitude of this band and its neighbor (for smooth transition)
            val amp0 = splineBuffer[bandIndex]
            val amp1 = splineBuffer[(bandIndex + 1).coerceAtMost(bandCount - 1)]
            val blendedAmp = lerp(amp0, amp1, t)

            // A gentle sin wave envelope within each segment adds organic motion
            // when the band amplitude is low (prevents perfectly flat segments)
            val idleWave = sin(x * 0.05f) * 0.08f
            val amplitude = maxAmplitudePx * (blendedAmp + idleWave).coerceAtLeast(0f)

            // Tension: waveform tapers toward the scrubber head for a satisfying
            // "caught up" visual at the playback position
            val distanceFromHead = activeWidth - x
            val tension = when {
                distanceFromHead < segmentWidth -> (distanceFromHead / segmentWidth).coerceIn(0.1f, 1f)
                else -> 1f
            }

            val y = midY - amplitude * tension  // Peaks upward
            wavePath.lineTo(x, y)
            x += step
        }
        // Close back to the midpoint at the scrubber head
        wavePath.lineTo(activeWidth, midY)

        drawIntoCanvas {
            drawPath(
                path = wavePath,
                color = color,
                style = Stroke(
                    width = size.height * 0.6f,  // Stroke thickness proportional to canvas height
                    cap = StrokeCap.Round
                )
            )
        }

        // ── Scrubber head ─────────────────────────────────────────────────────

        val scrubberRadius = if (isDragging) 7f else 4.5f
        val animatedScrubberRadius by Animatable(scrubberRadius).let { anim ->
            // Can't use Animatable inside Canvas directly; use the dragBoost as proxy
            val r = lerp(4.5f, 7f, dragBoost)
            mutableFloatStateOf(r)
        }

        drawCircle(
            color = Color.White,
            radius = lerp(4.5f, 7f, dragBoost),
            center = Offset(activeWidth, midY)
        )
        // Inner dot for tactile feel
        drawCircle(
            color = color.copy(alpha = 0.6f),
            radius = lerp(2f, 3.5f, dragBoost),
            center = Offset(activeWidth, midY)
        )
    }
}

// ── Utility functions ────────────────────────────────────────────────────────

/** Linear interpolation */
private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

/**
 * Smoothstep: a cubic Hermite interpolation between 0 and 1.
 * Unlike linear lerp, this eases in and out — gives organic transitions
 * between frequency band segments.
 */
private fun smoothstep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3 - 2 * x)
}
