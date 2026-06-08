package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 🛠️ CONFIGURATION SYNC MANAGER
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
            broadcastReload(context)
        }
    }

    fun commitAndBroadcast(
        prefs: SharedPreferences,
        scope: CoroutineScope,
        context: Context,
        editBlock: SharedPreferences.Editor.() -> Unit,
        onDone: (() -> Unit)? = null
    ) {
        scope.launch {
            prefs.edit().apply(editBlock).apply()
            broadcastReload(context)
            onDone?.invoke()
        }
    }

    fun broadcastUpdateSingle(context: Context, prefs: SharedPreferences, key: String) {
        broadcastReload(context)
    }

    fun sendGestureUpdate(context: Context, prefs: SharedPreferences) {
        broadcastReload(context)
    }

    fun setCalibrationMode(context: Context, enabled: Boolean, prefix: String) {
        val intent = android.content.Intent("com.example.dynamicisland.CALIBRATION_MODE")
        intent.putExtra("enabled", enabled)
        intent.putExtra("prefix", prefix)
        context.sendBroadcast(intent)
    }

    private fun broadcastReload(context: Context) {
        val intent = android.content.Intent("com.example.dynamicisland.RELOAD_PREFS")
        context.sendBroadcast(intent)
    }

    fun getDefaultWidth(prefix: String) = 180f
    fun getDefaultHeight(prefix: String) = 45f
    fun getDefaultRadius(prefix: String) = 24f
}
