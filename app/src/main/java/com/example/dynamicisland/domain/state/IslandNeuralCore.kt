package com.example.dynamicisland.domain.state

import android.content.Context
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.ui.mvi.IslandIntent
import com.example.dynamicisland.ui.mvi.IslandUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * 🧠 ISLAND NEURAL CORE (iNC)
 *
 * The definitive state authority and intelligence engine for the Redwood project.
 *
 * ## Responsibilities:
 * 1. **State Authority**: Serves as the Single Source of Truth (SSoT) for all UI components.
 * 2. **UDF Implementation**: Processes all changes via the Unidirectional Data Flow pattern (Intent -> Reducer -> State).
 * 3. **Intelligence Layer**: Implements a lightweight, on-device reinforcement learning engine to predict user gestures.
 * 4. **Persistence**: Ensures system-level state survives SystemUI process churn via JSON snapshots.
 *
 * ## Thread Safety:
 * Employs a combination of `MutableStateFlow` (for atomic state updates) and `Mutex` (for safe filesystem I/O).
 *
 * @property context Hilt-injected application context for internal storage access.
 */
@Singleton
class IslandNeuralCore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val weightsFile = File(context.filesDir, "neural_weights.json")
    private val stateSnapshotFile = File(context.filesDir, "island_state_snapshot.json")
    private val mutex = Mutex()
    private var weights = JSONObject()

    private val _uiState = MutableStateFlow(loadStateSnapshot())
    
    /**
     * The reactive UI state observable by any component in the SystemUI process.
     */
    val uiState = _uiState.asStateFlow()

    init {
        loadWeights()
    }

    private fun loadWeights() {
        if (weightsFile.exists()) {
            try { 
                weights = JSONObject(weightsFile.readText()) 
            } catch (e: Exception) { 
                weights = JSONObject() 
            }
        }
    }

    private fun saveWeights() {
        scope.launch {
            mutex.withLock {
                try {
                    val data = weights.toString(2)
                    weightsFile.writeText(data)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Dispatches a new intent to the state machine.
     * This is the ONLY entry point for modifying the Island state.
     *
     * @param intent The logical action to be processed (e.g., UpdateBattery, SyncState).
     */
    fun dispatch(intent: IslandIntent) {
        _uiState.update { currentState ->
            val newState = reduce(currentState, intent)
            if (newState != currentState) {
                saveStateSnapshot(newState)
            }
            newState
        }
    }

    /**
     * PURE REDUCER FUNCTION
     *
     * Computes the next state based on the current state and the incoming intent.
     * NO side-effects allowed here (except for specialized coroutine triggers like BatteryPulse).
     */
    private fun reduce(currentState: IslandUiState, intent: IslandIntent): IslandUiState {
        return when (intent) {
            is IslandIntent.SyncState -> currentState.copy(
                islandState = intent.state,
                activeModel = intent.activeModel,
                splitModel = intent.splitModel
            )
            is IslandIntent.UpdateBattery -> currentState.copy(
                batteryLevel = intent.level,
                isCharging = intent.isCharging
            )
            is IslandIntent.BatteryPulse -> {
                scope.launch {
                    _uiState.update { it.copy(isBatteryPulsing = true) }
                    delay(1000)
                    _uiState.update { it.copy(isBatteryPulsing = false) }
                }
                currentState.copy(batteryLevel = intent.level)
            }
            is IslandIntent.UpdateScreenState -> currentState.copy(isScreenOn = intent.isScreenOn)
            is IslandIntent.UpdateGamingStats -> currentState.copy(
                gamingFps = intent.fps,
                gamingFrameMs = intent.frameMs,
                gamingJankPct = intent.jankPct,
                gamingCpuUsage = intent.cpuUsage,
                gamingGpuUsage = intent.gpuUsage
            )
            is IslandIntent.UpdateSettings -> currentState.copy(settings = intent.settings)
            is IslandIntent.UpdateTheme -> currentState.copy(theme = intent.theme)
            is IslandIntent.ToggleExpand -> currentState.copy(isExpanded = !currentState.isExpanded)
            is IslandIntent.Collapse -> currentState.copy(isExpanded = false)
            is IslandIntent.DismissActive -> {
                val nextModel = currentState.queue.firstOrNull()
                currentState.copy(
                    activeModel = nextModel,
                    queue = currentState.queue.drop(1),
                    islandState = if (nextModel != null) mapModelToState(nextModel) else IslandState.HIDDEN
                )
            }
            else -> currentState
        }
    }

    /**
     * Map model types to physical Island dimensions.
     */
    private fun mapModelToState(model: LiveActivityModel): IslandState {
        return when (model) {
            is LiveActivityModel.Music -> IslandState.TYPE_2_MID
            is LiveActivityModel.Call -> IslandState.TYPE_2_MID
            is LiveActivityModel.Charging -> IslandState.TYPE_2_MID
            is LiveActivityModel.SystemAlert -> IslandState.TYPE_2_MID
            is LiveActivityModel.HardwareMonitor -> IslandState.TYPE_1_MINI
            is LiveActivityModel.Dashboard -> IslandState.TYPE_3_MAX
            is LiveActivityModel.Otp -> IslandState.TYPE_2_MID
            is LiveActivityModel.TimerEvent -> IslandState.TYPE_2_MID
            else -> IslandState.TYPE_1_MINI
        }
    }

    private fun saveStateSnapshot(state: IslandUiState) {
        scope.launch {
            mutex.withLock {
                try {
                    val json = JSONObject().apply {
                        put("islandState", state.islandState.name)
                        put("batteryLevel", state.batteryLevel)
                        put("isCharging", state.isCharging)
                        put("isScreenOn", state.isScreenOn)
                    }
                    stateSnapshotFile.writeText(json.toString())
                } catch (e: Exception) {}
            }
        }
    }

    private fun loadStateSnapshot(): IslandUiState {
        if (!stateSnapshotFile.exists()) return IslandUiState()
        return try {
            val json = JSONObject(stateSnapshotFile.readText())
            IslandUiState(
                islandState = IslandState.valueOf(json.optString("islandState", IslandState.HIDDEN.name)),
                batteryLevel = json.optInt("batteryLevel", 100),
                isCharging = json.optBoolean("isCharging", false),
                isScreenOn = json.optBoolean("isScreenOn", true)
            )
        } catch (e: Exception) {
            IslandUiState()
        }
    }

    /**
     * Reinforce a positive interaction in the prediction model.
     *
     * @param pkg Target application package.
     * @param gesture The gesture performed (SWIPE_LEFT, etc.).
     * @param action The resulting action to be prioritized in the future.
     */
    fun reinforce(pkg: String, islandState: String, isMediaPlaying: Boolean, gesture: String, action: String) {
        val contextKey = "$pkg|$islandState|$isMediaPlaying"
        val contextObj = weights.optJSONObject(contextKey) ?: JSONObject().also { weights.put(contextKey, it) }
        val gestureObj = contextObj.optJSONObject(gesture) ?: JSONObject().also { contextObj.put(gesture, it) }
        
        val currentScore = gestureObj.optInt(action, 0)
        gestureObj.put(action, currentScore + 1)
        
        val keys = gestureObj.keys()
        while(keys.hasNext()) {
            val key = keys.next()
            if (key != action) {
                val score = gestureObj.getInt(key)
                if (score > 0) gestureObj.put(key, score - 1)
            }
        }
        
        saveWeights()
    }

    /**
     * Predict the best action for a given context and gesture.
     *
     * @return The action ID with the highest confidence score, or null if below threshold.
     */
    fun predict(pkg: String, islandState: String, isMediaPlaying: Boolean, gesture: String, threshold: Int = 10): String? {
        val contextKey = "$pkg|$islandState|$isMediaPlaying"
        val contextObj = weights.optJSONObject(contextKey) ?: return null
        val gestureObj = contextObj.optJSONObject(gesture) ?: return null
        
        var bestAction: String? = null
        var maxScore = 0
        
        val keys = gestureObj.keys()
        while(keys.hasNext()) {
            val key = keys.next()
            val score = gestureObj.getInt(key)
            if (score > maxScore) {
                maxScore = score
                bestAction = key
            }
        }
        
        return if (maxScore >= threshold) bestAction else null
    }

    /**
     * Wipes all learned behavioral data.
     */
    fun clearMemory() {
        weights = JSONObject()
        saveWeights()
    }

    /**
     * Exports the neural weights as a JSON string for debugging.
     */
    fun exportData(): String? {
        return try { weights.toString(4) } catch (e: Exception) { null }
    }
}
