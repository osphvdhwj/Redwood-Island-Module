package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🛠️ CONFIGURATION SYNC MANAGER
 * 
 * Synchronizes layout positions and ring dimensions across processes.
 */
object NewConfigManager {
    fun saveAndBroadcast(
        prefs: SharedPreferences,
        scope: CoroutineScope,
        context: Context,
        prefix: String,
        w: Float, h: Float, x: Float, y: Float, r: Float, ringT: Float
    ) {
        scope.launch {
            prefs.edit().apply {
                putFloat("${prefix}_w", w)
                putFloat("${prefix}_h", h)
                putFloat("${prefix}_x", x)
                putFloat("${prefix}_y", y)
                putFloat("${prefix}_r", r)
                putFloat("ring_thickness", ringT)
            }.apply()
            
            // Trigger system-wide reload
            val intent = android.content.Intent("com.example.dynamicisland.RELOAD_PREFS")
            context.sendBroadcast(intent)
        }
    }
}
