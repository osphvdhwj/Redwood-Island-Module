package com.example.dynamicisland.core.ui.mvi

import androidx.lifecycle.ViewModel
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.model.IslandUiState
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
/**
 * Central ViewModel for managing the Dynamic Island state using MVI.
 * Now acts as a thin wrapper around IslandNeuralCore's central state.
 */
@HiltViewModel
class IslandViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val neuralCore: IslandNeuralCore
) : ViewModel() {
    val uiState: StateFlow<IslandUiState> = neuralCore.uiState
    /**
     * Entry point for all state changes.
     * Delegates to IslandNeuralCore for centralized processing.
     */
    fun handleIntent(intent: IslandIntent) {
        neuralCore.dispatch(intent)
    }
}
