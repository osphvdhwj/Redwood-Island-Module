package com.example.dynamicisland.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.system.hook.SystemEventProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// 1. The Single Source of Truth
// Removed duplicate IslandUiState

/**
 * Central ViewModel for managing the Dynamic Island state using MVI.
 */
@HiltViewModel
class IslandViewModel @Inject constructor(
    private val eventProvider: SystemEventProvider,
    private val settingsManager: SettingsManager,
    private val eventBus: IslandEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(IslandUiState())
    val uiState: StateFlow<IslandUiState> = _uiState.asStateFlow()

    init {
        // Collect from the central event bus
        eventBus.intents
            .onEach { intent -> handleIntent(intent) }
            .launchIn(viewModelScope)

        // Pillar 5: Debounce hardware events to reduce CPU load and UI redraws
        eventProvider.hardwareEvents
            .debounce(250L) // Limit to 4 updates per second max
            .onEach { event ->
                when (event) {
                    is com.example.dynamicisland.system.hook.HardwareEvent.BatteryChanged -> {
                        handleIntent(IslandIntent.UpdateBattery(event.level, event.isCharging))
                    }
                    is com.example.dynamicisland.system.hook.HardwareEvent.GamingStatsChanged -> {
                        handleIntent(IslandIntent.UpdateGamingStats(event.fps, event.frameMs, event.jankPct))
                    }
                    // Handle other hardware events here as needed
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Entry point for all state changes.
     */
    fun handleIntent(intent: IslandIntent) {
        // Most state updates are simple and can be done immediately.
        // For complex logic involving side effects, use viewModelScope.launch.
        _uiState.update { currentState ->
            when (intent) {
                is IslandIntent.UpdateState -> currentState.copy(islandState = intent.state)
                is IslandIntent.SyncState -> currentState.copy(
                    islandState = intent.state,
                    activeModel = intent.activeModel,
                    splitModel = intent.splitModel
                )
                is IslandIntent.NewActivity -> processNewActivity(currentState, intent.model)
                is IslandIntent.RemoveActivity -> processRemoveActivity(currentState, intent.activityId)
                
                is IslandIntent.UpdateBattery -> currentState.copy(
                    batteryLevel = intent.level, 
                    isCharging = intent.isCharging
                )
                
                is IslandIntent.UpdateVolume -> currentState.copy(volume = intent.volume)
                
                is IslandIntent.UpdateBrightness -> currentState.copy(
                    brightness = intent.brightness, 
                    isAutoBrightness = intent.isAuto
                )
                
                is IslandIntent.UpdateRingerMode -> currentState.copy(ringerMode = intent.mode)
                
                is IslandIntent.UpdateMediaPosition -> currentState.copy(mediaPositionMs = intent.positionMs)
                
                is IslandIntent.UpdateGamingStats -> currentState.copy(
                    gamingFps = intent.fps,
                    gamingFrameMs = intent.frameMs,
                    gamingJankPct = intent.jankPct
                )
                
                is IslandIntent.UpdateSettings -> currentState.copy(settings = intent.settings)
                
                is IslandIntent.UpdateTheme -> currentState.copy(theme = intent.theme)
                
                is IslandIntent.UpdateDisplayCutout -> currentState.copy(displayCutoutWidth = intent.width)
                
                IslandIntent.DismissActive -> processDismissActive(currentState)
                
                IslandIntent.ToggleExpand -> currentState.copy(isExpanded = !currentState.isExpanded)
                
                IslandIntent.Collapse -> currentState.copy(isExpanded = false)
                
                is IslandIntent.UpdateScreenState -> currentState.copy(isScreenOn = intent.isScreenOn)
                
                is IslandIntent.UpdatePowerSaveMode -> currentState.copy(isPowerSaveMode = intent.isPowerSaveMode)
                
                is IslandIntent.ToggleCalibration -> currentState // Handled via side-effects or direct view state
            }
        }
    }

    private fun processNewActivity(currentState: IslandUiState, model: LiveActivityModel): IslandUiState {
        // Priority logic: if it's critical, make it active. Otherwise, queue it.
        // If nothing is active, make it active.
        return if (currentState.activeModel == null || model.isCritical) {
            currentState.copy(
                activeModel = model,
                // If we displaced an active model, move it to the queue
                queue = currentState.activeModel?.let { listOf(it) + currentState.queue } ?: currentState.queue,
                islandState = mapModelToState(model)
            )
        } else {
            currentState.copy(queue = currentState.queue + model)
        }
    }

    private fun processRemoveActivity(currentState: IslandUiState, activityId: String): IslandUiState {
        val newQueue = currentState.queue.filter { it.id != activityId }
        return if (currentState.activeModel?.id == activityId) {
            val nextModel = newQueue.firstOrNull()
            currentState.copy(
                activeModel = nextModel,
                queue = if (nextModel != null) newQueue.drop(1) else newQueue,
                islandState = if (nextModel != null) mapModelToState(nextModel) else IslandState.HIDDEN
            )
        } else {
            currentState.copy(queue = newQueue)
        }
    }

    private fun processDismissActive(currentState: IslandUiState): IslandUiState {
        val nextModel = currentState.queue.firstOrNull()
        return currentState.copy(
            activeModel = nextModel,
            queue = currentState.queue.drop(1),
            islandState = if (nextModel != null) mapModelToState(nextModel) else IslandState.HIDDEN
        )
    }

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
}
