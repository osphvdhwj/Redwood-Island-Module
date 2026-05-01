package com.example.dynamicisland.manager

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.ipc.IslandIPCClient
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import kotlinx.coroutines.*

/**
 * BATCH 1: Replaces IslandPreferencesManager (which used XSharedPreferences).
 *
 * Reads all configuration from IslandIPCClient which reads from the
 * ContentProvider — no Xposed file access, no world-readable permissions.
 *
 * Also replaces the BroadcastReceiver-based RELOAD_PREFS mechanism with
 * a ContentObserver flow that fires automatically when any key changes.
 */
object IslandPreferencesManagerV2 {

    /**
     * Initial load — called once when DynamicIslandView is first created.
     * Reads a full snapshot from the ContentProvider and applies it to the view.
     */
    fun load(view: DynamicIslandView, context: Context) {
        val client = IslandIPCClient.get(context)

        // Layout dimensions for all pill types
        val prefixes = listOf("ring", "mini", "mid", "max", "cube")
        prefixes.forEach { prefix ->
            applyLayoutToView(view, prefix, client)
        }

        // Padding
        view.padT.value = client.getFloat("pad_t", 0f)
        view.padB.value = client.getFloat("pad_b", 0f)
        view.padL.value = client.getFloat("pad_l", 0f)
        view.padR.value = client.getFloat("pad_r", 0f)

        view.ringThickness.value        = client.getFloat("ring_thickness", 6f)
        view.expandUpwards.value        = client.getBoolean("expand_upwards", false)
        view.isCubeRotationEnabled.value = client.getBoolean("rotate_cube", true)

        // Theme
        view.activeTheme.value = buildThemeFromClient(client)

        // Pinned apps and QS tiles
        for (i in 0..7) {
            val pkg = client.getString("pinned_app_$i", "")
            if (view.pinnedApps.size > i) view.pinnedApps[i] = pkg
        }
        for (i in 0..6) {
            val label = client.getString("qs_tile_label_$i", defaultQsLabel(i))
            if (view.qsTiles.size > i) view.qsTiles[i] = label
        }
    }

    /**
     * Registers a ContentObserver-based watcher.
     * When ANY preference changes in the ContentProvider, the view is
     * updated immediately — no broadcast, no receiver registration needed.
     *
     * Returns a cleanup lambda — call it when the view detaches.
     */
    fun startWatching(view: DynamicIslandView, context: Context): () -> Unit {
        val client   = IslandIPCClient.get(context)
        val scope    = CoroutineScope(Dispatchers.Main + SupervisorJob())

        val job = scope.launch {
            client.changeFlow.collect { changedKey ->
                applyKeyChange(view, changedKey, client)
            }
        }

        val layoutJob = scope.launch {
            client.layoutFlow.filterNotNull().collect { layout ->
                applyLayoutToView(view, layout.prefix, client)
            }
        }

        val themeJob = scope.launch {
            client.themeFlow.filterNotNull().collect { _ ->
                view.activeTheme.value = buildThemeFromClient(client)
            }
        }

        val dashboardJob = scope.launch {
            client.dashboardFlow.filterNotNull().collect { dashboard ->
                dashboard.pinnedApps.forEachIndexed { i, pkg ->
                    if (view.pinnedApps.size > i) view.pinnedApps[i] = pkg
                }
                dashboard.qsTiles.forEachIndexed { i, (_, label) ->
                    if (view.qsTiles.size > i) view.qsTiles[i] = label
                }
                // Notify the view that gesture settings changed
                view.onGestureSettingsUpdated?.invoke(null)
            }
        }

        val gestureJob = scope.launch {
            client.gestureFlow.collect { matrix ->
                val payload = org.json.JSONObject()
                matrix.forEach { (k, v) -> payload.put(k, v) }
                view.onGestureSettingsUpdated?.invoke(payload.toString())
            }
        }

        // Return cleanup lambda
        return {
            job.cancel()
            layoutJob.cancel()
            themeJob.cancel()
            dashboardJob.cancel()
            gestureJob.cancel()
            scope.cancel()
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun applyLayoutToView(view: DynamicIslandView, prefix: String, client: IslandIPCClient) {
        val w = client.getFloat("${prefix}_w", NewConfigManager.getDefaultWidth(prefix))
        val h = client.getFloat("${prefix}_h", NewConfigManager.getDefaultHeight(prefix))
        val x = client.getFloat("${prefix}_x", 0f)
        val y = client.getFloat("${prefix}_y", 48f)

        when (prefix) {
            "ring" -> { view.ringW.value = w; view.ringH.value = h; view.ringX.value = x; view.ringY.value = y }
            "mini" -> { view.miniW.value = w; view.miniH.value = h; view.miniX.value = x; view.miniY.value = y }
            "mid"  -> { view.midW.value  = w; view.midH.value  = h; view.midX.value  = x; view.midY.value  = y }
            "max"  -> { view.maxW.value  = w; view.maxH.value  = h; view.maxX.value  = x; view.maxY.value  = y }
            "cube" -> { view.cubeW.value = w; view.cubeH.value = h; view.cubeX.value = x; view.cubeY.value = y }
        }
    }

    private fun applyKeyChange(view: DynamicIslandView, key: String, client: IslandIPCClient) {
        when {
            key.endsWith("_w") || key.endsWith("_h") ||
            key.endsWith("_x") || key.endsWith("_y") -> {
                val prefix = key.substringBefore("_")
                applyLayoutToView(view, prefix, client)
            }
            key == "pad_t" -> view.padT.value = client.getFloat("pad_t", 0f)
            key == "pad_b" -> view.padB.value = client.getFloat("pad_b", 0f)
            key == "pad_l" -> view.padL.value = client.getFloat("pad_l", 0f)
            key == "pad_r" -> view.padR.value = client.getFloat("pad_r", 0f)
            key == "ring_thickness"   -> view.ringThickness.value = client.getFloat("ring_thickness", 6f)
            key == "expand_upwards"   -> view.expandUpwards.value = client.getBoolean("expand_upwards", false)
            key == "rotate_cube"      -> view.isCubeRotationEnabled.value = client.getBoolean("rotate_cube", true)
            key.startsWith("theme_")  ||
            key == "glass_mode"       ||
            key == "spring_damping"   ||
            key == "spring_stiffness" ||
            key == "charging_style"   ||
            key == "blur_intensity"   -> {
                view.activeTheme.value = buildThemeFromClient(client)
            }
            key.startsWith("pinned_app_") -> {
                val index = key.removePrefix("pinned_app_").toIntOrNull() ?: return
                if (view.pinnedApps.size > index) {
                    view.pinnedApps[index] = client.getString(key, "")
                }
            }
            key.startsWith("qs_tile_label_") -> {
                val index = key.removePrefix("qs_tile_label_").toIntOrNull() ?: return
                if (view.qsTiles.size > index) {
                    view.qsTiles[index] = client.getString(key, defaultQsLabel(index))
                }
            }
            key.startsWith("TYPE_") ||
            key == "idle_swipe_action" ||
            key == "long_press_action" -> {
                // Rebuild gesture matrix
                val matrix = org.json.JSONObject()
                client.readGestureMatrix().forEach { (k, v) -> matrix.put(k, v) }
                view.onGestureSettingsUpdated?.invoke(matrix.toString())
            }
        }
    }

    private fun buildThemeFromClient(client: IslandIPCClient): IslandTheme {
        return IslandTheme(
            buttonSize       = client.getFloat("theme_button_size", 48f).dp,
            buttonSpacing    = client.getFloat("theme_button_spacing", 16f).dp,
            buttonCornerRadius = client.getFloat("theme_button_radius", 50f).dp,
            actionAnimType   = client.getString("theme_anim_type", "BOUNCE"),
            cornerRadius     = client.getFloat("theme_corner_radius", 50f).dp,
            textPrimary      = client.getFloat("theme_text_primary", 16f).sp,
            textSecondary    = client.getFloat("theme_text_secondary", 14f).sp,
            progressThick    = client.getFloat("theme_progress_thick", 4f).dp,
            ringThick        = client.getFloat("theme_ring_thick", 12f).dp,
            elementGap       = client.getFloat("theme_element_gap", 8f).dp,
            musicTitleSize   = client.getFloat("theme_music_title", 16f).sp,
            musicArtistSize  = client.getFloat("theme_music_artist", 14f).sp,
            musicSeekerThick = client.getFloat("theme_music_seeker", 4f).dp,
            batTextSize      = client.getFloat("theme_bat_text", 16f).sp,
            batIconSize      = client.getFloat("theme_bat_icon", 36f).dp,
            batRingThick     = client.getFloat("theme_bat_ring", 12f).dp,
            alertTitleSize   = client.getFloat("theme_alert_title", 16f).sp,
            alertMsgSize     = client.getFloat("theme_alert_msg", 14f).sp,
            hapticStrength   = client.getInt("haptic_strength", 1),
            chargingStyle    = client.getString("charging_style", "CUBE"),
            blurIntensity    = client.getFloat("blur_intensity", 16f).dp,
            hideOnLandscape  = client.getBoolean("hide_landscape", false),
            isGlassmorphism  = client.getBoolean("glass_mode", true),
            springDamping    = client.getFloat("spring_damping", 0.85f),
            springStiffness  = client.getFloat("spring_stiffness", 400f)
        )
    }

    private fun defaultQsLabel(index: Int) = listOf(
        "WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings"
    ).getOrElse(index) { "" }
}

// Extension to filter non-null from StateFlow
private fun <T> kotlinx.coroutines.flow.Flow<T?>.filterNotNull(): kotlinx.coroutines.flow.Flow<T> =
    kotlinx.coroutines.flow.transform { value -> if (value != null) emit(value) }
