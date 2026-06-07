package com.example.dynamicisland.core.manager

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.example.dynamicisland.shared.model.*

/**
 * 📦 ISLAND NOTIFICATION MANAGER
 * 
 * Processes incoming system notifications, extracts rich metadata,
 * and manages the notification stack logic for the Brain.
 */
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
                        val bitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
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

            val notificationId = "${packageName}_${title}_${text}_${notification.`when`}_${notification.number}".hashCode().toString()
            
            val simpleNotif = SimpleNotification(
                id = notificationId,
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
                        instruction = text,
                        distance = title,
                        isCritical = true
                    )
                    onNavigationCaught(navModel)
                    return@launch
                }
            }

            // 🍔 FEATURE 1.5: Smart Delivery & Ride Tracking
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
            
            val existingIdx = list.indexOfFirst { it.id == notificationId }
            if (existingIdx != -1) {
                val old = list[existingIdx]
                if (old.text == text && old.title == title) {
                    avatar?.recycle()
                    return@launch
                }
                list.removeAt(existingIdx)
                old.avatar?.recycle()
            }
            
            if (list.size >= 3) {
                val old = list.removeAt(0)
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

    fun dismissNotification(packageName: String) {
        val list = notificationMap[packageName] ?: return
        list.forEach { it.avatar?.recycle() }
        notificationMap.remove(packageName)
        
        onNotificationStackCaught(
            LiveActivityModel.NotificationStack(
                id = "stack_$packageName",
                pkgName = packageName,
                notifications = emptyList(),
                totalCount = 0
            )
        )
    }
}
