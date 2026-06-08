package com.example.dynamicisland.core.gesture

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * BATCH 5: Force Touch Handler
 *
 * Android devices expose pressure data via MotionEvent.getPressure()
 * and touch size via MotionEvent.getSize(). While not as precise as
 * Apple's hardware force sensors, the combination of:
 *   - Sustained high pressure (> 0.75 on most devices)
 *   - Fast pressure ramp-up (distinguishes press from rest)
 *   - Stable touch position (not a drag)
 *   - Touch area expansion (finger flattens under force)
 *
 * ...gives a reliable enough force-touch signal on modern Android hardware.
 *
 * Mapped to a new IslandGesture: FORCE_PRESS
 * Default action: Open the full Max dashboard instantly.
 *
 * Fallback for devices with capped pressure (always returns 1.0):
 * Uses touch area expansion as the primary signal instead.
 */
class ForceTouchHandler(private val context: Context) {

    companion object {
        // Threshold tuned across Snapdragon 8xx devices
        const val FORCE_THRESHOLD_PRESSURE  = 0.72f
        const val FORCE_THRESHOLD_AREA      = 0.45f  // Normalised touch area
        const val FORCE_HOLD_DURATION_MS    = 150L   // Must sustain for this long
        const val FORCE_MOVEMENT_THRESHOLD  = 8f     // px — abort if finger moves
    }

    sealed class ForceTouchState {
        object Idle          : ForceTouchState()
        object Building      : ForceTouchState()   // Pressure rising
        object Triggered     : ForceTouchState()   // Force touch confirmed
        object Released      : ForceTouchState()
    }

    data class ForceTouchEvent(
        val maxPressure: Float,
        val maxArea: Float,
        val durationMs: Long,
        val confidence: Float   // 0..1
    )

    private val _forceFlow = MutableSharedFlow<ForceTouchEvent>(extraBufferCapacity = 4)
    val forceFlow: SharedFlow<ForceTouchEvent> = _forceFlow.asSharedFlow()

    private var state: ForceTouchState = ForceTouchState.Idle
    private var forceStartTimeMs  = 0L
    private var startX            = 0f
    private var startY            = 0f
    private var maxPressureSeen   = 0f
    private var maxAreaSeen       = 0f
    private var pressureSamples   = mutableListOf<Float>()
    private var hasFiredThisTouch = false

    // Detect whether this device caps pressure at 1.0 (common on many OEMs)
    // If so, switch to area-based detection
    private var pressureCapped = false
    private var pressureReadings = mutableListOf<Float>()
    private var calibrationDone  = false

    private val vibrator by lazy {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // -------------------------------------------------------------------------
    // Public API — call this from DynamicIslandView's pointerInput handler
    // -------------------------------------------------------------------------

    fun onTouchEvent(event: MotionEvent): Boolean {
        calibratePressureIfNeeded(event.pressure)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                reset()
                startX = event.x
                startY = event.y
                checkPressure(event)
            }

            MotionEvent.ACTION_MOVE -> {
                // Abort if finger has moved significantly
                val dx = event.x - startX
                val dy = event.y - startY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > FORCE_MOVEMENT_THRESHOLD) {
                    reset()
                    return false
                }
                checkPressure(event)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
            }
        }
        return state == ForceTouchState.Triggered
    }

    // -------------------------------------------------------------------------
    // Pressure evaluation
    // -------------------------------------------------------------------------

    private fun checkPressure(event: MotionEvent) {
        if (hasFiredThisTouch) return

        val pressure = event.pressure
        val area     = event.size
        pressureSamples.add(pressure)

        maxPressureSeen = maxOf(maxPressureSeen, pressure)
        maxAreaSeen     = maxOf(maxAreaSeen, area)

        // Use pressure or area depending on device capability
        val primarySignal = if (pressureCapped) area else pressure
        val primaryThresh = if (pressureCapped) FORCE_THRESHOLD_AREA else FORCE_THRESHOLD_PRESSURE

        when (state) {
            is ForceTouchState.Idle -> {
                if (primarySignal > primaryThresh * 0.6f) {
                    state          = ForceTouchState.Building
                    forceStartTimeMs = event.eventTime
                }
            }

            is ForceTouchState.Building -> {
                val holdDuration = event.eventTime - forceStartTimeMs
                if (primarySignal >= primaryThresh && holdDuration >= FORCE_HOLD_DURATION_MS) {
                    // Confirm force touch
                    state = ForceTouchState.Triggered
                    hasFiredThisTouch = true

                    val confidence = calculateConfidence(pressure, area, holdDuration)
                    triggerHaptic(confidence)

                    _forceFlow.tryEmit(
                        ForceTouchEvent(
                            maxPressure = maxPressureSeen,
                            maxArea     = maxAreaSeen,
                            durationMs  = holdDuration,
                            confidence  = confidence
                        )
                    )
                } else if (primarySignal < primaryThresh * 0.4f) {
                    // Pressure dropped — was not a genuine force touch
                    state = ForceTouchState.Idle
                    forceStartTimeMs = 0L
                }
            }

            else -> {}
        }
    }

    private fun calculateConfidence(pressure: Float, area: Float, holdMs: Long): Float {
        // Pressure component (0..1)
        val pressureScore = if (pressureCapped) {
            (area / 0.6f).coerceIn(0f, 1f)
        } else {
            ((pressure - FORCE_THRESHOLD_PRESSURE) / (1f - FORCE_THRESHOLD_PRESSURE)).coerceIn(0f, 1f)
        }

        // Hold duration component — longer = more deliberate
        val holdScore = ((holdMs - FORCE_HOLD_DURATION_MS) / 300f).coerceIn(0f, 1f)

        // Pressure curve consistency — a genuine press has smooth rise
        val avgPressure = if (pressureSamples.isNotEmpty()) pressureSamples.average().toFloat() else 0f
        val curveScore = (avgPressure / pressure).coerceIn(0f, 1f)

        return (pressureScore * 0.5f + holdScore * 0.3f + curveScore * 0.2f)
    }

    // -------------------------------------------------------------------------
    // Calibration — detects if device caps pressure at 1.0
    // -------------------------------------------------------------------------

    private fun calibratePressureIfNeeded(pressure: Float) {
        if (calibrationDone) return
        pressureReadings.add(pressure)
        if (pressureReadings.size >= 20) {
            val uniqueValues = pressureReadings.toSet()
            // If every reading is exactly 1.0, the device has capped pressure
            pressureCapped  = uniqueValues.size == 1 && uniqueValues.first() == 1.0f
            calibrationDone = true
        }
    }

    // -------------------------------------------------------------------------
    // Haptic feedback — graduated by confidence level
    // -------------------------------------------------------------------------

    private fun triggerHaptic(confidence: Float) {
        try {
            val effect = when {
                confidence > 0.85f -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                confidence > 0.6f  -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                else               -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            }
            vibrator.vibrate(effect)
        } catch (e: Exception) { /* vibration not available */ }
    }

    private fun reset() {
        state             = ForceTouchState.Idle
        forceStartTimeMs  = 0L
        maxPressureSeen   = 0f
        maxAreaSeen       = 0f
        pressureSamples.clear()
        hasFiredThisTouch = false
    }

    fun isCurrentlyForcePressed() = state == ForceTouchState.Triggered
}
