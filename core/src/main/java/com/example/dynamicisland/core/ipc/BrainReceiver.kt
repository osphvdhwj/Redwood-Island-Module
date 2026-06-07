package com.example.dynamicisland.core.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.settings.*
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
                "APP_CHANGED" -> {
                    val pkg = extras.getString("pkg") ?: ""
                    IslandIntent.AppChanged(pkg)
                }
                "NOTIFICATION_CAUGHT" -> {
                    val pkg = extras.getString("pkg") ?: ""
                    val notif = if (Build.VERSION.SDK_INT >= 33) {
                        extras.getParcelable("notification", android.app.Notification::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        extras.getParcelable("notification")
                    }
                    if (notif != null) IslandIntent.NotificationCaught(pkg, notif) else null
                }
                "MEDIA_SYNC" -> {
                    val pkg = extras.getString("pkg") ?: ""
                    val song = extras.getString("song") ?: ""
                    val artist = extras.getString("artist") ?: ""
                    val isPlaying = extras.getBoolean("isPlaying", false)
                    IslandIntent.MediaSync(pkg, song, artist, isPlaying)
                }
                "CALL_STATE_CHANGED" -> {
                    val state = extras.getString("state") ?: ""
                    val caller = extras.getString("caller") ?: ""
                    val number = extras.getString("number") ?: ""
                    IslandIntent.CallStateChanged(state, caller, number)
                }
                "SCREEN_CONTEXT" -> {
                    // Logic to handle semantic screen signals
                    null
                }
                "DISMISS" -> IslandIntent.DismissActive
                "COLLAPSE" -> IslandIntent.Collapse
                else -> null
            }
            
            islandIntent?.let { neuralCore.dispatch(it) }
        }
    }
}
