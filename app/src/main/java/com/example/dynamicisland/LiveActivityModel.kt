package com.example.dynamicisland

import android.graphics.Bitmap

enum class ActivityType { DEFAULT, BATTERY_LOW, CALL, ALARM }

sealed class LiveActivityModel {
    abstract val id: String
    abstract val isCritical: Boolean
    open val isSensitive: Boolean = false
    open val type: ActivityType = ActivityType.DEFAULT

    data class MediaAction(
        val name: String,
        val actionString: String,
        val iconRes: Int? = null 
    )

    data class Music(
        override val id: String,
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
        val customActions: List<MediaAction> = emptyList(),
        override val isCritical: Boolean = false
    ) : LiveActivityModel()

    data class Dashboard(override val id: String = "dash", override val isCritical: Boolean = false) : LiveActivityModel()
    data class Otp(override val id: String = "otp", val code: String, val sourceApp: String, override val isCritical: Boolean = true) : LiveActivityModel()
    data class SystemAlert(override val id: String = "alert", val title: String, val message: String, val alertColor: Long = 0xFFFFEB3B, override val isCritical: Boolean = true) : LiveActivityModel()
    data class Charging(override val id: String, val level: Int, val isPluggedIn: Boolean, val isTransient: Boolean = false, override val isCritical: Boolean = false) : LiveActivityModel()
    data class OngoingTask(override val id: String = "task", val pkgName: String, val title: String, val text: String, val progress: Int, val progressMax: Int, override val isCritical: Boolean = false) : LiveActivityModel()
    data class AppTimerWarning(override val id: String = "timer", val packageName: String, val appName: String, override val isCritical: Boolean = true) : LiveActivityModel()
    data class RealityPill(override val id: String = "reality", val appName: String, val sessionMinutes: Int, override val isCritical: Boolean = false) : LiveActivityModel()
    data class HardwareMonitor(override val id: String = "hw", val cpuFreqMhz: Int, val isGamingModeOn: Boolean, override val isCritical: Boolean = false) : LiveActivityModel()
}
