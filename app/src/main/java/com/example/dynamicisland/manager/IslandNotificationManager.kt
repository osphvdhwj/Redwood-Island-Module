package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import android.app.Notification
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID

class IslandNotificationManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onProgressCaught: (LiveActivityModel.OngoingTask) -> Unit,
    private val onNavigationCaught: (LiveActivityModel.Navigation) -> Unit,
    private val onNotificationStackCaught: (LiveActivityModel.NotificationStack) -> Unit
) {
    private val notificationMap = mutableMapOf<String, MutableList<SimpleNotification>>()

    fun clearAll() {
        notificationMap.values.flatten().forEach { it.avatar?.recycle() }
        notificationMap.clear()
    }

    // This function will be called directly by our Xposed Hook when a notification arrives
    fun processIncomingNotification(packageName: String, notification: Notification) {
        scope.launch {
            val extras: Bundle = notification.extras ?: return@launch
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
            
            // 🖼️ Extract Rich Metadata
            val avatar: android.graphics.Bitmap? = try {
                val icon = extras.getParcelable<android.graphics.drawable.Icon>(Notification.EXTRA_LARGE_ICON)
                if (icon != null) {
                    val drawable = icon.loadDrawable(context)
                    if (drawable != null) {
                        val bitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap
                    } else null
                } else null
            } catch (e: Exception) { null }

            val actions = mutableListOf<RemoteNotificationAction>()
            notification.actions?.forEach { action ->
                val isReply = action.remoteInputs?.isNotEmpty() == true
                actions.add(RemoteNotificationAction(action.title.toString(), action.actionIntent, isReply))
            }

            val simpleNotif = SimpleNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                avatar = avatar,
                remoteActions = actions
            )

            // 🗺️ FEATURE 1: The Navigation Hijacker (Google Maps)
            if (packageName == "com.google.android.apps.maps") {
                if (title.isNotEmpty() && text.isNotEmpty()) {
                    val navModel = LiveActivityModel.Navigation(
                        id = "sys_navigation",
                        instruction = text, // e.g., "Turn Left on Main St"
                        distance = title,    // e.g., "In 500 ft"
                        isCritical = true
                    )
                    onNavigationCaught(navModel)
                    return@launch
                }
            }

            // 🍔 FEATURE 1.5: Smart Delivery & Ride Tracking (Zomato, Swiggy, Uber)
            val deliveryApps = listOf("com.application.zomato", "in.swiggy.android", "com.ubercab", "com.ubercab.eats")
            if (deliveryApps.contains(packageName)) {
                val etaRegex = Regex("(\\d+)\\s*(min|mins|minutes)")
                val match = etaRegex.find(text)
                if (match != null) {
                    val etaMins = match.groupValues[1]
                    val deliveryModel = LiveActivityModel.OngoingTask(
                        id = "delivery_$packageName",
                        pkgName = packageName,
                        title = if (packageName.contains("uber")) "Ride Arriving" else "Order Arriving",
                        text = "$etaMins mins away",
                        progress = 100 - (etaMins.toInt().coerceIn(1, 60)), 
                        progressMax = 100
                    )
                    onProgressCaught(deliveryModel)
                    return@launch
                }
            }

            // ⬇️ FEATURE 2: Global Progress
            val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
            val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1)
            
            if (progress >= 0 && progressMax > 0) {
                val progressModel = LiveActivityModel.OngoingTask(
                    id = "sys_progress_$packageName",
                    pkgName = packageName,
                    title = title.ifEmpty { "Downloading..." },
                    text = text,
                    progress = progress,
                    progressMax = progressMax
                )
                onProgressCaught(progressModel)
                return@launch
            }

            // 📦 FEATURE 4: Smart Coalescing (Stacking)
            val list = notificationMap.getOrPut(packageName) { mutableListOf() }
            if (list.size > 3) {
                val old = list.removeAt(0) // Keep last 3
                old.avatar?.recycle()
            }
            list.add(simpleNotif)

            val stack = LiveActivityModel.NotificationStack(
                id = "stack_$packageName",
                pkgName = packageName,
                notifications = list.reversed(),
                totalCount = list.size,
                accentColor = notification.color
            )
            onNotificationStackCaught(stack)
        }
    }
}
