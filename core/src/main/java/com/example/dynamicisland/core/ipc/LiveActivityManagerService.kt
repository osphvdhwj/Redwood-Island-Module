package com.example.dynamicisland.core.ipc

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*

/**
 * 🛠️ LIVE ACTIVITY MANAGER SERVICE
 * 
 * Manages external activities posted by third-party apps or satellites.
 * Broadcasts updates to the Brain for processing.
 */
class LiveActivityManagerService : Service() {

    private val activeActivities = mutableMapOf<String, LiveActivityModel.ExternalActivity>()

    inner class LocalBinder : Binder() {
        fun startActivity(info: LiveActivityInfo): String {
            val token = info.activityId.ifEmpty { return "" }
            val model = LiveActivityModel.ExternalActivity(
                id          = token,
                type        = ActivityType.MESSAGE,
                info        = info,
                state       = info.initialState,
                isTransient = false,
                isCritical  = false,
                isSensitive = false
            )
            activeActivities[token] = model
            Log.d("LiveActivityService", "Started activity: $token")
            broadcastActivityUpdate(model)
            return token
        }

        fun updateActivity(token: String, state: Bundle) {
            activeActivities[token]?.let { existing ->
                val updated = LiveActivityModel.ExternalActivity(
                    id          = existing.id,
                    type        = existing.type,
                    info        = existing.info,
                    state       = state,
                    isTransient = existing.isTransient,
                    isCritical  = existing.isCritical,
                    isSensitive = existing.isSensitive
                )
                activeActivities[token] = updated
                Log.d("LiveActivityService", "Updated activity: $token")
                broadcastActivityUpdate(updated)
            }
        }

        fun endActivity(token: String) {
            val removed = activeActivities.remove(token)
            if (removed != null) {
                Log.d("LiveActivityService", "Ended activity: $token")
                broadcastActivityEnd(token)
            }
        }

        fun getActiveActivities(): List<LiveActivityInfo> =
            activeActivities.values.map { activity -> activity.info }
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    private fun broadcastActivityUpdate(model: LiveActivityModel.ExternalActivity) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_UPDATED").apply {
            setPackage("com.example.dynamicisland.core")
            putExtra("activity_id",  model.id)
            putExtra("package_name", model.info.appPackage)
            putExtra("layout_type",  model.info.layoutType)
            putExtra("state",        model.state)
        }
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }

    private fun broadcastActivityEnd(activityId: String) {
        val intent = Intent("com.example.dynamicisland.EXTERNAL_ACTIVITY_ENDED").apply {
            setPackage("com.example.dynamicisland.core")
            putExtra("activity_id", activityId)
        }
        sendBroadcast(intent, "com.redwood.permission.SECURE_IPC")
    }
}
