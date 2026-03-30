package com.example.dynamicisland

import android.graphics.Bitmap

sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean
    abstract val isCritical: Boolean
    abstract val isSensitive: Boolean

    data class General(
        override val id: String, 
        override val type: ActivityType, 
        val title: String, 
        val dataText: String, 
        val accentColor: Int = android.graphics.Color.WHITE, 
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
        override val type: ActivityType = ActivityType.ONGOING_TASK,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true,
        val callerName: String = "Unknown Caller", 
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
        val isVideo: Boolean = false, // 🎬 NEW: The Video Classifier Flag
        override val isTransient: Boolean = false, 
        override val isCritical: Boolean = false, 
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class HardwareMonitor(
        override val id: String, 
        override val type: ActivityType = ActivityType.HARDWARE, 
        val cpuTempCelsius: Float, 
        val cpuFreqMhz: Int, 
        val isGamingModeOn: Boolean, 
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
        val pinnedApps: List<String> = emptyList(), // 🚀 FIXED: Added the missing pinnedApps variable!
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

    data class RealityPill(
        override val id: String = "reality_pill", 
        override val type: ActivityType = ActivityType.TIMER, 
        val appName: String, 
        val sessionMinutes: Int, 
        override val isTransient: Boolean = true, 
        override val isCritical: Boolean = false, 
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // 🔗 FOR THE LINK SWITCHER
    data class LinkIntercept(
        override val id: String = "sys_link", 
        override val type: ActivityType = ActivityType.MESSAGE, 
        val targetAppName: String, 
        val targetAppIcon: Bitmap?, 
        val urlHost: String, // e.g., "youtube.com"
        val rawIntent: android.content.Intent,
        override val isTransient: Boolean = false, 
        override val isCritical: Boolean = true, 
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // ⬇️ FOR THE DOWNLOAD SPEED MONITOR (Updated)
    data class OngoingTask(
        override val id: String = "ongoing_task", 
        override val type: ActivityType = ActivityType.ONGOING_TASK, 
        val pkgName: String, 
        val title: String, 
        val text: String, 
        val progress: Int, 
        val progressMax: Int, 
        val networkSpeed: String? = null, // 🚀 NEW: e.g., "4.2 MB/s"
        override val isTransient: Boolean = true, 
        override val isCritical: Boolean = false, 
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()
}
