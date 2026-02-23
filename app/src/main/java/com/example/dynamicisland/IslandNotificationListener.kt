package com.example.dynamicisland

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.graphics.drawable.Icon
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class IslandNotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_POST = "com.example.dynamicisland.ACTION_POST"
        const val ACTION_REMOVE = "com.example.dynamicisland.ACTION_REMOVE"
        const val PERMISSION_TRIGGER = "com.example.dynamicisland.PERMISSION_TRIGGER"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing) return

        val ranking = Ranking()
        if (currentRanking.getRanking(sbn.key, ranking)) {
            if (ranking.importance < NotificationManager.IMPORTANCE_HIGH) {
                return
            }
        } else {
            // Fallback if ranking not found (unlikely)
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val icon = sbn.notification.smallIcon
        val contentIntent = sbn.notification.contentIntent

        val intent = Intent(ACTION_POST).apply {
            putExtra("title", title)
            putExtra("text", text)
            putExtra("icon", icon)
            putExtra("content_intent", contentIntent)
            setPackage("com.android.systemui")
        }

        try {
            sendBroadcast(intent, PERMISSION_TRIGGER)
        } catch (e: Exception) {
            Log.e("DynamicIsland", "Failed to send post broadcast", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
         val intent = Intent(ACTION_REMOVE).apply {
            setPackage("com.android.systemui")
         }
         try {
             sendBroadcast(intent, PERMISSION_TRIGGER)
         } catch (e: Exception) {
             Log.e("DynamicIsland", "Failed to send remove broadcast", e)
         }
    }
}
