package com.example.dynamicisland.core.domain.state

import android.content.Context
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.core.model.IslandUiState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.PerformanceLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * 🧠 ISLAND NEURAL CORE (iNC) - RELOCATED TO CORE DAEMON
 *
 * The definitive state authority and intelligence engine for the Redwood project.
 */
@Singleton
class IslandNeuralCore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val stateSnapshotFile = File(context.filesDir, "island_state_snapshot.json")
    private val mutex = Mutex()
    private var weights = JSONObject()

    private val _uiState = MutableStateFlow(loadStateSnapshot())
    val uiState = _uiState.asStateFlow()

    private val _intentFlow = MutableSharedFlow<IslandIntent>(extraBufferCapacity = 64)
    val intentFlow = _intentFlow.asSharedFlow()

    fun dispatch(intent: IslandIntent) {
        scope.launch { _intentFlow.emit(intent) }
        _uiState.update { currentState ->
            val newState = reduce(currentState, intent)
            if (newState != currentState) {
                saveStateSnapshot(newState)
            }
            newState
        }
    }

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
            is IslandIntent.UpdatePerformanceLevel -> currentState.copy(
                performanceLevel = intent.level
            )
            is IslandIntent.ToggleUltraBattery -> currentState.copy(isUltraBatteryActive = intent.enable)
            is IslandIntent.ToggleThermalBypass -> currentState.copy(isThermalBypassActive = intent.enable)
            is IslandIntent.UpdateSettings -> currentState.copy(settings = intent.settings)
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

    private fun mapModelToState(model: LiveActivityModel): IslandState {
        return when (model) {
            is LiveActivityModel.Music -> IslandState.TYPE_2_MID
            is LiveActivityModel.Call -> IslandState.TYPE_2_MID
            is LiveActivityModel.Charging -> IslandState.TYPE_2_MID
            is LiveActivityModel.SystemAlert -> IslandState.TYPE_2_MID
            is LiveActivityModel.HardwareMonitor -> IslandState.TYPE_1_MINI
            is LiveActivityModel.Dashboard -> IslandState.TYPE_3_MAX
            else -> IslandState.TYPE_1_MINI
        }
    }

    private fun saveStateSnapshot(state: IslandUiState) {
        scope.launch {
            mutex.withLock {
                try {
                    val json = JSONObject().apply {
                        put("islandState", state.islandState.name)
                        put("performanceLevel", state.performanceLevel.name)
                        put("batteryLevel", state.batteryLevel)
                        put("isCharging", state.isCharging)
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
                performanceLevel = PerformanceLevel.valueOf(json.optString("performanceLevel", PerformanceLevel.BALANCED.name)),
                batteryLevel = json.optInt("batteryLevel", 100),
                isCharging = json.optBoolean("isCharging", false)
            )
        } catch (e: Exception) {
            IslandUiState()
        }
    }
}
