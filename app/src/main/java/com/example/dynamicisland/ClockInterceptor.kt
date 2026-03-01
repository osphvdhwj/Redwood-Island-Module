package com.example.dynamicisland

import android.app.Notification
import android.graphics.Color
import android.service.notification.StatusBarNotification

object ClockInterceptor {

    private const val PKG_AOSP_CLOCK = "com.android.deskclock"
    private const val PKG_GOOGLE_CLOCK = "com.google.android.deskclock"

    fun inspect(sbn: StatusBarNotification): LiveActivityModel? {
        val packageName = sbn.packageName
        if (packageName != PKG_AOSP_CLOCK && packageName != PKG_GOOGLE_CLOCK) return null

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        // Determine if it's a Timer or Stopwatch based on notification channels or text patterns

        // Stopwatch Logic
        if (notification.channelId == "Stopwatch" || title.contains("Stopwatch", ignoreCase = true)) {
            return LiveActivityModel(
                id = "system_stopwatch",
                type = ActivityType.TIMER,
                title = "Stopwatch",
                dataText = text.ifEmpty { title }, // The ticking time is usually here
                accentColor = Color.parseColor("#FF9800"), // Orange for Stopwatch
                progress = null // Stopwatch has no progress bar
            )
        }

        // Timer Logic
        if (notification.channelId == "Timer" || title.contains("Timer", ignoreCase = true)) {
            // Try to calculate progress if max value is available (hard in standard notifs),
            // but we can definitely show the countdown text.
            return LiveActivityModel(
                id = "system_timer",
                type = ActivityType.TIMER,
                title = "Timer",
                dataText = text.ifEmpty { title },
                accentColor = Color.parseColor("#4CAF50"), // Green for Timer
                progress = null // Optional: You could parse the text "04:00" vs "05:00" to fake a bar
            )
        }

        // Alarm Logic (Upcoming Alarm)
        if (notification.channelId == "Alarm" || notification.category == Notification.CATEGORY_ALARM) {
             return LiveActivityModel(
                id = "system_alarm",
                type = ActivityType.ALARM,
                title = "Upcoming Alarm",
                dataText = text,
                accentColor = Color.LTGRAY
            )
        }

        return null
    }
}
