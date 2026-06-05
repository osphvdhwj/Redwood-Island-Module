package com.example.dynamicisland.shared.model

import android.graphics.Bitmap
import android.os.Bundle

/**
 * Clean version of LiveActivityModel for shared usage.
 * Removed Compose dependencies.
 */
sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean
    abstract val isCritical: Boolean
    abstract val isSensitive: Boolean

    data class General(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val title: String,
        val dataText: String = "",
        val accentColor: Int = -1, // WHITE
        val progress: Float? = null,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class Charging(
        override val id: String,
        override val type: ActivityType = ActivityType.CHARGING,
        val level: Int,
        val isPluggedIn: Boolean,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class Call(
        override val id: String = "sys_call",
        override val type: ActivityType = ActivityType.CALL,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true,
        val callerName: String = "Unknown Caller",
        val phoneNumber: String? = null,
        val state: String = "ONGOING",
        val startTime: Long = System.currentTimeMillis(),
        val sourceApp: String? = null,
        val photoUri: String? = null,
        val contactPhoto: Bitmap? = null,
        val relationLabel: String? = null,
        val isSpam: Boolean = false
    ) : LiveActivityModel()

    data class Music(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val albumArtUri: String? = null,
        val blurredAlbumArt: Bitmap? = null,
        val appIcon: Bitmap? = null,
        val dominantColor: Int? = null,
        val titleTextColor: Int = -1, // WHITE
        val isPlaying: Boolean,
        val durationMs: Long,
        val positionMs: Long,
        val appPackageName: String,
        val customActions: List<CustomMediaAction> = emptyList(),
        val isShuffled: Boolean = false,
        val repeatMode: Int = 0,
        val isLiked: Boolean = false,
        val isVideo: Boolean = false,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel() {
        val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    data class HardwareMonitor(
        override val id: String,
        override val type: ActivityType = ActivityType.HARDWARE,
        val cpuTempCelsius: Float,
        val cpuFreqMhz: Int,
        val isGamingModeOn: Boolean,
        val fps: Float = 0f,
        val frameMs: Float = 0f,
        val jankPct: Float = 0f,
        val ramFreeBytes: Long = 0,
        val batteryCycles: Int = 0,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class SystemAlert(
        override val id: String,
        override val type: ActivityType = ActivityType.ALARM,
        val alertType: String,
        val title: String,
        val message: String,
        val alertColor: Int,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class Dashboard(
        val activeTiles: List<QSTileState> = emptyList(),
        val pinnedApps: List<String> = emptyList(),
        override val id: String = "dashboard",
        override val type: ActivityType = ActivityType.HARDWARE,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = true
    ) : LiveActivityModel()

    data class AppTimerWarning(
        override val id: String = "app_timer",
        override val type: ActivityType = ActivityType.TIMER,
        val packageName: String,
        val appName: String,
        val appIcon: Bitmap?,
        val targetTimeMs: Long,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class OngoingTask(
        override val id: String = "ongoing_task",
        override val type: ActivityType = ActivityType.ONGOING_TASK,
        val pkgName: String,
        val title: String,
        val text: String,
        val progress: Int,
        val progressMax: Int,
        val networkSpeed: String? = null,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class Otp(
        override val id: String = "sys_otp",
        override val type: ActivityType = ActivityType.MESSAGE,
        val code: String,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true
    ) : LiveActivityModel()

    data class TimerEvent(
        override val id: String = "sys_timer",
        override val type: ActivityType = ActivityType.TIMER,
        val remainingMillis: Long,
        val totalMillis: Long,
        val label: String = "Timer",
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Simplified Navigation for Shared
    data class Navigation(
        override val id: String = "sys_nav",
        override val type: ActivityType = ActivityType.NAVIGATION,
        val instruction: String,
        val distance: String,
        val nextTurnIconId: String? = null, // Use ID instead of ImageVector
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()
}

data class CustomMediaAction(
    val action: String,
    val icon: Bitmap?,
    val label: String
)

data class QSTileState(
    val tileName: String,
    val isActive: Boolean,
    val iconRes: Int,
    val isUnavailable: Boolean = false
)

enum class ActivityType {
    MESSAGE, ONGOING_TASK, CHARGING, HARDWARE, ALARM, TIMER, CALL, NAVIGATION, BATTERY_LOW, BLUETOOTH, WIFI, NONE
}
