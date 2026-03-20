package com.example.dynamicisland

import android.app.Notification
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

        // Stopwatch Logic
        if (notification.channelId == "Stopwatch" || title.contains("Stopwatch", ignoreCase = true)) {
            return LiveActivityModel.OngoingTask(
                id = "system_stopwatch",
                pkgName = packageName,
                title = "Stopwatch",
                text = text.ifEmpty { title }, // The ticking time is usually here
                progress = -1, // -1 signifies indeterminate/no progress bar
                progressMax = -1,
                type = ActivityType.TIMER
            )
        }

        // Timer Logic
        if (notification.channelId == "Timer" || title.contains("Timer", ignoreCase = true)) {
            return LiveActivityModel.OngoingTask(
                id = "system_timer",
                pkgName = packageName,
                title = "Timer",
                text = text.ifEmpty { title },
                progress = -1,
                progressMax = -1,
                type = ActivityType.TIMER
            )
        }

        // Alarm Logic (Upcoming Alarm)
        if (notification.channelId == "Alarm" || notification.category == Notification.CATEGORY_ALARM) {
             return LiveActivityModel.SystemAlert(
                id = "system_alarm",
                title = "Upcoming Alarm",
                message = text,
                alertColor = 0xFFCCCCCC, // LTGRAY
                type = ActivityType.ALARM
            )
        }

        return null
    }
}
