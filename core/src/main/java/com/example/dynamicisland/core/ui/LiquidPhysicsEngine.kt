package com.example.dynamicisland.core.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BATCH 3: Accelerometer-Reactive Liquid Charging Animation
 *
 * Replaces the static radial-arc and HyperOS-gradient charging styles with
 * a physically-simulated liquid fill that sloshes in response to how the
 * user is holding their phone.
 *
 * Physics model:
 *   The liquid surface is modelled as a horizontal sine wave whose phase
 *   and tilt are driven by gravity read from TYPE_GRAVITY sensor.
 *
 *   Surface height at pixel x:
 *     y(x, t) = baseLevel
 *              + tilt  * (x / canvasWidth - 0.5)   // gravity tilt
 *              + Σ wave_i.amplitude * sin(wave_i.freq * x + wave_i.phase)
 *
 *   Each wave has a spring-damped response to the tilt impulse:
 *     dv/dt = -k * (phase - targetPhase) - b * v
 *
 *   Three overlapping waves at slightly different frequencies produce
 *   an organic sloshing look without visible repetition.
 *
 * The fill level tracks battery percentage with a spring animation
 * (dampingRatio = 0.75, stiffness = 200) so plugging in shows a
 * satisfying liquid-rising motion.
 *
 * Usage — drop into IslandChargingUI.kt as:
 *
 *   LiquidChargingCanvas(
 *       level = charging.level,
 *       color = batteryColor,
 *       isCharging = charging.isPluggedIn
 *   )
 */

// ── Physics engine (runs off-UI thread) ───────────────────────────────────────

class LiquidPhysicsEngine(context: Context) : SensorEventListener {

    data class WaveState(
        var amplitude:    Float = 0f,
        var phase:        Float = 0f,
        var velocity:     Float = 0f,
        val frequency:    Float,          // spatial frequency (rad/px normalised)
        val springK:      Float = 18f,    // spring stiffness
        val dampingB:     Float = 5.5f    // damping coefficient
    )

    private val _tiltX        = MutableStateFlow(0f)  // normalised -1..1 (left/right)
    private val _waveStates   = MutableStateFlow(
        listOf(
            WaveState(frequency = 0.85f,  springK = 22f, dampingB = 5f),
            WaveState(frequency = 1.43f,  springK = 16f, dampingB = 6f),
            WaveState(frequency = 2.17f,  springK = 12f, dampingB = 7f)
        )
    )
    val tiltX:      StateFlow<Float>           = _tiltX.asStateFlow()
    val waveStates: StateFlow<List<WaveState>> = _waveStates.asStateFlow()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor  = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val physicsScope   = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var gravX          = 0f
    private var lastTickMs     = System.currentTimeMillis()

    fun start() {
        val sensor = gravitySensor ?: accelSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        physicsScope.launch { tickLoop() }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        physicsScope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Gravity X: positive = tilted right, negative = tilted left
        gravX = event.values[0] / SensorManager.GRAVITY_EARTH   // normalised -1..1
        _tiltX.value = gravX
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private suspend fun tickLoop() {
        while (true) {
            delay(16)   // ~60Hz physics tick
            val nowMs   = System.currentTimeMillis()
            val dtSec   = ((nowMs - lastTickMs) / 1000f).coerceIn(0f, 0.05f)
            lastTickMs  = nowMs

            // Each wave's target phase is driven by the current tilt
            val updated = _waveStates.value.map { wave ->
                val targetPhase = gravX * 2.5f * wave.frequency
                val acc = -wave.springK * (wave.phase - targetPhase) - wave.dampingB * wave.velocity
                val newVel   = wave.velocity + acc * dtSec
                val newPhase = wave.phase    + newVel * dtSec
                // Amplitude grows with the magnitude of tilt, decays when flat
                val targetAmp = abs(gravX) * 0.06f + 0.015f   // base idle ripple
                val newAmp    = wave.amplitude + (targetAmp - wave.amplitude) * dtSec * 3f
                wave.copy(amplitude = newAmp, phase = newPhase, velocity = newVel)
            }
            _waveStates.value = updated
        }
    }
}

// ── Composable ────────────────────────────────────────────────────────────────

/**
 * Drop this wherever a charging animation is needed.
 * Internally creates and remembers a [LiquidPhysicsEngine] tied to the
 * composition lifecycle.
 */
@Composable
fun LiquidChargingCanvas(
    level:      Int,
    color:      Color,
    isCharging: Boolean,
    modifier:   Modifier = Modifier.size(70.dp)
) {
    val context = LocalContext.current

    // Lifecycle-scoped physics engine
    val engine = remember { LiquidPhysicsEngine(context) }
    DisposableEffect(Unit) {
        engine.start()
        onDispose { engine.stop() }
    }

    val tiltX      by engine.tiltX.collectAsState()
    val waveStates by engine.waveStates.collectAsState()

    // Spring-animated fill level
    val animatedFill by animateFloatAsState(
        targetValue   = level / 100f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 200f),
        label         = "fill"
    )

    // Charging glow pulse
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isCharging) 0.15f else 0f,
        targetValue   = if (isCharging) 0.50f else 0f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "glow_alpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width;  val h = size.height
        val canvasPath = Path()

        // ── Clip to rounded rectangle (the battery container) ──────────────────
        val cornerRadius = w * 0.22f
        canvasPath.addRoundRect(
            RoundRect(0f, 0f, w, h, CornerRadius(cornerRadius))
        )
        clipPath(canvasPath) {

            // ── Background ─────────────────────────────────────────────────────
            drawRect(color.copy(alpha = 0.10f))

            // ── Glow halo behind the liquid (charging only) ────────────────────
            if (isCharging && glowAlpha > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors    = listOf(Color.Transparent, color.copy(alpha = glowAlpha)),
                        startY    = h * 0.4f,
                        endY      = h
                    )
                )
            }

            // ── Liquid surface path ────────────────────────────────────────────
            val baseY        = h * (1f - animatedFill)       // fill height from bottom
            val tiltOffset   = tiltX * w * 0.12f             // max ±12% width of canvas

            val liquidPath = Path()
            liquidPath.moveTo(0f, h)    // bottom-left
            liquidPath.lineTo(w, h)     // bottom-right
            liquidPath.lineTo(w, h)

            // Trace the surface from right to left
            val step = 3
            for (xi in w.toInt() downTo 0 step step) {
                val xNorm = xi / w           // 0..1
                var surfaceY = baseY + tiltOffset * (xNorm - 0.5f)

                // Add each wave
                for (wave in waveStates) {
                    surfaceY += wave.amplitude * h *
                        sin(xNorm * wave.frequency * 2f * PI.toFloat() + wave.phase)
                }

                // Clamp so liquid never overflows or underflows the canvas
                surfaceY = surfaceY.coerceIn(0f, h)
                liquidPath.lineTo(xi.toFloat(), surfaceY)
            }
            liquidPath.close()

            // Fill the liquid
            val liquidBrush = Brush.verticalGradient(
                colors    = listOf(color.copy(alpha = 0.85f), color),
                startY    = baseY - h * 0.1f,
                endY      = h
            )
            drawPath(liquidPath, liquidBrush)

            // ── Surface shimmer (thin bright line at the liquid/air boundary) ──
            val shimmerPath = Path()
            shimmerPath.moveTo(0f, baseY + tiltOffset * (-0.5f))
            for (xi in 0..w.toInt() step step) {
                val xNorm  = xi / w
                var sY     = baseY + tiltOffset * (xNorm - 0.5f)
                for (wave in waveStates) {
                    sY += wave.amplitude * h *
                        sin(xNorm * wave.frequency * 2f * PI.toFloat() + wave.phase)
                }
                shimmerPath.lineTo(xi.toFloat(), sY.coerceIn(0f, h))
            }
            drawPath(
                shimmerPath,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // ── Border ─────────────────────────────────────────────────────────────
        drawRoundRect(
            color       = color.copy(alpha = 0.40f),
            cornerRadius = CornerRadius(cornerRadius),
            style       = Stroke(width = 1.5.dp.toPx())
        )

        // ── Percentage label ───────────────────────────────────────────────────
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                textSize    = h * 0.28f
                textAlign   = android.graphics.Paint.Align.CENTER
                this.color  = if (animatedFill > 0.5f) android.graphics.Color.BLACK
                              else                      android.graphics.Color.WHITE
                isFakeBoldText = true
                isAntiAlias = true
            }
            drawText("$level%", w / 2f, h * 0.62f, paint)
        }
    }
}
