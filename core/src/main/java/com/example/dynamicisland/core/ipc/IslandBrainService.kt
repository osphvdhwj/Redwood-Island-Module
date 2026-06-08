package com.example.dynamicisland.core.ipc

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.ipc.IIslandBrain
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 🧠 ISLAND BRAIN SERVICE
 * 
 * The central IPC gateway for the standalone Root Core Application.
 * Satellites bind to this service to send events and receive state updates.
 */
@AndroidEntryPoint
class IslandBrainService : Service() {

    @Inject
    lateinit var neuralCore: IslandNeuralCore

    private val binder = object : IIslandBrain.Stub() {
        override fun dispatch(action: String, extras: Bundle) {
            val intent = mapToIntent(action, extras)
            if (intent != null) {
                neuralCore.dispatch(intent)
            }
        }

        override fun updateSettings(settingsBundle: Bundle) {
            // Implementation for settings sync
        }

        override fun postActivity(modelJson: String) {
            // Implementation for posting new live activities via JSON
        }

        override fun removeActivity(activityId: String) {
            neuralCore.dispatch(IslandIntent.RemoveActivity(activityId))
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun mapToIntent(action: String, extras: Bundle): IslandIntent? {
        return when (action) {
            "UPDATE_BATTERY" -> IslandIntent.UpdateBattery(
                level = extras.getInt("level", 100),
                isCharging = extras.getBoolean("is_charging", false)
            )
            "BATTERY_PULSE" -> IslandIntent.BatteryPulse(
                level = extras.getInt("level", 100)
            )
            "DISMISS" -> IslandIntent.DismissActive
            "TOGGLE_EXPAND" -> IslandIntent.ToggleExpand
            "COLLAPSE" -> IslandIntent.Collapse
            "SCREEN_STATE" -> IslandIntent.UpdateScreenState(
                isScreenOn = extras.getBoolean("is_on", true)
            )
            "GAMING_STATS" -> IslandIntent.UpdateGamingStats(
                fps = extras.getFloat("fps"),
                frameMs = extras.getFloat("frame_ms"),
                jankPct = extras.getFloat("jank_pct"),
                cpuUsage = extras.getInt("cpu_usage"),
                gpuUsage = extras.getInt("gpu_usage")
            )
            else -> null
        }
    }
}
