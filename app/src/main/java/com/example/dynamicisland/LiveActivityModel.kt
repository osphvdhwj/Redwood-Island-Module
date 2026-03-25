package com.example.dynamicisland

import android.graphics.Bitmap

enum class ActivityType { CALL, NAVIGATION, TIMER, MESSAGE, ALARM, CHARGING, BATTERY_LOW, BLUETOOTH, WIFI, HARDWARE, ONGOING_TASK }
enum class IslandState { HIDDEN, TYPE_0_RING, TYPE_1_MINI, TYPE_2_MID, TYPE_3_MAX, TYPE_SPLIT, TYPE_CUBE }
enum class IslandGesture { SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN }
enum class IslandAction { NONE, PLAY_PAUSE, NEXT_TRACK, PREV_TRACK, VOL_UP, VOL_DOWN, OPEN_APP, EXPAND, COLLAPSE, OPEN_DASHBOARD, HEART_SONG }

data class CustomMediaAction(val actionName: String, val iconBitmap: Bitmap?, val iconResId: Int?, val isEnabled: Boolean)

sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean
    abstract val isCritical: Boolean
    abstract val isSensitive: Boolean

    data class General(override val id: String, override val type: ActivityType, val title: String, val dataText: String, val accentColor: Int = android.graphics.Color.WHITE, val progress: Float? = null, override val isTransient: Boolean = true, override val isCritical: Boolean = false, override val isSensitive: Boolean = false) : LiveActivityModel()
    
    data class Charging(override val id: String, override val type: ActivityType = ActivityType.CHARGING, val level: Int, val isPluggedIn: Boolean, override val isTransient: Boolean = true, override val isCritical: Boolean = false, override val isSensitive: Boolean = false) : LiveActivityModel()

    // Add this inside your LiveActivityModel sealed class
 data class Call(
        override val id: String = "sys_call",
        override val type: ActivityType = ActivityType.ONGOING_TASK,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true,
        val callerName: String = "Unknown Caller", // 🎛️ NEW: Caller Name Slot
        val state: String = "ONGOING", 
        val startTime: Long = System.currentTimeMillis()
    ) : LiveActivityModel()

    data class Music(
        override val id: String, 
        override val type: ActivityType = ActivityType.MESSAGE, 
        val title: String, 
        val artist: String, 
        val albumArt: Bitmap?, 
        val blurredAlbumArt: Bitmap?, 
        val appIcon: Bitmap?, 
        val dominantColor: Int?, 
        val titleTextColor: Int, 
        val isPlaying: Boolean, 
        val durationMs: Long, 
        val positionMs: Long, 
        val appPackageName: String, 
        val customActions: List<CustomMediaAction> = emptyList(), 
        val isShuffled: Boolean = false,
        val repeatMode: Int = 0,
        val isLiked: Boolean = false,
        override val isTransient: Boolean = false, 
        override val isCritical: Boolean = false, 
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class HardwareMonitor(override val id: String, override val type: ActivityType = ActivityType.HARDWARE, val cpuTempCelsius: Float, val cpuFreqMhz: Int, val isGamingModeOn: Boolean, override val isTransient: Boolean = false, override val isCritical: Boolean = false, override val isSensitive: Boolean = false) : LiveActivityModel()

    data class SystemAlert(override val id: String, override val type: ActivityType = ActivityType.ALARM, val alertType: String, val title: String, val message: String, val alertColor: Int, override val isTransient: Boolean = true, override val isCritical: Boolean = true, override val isSensitive: Boolean = false) : LiveActivityModel()

    data class Dashboard(override val id: String = "dashboard", override val type: ActivityType = ActivityType.HARDWARE, override val isTransient: Boolean = false, override val isCritical: Boolean = false, override val isSensitive: Boolean = true) : LiveActivityModel()

    data class AppTimerWarning(override val id: String = "app_timer", override val type: ActivityType = ActivityType.TIMER, val packageName: String, val appName: String, val appIcon: Bitmap?, val targetTimeMs: Long, override val isTransient: Boolean = true, override val isCritical: Boolean = true, override val isSensitive: Boolean = false) : LiveActivityModel()

    data class RealityPill(override val id: String = "reality_pill", override val type: ActivityType = ActivityType.TIMER, val appName: String, val sessionMinutes: Int, override val isTransient: Boolean = true, override val isCritical: Boolean = false, override val isSensitive: Boolean = false) : LiveActivityModel()

    data class OngoingTask(override val id: String = "ongoing_task", override val type: ActivityType = ActivityType.ONGOING_TASK, val pkgName: String, val title: String, val text: String, val progress: Int, val progressMax: Int, override val isTransient: Boolean = true, override val isCritical: Boolean = false, override val isSensitive: Boolean = false) : LiveActivityModel()
}
