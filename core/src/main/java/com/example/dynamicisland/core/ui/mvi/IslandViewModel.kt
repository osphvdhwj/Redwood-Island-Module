package com.example.dynamicisland.core.ui.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.core.system.hook.SystemEventProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Central ViewModel for managing the Dynamic Island state using MVI.
 * Now acts as a thin wrapper around IslandNeuralCore's central state.
 */
@HiltViewModel
class IslandViewModel @Inject constructor(
    private val eventProvider: SystemEventProvider,
    private val settingsManager: SettingsManager,
    private val neuralCore: IslandNeuralCore
) : ViewModel() {

    val uiState: StateFlow<IslandUiState> = neuralCore.uiState

    init {
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
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Entry point for all state changes.
     * Delegates to IslandNeuralCore for centralized processing.
     */
    fun handleIntent(intent: IslandIntent) {
        neuralCore.dispatch(intent)
    }
}
