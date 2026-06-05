package com.example.dynamicisland.core.ui.mvi

import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.core.model.IslandTheme
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.SettingsState

/**
 * Intents representing user or system actions that affect the Dynamic Island.
 */
sealed class IslandIntent {
    data class UpdateState(val state: IslandState) : IslandIntent()
    data class SyncState(val state: IslandState, val activeModel: LiveActivityModel?, val splitModel: LiveActivityModel?) : IslandIntent()
    data class NewActivity(val model: LiveActivityModel) : IslandIntent()
    data class RemoveActivity(val activityId: String) : IslandIntent()
    
    data class UpdateBattery(val level: Int, val isCharging: Boolean) : IslandIntent()
    data class BatteryPulse(val level: Int) : IslandIntent()
    data class UpdateVolume(val volume: Int) : IslandIntent()
    data class UpdateBrightness(val brightness: Int, val isAuto: Boolean) : IslandIntent()
    data class UpdateRingerMode(val mode: Int) : IslandIntent()
    
    data class UpdateMediaPosition(val positionMs: Long) : IslandIntent()
    data class UpdateGamingStats(
        val fps: Float, 
        val frameMs: Float, 
        val jankPct: Float,
        val cpuUsage: Int,
        val gpuUsage: Int
    ) : IslandIntent()
    
    data class UpdateSettings(val settings: SettingsState) : IslandIntent()
    data class UpdateTheme(val theme: IslandTheme) : IslandIntent()
    
    data class UpdateDisplayCutout(val width: Float) : IslandIntent()

    object DismissActive : IslandIntent()
    object ToggleExpand : IslandIntent()
    object Collapse : IslandIntent()
    
    // Performance & Lifecycle
    data class UpdateScreenState(val isScreenOn: Boolean) : IslandIntent()
    data class UpdatePowerSaveMode(val isPowerSaveMode: Boolean) : IslandIntent()
    data class ToggleCalibration(val enabled: Boolean, val targetState: String? = null) : IslandIntent()
}
