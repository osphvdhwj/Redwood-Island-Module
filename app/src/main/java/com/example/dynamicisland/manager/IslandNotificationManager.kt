package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import android.app.Notification
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IslandNotificationManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onProgressCaught: (LiveActivityModel.OngoingTask) -> Unit,
    private val onNavigationCaught: (LiveActivityModel.General) -> Unit
) {
    // This function will be called directly by our Xposed Hook when a notification arrives
    fun processIncomingNotification(packageName: String, notification: Notification) {
        scope.launch {
            val extras: Bundle = notification.extras ?: return@launch
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

            // 🗺️ FEATURE 1: The Navigation Hijacker (Google Maps)
            if (packageName == "com.google.android.apps.maps") {
                // Maps usually puts the ETA in the subtext or title, and directions in the text
                val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
                if (title.isNotEmpty() && text.isNotEmpty()) {
                    val navModel = LiveActivityModel.General(
                        id = "sys_navigation",
                        type = ActivityType.MESSAGE, // We will map this to a custom Nav UI later
                        title = title, // e.g., "In 500 ft"
                        dataText = text, // e.g., "Turn Left on Main St"
                        accentColor = android.graphics.Color.parseColor("#34A853"), // Google Maps Green
                        isCritical = true
                    )
                    onNavigationCaught(navModel)
                    return@launch
                }
            }

            // 🔐 FEATURE 3: Smart OTP & 2FA Catcher
            val lowerText = text.lowercase()
            val lowerTitle = title.lowercase()
            
            if (lowerText.contains("code") || lowerText.contains("otp") || lowerText.contains("verification") ||
                lowerTitle.contains("code") || lowerTitle.contains("otp") || lowerTitle.contains("verification")) {
                
                // Find 4 to 8 digit numbers isolated by word boundaries
                val otpRegex = Regex("\\b\\d{4,8}\\b")
                val match = otpRegex.find(text) ?: otpRegex.find(title)
                
                if (match != null) {
                    val otpCode = match.value
                    val alertModel = LiveActivityModel.SystemAlert(
                        id = "sys_otp",
                        alertType = "OTP_CATCHER",
                        title = "Verification Code",
                        message = otpCode, // The UI will render this massive with a Copy button
                        alertColor = android.graphics.Color.parseColor("#4285F4"), // Google Blue
                        isCritical = true
                    )
                    // Trigger your standard alert callback (Requires adding onAlertCaught to constructor)
                    // onAlertCaught(alertModel) 
                    return@launch
                }
            }

            // ⬇️ FEATURE 2: The Global Progress Director (Downloads & Installs)
            val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)
            
            if (progress >= 0 && progressMax > 0) {
                // We caught a live progress bar!
                val progressModel = LiveActivityModel.OngoingTask(
                    id = "sys_progress_$packageName",
                    pkgName = packageName,
                    title = title.ifEmpty { "Downloading..." },
                    text = text,
                    progress = progress,
                    progressMax = progressMax
                )
                onProgressCaught(progressModel)
            }
        }
    }
}
