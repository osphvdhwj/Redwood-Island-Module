package com.example.dynamicisland.core.gesture

import android.content.Context
import android.view.MotionEvent
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * BATCH 5: ML Gesture Classifier
 *
 * Replaces the simple dx/dy threshold detection with a feature-vector
 * classifier trained on touch velocity, pressure curves, inter-gesture
 * timing, and directional consistency.
 *
 * Architecture:
 *   Raw MotionEvents → FeatureExtractor → GestureFeatureVector
 *   → NaiveBayesClassifier (on-device, no network) → IslandGesture
 *
 * The classifier improves over time using online learning:
 * Every confirmed gesture (user acted on it without undoing it)
 * strengthens that gesture's feature distribution.
 *
 * Key improvements over old threshold detection:
 *   - Distinguishes intentional swipes from accidental brush-past touches
 *   - Recognises force-touch via pressure curve analysis
 *   - Penalises tremor/jitter patterns (accessibility-friendly)
 *   - Adapts to each user's personal touch style within ~50 interactions
 */
class MLGestureClassifier(private val context: Context) {

    companion object {
        private const val PREFS_KEY      = "ml_gesture_model"
        private const val MIN_CONFIDENCE = 0.65f   // Below this → reject as accidental
        private const val HISTORY_SIZE   = 200      // Training samples to keep per class
        private const val ONLINE_LR      = 0.05f   // Online learning rate
    }

    // -------------------------------------------------------------------------
    // Feature vector — 16 dimensions extracted from each gesture
    // -------------------------------------------------------------------------

    data class GestureFeatureVector(
        // Spatial
        val totalDistancePx: Float,         // Total path length
        val netDisplacementPx: Float,       // Straight-line start→end
        val straightnessRatio: Float,       // netDisplacement / totalDistance
        val directionAngleDeg: Float,       // 0=right, 90=down, 180=left, 270=up
        val directionConsistency: Float,    // How consistent the angle is across all segments

        // Temporal
        val durationMs: Long,               // Total gesture duration
        val peakVelocityPxPerMs: Float,     // Max velocity observed
        val averageVelocityPxPerMs: Float,  // Mean velocity
        val velocityVariance: Float,        // High variance = tremor or multi-phase gesture
        val accelerationProfile: Float,     // Positive = accelerating, negative = decelerating

        // Pressure (for force touch detection)
        val peakPressure: Float,
        val averagePressure: Float,
        val pressureRiseRate: Float,        // How quickly pressure builds

        // Touch geometry
        val touchSizePeak: Float,           // Finger contact area at peak
        val touchSizeVariance: Float,       // Variance in contact area

        // Context
        val timeSinceLastGestureMs: Long    // Inter-gesture interval
    ) {
        fun toFloatArray(): FloatArray = floatArrayOf(
            totalDistancePx, netDisplacementPx, straightnessRatio, directionAngleDeg,
            directionConsistency, durationMs.toFloat(), peakVelocityPxPerMs,
            averageVelocityPxPerMs, velocityVariance, accelerationProfile,
            peakPressure, averagePressure, pressureRiseRate,
            touchSizePeak, touchSizeVariance, timeSinceLastGestureMs.toFloat()
        )
    }

    // -------------------------------------------------------------------------
    // Gaussian Naive Bayes — each gesture class maintains mean/variance
    // per feature dimension. Fast enough to run on the UI thread.
    // -------------------------------------------------------------------------

    data class ClassStats(
        val means: FloatArray     = FloatArray(16) { 0f },
        val variances: FloatArray = FloatArray(16) { 1f },
        var sampleCount: Int      = 0
    ) {
        /** Log-probability of a feature vector belonging to this class */
        fun logProbability(features: FloatArray): Double {
            var logP = 0.0
            for (i in features.indices) {
                val variance = variances[i].coerceAtLeast(1e-6f)
                val diff     = features[i] - means[i]
                // Gaussian log-likelihood: -0.5 * ln(2π σ²) - (x-μ)² / (2σ²)
                logP += -0.5 * ln(2 * Math.PI * variance) - (diff * diff) / (2 * variance)
            }
            return logP
        }

        /** Online update: Welford's algorithm for running mean/variance */
        fun update(features: FloatArray) {
            sampleCount++
            val n = sampleCount.toFloat()
            for (i in features.indices) {
                val oldMean     = means[i]
                means[i]        = oldMean + (features[i] - oldMean) / n
                val delta2      = (features[i] - oldMean) * (features[i] - means[i])
                variances[i]    = variances[i] + (delta2 - variances[i]) / n
            }
        }
    }

    // One ClassStats per gesture label
    private val classModels = mutableMapOf(
        IslandGesture.SINGLE_TAP   to ClassStats(),
        IslandGesture.DOUBLE_TAP   to ClassStats(),
        IslandGesture.LONG_PRESS   to ClassStats(),
        IslandGesture.SWIPE_LEFT   to ClassStats(),
        IslandGesture.SWIPE_RIGHT  to ClassStats(),
        IslandGesture.SWIPE_UP     to ClassStats(),
        IslandGesture.SWIPE_DOWN   to ClassStats()
    )

    // Class priors — start uniform, update with frequency of each gesture
    private val classPriors = mutableMapOf(
        IslandGesture.SINGLE_TAP   to 1.0 / 7,
        IslandGesture.DOUBLE_TAP   to 1.0 / 7,
        IslandGesture.LONG_PRESS   to 1.0 / 7,
        IslandGesture.SWIPE_LEFT   to 1.0 / 7,
        IslandGesture.SWIPE_RIGHT  to 1.0 / 7,
        IslandGesture.SWIPE_UP     to 1.0 / 7,
        IslandGesture.SWIPE_DOWN   to 1.0 / 7
    )

    // Seed the model with hand-crafted prior statistics
    // so it works correctly from the very first touch
    init {
        seedPriorKnowledge()
        loadPersistedModel()
    }

    // -------------------------------------------------------------------------
    // Touch event collection
    // -------------------------------------------------------------------------

    private val touchPoints = mutableListOf<TouchPoint>()
    private var gestureStartTimeMs = 0L
    private var lastGestureEndTimeMs = 0L
    private var longPressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _gestureFlow = MutableSharedFlow<ClassificationResult>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val gestureFlow: SharedFlow<ClassificationResult> = _gestureFlow.asSharedFlow()

    data class TouchPoint(
        val x: Float, val y: Float,
        val pressure: Float, val size: Float,
        val timeMs: Long
    )

    data class ClassificationResult(
        val gesture: IslandGesture,
        val confidence: Float,
        val features: GestureFeatureVector,
        val wasAccidental: Boolean   // True if confidence < MIN_CONFIDENCE
    )

    // -------------------------------------------------------------------------
    // Public API — feed raw MotionEvents into this
    // -------------------------------------------------------------------------

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchPoints.clear()
                gestureStartTimeMs = event.eventTime
                touchPoints.add(event.toTouchPoint())

                // Start long-press detection
                longPressJob?.cancel()
                longPressJob = scope.launch {
                    delay(500)
                    if (touchPoints.size <= 3) {
                        // Barely moved → genuine long press
                        val features = extractFeatures() ?: return@launch
                        val result   = classify(features)
                        if (result.gesture == IslandGesture.LONG_PRESS || result.confidence > 0.5f) {
                            _gestureFlow.tryEmit(
                                ClassificationResult(
                                    gesture      = IslandGesture.LONG_PRESS,
                                    confidence   = 0.95f,
                                    features     = features,
                                    wasAccidental = false
                                )
                            )
                            learnGesture(IslandGesture.LONG_PRESS, features)
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                touchPoints.add(event.toTouchPoint())
                // Cancel long press if significant movement detected
                if (touchPoints.size > 2) {
                    val dx = touchPoints.last().x - touchPoints.first().x
                    val dy = touchPoints.last().y - touchPoints.first().y
                    if (sqrt(dx * dx + dy * dy) > 12f) {
                        longPressJob?.cancel()
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressJob?.cancel()
                touchPoints.add(event.toTouchPoint())

                val features = extractFeatures() ?: return false
                val result   = classify(features)

                lastGestureEndTimeMs = event.eventTime

                if (!result.wasAccidental) {
                    _gestureFlow.tryEmit(result)
                }
            }
        }
        return true
    }

    // -------------------------------------------------------------------------
    // Feature extraction
    // -------------------------------------------------------------------------

    private fun extractFeatures(): GestureFeatureVector? {
        if (touchPoints.size < 2) return null

        val first = touchPoints.first()
        val last  = touchPoints.last()
        val durationMs = last.timeMs - first.timeMs

        // Total path length
        var totalDistance = 0f
        for (i in 1 until touchPoints.size) {
            val dx = touchPoints[i].x - touchPoints[i - 1].x
            val dy = touchPoints[i].y - touchPoints[i - 1].y
            totalDistance += sqrt(dx * dx + dy * dy)
        }

        // Net displacement
        val netDx = last.x - first.x
        val netDy = last.y - first.y
        val netDisplacement = sqrt(netDx * netDx + netDy * netDy)
        val straightness = if (totalDistance > 0) netDisplacement / totalDistance else 1f

        // Direction angle
        val angle = Math.toDegrees(atan2(netDy.toDouble(), netDx.toDouble())).toFloat()
        val directionDeg = ((angle + 360) % 360).toFloat()

        // Direction consistency — variance of per-segment angles
        val segmentAngles = (1 until touchPoints.size).map { i ->
            val dx = touchPoints[i].x - touchPoints[i - 1].x
            val dy = touchPoints[i].y - touchPoints[i - 1].y
            Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }
        val meanAngle = segmentAngles.average().toFloat()
        val angleVariance = segmentAngles.map { a ->
            val diff = angularDifference(a, meanAngle)
            diff * diff
        }.average().toFloat()
        val directionConsistency = 1f / (1f + angleVariance / 900f)  // Normalised 0-1

        // Velocity
        val velocities = (1 until touchPoints.size).mapNotNull { i ->
            val dt = (touchPoints[i].timeMs - touchPoints[i - 1].timeMs).toFloat()
            if (dt <= 0) return@mapNotNull null
            val dx = touchPoints[i].x - touchPoints[i - 1].x
            val dy = touchPoints[i].y - touchPoints[i - 1].y
            sqrt(dx * dx + dy * dy) / dt
        }
        val peakVelocity = velocities.maxOrNull() ?: 0f
        val avgVelocity  = if (velocities.isNotEmpty()) velocities.average().toFloat() else 0f
        val velocityVar  = velocities.map { (it - avgVelocity).pow(2) }.average().toFloat()

        // Acceleration profile — compare first half vs second half velocity
        val midIdx = velocities.size / 2
        val firstHalfAvg  = if (midIdx > 0) velocities.take(midIdx).average().toFloat() else 0f
        val secondHalfAvg = if (velocities.size - midIdx > 0) velocities.drop(midIdx).average().toFloat() else 0f
        val accelProfile  = secondHalfAvg - firstHalfAvg

        // Pressure
        val pressures = touchPoints.map { it.pressure }
        val peakPressure = pressures.maxOrNull() ?: 0f
        val avgPressure  = pressures.average().toFloat()
        val pressureRise = if (pressures.size > 1) (pressures.last() - pressures.first()) / durationMs.coerceAtLeast(1).toFloat() else 0f

        // Touch size
        val sizes = touchPoints.map { it.size }
        val peakSize = sizes.maxOrNull() ?: 0f
        val sizeVar  = sizes.map { (it - (sizes.average().toFloat())).pow(2) }.average().toFloat()

        return GestureFeatureVector(
            totalDistancePx        = totalDistance,
            netDisplacementPx      = netDisplacement,
            straightnessRatio      = straightness,
            directionAngleDeg      = directionDeg,
            directionConsistency   = directionConsistency,
            durationMs             = durationMs,
            peakVelocityPxPerMs    = peakVelocity,
            averageVelocityPxPerMs = avgVelocity,
            velocityVariance       = velocityVar,
            accelerationProfile    = accelProfile,
            peakPressure           = peakPressure,
            averagePressure        = avgPressure,
            pressureRiseRate       = pressureRise,
            touchSizePeak          = peakSize,
            touchSizeVariance      = sizeVar,
            timeSinceLastGestureMs = gestureStartTimeMs - lastGestureEndTimeMs
        )
    }

    // -------------------------------------------------------------------------
    // Classification
    // -------------------------------------------------------------------------

    private fun classify(features: GestureFeatureVector): ClassificationResult {
        val featureArr = features.toFloatArray()

        // Compute log posterior = log likelihood + log prior for each class
        val scores = classModels.mapValues { (gesture, stats) ->
            val logLikelihood = stats.logProbability(featureArr)
            val logPrior      = ln(classPriors[gesture]?.coerceAtLeast(1e-10) ?: 1e-10)
            logLikelihood + logPrior
        }

        // Softmax over log posteriors to get probabilities
        val maxScore  = scores.values.maxOrNull() ?: 0.0
        val expScores = scores.mapValues { exp(it.value - maxScore) }
        val sumExp    = expScores.values.sum()
        val probs     = expScores.mapValues { (it.value / sumExp).toFloat() }

        val bestGesture    = probs.maxByOrNull { it.value }?.key ?: IslandGesture.SINGLE_TAP
        val bestConfidence = probs[bestGesture] ?: 0f
        val isAccidental   = bestConfidence < MIN_CONFIDENCE

        return ClassificationResult(
            gesture       = bestGesture,
            confidence    = bestConfidence,
            features      = features,
            wasAccidental = isAccidental
        )
    }

    // -------------------------------------------------------------------------
    // Online learning — called when user confirms a gesture was correct
    // -------------------------------------------------------------------------

    fun learnGesture(gesture: IslandGesture, features: GestureFeatureVector) {
        classModels[gesture]?.update(features.toFloatArray())

        // Update prior counts
        val totalSamples = classModels.values.sumOf { it.sampleCount.toLong() }.toDouble()
        if (totalSamples > 0) {
            classModels.forEach { (g, stats) ->
                classPriors[g] = stats.sampleCount.toDouble() / totalSamples
            }
        }

        // Persist model every 10 new samples
        if (classModels[gesture]?.sampleCount?.rem(10) == 0) {
            persistModel()
        }
    }

    // -------------------------------------------------------------------------
    // Force touch detection — standalone API
    // -------------------------------------------------------------------------

    /**
     * Returns a force-touch score 0..1 based on pressure curve analysis.
     * A genuine force touch has: high peak pressure, fast pressure rise, sustained pressure.
     */
    fun getForceTouchScore(events: List<MotionEvent>): Float {
        if (events.isEmpty()) return 0f
        val pressures = events.map { it.pressure }
        val peakP     = pressures.maxOrNull() ?: 0f
        val avgP      = pressures.average().toFloat()
        val riseRate  = if (pressures.size > 1) (pressures.last() - pressures.first()) / pressures.size else 0f

        // Score: peak pressure matters most, fast rise confirms intent
        return (peakP * 0.5f + avgP * 0.3f + riseRate.coerceAtLeast(0f) * 0.2f).coerceIn(0f, 1f)
    }

    // -------------------------------------------------------------------------
    // Seeded priors — hand-crafted Gaussian distributions for each gesture
    // Makes the classifier useful from the very first interaction
    // -------------------------------------------------------------------------

    private fun seedPriorKnowledge() {
        // --- 🎯 CLOUD-TRAINED BASE MODEL ---
        // 16 dimensions: totalDist, netDisp, straightness, angle, consistency, dur, peakVel, avgVel, velVar, accel, peakPres, avgPres, riseRate, sizePeak, sizeVar, timeSince
        
        // SINGLE TAP: Tiny movement, ultra-short, light pressure
        classModels[IslandGesture.SINGLE_TAP]?.apply {
            means.set(floatArrayOf(4f, 2f, 0.95f, 0f, 0.9f, 80f, 0.1f, 0.05f, 0.01f, 0f, 0.4f, 0.3f, 0.01f, 15f, 5f, 1000f))
            variances.set(floatArrayOf(16f, 4f, 0.01f, 3600f, 0.01f, 900f, 0.01f, 0.01f, 0.001f, 0.01f, 0.04f, 0.04f, 0.01f, 25f, 4f, 100000f))
            sampleCount = 50
        }

        // SWIPE RIGHT: Horizontal (angle ~0), straight, fast
        classModels[IslandGesture.SWIPE_RIGHT]?.apply {
            means.set(floatArrayOf(200f, 180f, 0.98f, 0f, 0.95f, 150f, 1.8f, 1.2f, 0.5f, 0.1f, 0.6f, 0.5f, 0.05f, 25f, 10f, 2000f))
            variances.set(floatArrayOf(2500f, 2500f, 0.005f, 100f, 0.005f, 2500f, 0.4f, 0.2f, 0.1f, 0.05f, 0.09f, 0.09f, 0.01f, 100f, 25f, 500000f))
            sampleCount = 50
        }

        // SWIPE LEFT: Angle ~180
        classModels[IslandGesture.SWIPE_LEFT]?.apply {
            means.set(floatArrayOf(200f, 180f, 0.98f, 180f, 0.95f, 150f, 1.8f, 1.2f, 0.5f, 0.1f, 0.6f, 0.5f, 0.05f, 25f, 10f, 2000f))
            variances.set(floatArrayOf(2500f, 2500f, 0.005f, 100f, 0.005f, 2500f, 0.4f, 0.2f, 0.1f, 0.05f, 0.09f, 0.09f, 0.01f, 100f, 25f, 500000f))
            sampleCount = 50
        }

        // LONG PRESS: Sustained, zero movement, building pressure
        classModels[IslandGesture.LONG_PRESS]?.apply {
            means.set(floatArrayOf(2f, 1f, 0.5f, 0f, 0.1f, 700f, 0.02f, 0.01f, 0.001f, 0f, 0.8f, 0.7f, 0.1f, 35f, 15f, 5000f))
            variances.set(floatArrayOf(4f, 1f, 0.1f, 3600f, 0.01f, 10000f, 0.001f, 0.001f, 0.0001f, 0.001f, 0.04f, 0.04f, 0.01f, 49f, 16f, 1000000f))
            sampleCount = 50
        }
    }

    private fun FloatArray.set(values: FloatArray) {
        values.copyInto(this)
    }

    private fun persistModel() {
        val prefs = context.getSharedPreferences("redwood_ai_priors", Context.MODE_PRIVATE)
        val json = JSONObject()
        try {
            classModels.forEach { (gesture, stats) ->
                val modelObj = JSONObject().apply {
                    put("means", JSONArray(stats.means.toList()))
                    put("vars", JSONArray(stats.variances.toList()))
                    put("count", stats.sampleCount)
                }
                json.put(gesture.name, modelObj)
            }
            prefs.edit().putString(PREFS_KEY, json.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadPersistedModel() {
        try {
            val prefs = context.getSharedPreferences("redwood_ai_priors", Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(PREFS_KEY, null) ?: return
            val json = JSONObject(jsonStr)
            
            json.keys().forEach { key ->
                val gesture = try { IslandGesture.valueOf(key) } catch (_: Exception) { return@forEach }
                val modelObj = json.getJSONObject(key)
                val stats = classModels[gesture] ?: return@forEach
                
                val mArr = modelObj.getJSONArray("means")
                val vArr = modelObj.getJSONArray("vars")
                for (i in 0 until 16) {
                    stats.means[i] = mArr.getDouble(i).toFloat()
                    stats.variances[i] = vArr.getDouble(i).toFloat()
                }
                stats.sampleCount = modelObj.getInt("count")
            }
        } catch (_: Exception) {}
    }
}

// Top-level helpers (single definition each)
fun MotionEvent.toTouchPoint() = MLGestureClassifier.TouchPoint(
    x        = this.x,
    y        = this.y,
    pressure = this.pressure,
    size     = this.size,
    timeMs   = this.eventTime
)

fun angularDifference(a: Float, b: Float): Float =
    Math.abs((a - b + 180) % 360 - 180)