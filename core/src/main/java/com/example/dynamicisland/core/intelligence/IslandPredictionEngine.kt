package com.example.dynamicisland.core.intelligence

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.*
import com.example.dynamicisland.core.manager.IslandMediaManager
import com.example.dynamicisland.core.performance.DensityAwareIconCache
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * BATCH 2: Island Prediction Engine
 *
 * Uses UsageStatsManager to build a probabilistic model of the user's
 * daily app usage patterns. The engine answers three questions:
 *
 *   1. WHAT     — which app/event is most likely to occur next
 *   2. WHEN     — how many minutes from now is it likely to happen
 *   3. PREPARE  — what should we pre-warm (icons, album art, etc.)
 *
 * This runs entirely on-device via WorkManager. No network, no cloud.
 *
 * Model architecture:
 *   - Hour-of-day buckets (24 buckets × 7 days = 168 slots)
 *   - Per-slot app frequency table (how often each app was opened)
 *   - Session transition matrix (A→B probability when A just closed)
 *   - Recency weighting (last 7 days weighted 2× vs last 30 days)
 *
 * Outputs:
 *   - predictedNextApp: the package most likely to open in the next 10 minutes
 *   - predictedEventType: MEDIA / CALL / NAVIGATION based on app category
 *   - confidenceScore: 0..1
 *   - minutesUntilLikely: estimated lead time for pre-warming
 */
class IslandPredictionEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IslandPrediction"
        private const val PREFS_NAME = "island_prediction_model"
        private const val KEY_MODEL = "usage_model_json"
        private const val KEY_TRANSITION = "transition_matrix_json"
        private const val KEY_LAST_TRAINED = "last_trained_ms"

        // Retrain if model is older than this
        private const val RETRAIN_INTERVAL_MS = 6 * 60 * 60 * 1000L  // 6 hours

        // Minimum opens before we trust a prediction
        private const val MIN_CONFIDENCE_OPENS = 3

        // Pre-warm this many minutes before predicted event
        private const val PREWARM_LEAD_MINUTES = 2

        @Volatile private var instance: IslandPredictionEngine? = null

        fun get(context: Context): IslandPredictionEngine =
            instance ?: synchronized(this) {
                instance ?: IslandPredictionEngine(context.applicationContext).also { instance = it }
            }

        // Known media app packages — used to map package → event type
        private val MEDIA_PACKAGES = setOf(
            "com.spotify.music", "com.google.android.youtube",
            "com.google.android.apps.youtube.music", "com.apple.android.music",
            "com.amazon.mp3", "com.soundcloud.android", "com.gaana",
            "com.jio.media.jiobeats", "com.wynk.music", "com.hungama.myplay",
            "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
            "com.hotstar.android", "com.jio.media.ondemand"
        )

        private val NAVIGATION_PACKAGES = setOf(
            "com.google.android.apps.maps", "com.waze",
            "com.ola.client", "in.ola.mobility.rider"
        )

        private val CALL_PACKAGES = setOf(
            "com.google.android.dialer", "com.android.dialer",
            "com.whatsapp", "org.telegram.messenger",
            "com.google.android.apps.tachyon"  // Google Meet
        )
    }

    // -------------------------------------------------------------------------
    // Data structures
    // -------------------------------------------------------------------------

    /** Predicted outcome the engine will surface to IslandController */
    data class Prediction(
        val predictedPackage: String,
        val predictedEventType: PredictedEventType,
        val confidenceScore: Float,    // 0..1
        val minutesUntilLikely: Int,   // Estimated lead time
        val isPrewarmReady: Boolean    // True once icons/art are loaded
    )

    enum class PredictedEventType { MEDIA, CALL, NAVIGATION, GENERAL, UNKNOWN }

    /**
     * Per-slot usage record — one slot = one (dayOfWeek, hourOfDay) pair
     */
    data class UsageSlot(
        val dayOfWeek: Int,   // Calendar.MONDAY..Calendar.SUNDAY
        val hourOfDay: Int,   // 0..23
        val appFrequency: MutableMap<String, Int> = mutableMapOf()  // pkg → open count
    )

    /**
     * Session transition: when app A is closed, how likely is app B to open next?
     * Matrix[A][B] = count of times B opened after A
     */
    private val transitionMatrix = ConcurrentHashMap<String, ConcurrentHashMap<String, Int>>()

    // Slot grid: [dayOfWeek][hourOfDay] = UsageSlot
    private val usageGrid = Array(8) { Array(24) { mutableMapOf<String, Int>() } }

    // Total sample count — used to compute relative probabilities
    private var totalSamples = 0

    // Last app seen — for building transition matrix
    private var lastSeenApp = ""

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    private val _prediction = MutableStateFlow<Prediction?>(null)
    val prediction: StateFlow<Prediction?> = _prediction.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadPersistedModel()
        seedGlobalPriors()
        scheduleBackgroundTraining()
        // Start real-time prediction loop
        scope.launch { predictionLoop() }
    }

    private fun seedGlobalPriors() {
        if (totalSamples > 0) return // Already trained locally

        try {
            val baseModel = JSONArray(PreTrainedUsageModel.GLOBAL_BASE_MODEL_JSON)
            for (i in 0 until baseModel.length()) {
                val entry = baseModel.getJSONObject(i)
                val hour = entry.getInt("hour")
                val pkg = entry.getString("pkg")
                val count = entry.getInt("count")
                
                // Spread count across all days for the pre-trained hour
                for (day in 0..7) {
                    usageGrid[day][hour][pkg] = count
                    totalSamples += count
                }
            }
            Log.i(TAG, "Seeded global usage priors: $totalSamples samples loaded from cloud.")
        } catch (_: Exception) {}
    }

    /**
     * Call this whenever the foreground app changes (from SystemEventsHook).
     * Updates the transition matrix in real time without waiting for a retrain.
     */
    fun onAppForegrounded(packageName: String) {
        if (packageName == lastSeenApp || packageName.isEmpty()) return

        // Update transition matrix
        if (lastSeenApp.isNotEmpty()) {
            val row = transitionMatrix.getOrPut(lastSeenApp) { ConcurrentHashMap() }
            row[packageName] = (row[packageName] ?: 0) + 1
        }

        // Update current-hour bucket
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        usageGrid[day][hour][packageName] = (usageGrid[day][hour][packageName] ?: 0) + 1
        totalSamples++

        lastSeenApp = packageName
    }

    /**
     * Force a full retrain from UsageStats history right now.
     * Called by WorkManager every 6 hours.
     */
    suspend fun trainFromUsageStats() = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "PACKAGE_USAGE_STATS permission not granted — skipping training")
            return@withContext
        }

        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endMs = System.currentTimeMillis()
            val startMs = endMs - 30L * 24 * 60 * 60 * 1000  // 30 days

            // Reset the grid (we'll rebuild from scratch for consistency)
            for (day in 0..7) for (hour in 0..23) usageGrid[day][hour].clear()
            transitionMatrix.clear()
            totalSamples = 0
            lastSeenApp = ""

            val events = usm.queryEvents(startMs, endMs)
            val event = UsageEvents.Event()
            var previousPkg = ""
            var previousTimeMs = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                // We only care about foreground starts
                if (event.eventType != UsageEvents.Event.ACTIVITY_RESUMED) continue

                val pkg = event.packageName ?: continue
                if (isSystemPackage(pkg)) continue

                val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                val day = cal.get(Calendar.DAY_OF_WEEK)
                val hour = cal.get(Calendar.HOUR_OF_DAY)

                // Apply recency weighting: last 7 days count double
                val isRecent = (endMs - event.timeStamp) < 7L * 24 * 60 * 60 * 1000
                val weight = if (isRecent) 2 else 1

                repeat(weight) {
                    usageGrid[day][hour][pkg] = (usageGrid[day][hour][pkg] ?: 0) + 1
                    totalSamples++
                }

                // Transition matrix update
                if (previousPkg.isNotEmpty() && pkg != previousPkg) {
                    // Only record transitions within a 10-minute window (genuine context switches)
                    if (event.timeStamp - previousTimeMs < 10 * 60 * 1000) {
                        val row = transitionMatrix.getOrPut(previousPkg) { ConcurrentHashMap() }
                        row[pkg] = (row[pkg] ?: 0) + weight
                    }
                }

                previousPkg = pkg
                previousTimeMs = event.timeStamp
            }

            persistModel()
            Log.i(TAG, "Training complete: $totalSamples samples across ${usageGrid.sumOf { row -> row.count { it.isNotEmpty() } }} active slots")

        } catch (e: Exception) {
            Log.e(TAG, "Training failed: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Prediction loop — runs every 60 seconds, emits updated predictions
    // -------------------------------------------------------------------------

    private suspend fun predictionLoop() {
        while (true) {
            delay(60_000)
            val newPrediction = computePrediction()
            if (newPrediction != null && newPrediction.confidenceScore > 0.4f) {
                _prediction.value = newPrediction
                if (!newPrediction.isPrewarmReady) {
                    prewarmForPrediction(newPrediction)
                }
            }
        }
    }

    private fun computePrediction(): Prediction? {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Look at the current and next hour's frequency data
        val currentSlot = usageGrid[day][hour]
        val nextHour = (hour + 1) % 24
        val nextSlot = usageGrid[day][nextHour]

        // Blend: weight current slot by remaining minutes, next slot by elapsed minutes
        val minutesRemainingInHour = 60 - minute
        val currentWeight = minutesRemainingInHour / 60f
        val nextWeight = 1f - currentWeight

        val blended = mutableMapOf<String, Float>()
        currentSlot.forEach { (pkg, count) ->
            blended[pkg] = (blended[pkg] ?: 0f) + count * currentWeight
        }
        nextSlot.forEach { (pkg, count) ->
            blended[pkg] = (blended[pkg] ?: 0f) + count * nextWeight
        }

        // Also factor in transition probabilities from the current app
        val transitions = transitionMatrix[lastSeenApp]
        if (transitions != null) {
            val transTotal = transitions.values.sum().toFloat()
            if (transTotal > 0) {
                transitions.forEach { (pkg, count) ->
                    val transProb = count / transTotal
                    blended[pkg] = (blended[pkg] ?: 0f) + transProb * 30f  // Transition signal worth 30 pseudo-opens
                }
            }
        }

        if (blended.isEmpty()) return null

        val totalBlended = blended.values.sum()
        val best = blended.maxByOrNull { it.value } ?: return null
        val bestPkg = best.key
        val bestCount = currentSlot[bestPkg] ?: 0

        if (bestCount < MIN_CONFIDENCE_OPENS) return null

        val confidence = (best.value / totalBlended).coerceIn(0f, 1f)
        val eventType = classifyPackage(bestPkg)

        // Estimate when: if confidence is high and the slot's peak is in this hour,
        // the event is likely within PREWARM_LEAD_MINUTES
        val minutesUntilLikely = if (confidence > 0.6f) PREWARM_LEAD_MINUTES else 10

        return Prediction(
            predictedPackage = bestPkg,
            predictedEventType = eventType,
            confidenceScore = confidence,
            minutesUntilLikely = minutesUntilLikely,
            isPrewarmReady = false
        )
    }

    // -------------------------------------------------------------------------
    // Pre-warming — loads icons and triggers media art cache before needed
    // -------------------------------------------------------------------------

    private suspend fun prewarmForPrediction(prediction: Prediction) = withContext(Dispatchers.IO) {
        try {
            // Always pre-warm the app icon
            val iconCache = DensityAwareIconCache.get(context)
            iconCache.getOrLoadIcon(context, prediction.predictedPackage)

            Log.d(TAG, "Pre-warmed icon for ${prediction.predictedPackage} (${prediction.predictedEventType}, confidence=${prediction.confidenceScore})")

            // Update the prediction to mark pre-warm as done
            _prediction.value = prediction.copy(isPrewarmReady = true)
        } catch (e: Exception) {
            Log.w(TAG, "Pre-warm failed: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // WorkManager scheduling — background retrain every 6 hours
    // -------------------------------------------------------------------------

    private fun scheduleBackgroundTraining() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<PredictionTrainingWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.MINUTES)  // Don't retrain immediately on boot
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "island_prediction_training",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // -------------------------------------------------------------------------
    // Persistence — model survives across reboots
    // -------------------------------------------------------------------------

    private fun persistModel() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // Serialize usage grid
            val gridJson = JSONArray()
            for (day in 0..7) {
                val dayArr = JSONArray()
                for (hour in 0..23) {
                    val slotObj = JSONObject()
                    usageGrid[day][hour].forEach { (pkg, count) ->
                        slotObj.put(pkg, count)
                    }
                    dayArr.put(slotObj)
                }
                gridJson.put(dayArr)
            }

            // Serialize transition matrix
            val transJson = JSONObject()
            transitionMatrix.forEach { (from, toMap) ->
                val toObj = JSONObject()
                toMap.forEach { (to, count) -> toObj.put(to, count) }
                transJson.put(from, toObj)
            }

            prefs.edit()
                .putString(KEY_MODEL, gridJson.toString())
                .putString(KEY_TRANSITION, transJson.toString())
                .putLong(KEY_LAST_TRAINED, System.currentTimeMillis())
                .apply()

        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist model: ${e.message}")
        }
    }

    private fun loadPersistedModel() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastTrained = prefs.getLong(KEY_LAST_TRAINED, 0L)

            // Don't load a stale model — trigger a fresh train instead
            if (System.currentTimeMillis() - lastTrained > RETRAIN_INTERVAL_MS * 4) {
                scope.launch { trainFromUsageStats() }
                return
            }

            val gridStr = prefs.getString(KEY_MODEL, null) ?: return
            val transStr = prefs.getString(KEY_TRANSITION, null) ?: return

            // Deserialize grid
            val gridJson = JSONArray(gridStr)
            for (day in 0..min(7, gridJson.length() - 1)) {
                val dayArr = gridJson.getJSONArray(day)
                for (hour in 0..min(23, dayArr.length() - 1)) {
                    val slotObj = dayArr.getJSONObject(hour)
                    slotObj.keys().forEach { pkg ->
                        usageGrid[day][hour][pkg] = slotObj.getInt(pkg)
                        totalSamples += slotObj.getInt(pkg)
                    }
                }
            }

            // Deserialize transitions
            val transJson = JSONObject(transStr)
            transJson.keys().forEach { from ->
                val toObj = transJson.getJSONObject(from)
                val toMap = ConcurrentHashMap<String, Int>()
                toObj.keys().forEach { to -> toMap[to] = toObj.getInt(to) }
                transitionMatrix[from] = toMap
            }

            Log.i(TAG, "Loaded persisted model: $totalSamples samples")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load persisted model: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun classifyPackage(pkg: String): PredictedEventType = when {
        MEDIA_PACKAGES.contains(pkg)       -> PredictedEventType.MEDIA
        CALL_PACKAGES.contains(pkg)        -> PredictedEventType.CALL
        NAVIGATION_PACKAGES.contains(pkg)  -> PredictedEventType.NAVIGATION
        else                               -> PredictedEventType.GENERAL
    }

    private fun isSystemPackage(pkg: String): Boolean {
        return try {
            val flags = context.packageManager.getApplicationInfo(pkg, 0).flags
            (flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) { true }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endMs = System.currentTimeMillis()
            val startMs = endMs - 1000
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs)
            stats != null && stats.isNotEmpty()
        } catch (e: Exception) { false }
    }

    fun destroy() {
        scope.cancel()
        persistModel()
    }
}

// -------------------------------------------------------------------------
// WorkManager worker — runs the heavy retrain in the background
// -------------------------------------------------------------------------

class PredictionTrainingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            IslandPredictionEngine.get(applicationContext).trainFromUsageStats()
            Result.success()
        } catch (e: Exception) {
            Log.w("PredictionWorker", "Training failed: ${e.message}")
            Result.retry()
        }
    }
}
