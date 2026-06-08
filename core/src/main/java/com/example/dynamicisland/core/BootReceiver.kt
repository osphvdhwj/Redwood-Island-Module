package com.example.dynamicisland.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dynamicisland.core.domain.state.IslandController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import dagger.hilt.android.EntryPointAccessors

/**
 * Listens for system boot events to initialize the Island configuration.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val scope = MainScope()
            scope.launch {
                try {
                    val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                    val prefix = "pos_ring"
                    val w = prefs.getFloat("${prefix}_w", 180f)
                    val h = prefs.getFloat("${prefix}_h", 45f)
                    val x = prefs.getFloat("${prefix}_x", 0f)
                    val y = prefs.getFloat("${prefix}_y", 48f)
                    val r = prefs.getFloat("${prefix}_r", 24f)
                    val ringT = prefs.getFloat("ring_thickness", 3f)
                    
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, prefix, w, h, x, y, r, ringT)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
}
