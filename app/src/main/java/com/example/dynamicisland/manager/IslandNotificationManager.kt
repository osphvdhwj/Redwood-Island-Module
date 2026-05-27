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
    private val onNavigationCaught: (LiveActivityModel.General) -> Unit,
    private val onNotificationStackCaught: (LiveActivityModel.NotificationStack) -> Unit
) {
    private val notificationMap = mutableMapOf<String, MutableList<SimpleNotification>>()

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
                    val bitmap = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
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
...
                onNavigationCaught(navModel)
                return@launch
            }

            // ⬇️ FEATURE 2: Global Progress
...
                onProgressCaught(progressModel)
                return@launch
            }

            // 📦 FEATURE 4: Smart Coalescing (Stacking)
            val list = notificationMap.getOrPut(packageName) { mutableListOf() }
            if (list.size > 5) list.removeAt(0) // Keep last 5
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
