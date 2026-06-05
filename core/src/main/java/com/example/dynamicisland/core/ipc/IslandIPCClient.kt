package com.example.dynamicisland.core.ipc

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * BATCH 1: Unified IPC client used by BOTH processes.
 *
 * The module app writes through this client.
 * SystemUI reads through this client.
 * No file permissions. No world-readable hacks. No race conditions.
 *
 * Uses a three-tier strategy:
 *   1. In-process memory cache  → zero-latency reads
 *   2. ContentProvider call()   → typed atomic ops when cache is cold
 *   3. ContentObserver          → push-based change notifications to SystemUI
 */
class IslandIPCClient private constructor(private val context: Context) {

    companion object {
        private const val TAG = "IslandIPCClient"

        @Volatile
        private var instance: IslandIPCClient? = null

        fun get(context: Context): IslandIPCClient =
            instance ?: synchronized(this) {
                instance ?: IslandIPCClient(context.applicationContext).also { instance = it }
            }
    }

    // -------------------------------------------------------------------------
    // In-process memory cache — avoids IPC overhead on hot reads
    // -------------------------------------------------------------------------

    private val cache = ConcurrentHashMap<String, Any>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Reactive change stream — SystemUI collectors receive instant updates
    private val _changeFlow = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val changeFlow: SharedFlow<String> = _changeFlow.asSharedFlow()

    // Specific typed flows for common high-frequency keys
    private val _layoutFlow   = MutableStateFlow<LayoutConfig?>(null)
    private val _themeFlow    = MutableStateFlow<ThemeConfig?>(null)
    private val _gestureFlow  = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _dashboardFlow = MutableStateFlow<DashboardConfig?>(null)

    val layoutFlow:    StateFlow<LayoutConfig?>    = _layoutFlow.asStateFlow()
    val themeFlow:     StateFlow<ThemeConfig?>     = _themeFlow.asStateFlow()
    val gestureFlow:   StateFlow<Map<String, String>> = _gestureFlow.asStateFlow()
    val dashboardFlow: StateFlow<DashboardConfig?> = _dashboardFlow.asStateFlow()

    // -------------------------------------------------------------------------
    // ContentObserver — receives push notifications from the Provider
    // -------------------------------------------------------------------------

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            val key = uri?.lastPathSegment ?: "unknown"
            cache.remove(key) // Invalidate cache entry
            scope.launch {
                _changeFlow.emit(key)
                // Reload typed config blocks if their domain key changed
                when {
                    key.endsWith("_w") || key.endsWith("_h") ||
                    key.endsWith("_x") || key.endsWith("_y") -> reloadLayout()
                    key.startsWith("theme_") || key == "glass_mode" ||
                    key == "spring_damping" || key == "spring_stiffness" -> reloadTheme()
                    key.startsWith("TYPE_")  -> reloadGestures()
                    key.startsWith("pinned_app_") ||
                    key.startsWith("qs_tile_")   -> reloadDashboard()
                }
            }
        }
    }

    init {
        // Register observer so this process receives all provider changes
        context.contentResolver.registerContentObserver(
            IslandContentProvider.prefsUri(),
            true,
            observer
        )
        // Warm the cache from a full snapshot on first init
        scope.launch { warmCache() }
    }

    // -------------------------------------------------------------------------
    // WRITE API — module app calls these
    // -------------------------------------------------------------------------

    fun putFloat(key: String, value: Float) {
        cache[key] = value
        callProvider("PUT_FLOAT", key, Bundle().apply { putFloat("v", value) })
    }

    fun putInt(key: String, value: Int) {
        cache[key] = value
        callProvider("PUT_INT", key, Bundle().apply { putInt("v", value) })
    }

    fun putBoolean(key: String, value: Boolean) {
        cache[key] = value
        callProvider("PUT_BOOLEAN", key, Bundle().apply { putBoolean("v", value) })
    }

    fun putString(key: String, value: String) {
        cache[key] = value
        callProvider("PUT_STRING", key, Bundle().apply { putString("v", value) })
    }

    fun putJson(key: String, json: JSONObject) {
        cache[key] = json
        callProvider("PUT_JSON", key, Bundle().apply { putString("v", json.toString()) })
    }

    fun clearAiMemory(): Boolean {
        return callProvider("CLEAR_AI_MEMORY", null, null)?.getBoolean("ok", false) ?: false
    }

    fun exportAiData(): String? {
        return callProvider("EXPORT_AI_DATA", null, null)?.getString("v")
    }

    fun getAiWeights(): String? {
        return callProvider("GET_AI_WEIGHTS", null, null)?.getString("v")
    }

    fun setAiWeights(data: String): Boolean {
        return callProvider("SET_AI_WEIGHTS", null, Bundle().apply { putString("v", data) })?.getBoolean("ok", false) ?: false
    }

    /**
     * Atomic bulk write — sends all key-value pairs in a single IPC transaction.
     * Use this instead of multiple individual puts to guarantee atomicity and
     * prevent SystemUI from reading a half-written config.
     */
    fun bulkPut(pairs: Map<String, Any>) {
        // Update cache instantly
        pairs.forEach { (k, v) -> cache[k] = v }

        // Build typed JSON payload
        val payload = JSONObject()
        pairs.forEach { (k, v) ->
            val entry = JSONObject()
            when (v) {
                is Float   -> entry.put("v", v.toString()).put("t", IslandContentProvider.TYPE_FLOAT)
                is Int     -> entry.put("v", v.toString()).put("t", IslandContentProvider.TYPE_INT)
                is Boolean -> entry.put("v", v.toString()).put("t", IslandContentProvider.TYPE_BOOLEAN)
                is String  -> entry.put("v", v).put("t", IslandContentProvider.TYPE_STRING)
                is JSONObject -> entry.put("v", v.toString()).put("t", IslandContentProvider.TYPE_JSON)
                else       -> return@forEach
            }
            payload.put(k, entry)
        }

        callProvider("BULK_PUT", null, Bundle().apply { putString("payload", payload.toString()) })
    }

    // -------------------------------------------------------------------------
    // READ API — SystemUI calls these (cache-first, then IPC fallback)
    // -------------------------------------------------------------------------

    fun getFloat(key: String, default: Float = 0f): Float {
        (cache[key] as? Float)?.let { return it }
        val result = callProvider("GET_FLOAT", key, Bundle().apply { putFloat("d", default) })
            ?.getFloat("v", default) ?: default
        cache[key] = result
        return result
    }

    fun getInt(key: String, default: Int = 0): Int {
        (cache[key] as? Int)?.let { return it }
        val result = callProvider("GET_INT", key, Bundle().apply { putInt("d", default) })
            ?.getInt("v", default) ?: default
        cache[key] = result
        return result
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        (cache[key] as? Boolean)?.let { return it }
        val result = callProvider("GET_BOOLEAN", key, Bundle().apply { putBoolean("d", default) })
            ?.getBoolean("v", default) ?: default
        cache[key] = result
        return result
    }

    fun getString(key: String, default: String = ""): String {
        (cache[key] as? String)?.let { return it }
        val result = callProvider("GET_STRING", key, Bundle().apply { putString("d", default) })
            ?.getString("v", default) ?: default
        cache[key] = result
        return result
    }

    fun getJson(key: String): JSONObject? {
        (cache[key] as? JSONObject)?.let { return it }
        val raw = callProvider("GET_STRING", key, null)?.getString("v") ?: return null
        return try {
            val json = JSONObject(raw)
            cache[key] = json
            json
        } catch (e: Exception) { null }
    }

    // -------------------------------------------------------------------------
    // Typed config accessors — returns structured objects ready to use
    // -------------------------------------------------------------------------

    fun readLayoutConfig(prefix: String): LayoutConfig {
        return LayoutConfig(
            prefix   = prefix,
            width    = getFloat("${prefix}_w", defaultWidth(prefix)),
            height   = getFloat("${prefix}_h", defaultHeight(prefix)),
            x        = getFloat("${prefix}_x", 0f),
            y        = getFloat("${prefix}_y", 48f),
            padTop   = getFloat("pad_t", 0f),
            padBottom = getFloat("pad_b", 0f),
            padLeft  = getFloat("pad_l", 0f),
            padRight = getFloat("pad_r", 0f),
            ringThickness = getFloat("ring_thickness", 6f),
            expandUpwards = getBoolean("expand_upwards", false)
        )
    }

    fun readThemeConfig(): ThemeConfig {
        return ThemeConfig(
            buttonSize       = getFloat("theme_button_size", 48f),
            buttonSpacing    = getFloat("theme_button_spacing", 16f),
            buttonRadius     = getFloat("theme_button_radius", 50f),
            animType         = getString("theme_anim_type", "BOUNCE"),
            cornerRadius     = getFloat("theme_corner_radius", 50f),
            textPrimary      = getFloat("theme_text_primary", 16f),
            textSecondary    = getFloat("theme_text_secondary", 14f),
            progressThick    = getFloat("theme_progress_thick", 4f),
            ringThick        = getFloat("theme_ring_thick", 12f),
            elementGap       = getFloat("theme_element_gap", 8f),
            musicTitleSize   = getFloat("theme_music_title", 16f),
            musicArtistSize  = getFloat("theme_music_artist", 14f),
            musicSeekerThick = getFloat("theme_music_seeker", 4f),
            batTextSize      = getFloat("theme_bat_text", 16f),
            batIconSize      = getFloat("theme_bat_icon", 36f),
            batRingThick     = getFloat("theme_bat_ring", 12f),
            alertTitleSize   = getFloat("theme_alert_title", 16f),
            alertMsgSize     = getFloat("theme_alert_msg", 14f),
            hapticStrength   = getInt("haptic_strength", 1),
            chargingStyle    = getString("charging_style", "CUBE"),
            blurIntensity    = getFloat("blur_intensity", 16f),
            hideOnLandscape  = getBoolean("hide_landscape", false),
            glassmorphism    = getBoolean("glass_mode", true),
            springDamping    = getFloat("spring_damping", 0.85f),
            springStiffness  = getFloat("spring_stiffness", 400f),
            enableMedia      = getBoolean("enable_media", true),
            enableCharging   = getBoolean("enable_charging", true),
            enableAlerts     = getBoolean("enable_alerts", true),
            enableTimers     = getBoolean("enable_timers", true),
            rotateCube       = getBoolean("rotate_cube", true),
            idleSwipeAction  = getString("idle_swipe_action", "BRIGHTNESS"),
            longPressAction  = getString("long_press_action", "SCREENSHOT")
        )
    }

    fun readGestureMatrix(): Map<String, String> {
        val cursor = context.contentResolver.query(
            IslandContentProvider.gestureUri(), null, null, null, null
        ) ?: return emptyMap()

        val map = mutableMapOf<String, String>()
        cursor.use {
            val keyIdx = it.getColumnIndex(IslandContentProvider.COL_KEY)
            val valIdx = it.getColumnIndex(IslandContentProvider.COL_VALUE)
            while (it.moveToNext()) {
                map[it.getString(keyIdx)] = it.getString(valIdx)
            }
        }
        return map
    }

    fun readDashboardConfig(): DashboardConfig {
        val pinnedApps = (0..7).map { getString("pinned_app_$it", "") }
        val qsTileSpecs  = (0..6).map { getString("qs_tile_spec_$it", "") }
        val qsTileLabels = (0..6).map { getString("qs_tile_label_$it", defaultQsLabel(it)) }
        return DashboardConfig(pinnedApps, qsTileSpecs.zip(qsTileLabels))
    }

    // -------------------------------------------------------------------------
    // Snapshot sync — used when SystemUI first boots and needs all config at once
    // -------------------------------------------------------------------------

    fun fullSnapshot(): JSONObject {
        val result = callProvider("GET_SNAPSHOT", null, null)
        val raw = result?.getString("v") ?: return JSONObject()
        return try { JSONObject(raw) } catch (e: Exception) { JSONObject() }
    }

    fun loadFromSnapshot(snapshot: JSONObject) {
        snapshot.keys().forEach { key ->
            try {
                val entry = snapshot.getJSONObject(key)
                val value = entry.getString("v")
                val type  = entry.getString("t")
                when (type) {
                    IslandContentProvider.TYPE_FLOAT   -> cache[key] = value.toFloat()
                    IslandContentProvider.TYPE_INT     -> cache[key] = value.toInt()
                    IslandContentProvider.TYPE_BOOLEAN -> cache[key] = value.toBoolean()
                    IslandContentProvider.TYPE_JSON    -> cache[key] = JSONObject(value)
                    else                               -> cache[key] = value
                }
            } catch (e: Exception) { /* skip malformed entry */ }
        }
        Log.i(TAG, "Loaded ${cache.size} keys from full snapshot")
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun callProvider(method: String, arg: String?, extras: Bundle?): Bundle? {
        return try {
            context.contentResolver.call(
                IslandContentProvider.BASE_URI,
                method,
                arg,
                extras
            )
        } catch (e: Exception) {
            Log.w(TAG, "Provider call failed: $method / $arg — ${e.message}")
            null
        }
    }

    private suspend fun warmCache() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = fullSnapshot()
                loadFromSnapshot(snapshot)
                // Emit initial typed configs
                _layoutFlow.value   = readLayoutConfig("mini")
                _themeFlow.value    = readThemeConfig()
                _gestureFlow.value  = readGestureMatrix()
                _dashboardFlow.value = readDashboardConfig()
                Log.i(TAG, "Cache warmed with ${cache.size} entries")
            } catch (e: Exception) {
                Log.w(TAG, "Cache warm failed: ${e.message}")
            }
        }
    }

    private fun reloadLayout() {
        scope.launch(Dispatchers.IO) {
            _layoutFlow.value = readLayoutConfig("mini")
        }
    }

    private fun reloadTheme() {
        scope.launch(Dispatchers.IO) {
            _themeFlow.value = readThemeConfig()
        }
    }

    private fun reloadGestures() {
        scope.launch(Dispatchers.IO) {
            _gestureFlow.value = readGestureMatrix()
        }
    }

    private fun reloadDashboard() {
        scope.launch(Dispatchers.IO) {
            _dashboardFlow.value = readDashboardConfig()
        }
    }

    private fun defaultWidth(prefix: String) = when (prefix) {
        "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "cube" -> 85f; else -> 0f
    }

    private fun defaultHeight(prefix: String) = when (prefix) {
        "ring" -> 45f; "mini" -> 36f;  "mid" -> 80f;  "max" -> 220f; "cube" -> 85f; else -> 0f
    }

    private fun defaultQsLabel(index: Int) = listOf(
        "WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings"
    ).getOrElse(index) { "" }

    fun destroy() {
        try { context.contentResolver.unregisterContentObserver(observer) } catch (e: Exception) {}
        scope.cancel()
    }
}

// -------------------------------------------------------------------------
// Typed config data classes — replaces raw float/string passing everywhere
// -------------------------------------------------------------------------

data class LayoutConfig(
    val prefix: String,
    val width: Float,
    val height: Float,
    val x: Float,
    val y: Float,
    val padTop: Float,
    val padBottom: Float,
    val padLeft: Float,
    val padRight: Float,
    val ringThickness: Float,
    val expandUpwards: Boolean
)

data class ThemeConfig(
    val buttonSize: Float,
    val buttonSpacing: Float,
    val buttonRadius: Float,
    val animType: String,
    val cornerRadius: Float,
    val textPrimary: Float,
    val textSecondary: Float,
    val progressThick: Float,
    val ringThick: Float,
    val elementGap: Float,
    val musicTitleSize: Float,
    val musicArtistSize: Float,
    val musicSeekerThick: Float,
    val batTextSize: Float,
    val batIconSize: Float,
    val batRingThick: Float,
    val alertTitleSize: Float,
    val alertMsgSize: Float,
    val hapticStrength: Int,
    val chargingStyle: String,
    val blurIntensity: Float,
    val hideOnLandscape: Boolean,
    val glassmorphism: Boolean,
    val springDamping: Float,
    val springStiffness: Float,
    val enableMedia: Boolean,
    val enableCharging: Boolean,
    val enableAlerts: Boolean,
    val enableTimers: Boolean,
    val rotateCube: Boolean,
    val idleSwipeAction: String,
    val longPressAction: String
)

data class DashboardConfig(
    val pinnedApps: List<String>,
    val qsTiles: List<Pair<String, String>>  // spec → label
)
