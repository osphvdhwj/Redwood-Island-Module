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

            val notificationId = "${packageName}_${title}_${text}_${notification.`when`}_${notification.number}".hashCode().toString()
            
            val simpleNotif = SimpleNotification(
                id = notificationId,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                avatar = avatar,
                remoteActions = actions
            )

            // ... (Navigation and Delivery features remain same) ...

            // 📦 FEATURE 4: Smart Coalescing (Stacking)
            val list = notificationMap.getOrPut(packageName) { mutableListOf() }
            
            // 🔄 UPDATE LOGIC: If notification exists, replace it to update timestamp
            val existingIdx = list.indexOfFirst { it.id == notificationId }
            if (existingIdx != -1) {
                val old = list[existingIdx]
                if (old.text == text && old.title == title) {
                    // Content identical, skip update to prevent flickering
                    avatar?.recycle()
                    return@launch
                }
                list.removeAt(existingIdx)
                old.avatar?.recycle()
            }
            
            if (list.size >= 3) {
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

    fun dismissNotification(packageName: String) {
        val list = notificationMap[packageName] ?: return
        list.forEach { it.avatar?.recycle() }
        notificationMap.remove(packageName)
        
        // Emit empty stack to trigger priority re-evaluation
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
