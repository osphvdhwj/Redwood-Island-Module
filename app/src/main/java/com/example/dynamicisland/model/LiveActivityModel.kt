// File: app/src/main/java/com/dynamicisland/model/LiveActivityModel.kt
package com.example.dynamicisland.model

import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.graphics.Color

/**
 * Sealed class representing every possible dynamic island activity.
 * All subclasses are designed to work with the premium UI engine.
 */
sealed class LiveActivityModel {
    abstract val id: String
    abstract val type: ActivityType
    abstract val isTransient: Boolean
    abstract val isCritical: Boolean
    abstract val isSensitive: Boolean

    // ==================== ORIGINAL SUBCLASSES (from your existing code) ====================

    data class General(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val title: String,
        val dataText: String = "",
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
        val state: String = "ONGOING",                     // RINGING, ONGOING, ENDED
        val startTime: Long = System.currentTimeMillis()
    ) : LiveActivityModel()

    data class Music(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val title: String,
        val artist: String,
        val albumArt: Bitmap? = null,                     // Full bitmap (optional)
        val albumArtUri: String? = null,                  // For Coil AsyncImage (preferred)
        val blurredAlbumArt: Bitmap? = null,              // Pre‑blurred if available
        val appIcon: Bitmap? = null,
        val dominantColor: Int? = null,                   // Android color int
        val titleTextColor: Int = android.graphics.Color.WHITE,
        val isPlaying: Boolean,
        val durationMs: Long,
        val positionMs: Long,
        val appPackageName: String,
        val customActions: List<CustomMediaAction> = emptyList(),
        val isShuffled: Boolean = false,
        val repeatMode: Int = 0,                          // 0 = none, 1 = all, 2 = one
        val isLiked: Boolean = false,
        val isVideo: Boolean = false,                     // The video classifier flag
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel() {
        /** Convenient progress for the AGSL waveform (0f..1f) */
        val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

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
        val alertType: String,                            // e.g. "BATTERY_LOW", "OVERHEAT"
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

    data class RealityPill(
        override val id: String = "reality_pill",
        override val type: ActivityType = ActivityType.TIMER,
        val appName: String,
        val sessionMinutes: Int,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    data class LinkIntercept(
        override val id: String = "sys_link",
        override val type: ActivityType = ActivityType.MESSAGE,
        val targetAppName: String,
        val targetAppIcon: Bitmap?,
        val urlHost: String,
        val rawIntent: android.content.Intent,
        override val isTransient: Boolean = false,
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

    data class ExternalActivity(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val info: LiveActivityInfo,
        val state: Bundle,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // ==================== NEW SUBCLASSES (Phase 2–4 + premium UI) ====================

    // OTP from messages/notifications
    data class Otp(
        override val id: String = "sys_otp",
        override val type: ActivityType = ActivityType.MESSAGE,
        val code: String,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true
    ) : LiveActivityModel()

    // Quick translation result
    data class Translation(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val original: String,
        val translated: String,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Barcode / QR scanner result
    data class Barcode(
        override val id: String = "sys_barcode",
        override val type: ActivityType = ActivityType.MESSAGE,
        val content: String,
        val format: String,                         // "QR_CODE", "EAN_13", etc.
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Navigation instruction
    data class Navigation(
        override val id: String = "sys_nav",
        override val type: ActivityType = ActivityType.ONGOING_TASK,
        val instruction: String,                    // "Turn left in 200m"
        val distance: Int,                          // meters
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Timer / stopwatch event
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

    // Weather mood ring (idle colour)
    data class WeatherMood(
        override val id: String = "sys_weather",
        override val type: ActivityType = ActivityType.MESSAGE,
        val condition: String,                      // "rain", "clear", "sunset"
        val color: Int = android.graphics.Color.WHITE,  // Android color int
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Now Playing (Shazam‑like ambient recognition)
    data class NowPlaying(
        override val id: String = "sys_nowplaying",
        override val type: ActivityType = ActivityType.MESSAGE,
        val song: String?,                          // Detected song title
        val confidence: Float,                      // 0..1
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Live Caption pill
    data class LiveCaption(
        override val id: String = "sys_livecaption",
        override val type: ActivityType = ActivityType.MESSAGE,
        val text: String,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Call screen transcript (spam detection)
    data class CallScreenTranscript(
        override val id: String = "sys_calltranscript",
        override val type: ActivityType = ActivityType.ONGOING_TASK,
        val transcript: String,
        val isSpam: Boolean = false,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = true,
        override val isSensitive: Boolean = true
    ) : LiveActivityModel()

    // Cross‑device clipboard sync
    data class ClipboardSync(
        override val id: String = "sys_clipboard",
        override val type: ActivityType = ActivityType.MESSAGE,
        val content: String,
        val source: String,                         // "Mac", "PC", "Phone"
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // AirPods / Bluetooth accessory pop‑up
    data class AirPodsPopup(
        override val id: String = "sys_airpods",
        override val type: ActivityType = ActivityType.HARDWARE,
        val deviceName: String,
        val leftBattery: Float,                     // 0..1
        val rightBattery: Float,
        val caseBattery: Float,
        val isConnected: Boolean,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Focus / Do Not Disturb mode pill
    data class FocusMode(
        override val id: String = "sys_focus",
        override val type: ActivityType = ActivityType.MESSAGE,
        val modeName: String,                       // "Sleep", "Work", "Driving"
        val iconRes: Int = 0,                       // Android resource id
        val remainingMinutes: Int? = null,          // null if indefinite
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Live Activities API (sports, delivery, etc.)
    data class LiveActivity(
        override val id: String,
        override val type: ActivityType = ActivityType.MESSAGE,
        val info: LiveActivityInfo,                 // IPC wrapper (same as ExternalActivity)
        val title: String,
        val subtitle: String = "",
        val progress: Float? = null,
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Gamification / streak display (optional transient pill)
    data class AchievementBadge(
        override val id: String = "sys_achievement",
        override val type: ActivityType = ActivityType.MESSAGE,
        val displayName: String,
        val description: String,
        override val isTransient: Boolean = true,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()

    // Experimental: crypto / stock ticker
    data class TickerUpdate(
        override val id: String = "sys_ticker",
        override val type: ActivityType = ActivityType.MESSAGE,
        val items: List<Pair<String, Float>>,       // symbol -> price
        override val isTransient: Boolean = false,
        override val isCritical: Boolean = false,
        override val isSensitive: Boolean = false
    ) : LiveActivityModel()
}

// ==================== Supporting Types ====================

data class CustomMediaAction(
    val action: String,            // "like", "dislike", "share"
    val icon: Bitmap?,
    val label: String
)

data class QSTileState(
    val tileName: String,
    val isActive: Boolean,
    val iconRes: Int
)

/**
 * Simplified activity type enum – can be extended to match your IPC layer.
 */
enum class ActivityType {
    MESSAGE,
    ONGOING_TASK,
    CHARGING,
    HARDWARE,
    ALARM,
    TIMER
}

/**
 * Minimal IPC wrapper (original `LiveActivityInfo` from your hook layer).
 * Keep this in sync with your actual IPC model.
 */
data class LiveActivityInfo(
    val packageName: String = "",
    val action: String = "",
    val extras: Bundle = Bundle.EMPTY
)