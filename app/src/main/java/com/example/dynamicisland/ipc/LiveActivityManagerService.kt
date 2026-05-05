package com.example.dynamicisland.ipc

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LiveActivityModel

class LiveActivityManagerService : Service() {

    private val activeActivities = ConcurrentHashMap<String, LiveActivityModel.ExternalActivity>()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val binder = object : com.example.dynamicisland.ipc.ILiveActivityManager.Stub() {
        override fun startActivity(info: com.example.dynamicisland.ipc.LiveActivityInfo): String {
            val token = info.activityId
            val model = LiveActivityModel.ExternalActivity(
                id = token,
                info = info,
                state = info.initialState,
                isTransient = false,
                isCritical = false
            )
            activeActivities[token] = model
            Log.d("LiveActivityService", "Started activity: $token")
            broadcastActivityUpdate(model)
            return token
        }

        override fun updateActivity(token: String, state: Bundle) {
            activeActivities[token]?.let { model ->
                val updatedModel = model.copy(state = state)
                activeActivities[token] = updatedModel
                Log.d("LiveActivityService", "Updated activity: $token")
                broadcastActivityUpdate(updatedModel)
            }
        }

        override fun endActivity(token: String) {
            activeActivities.remove(token)?.let {
                Log.d("LiveActivityService", "Ended activity: $token")
                broadcastActivityEnd(token)
            }
        }
        
        override fun getActiveActivities(): List<LiveActivityInfo> {
            return activeActivities.values.map { it.info }
        }
    } // <--- This missing bracket caused most of the errors!

    private fun broadcastActivityUpdate(model: LiveActivityModel.ExternalActivity) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_UPDATED")
        intent.setPackage("com.android.systemui")
        intent.putExtra("activity_id", model.id)
        intent.putExtra("package_name", model.info.appPackage)
        intent.putExtra("layout_type", model.info.layoutType)
        intent.putExtra("state", model.state)
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }

    private fun broadcastActivityEnd(activityId: String) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_ENDED")
        intent.setPackage("com.android.systemui")
        intent.putExtra("activity_id", activityId)
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }
}