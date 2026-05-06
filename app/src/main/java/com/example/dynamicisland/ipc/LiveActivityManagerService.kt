package com.example.dynamicisland.ipc

import com.example.dynamicisland.model.ExternalActivity
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LiveActivityModel.ExternalActivity

class LiveActivityManagerService : Service() {

    // Standard initialization is usually preferred over explicit left-hand typing
    private val activeActivities = mutableMapOf<String, ExternalActivity>()

    override fun onBind(intent: Intent?): IBinder = binder

    private val binder = object : com.example.dynamicisland.ipc.ILiveActivityManager.Stub() {

        private val it: Any = TODO()

        override fun startActivity(info: com.example.dynamicisland.ipc.LiveActivityInfo): String {
            // Ensure token is treated as a non-null String
            val token = info.activityId ?: return ""

            val model = ExternalActivity(
                id = token,
                type = ActivityType.MESSAGE,
                info = info,
                state = info.initialState,
                isTransient = false,
                isCritical = false,
                isSensitive = false
            )

            activeActivities[token] = model
            Log.d("LiveActivityService", "Started activity: $token")
            broadcastActivityUpdate(model)

            return token
        }

        override fun updateActivity(token: String, state: Bundle) {
            // Safe call with .let ensures we only operate if the activity exists
            activeActivities[token]?.let { existing ->
                val updatedModel = ExternalActivity(
                    id = existing.id,
                    type = existing.type,
                    info = existing.info,
                    state = state,
                    isTransient = existing.isTransient,
                    isCritical = existing.isCritical,
                    isSensitive = existing.isSensitive
                )

                activeActivities[token] = updatedModel
                Log.d("LiveActivityService", "Updated activity: $token")
                broadcastActivityUpdate(updatedModel)
            }
        }

        override fun endActivity(token: String) {
            // Remove returns the removed item, or null if it wasn't there
            val removed = activeActivities.remove(token)
            if (removed != null) {
                Log.d("LiveActivityService", "Ended activity: $token")
                broadcastActivityEnd(token)
            }
        }

        override fun getActiveActivities(): List<com.example.dynamicisland.ipc.LiveActivityInfo> {
            // Idiomatic way to map map values to a list
            return activeActivities.values.map { it.info }
        }
    }

    private fun broadcastActivityUpdate(model: ExternalActivity) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_UPDATED").apply {
            setPackage("com.android.systemui")
            putExtra("activity_id", model.id)
            putExtra("package_name", model.info.appPackage)
            putExtra("layout_type", model.info.layoutType)
            putExtra("state", model.state)
        }
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }

    private fun broadcastActivityEnd(activityId: String) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_ENDED").apply {
            setPackage("com.android.systemui")
            putExtra("activity_id", activityId)
        }
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }
}