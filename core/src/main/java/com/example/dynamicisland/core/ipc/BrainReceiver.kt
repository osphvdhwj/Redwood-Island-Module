package com.example.dynamicisland.core.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.model.IslandIntent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 🧠 BRAIN RECEIVER (Core-side)
 * 
 * Listens for events broadcast by Ghost Satellites and dispatches them to the Neural Core.
 */
@AndroidEntryPoint
class BrainReceiver : BroadcastReceiver() {

    @Inject
    lateinit var neuralCore: IslandNeuralCore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.dynamicisland.BRAIN_EVENT") {
            val action = intent.getStringExtra("action") ?: return
            val extras = intent.extras ?: return
            
            val islandIntent = when (action) {
                "UPDATE_BATTERY" -> IslandIntent.UpdateBattery(
                    level = extras.getInt("level", 100),
                    isCharging = extras.getBoolean("is_charging", false)
                )
                "BATTERY_PULSE" -> IslandIntent.BatteryPulse(
                    level = extras.getInt("level", 100)
                )
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
                "DISMISS" -> IslandIntent.DismissActive
                "COLLAPSE" -> IslandIntent.Collapse
                else -> null
            }
            
            islandIntent?.let { neuralCore.dispatch(it) }
        }
    }
}
