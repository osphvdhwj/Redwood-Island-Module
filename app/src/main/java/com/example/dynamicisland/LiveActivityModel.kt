package com.example.dynamicisland

import android.app.PendingIntent
import android.graphics.Bitmap


enum class IslandState {
    TYPE_0_RING,      // Just the wavy ring around the punch hole
    TYPE_1_MINI,      // Smallest pill (e.g., persistent icon + small text)
    TYPE_2_MID,       // Expanded pill (e.g., standard notification)
    TYPE_3_MAX,       // Full expanded state (Media Player, Dashboard, etc.)
    TYPE_SPLIT,
    HIDDEN            // Completely gone
}

enum class ActivityType(val priority: Int) {
    DASHBOARD(100),     // User explicitly opened the Control Center
    ALARM(90),
    CALL(80),
    NAVIGATION(70),
    TIMER(60),
    MEDIA(50),          // Media Player
    MESSAGE(40),
    DOWNLOAD(30),
    CHARGING(20),       // Transient (auto-dismisses)
    HARDWARE(10)
}

sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean
    val priority: Int get() = type.priority

    data class General(
        override val id: String,
        override val type: ActivityType,
        val title: String,
        val dataText: String,
        val progress: Float? = null,
        val accentColor: Int = android.graphics.Color.WHITE,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class Music(
        override val id: String = "sys_media",
        override val type: ActivityType = ActivityType.MEDIA,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val dominantColor: Int? = null,
        val isPlaying: Boolean = false,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val appPackageName: String = "",
        val launchIntent: PendingIntent? = null,
        val customActions: List<CustomMediaAction> = emptyList(),
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    data class Dashboard(
        override val id: String = "sys_dashboard",
        override val type: ActivityType = ActivityType.DASHBOARD,
        val currentVolume: Int = 0,
        val maxVolume: Int = 15,
        val isWifiOn: Boolean = false,
        val isTorchOn: Boolean = false,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    // --- 3. HARDWARE & GAMING MONITOR ---
    data class HardwareMonitor(
        override val id: String = "hardware_monitor",
        override val type: ActivityType = ActivityType.HARDWARE,
        val cpuTempCelsius: Float = 0f,
        val batteryTempCelsius: Float = 0f,
        val cpuFreqMhz: Int = 0,
        val isGamingModeOn: Boolean = false,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()

    // --- 4. TRANSIENT NOTIFICATIONS (Charging, Calls, etc.) ---
    data class Charging(
        override val id: String,
        override val type: ActivityType = ActivityType.CHARGING,
        val level: Int,
        val isPluggedIn: Boolean,
        val isFastCharging: Boolean = false,
        override val isTransient: Boolean = false
    ) : LiveActivityModel()
}

/**
 * Represents a custom action provided by a Media App (e.g., Spotify's Heart, YouTube's Close)
 */
data class CustomMediaAction(
    val actionName: String,
    val icon: android.graphics.drawable.Icon?,
    val pendingIntent: PendingIntent?,
    val isEnabled: Boolean = true
)

/**
 * Represents an app pinned by the user in the Dashboard.
 */
data class PinnedApp(
    val packageName: String,
    val appName: String,
    val appIcon: android.graphics.drawable.Drawable?
)
