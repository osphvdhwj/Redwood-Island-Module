package com.example.dynamicisland

import android.app.PendingIntent
import android.graphics.Bitmap

enum class IslandState {
    TYPE_0_RING, TYPE_1_MINI, TYPE_2_MID, TYPE_3_MAX, TYPE_CUBE, TYPE_SPLIT, HIDDEN
}

enum class ActivityType {
    CALL, MEDIA, NAVIGATION, TIMER, MESSAGE, ALARM, DASHBOARD, BATTERY_LOW, CHARGING, WIFI, BLUETOOTH, HARDWARE, SYSTEM_ALERT
}

enum class IslandGesture {
    SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN
}

enum class IslandAction {
    NONE, PLAY_PAUSE, NEXT_TRACK, PREV_TRACK, VOL_UP, VOL_DOWN, EXPAND, COLLAPSE, OPEN_APP, HEART_SONG, TOGGLE_TORCH
}

data class CustomMediaAction(
    val actionName: String,
    val icon: Bitmap?,
    val pendingIntent: PendingIntent?,
    val isEnabled: Boolean
)

sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean

    // 🚀 NEW: Universal System Alert Model for Ecosystem IPC
    // 1. Updated System Alert (Now accepts dynamic Color Hex)
    data class SystemAlert(
        override val id: String,
        override val type: ActivityType = ActivityType.SYSTEM_ALERT,
        val alertType: String, // e.g., "THERMAL", "ROGUE"
        val title: String,
        val message: String,
        val alertColor: Int, // Will be parsed from Hex
        override val isTransient: Boolean = true
    ) : LiveActivityModel()

    // 2. NEW: The Reality Pill Tick
    data class RealityPill(
        override val id: String = "sys_reality_tick",
        override val type: ActivityType = ActivityType.SYSTEM_ALERT,
        val appName: String,
        val sessionMinutes: Int,
        override val isTransient: Boolean = true
    ) : LiveActivityModel()
    // 🚀 NEW: The 60-Second Execution Warning
    data class AppTimerWarning(
        override val id: String = "sys_app_timer",
        override val type: ActivityType = ActivityType.SYSTEM_ALERT,
        val packageName: String,
        val appName: String,
        val appIcon: Bitmap?,
        val targetTimeMs: Long,
        override val isTransient: Boolean = true
    ) : LiveActivityModel()


    data class Music(
        override val id: String = "sys_media",
        override val type: ActivityType = ActivityType.MEDIA,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val appIcon: Bitmap? = null,
        val dominantColor: Int? = null,
        val titleTextColor: Int = android.graphics.Color.WHITE, 
        val isPlaying: Boolean = false,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val appPackageName: String = "",
        val launchIntent: PendingIntent? = null,
        val customActions: List<CustomMediaAction> = emptyList(),
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class General(
        override val id: String,
        override val type: ActivityType,
        val title: String,
        val dataText: String,
        val progress: Float? = null,
        val accentColor: Int = android.graphics.Color.WHITE,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class Dashboard(
        override val id: String = "sys_dash",
        override val type: ActivityType = ActivityType.DASHBOARD,
        val isWifiOn: Boolean = true,
        val isBluetoothOn: Boolean = false,
        val isTorchOn: Boolean = false,
        val currentVolume: Float = 0.5f,
        val ringerMode: Int = android.media.AudioManager.RINGER_MODE_NORMAL,
        val currentBrightness: Float = 0.5f,
        val isAutoBrightness: Boolean = false,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class Charging(
        override val id: String,
        override val type: ActivityType = ActivityType.CHARGING,
        val level: Int,
        val isPluggedIn: Boolean,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class HardwareMonitor(
        override val id: String = "sys_hw",
        override val type: ActivityType = ActivityType.HARDWARE,
        val cpuTempCelsius: Float = 0f,
        val cpuFreqMhz: Int = 0,
        val isGamingModeOn: Boolean = false,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()
}
