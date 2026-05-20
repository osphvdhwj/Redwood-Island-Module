package com.example.dynamicisland.ui.state

import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.IslandTheme
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsState

/**
 * Single source of truth for the Dynamic Island UI.
 * This class is immutable and represents the entire visual state.
 */
data class IslandUiState(
    val islandState: IslandState = IslandState.HIDDEN,
    val activeModel: LiveActivityModel? = null,
    val splitModel: LiveActivityModel? = null,
    val queue: List<LiveActivityModel> = emptyList(),
    
    // Hardware State
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val volume: Int = 0,
    val brightness: Int = 0,
    val isAutoBrightness: Boolean = false,
    val ringerMode: Int = 2, // AudioManager.RINGER_MODE_NORMAL
    
    // Media Ticker
    val mediaPositionMs: Long = 0L,
    
    // Gaming Stats
    val gamingFps: Float = 0f,
    val gamingFrameMs: Float = 0f,
    val gamingJankPct: Float = 0f,
    
    // Settings & Theme
    val settings: SettingsState = SettingsState(),
    val theme: IslandTheme = IslandTheme(),
    
    // UI Dimensions
    val displayCutoutWidth: Float = 0f,
    
    // UI Interactions
    val isExpanded: Boolean = false,
    
    // Performance & Lifecycle
    val isScreenOn: Boolean = true,
    val isPowerSaveMode: Boolean = false
)
