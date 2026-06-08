package com.example.dynamicisland.core.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.model.IslandIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 🔔 ISLAND NOTIFICATION LISTENER
 * 
 * Real-time interception of system notifications. 
 * Communicates with NeuralCore to trigger UI transitions (like the Game Hub shrink).
 */
@AndroidEntryPoint
class IslandNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var neuralCore: IslandNeuralCore

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            if (it.isOngoing) return // Filter ongoing noise
            
            Log.d("NotificationListener", "Intercepted: ${it.packageName}")
            
            // Dispatch intent to trigger UI reactions (like shrinking the Game Panel)
            neuralCore.dispatch(IslandIntent.UpdateNotificationState(isActive = true))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Check if stack is empty to reset state
        val activeCount = activeNotifications.filter { !it.isOngoing }.size
        if (activeCount == 0) {
            neuralCore.dispatch(IslandIntent.UpdateNotificationState(isActive = false))
        }
    }
}
