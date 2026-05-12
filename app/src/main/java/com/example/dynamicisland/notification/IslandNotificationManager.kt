package com.example.dynamicisland.manager

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.model.LiveActivityModel.*
import com.example.dynamicisland.intelligence.OtpTokenizer
import com.example.dynamicisland.settings.SettingsState
import com.example.dynamicisland.settings.SettingsManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manager that listens to notifications via NotificationListenerService,
 * extracts relevant data (OTP, links, translation triggers, navigation, etc.)
 * and posts LiveActivityModel events to IslandController.
 */
class IslandNotificationManager(context: Context) {
    private val ctx = context.applicationContext
    private val scope = MainScope()

    private val _currentModel = MutableStateFlow<LiveActivityModel?>(null)
    val currentModel: StateFlow<LiveActivityModel?> = _currentModel

    private var controller: IslandController? = null
    private var settings: SettingsState = SettingsState()
    private val allowedApps: Set<String> get() = settings.allowedNotificationApps

    // Notification coalescing buffer
    private val coalescedNotifications = mutableMapOf<String, MutableList<StatusBarNotification>>()
    private var coalesceTimer: kotlinx.coroutines.Job? = null

    fun attachController(c: IslandController) {
        controller = c
    }

    fun updateSettings(newSettings: SettingsState) {
        settings = newSettings
    }

    // Called by the actual NotificationListenerService on notification posted
    fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!settings.isIslandEnabled) return
        if (allowedApps.isNotEmpty() && sbn.packageName !in allowedApps) return

        val notif = sbn.notification
        val extras = notif.extras

        // Extract title & text
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text  = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        // OTP detection (if enabled)
        if (settings.otpDetection) {
            val otp = OtpTokenizer.extractOtp(title + " " + text)
            if (otp != null) {
                postModel(Otp(otp), "otp")
                return
            }
        }

        // Link intercept
        if (settings.linkIntercept) {
            val url = extractUrl(text)
            if (url != null) {
                postModel(LinkIntercept(url), "link")
                return
            }
        }

        // Translation trigger (simple check for foreign language text)
        if (settings.translation) {
            if (isTranslatable(text)) {
                postModel(Translation(text, "Auto-detect"), "translation")
                return
            }
        }

        // Navigation detection (simplistic: look for "min", "turn", "arrive")
        if (settings.navigation) {
            if (Regex("(\\d+\\s*min|turn|arrive)").containsMatchIn(text.lowercase())) {
                val distance = Regex("(\\d+)\\s*min").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                postModel(Navigation(text, distance), "navigation")
                return
            }
        }

        // General notification – coalesce if enabled
        if (settings.notificationCoalescing) {
            val key = sbn.packageName
            val buffer = coalescedNotifications.getOrPut(key) { mutableListOf() }
            buffer.add(sbn)
            startCoalesceTimer()
        } else {
            postModel(General(title, text))
        }
    }

    fun onNotificationRemoved(sbn: StatusBarNotification) {
        // If the dismissed notification was the current island event, dismiss island
        if (_currentModel.value != null) {
            // Could add custom logic to match and dismiss
        }
    }

    private fun postModel(model: LiveActivityModel, tag: String = "general") {
        _currentModel.value = model
        controller?.transitionTo(IslandState.TYPE_2_MID, model)
    }

    private fun startCoalesceTimer() {
        coalesceTimer?.cancel()
        coalesceTimer = scope.launch {
            kotlinx.coroutines.delay(2000) // wait 2 seconds for more notifications
            flushCoalesced()
        }
    }

    private fun flushCoalesced() {
        if (coalescedNotifications.isEmpty()) return
        // Take the first app’s notifications and create a summary
        val entry = coalescedNotifications.entries.first()
        val packageName = entry.key
        val notifications = entry.value
        coalescedNotifications.clear()

        val summaryTitle = "New notifications"
        val summaryText = "${notifications.size} new from ${packageName}"
        postModel(General(summaryTitle, summaryText))
    }

    private fun extractUrl(text: String): String? {
        val urlRegex = Regex("(https?://[\\w./?=&#\\-]+)")
        return urlRegex.find(text)?.value
    }

    private fun isTranslatable(text: String): Boolean {
        // Quick heuristic: contains non-ASCII characters and not just emojis
        return text.any { it.code > 127 } && text.length > 5
    }

    // Optional: register a broadcast receiver for clipboard-based OTP if not using notification listening
    fun startClipboardMonitoring() {
        // Placeholder – could use ClipboardManager.OnPrimaryClipChangedListener
    }
}