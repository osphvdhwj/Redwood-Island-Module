package com.example.dynamicisland

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

data class LiveActivityModel(
    val id: String,
    val type: ActivityType,
    val title: String,
    val dataText: String,
    val progress: Float? = null,
    val accentColor: Int = android.graphics.Color.WHITE,
    val isTransient: Boolean = false, // If true, it auto-dismisses after a few seconds
    val timestamp: Long = System.currentTimeMillis()
)
