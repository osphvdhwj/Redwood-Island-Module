package com.example.dynamicisland.shared.model

import android.app.Notification
import android.os.Bundle
import com.example.dynamicisland.shared.settings.SettingsState
import com.example.dynamicisland.shared.model.LiveActivityModel

/**
 * Intents representing user or system actions that affect the Dynamic Island.
 * Clean version for Shared module.
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
    
    data class UpdateDisplayCutout(val width: Float) : IslandIntent()

    object DismissActive : IslandIntent()
    object ToggleExpand : IslandIntent()
    object Collapse : IslandIntent()
    
    // External App Events
    data class AppChanged(val pkg: String) : IslandIntent()
    data class NotificationCaught(val pkg: String, val notification: Notification) : IslandIntent()
    data class MediaSync(val pkg: String, val song: String, val artist: String, val isPlaying: Boolean) : IslandIntent()
    data class CallStateChanged(val state: String, val caller: String, val number: String) : IslandIntent()

    // Advanced Daemon Hooks
    data class UpdateCameraState(val isActive: Boolean) : IslandIntent()
    data class UpdateThermalState(val cpuTemp: Float) : IslandIntent()
    data class UpdateForegroundApp(val pkg: String) : IslandIntent()
    data class UpdateRefreshRate(val hz: Int) : IslandIntent()
    data class UpdateBatteryStats(val dischargeRate: Int) : IslandIntent()
    data class UpdateNetworkStats(val txSpeed: Long, val rxSpeed: Long) : IslandIntent()

    // Performance & Lifecycle
    data class UpdateScreenState(val isScreenOn: Boolean) : IslandIntent()
    data class UpdatePowerSaveMode(val isPowerSaveMode: Boolean) : IslandIntent()
    data class UpdatePerformanceLevel(val level: PerformanceLevel) : IslandIntent()
    data class UpdateMicState(val isMicActive: Boolean) : IslandIntent()
    data class UpdatePerAppVolumeState(val isActive: Boolean) : IslandIntent()
    object CleanupStorage : IslandIntent()
    object FreezeBackground : IslandIntent()
    data class ToggleUltraBattery(val enable: Boolean) : IslandIntent()
    data class ToggleThermalBypass(val enable: Boolean) : IslandIntent()
    data class ToggleCalibration(val enabled: Boolean, val targetState: String? = null) : IslandIntent()
}
