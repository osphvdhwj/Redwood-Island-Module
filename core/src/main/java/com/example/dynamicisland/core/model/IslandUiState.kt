package com.example.dynamicisland.core.model
import com.example.dynamicisland.core.model.IslandUiState

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*

/**
 * Single source of truth for the Dynamic Island UI.
 * This class is immutable and represents the entire visual state.
import com.example.dynamicisland.shared.model.*
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
    val gamingCpuUsage: Int = 0,
    val gamingGpuUsage: Int = 0,
    val performanceLevel: PerformanceLevel = PerformanceLevel.BALANCED,
    val isUltraBatteryActive: Boolean = false,
    val isThermalBypassActive: Boolean = false,
    
    // Settings & Theme
    val settings: SettingsState = SettingsState(),
    val theme: IslandTheme = IslandTheme(),
    
    // UI Dimensions
    val displayCutoutWidth: Float = 0f,
    
    // UI Interactions
    val isExpanded: Boolean = false,
    
    // Performance & Lifecycle
    val isScreenOn: Boolean = true,
    val isPowerSaveMode: Boolean = false,
    val isBatteryPulsing: Boolean = false
)
