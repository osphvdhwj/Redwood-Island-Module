package com.example.dynamicisland.manager

import android.content.Context
import android.content.SharedPreferences
import com.example.dynamicisland.ipc.IslandContentProvider
import com.example.dynamicisland.ipc.IslandIPCClient
import com.example.dynamicisland.ipc.StatePublisher
import com.example.dynamicisland.model.*
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * BATCH 1: Drop-in replacement for ConfigManager.
 *
 * ALL writes now go through IslandIPCClient → ContentProvider.
 * Eliminates every makePrefsWorldReadable() call.
 * Eliminates every explicit broadcast to RELOAD_PREFS.
 * SystemUI is notified automatically via ContentObserver.
 *
 * Naming kept identical to the original ConfigManager so call sites
 * need zero changes other than the import line.
 */
object NewConfigManager {

    // -------------------------------------------------------------------------
    // Core write operations
    // -------------------------------------------------------------------------

    fun commitAndBroadcast(
        prefs: SharedPreferences,
        scope: CoroutineScope,
        context: Context,
        editBlock: SharedPreferences.Editor.() -> Unit,
        broadcastBlock: () -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            val client = IslandIPCClient.get(context)

            // Write to legacy SharedPreferences during migration period
            val editor = prefs.edit()
            editor.editBlock()
            editor.commit()

            // Extract what was written and mirror to ContentProvider
            val changedKeys = extractChangedKeys(prefs, editBlock)
            if (changedKeys.isNotEmpty()) {
                client.bulkPut(changedKeys)
            }

            withContext(Dispatchers.Main) {
                try { broadcastBlock() } catch (e: Exception) { /* isolated */ }
            }
        }
    }

    fun saveAndBroadcast(
        prefs: SharedPreferences,
        scope: CoroutineScope,
        context: Context,
        prefix: String,
        w: Float, h: Float, x: Float, y: Float,
        ringT: Float,
        expandUp: Boolean
    ) {
        scope.launch(Dispatchers.IO) {
            val client = IslandIPCClient.get(context)

            val payload = mapOf<String, Any>(
                "${prefix}_w" to w,
                "${prefix}_h" to h,
                "${prefix}_x" to x,
                "${prefix}_y" to y,
                "ring_thickness" to ringT,
                "expand_upwards" to expandUp
            )

            prefs.edit().apply {
                putFloat("${prefix}_w", w)
                putFloat("${prefix}_h", h)
                putFloat("${prefix}_x", x)
                putFloat("${prefix}_y", y)
                putFloat("ring_thickness", ringT)
                putBoolean("expand_upwards", expandUp)
            }.commit()

            client.bulkPut(payload)
        }
    }

    fun sendGestureUpdate(context: Context, prefs: SharedPreferences) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val client = IslandIPCClient.get(context)
            val payload = buildThemePayload(prefs)
            client.bulkPut(payload)

            val gesturePayload = buildGesturePayload(prefs)
            if (gesturePayload.isNotEmpty()) {
                client.bulkPut(gesturePayload)
            }

            val dashPayload = buildDashboardPayload(prefs)
            client.bulkPut(dashPayload)
        }
    }

    fun broadcastUpdateSingle(context: Context, prefs: SharedPreferences, prefix: String) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            val client = IslandIPCClient.get(context)
            when (prefix) {
                "dashboard" -> client.bulkPut(buildDashboardPayload(prefs))
                else        -> client.bulkPut(buildThemePayload(prefs))
            }
        }
    }

    fun getDefaultWidth(prefix: String): Float = when (prefix) {
        "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "cube" -> 85f; else -> 0f
    }

    fun getDefaultHeight(prefix: String): Float = when (prefix) {
        "ring" -> 45f; "mini" -> 36f; "mid" -> 80f; "max" -> 220f; "cube" -> 85f; else -> 0f
    }

    fun migrateFromSharedPreferences(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                IslandContentProvider.bootstrapFromSharedPrefs(prefs)
                android.util.Log.i("NewConfigManager", "Migration complete: ${prefs.all.size} keys")
            } catch (e: Exception) {
                android.util.Log.w("NewConfigManager", "Migration failed: ${e.message}")
            }
        }
    }

    private fun buildThemePayload(prefs: SharedPreferences): Map<String, Any> {
        return mapOf(
            "tweak_offset_y"       to prefs.getFloat("tweak_offset_y", 0f),
            "tweak_base_width"     to prefs.getFloat("tweak_base_width", 100f),
            "theme_button_size"    to prefs.getFloat("theme_button_size", 48f),
            "theme_button_spacing" to prefs.getFloat("theme_button_spacing", 16f),
            "theme_button_radius"  to prefs.getFloat("theme_button_radius", 50f),
            "theme_anim_type"      to (prefs.getString("theme_anim_type", "BOUNCE") ?: "BOUNCE"),
            "theme_corner_radius"  to prefs.getFloat("theme_corner_radius", 50f),
            "theme_text_primary"   to prefs.getFloat("theme_text_primary", 16f),
            "theme_text_secondary" to prefs.getFloat("theme_text_secondary", 14f),
            "theme_progress_thick" to prefs.getFloat("theme_progress_thick", 4f),
            "theme_ring_thick"     to prefs.getFloat("theme_ring_thick", 12f),
            "theme_element_gap"    to prefs.getFloat("theme_element_gap", 8f),
            "theme_music_title"    to prefs.getFloat("theme_music_title", 16f),
            "theme_music_artist"   to prefs.getFloat("theme_music_artist", 14f),
            "theme_music_seeker"   to prefs.getFloat("theme_music_seeker", 4f),
            "theme_bat_text"       to prefs.getFloat("theme_bat_text", 16f),
            "theme_bat_icon"       to prefs.getFloat("theme_bat_icon", 36f),
            "theme_bat_ring"       to prefs.getFloat("theme_bat_ring", 12f),
            "theme_alert_title"    to prefs.getFloat("theme_alert_title", 16f),
            "theme_alert_msg"      to prefs.getFloat("theme_alert_msg", 14f),
            "haptic_strength"      to prefs.getFloat("haptic_strength", 1f).toInt(),
            "charging_style"       to (prefs.getString("charging_style", "CUBE") ?: "CUBE"),
            "blur_intensity"       to prefs.getFloat("blur_intensity", 16f),
            "hide_landscape"       to prefs.getBoolean("hide_landscape", false),
            "enable_media"         to prefs.getBoolean("enable_media", true),
            "enable_charging"      to prefs.getBoolean("enable_charging", true),
            "enable_alerts"        to prefs.getBoolean("enable_alerts", true),
            "enable_timers"        to prefs.getBoolean("enable_timers", true),
            "rotate_cube"          to prefs.getBoolean("rotate_cube", true),
            "glass_mode"           to prefs.getBoolean("glass_mode", true),
            "spring_damping"       to prefs.getFloat("spring_damping", 0.85f),
            "spring_stiffness"     to prefs.getFloat("spring_stiffness", 400f),
            "idle_swipe_action"    to (prefs.getString("idle_swipe_action", "BRIGHTNESS") ?: "BRIGHTNESS"),
            "long_press_action"    to (prefs.getString("long_press_action", "SCREENSHOT") ?: "SCREENSHOT")
        )
    }

    private fun buildGesturePayload(prefs: SharedPreferences): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("TYPE_") && v is String) {
                map[k] = v
            }
        }
        return map
    }

    private fun buildDashboardPayload(prefs: SharedPreferences): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
        for (i in 0..7) map["pinned_app_$i"] = prefs.getString("pinned_app_$i", "") ?: ""
        for (i in 0..6) {
            map["qs_tile_spec_$i"]  = prefs.getString("qs_tile_spec_$i",  "") ?: ""
            map["qs_tile_label_$i"] = prefs.getString("qs_tile_label_$i", defaultQS.getOrElse(i) { "" })
        }
        return map
    }

    private fun extractChangedKeys(
        realPrefs: SharedPreferences,
        editBlock: SharedPreferences.Editor.() -> Unit
    ): Map<String, Any> {
        val captured = mutableMapOf<String, Any>()
        val capturingEditor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) captured[key] = value as Any
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null && values != null) captured[key] = values as Any
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) captured[key] = value as Any
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) captured[key] = value as Any
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) captured[key] = value as Any
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) captured[key] = value as Any
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor = this
            override fun clear(): SharedPreferences.Editor = this
            override fun commit(): Boolean = true
            override fun apply() = Unit
        }
        capturingEditor.editBlock()
        return captured
    }
}