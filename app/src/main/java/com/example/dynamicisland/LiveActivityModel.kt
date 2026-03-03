package com.example.dynamicisland

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Icon

/**
 * Master State Enum for the Island's physical form.
 */
enum class IslandState {
    TYPE_0_RING,      // Just the wavy ring around the punch hole
    TYPE_1_MINI,      // Smallest pill (e.g., persistent icon + small text)
    TYPE_2_MID,       // Expanded pill (e.g., standard notification)
    TYPE_3_MAX,       // Full expanded state (Media Player, Dashboard, etc.)
    TYPE_SPLIT,       // Added to support backwards compatibility temporarily
    HIDDEN            // Completely gone
}

/**
 * Base class for everything that can appear in the Dynamic Island.
 */
sealed class LiveActivityModel {
    abstract val id: String
    abstract val priority: Int

    // --- 1. MEDIA PLAYER ---
    data class Music(
        override val id: String,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,
        val dominantColor: Int? = null,
        val isPlaying: Boolean = false,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val appPackageName: String = "",
        val launchIntent: PendingIntent? = null,
        val customActions: List<CustomMediaAction> = emptyList()
    ) : LiveActivityModel() {
        override val priority: Int = 10
    }

    // --- 2. CONTROL CENTER DASHBOARD ---
    data class Dashboard(
        override val id: String = "system_dashboard",
        val currentVolume: Int = 0,
        val maxVolume: Int = 15,
        val isWifiOn: Boolean = false,
        val isBluetoothOn: Boolean = false,
        val isTorchOn: Boolean = false,
        val ringerMode: Int = 2, // 0=Silent, 1=Vibrate, 2=Normal
        val pinnedApps: List<PinnedApp> = emptyList()
    ) : LiveActivityModel() {
        // High priority when invoked via swipe/tap, but usually idle
        override val priority: Int = 100
    }

    // --- 3. HARDWARE & GAMING MONITOR ---
    data class HardwareMonitor(
        override val id: String = "hardware_monitor",
        val cpuTempCelsius: Float = 0f,
        val batteryTempCelsius: Float = 0f,
        val cpuFreqMhz: Int = 0,
        val isGamingModeOn: Boolean = false
    ) : LiveActivityModel() {
        override val priority: Int = 5
    }

    // --- 4. TRANSIENT NOTIFICATIONS (Charging, Calls, etc.) ---
    data class Charging(
        override val id: String,
        val level: Int,
        val isPluggedIn: Boolean,
        val isFastCharging: Boolean = false
    ) : LiveActivityModel() {
        override val priority: Int = 20
    }

    data class Notification(
        override val id: String,
        val title: String,
        val text: String,
        val icon: Icon? = null,
        val packageName: String = "",
        val launchIntent: PendingIntent? = null
    ) : LiveActivityModel() {
        override val priority: Int = 8
    }

    // --- 5. LEGACY SUPPORT (To prevent breaking existing plugins like BatteryPlugin, ClockInterceptor) ---
    data class General(
        override val id: String,
        override val priority: Int,
        val title: String,
        val dataText: String,
        val progress: Float? = null,
        val accentColor: Int = android.graphics.Color.WHITE,
        val isTransient: Boolean = false,
        val type: ActivityType = ActivityType.GENERAL
    ) : LiveActivityModel()
}

/**
 * Represents a custom action provided by a Media App (e.g., Spotify's Heart, YouTube's Close)
 */
data class CustomMediaAction(
    val actionName: String,
    val icon: Icon?,
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

enum class ActivityType(val priority: Int) {
    ALARM(100),         // Absolute highest priority
    CALL(90),           // Phone calls / WhatsApp calls
    NAVIGATION(80),     // Maps / Directions
    TIMER(70),          // Active clocks
    MEDIA(60),          // Spotify / YouTube Music
    MESSAGE(50),        // SMS / WhatsApp Messages (Transient)
    DOWNLOAD(40),       // Active downloads
    CHARGING(30),       // Plugged in
    DASHBOARD(25),      // System dashboard
    BATTERY_LOW(20),    // Disconnected / Low Battery
    BATTERY_FULL(18),   // Fully Charged
    BLUETOOTH(15),      // Bluetooth Connect/Disconnect
    WIFI(12),           // Network Connect/Disconnect
    GENERAL(10)         // Fallback
}
