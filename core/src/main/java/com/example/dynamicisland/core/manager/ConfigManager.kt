package com.example.dynamicisland.core.manager
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.ui.DynamicIslandView

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object ConfigManager {

    fun commitAndBroadcast(
        prefs: SharedPreferences,
        scope: CoroutineScope,
        context: Context,
        editBlock: SharedPreferences.Editor.() -> Unit,
        broadcastBlock: () -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val editor = prefs.edit()
            editor.editBlock()
            editor.commit()
            makePrefsWorldReadable(context)
            withContext(Dispatchers.Main) { broadcastBlock() }
        }
    }

    fun saveAndBroadcast(
        prefs: SharedPreferences, scope: CoroutineScope, context: Context,
        prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean
    ) {
        commitAndBroadcast(prefs, scope, context, {
            putFloat("${prefix}_w", w).putFloat("${prefix}_h", h).putFloat("${prefix}_x", x).putFloat("${prefix}_y", y)
            putBoolean("expand_upwards", expandUp)
        }) {
            broadcastUpdate(context, prefs, prefix, w, h, x, y, ringT, expandUp)
        }
    }

    fun broadcastUpdateSingle(context: Context, prefs: SharedPreferences, prefix: String) {
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            @Suppress("WrongConstant") addFlags(0x01000000)
            setPackage("com.android.systemui") 
            putExtra("prefix", prefix)
        }
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        context.sendBroadcast(intent)
    }

    private fun broadcastUpdate(context: Context, prefs: SharedPreferences, prefix: String, w: Float, h: Float, x: Float, y: Float, ringT: Float, expandUp: Boolean) {
        @Suppress("WrongConstant")
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").addFlags(0x01000000).apply { setPackage("com.android.systemui") } 
        intent.putExtra("prefix", prefix).putExtra("w", w).putExtra("h", h).putExtra("x", x).putExtra("y", y).putExtra("ring_thickness", ringT).putExtra("expand_upwards", expandUp)
        intent.putExtra("pad_t", prefs.getFloat("pad_t", 0f)).putExtra("pad_b", prefs.getFloat("pad_b", 0f)).putExtra("pad_l", prefs.getFloat("pad_l", 0f)).putExtra("pad_r", prefs.getFloat("pad_r", 0f))
        
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        context.sendBroadcast(intent)
    }

    fun sendGestureUpdate(context: Context, prefs: SharedPreferences) {
        val intent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
            @Suppress("WrongConstant") addFlags(0x01000000)
            setPackage("com.android.systemui") 
            
            putExtra("tweak_offset_y", prefs.getFloat("tweak_offset_y", 0f))
            putExtra("tweak_base_width", prefs.getFloat("tweak_base_width", 100f))
            putExtra("theme_button_size", prefs.getFloat("theme_button_size", 48f))
            putExtra("theme_button_spacing", prefs.getFloat("theme_button_spacing", 16f))
            putExtra("theme_button_radius", prefs.getFloat("theme_button_radius", 50f))
            putExtra("theme_anim_type", prefs.getString("theme_anim_type", "BOUNCE"))
            putExtra("theme_corner_radius", prefs.getFloat("theme_corner_radius", 50f))
            putExtra("theme_text_primary", prefs.getFloat("theme_text_primary", 16f))
            putExtra("theme_text_secondary", prefs.getFloat("theme_text_secondary", 14f))
            putExtra("theme_progress_thick", prefs.getFloat("theme_progress_thick", 4f))
            putExtra("theme_ring_thick", prefs.getFloat("theme_ring_thick", 12f))
            putExtra("theme_element_gap", prefs.getFloat("theme_element_gap", 8f))
            putExtra("theme_music_title", prefs.getFloat("theme_music_title", 16f))
            putExtra("theme_music_artist", prefs.getFloat("theme_music_artist", 14f))
            putExtra("theme_music_seeker", prefs.getFloat("theme_music_seeker", 4f))
            putExtra("theme_bat_text", prefs.getFloat("theme_bat_text", 16f))
            putExtra("theme_bat_icon", prefs.getFloat("theme_bat_icon", 36f))
            putExtra("theme_bat_ring", prefs.getFloat("theme_bat_ring", 12f))
            putExtra("theme_alert_title", prefs.getFloat("theme_alert_title", 16f))
            putExtra("theme_alert_msg", prefs.getFloat("theme_alert_msg", 14f))
            
            putExtra("haptic_strength", prefs.getFloat("haptic_strength", 1f).toInt())
            putExtra("charging_style", prefs.getString("charging_style", "CUBE"))
            putExtra("blur_intensity", prefs.getFloat("blur_intensity", 16f))
            putExtra("hide_landscape", prefs.getBoolean("hide_landscape", false))
            putExtra("enable_media", prefs.getBoolean("enable_media", true))
            putExtra("enable_charging", prefs.getBoolean("enable_charging", true))
            putExtra("enable_alerts", prefs.getBoolean("enable_alerts", true))
            putExtra("enable_timers", prefs.getBoolean("enable_timers", true))
            putExtra("rotate_cube", prefs.getBoolean("rotate_cube", true))
            putExtra("glass_mode", prefs.getBoolean("glass_mode", true))
            putExtra("spring_damping", prefs.getFloat("spring_damping", 0.85f))
            putExtra("spring_stiffness", prefs.getFloat("spring_stiffness", 400f))
        }
        val matrix = JSONObject()
        prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
        intent.putExtra("gesture_payload", matrix.toString())
        for (i in 0..7) intent.putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..6) intent.putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
        context.sendBroadcast(intent)
    }

    fun getDefaultWidth(prefix: String): Float = when(prefix) { "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "cube" -> 85f; else -> 0f }
    fun getDefaultHeight(prefix: String): Float = when(prefix) { "ring" -> 45f; "mini" -> 36f; "mid" -> 80f; "max" -> 220f; "cube" -> 85f; else -> 0f }

    private fun makePrefsWorldReadable(context: Context) {
        try {
            val rootDir = File(context.applicationInfo.dataDir)
            rootDir.setExecutable(true, false)
            rootDir.setReadable(true, false)
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) { 
                prefsDir.setExecutable(true, false)
                prefsDir.setReadable(true, false) 
            }
            val prefsFile = File(prefsDir, "island_prefs.xml")
            if (prefsFile.exists()) prefsFile.setReadable(true, false)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
