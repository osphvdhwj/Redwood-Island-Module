package com.example.dynamicisland.core.system.hook

import com.example.dynamicisland.shared.model.LiveActivityModel
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the contract for system event providers.
 * This allows the island to remain ROM-agnostic by abstracting
 * where and how system events are intercepted.
 */
interface SystemEventProvider {
    
    /**
     * Flow of new activities intercepted from the system (Notifications, OTPs, etc).
     */
    val activityEvents: Flow<LiveActivityModel>

    /**
     * Flow of hardware status changes (Battery, Volume, Ringer, Torch).
     */
    val hardwareEvents: Flow<HardwareEvent>

    /**
     * Flow of media metadata and playback state changes.
     */
    val mediaEvents: Flow<LiveActivityModel.Music>

    /**
     * Flow of telecom/call events.
     */
    val callEvents: Flow<LiveActivityModel.Call>

    /**
     * Initializes the provider with the system classloader (for Xposed hooks).
     */
    fun initialize(classLoader: ClassLoader)
}

/**
 * Sealed class for hardware-specific events that don't always 
 * warrant a full LiveActivityModel update.
 */
sealed class HardwareEvent {
    data class BatteryChanged(val level: Int, val isCharging: Boolean) : HardwareEvent()
    data class VolumeChanged(val streamType: Int, val level: Int, val max: Int) : HardwareEvent()
    data class RingerModeChanged(val mode: Int) : HardwareEvent()
    data class TorchChanged(val enabled: Boolean) : HardwareEvent()
    data class GamingStatsChanged(val fps: Float, val frameMs: Float, val jankPct: Float) : HardwareEvent()
}
