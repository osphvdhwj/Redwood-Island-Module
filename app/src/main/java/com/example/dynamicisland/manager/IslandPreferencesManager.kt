package com.example.dynamicisland.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.robv.android.xposed.XSharedPreferences
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView
import com.example.dynamicisland.settings.ChargingStyle

object IslandPreferencesManager {

    fun load(view: DynamicIslandView) {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable()
            pref.reload()
            
            // Layout Dimensions
            view.ringW.value = pref.getFloat("ring_w", 45f); view.ringH.value = pref.getFloat("ring_h", 45f); view.ringX.value = pref.getFloat("ring_x", 0f); view.ringY.value = pref.getFloat("ring_y", 48f)
            view.miniW.value = pref.getFloat("mini_w", 180f); view.miniH.value = pref.getFloat("mini_h", 36f); view.miniX.value = pref.getFloat("mini_x", 0f); view.miniY.value = pref.getFloat("mini_y", 48f)
            view.midW.value = pref.getFloat("mid_w", 320f); view.midH.value = pref.getFloat("mid_h", 80f); view.midX.value = pref.getFloat("mid_x", 0f); view.midY.value = pref.getFloat("mid_y", 48f)
            view.maxW.value = pref.getFloat("max_w", 360f); view.maxH.value = pref.getFloat("max_h", 220f); view.maxX.value = pref.getFloat("max_x", 0f); view.maxY.value = pref.getFloat("max_y", 48f)
            view.cubeW.value = pref.getFloat("cube_w", 85f); view.cubeH.value = pref.getFloat("cube_h", 85f); view.cubeX.value = pref.getFloat("cube_x", 0f); view.cubeY.value = pref.getFloat("cube_y", 48f)
            
            view.padT.value = pref.getFloat("pad_t", 0f); view.padB.value = pref.getFloat("pad_b", 0f); view.padL.value = pref.getFloat("pad_l", 0f); view.padR.value = pref.getFloat("pad_r", 0f)
            view.ringThickness.value = pref.getFloat("ring_thickness", 6f)
            view.expandUpwards.value = pref.getBoolean("expand_upwards", false)
            view.isCubeRotationEnabled.value = pref.getBoolean("rotate_cube", true)

            view.activeTheme.value = IslandTheme(
                buttonSize = pref.getFloat("theme_button_size", 48f).dp,
                buttonSpacing = pref.getFloat("theme_button_spacing", 16f).dp,
                buttonCornerRadius = pref.getFloat("theme_button_radius", 50f).dp,
                actionAnimType = pref.getString("theme_anim_type", "BOUNCE") ?: "BOUNCE",
                cornerRadius = pref.getFloat("theme_corner_radius", 50f).dp,
                textPrimary = pref.getFloat("theme_text_primary", 16f).sp,
                textSecondary = pref.getFloat("theme_text_secondary", 14f).sp,
                progressThick = pref.getFloat("theme_progress_thick", 4f).dp,
                ringThick = pref.getFloat("theme_ring_thick", 12f).dp,
                elementGap = pref.getFloat("theme_element_gap", 8f).dp,
                musicTitleSize = pref.getFloat("theme_music_title", 16f).sp,
                musicArtistSize = pref.getFloat("theme_music_artist", 14f).sp,
                musicSeekerThick = pref.getFloat("theme_music_seeker", 4f).dp,
                batTextSize = pref.getFloat("theme_bat_text", 16f).sp,
                batIconSize = pref.getFloat("theme_bat_icon", 36f).dp,
                batRingThick = pref.getFloat("theme_bat_ring", 12f).dp,
                alertTitleSize = pref.getFloat("theme_alert_title", 16f).sp,
                alertMsgSize = pref.getFloat("theme_alert_msg", 14f).sp,
                hapticStrength = pref.getInt("haptic_strength", 1),
                chargingStyle = try { ChargingStyle.valueOf(pref.getString("charging_style", "CUBE") ?: "CUBE") } catch (e: Exception) { ChargingStyle.CUBE },
                blurIntensity = pref.getFloat("blur_intensity", 16f).dp,
                hideOnLandscape = pref.getBoolean("hide_landscape", false),
                isGlassmorphism = pref.getBoolean("glass_mode", true),
                springDamping = pref.getFloat("spring_damping", 0.85f),
                springStiffness = pref.getFloat("spring_stiffness", 400f)
            )
        } catch (e: Exception) {}
    }

    fun getReceiver(view: DynamicIslandView): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                    
                    // 🚀 FIX: Check if this intent actually carries live data. If not, it's just a file-reload ping.
                    if (!intent.hasExtra("theme_button_size") && !intent.hasExtra("prefix")) {
                        load(view)
                        view.onGestureSettingsUpdated?.invoke(null)
                        return
                    }

                    val prefix = intent.getStringExtra("prefix")
                    if (prefix != null) {
                        val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f); val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                        when (prefix) { "ring" -> { view.ringW.value = w; view.ringH.value = h; view.ringX.value = x; view.ringY.value = y }; "mini" -> { view.miniW.value = w; view.miniH.value = h; view.miniX.value = x; view.miniY.value = y }; "mid" -> { view.midW.value = w; view.midH.value = h; view.midX.value = x; view.midY.value = y }; "max" -> { view.maxW.value = w; view.maxH.value = h; view.maxX.value = x; view.maxY.value = y }; "cube" -> { view.cubeW.value = w; view.cubeH.value = h; view.cubeX.value = x; view.cubeY.value = y } }
                        view.padT.value = intent.getFloatExtra("pad_t", view.padT.value); view.padB.value = intent.getFloatExtra("pad_b", view.padB.value); view.padL.value = intent.getFloatExtra("pad_l", view.padL.value); view.padR.value = intent.getFloatExtra("pad_r", view.padR.value)
                        view.ringThickness.value = intent.getFloatExtra("ring_thickness", view.ringThickness.value)
                        view.expandUpwards.value = intent.getBooleanExtra("expand_upwards", view.expandUpwards.value)
                        for (i in 0..7) { val pkg = intent.getStringExtra("pinned_app_$i"); if (pkg != null) view.pinnedApps[i] = pkg }
                        for (i in 0..6) { val qs = intent.getStringExtra("qs_tile_$i"); if (qs != null) view.qsTiles[i] = qs } 
                    } 

                    // Ensure we don't accidentally overwrite the active theme with zeros if intent is malformed
                    if (intent.hasExtra("theme_button_size")) {
                        view.activeTheme.value = IslandTheme(
                            buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp,
                            buttonSpacing = intent.getFloatExtra("theme_button_spacing", 16f).dp,
                            buttonCornerRadius = intent.getFloatExtra("theme_button_radius", 50f).dp,
                            actionAnimType = intent.getStringExtra("theme_anim_type") ?: "BOUNCE",
                            cornerRadius = intent.getFloatExtra("theme_corner_radius", 50f).dp,
                            textPrimary = intent.getFloatExtra("theme_text_primary", 16f).sp,
                            textSecondary = intent.getFloatExtra("theme_text_secondary", 14f).sp,
                            progressThick = intent.getFloatExtra("theme_progress_thick", 4f).dp,
                            ringThick = intent.getFloatExtra("theme_ring_thick", 12f).dp,
                            elementGap = intent.getFloatExtra("theme_element_gap", 8f).dp,
                            musicTitleSize = intent.getFloatExtra("theme_music_title", 16f).sp,
                            musicArtistSize = intent.getFloatExtra("theme_music_artist", 14f).sp,
                            musicSeekerThick = intent.getFloatExtra("theme_music_seeker", 4f).dp,
                            batTextSize = intent.getFloatExtra("theme_bat_text", 16f).sp,
                            batIconSize = intent.getFloatExtra("theme_bat_icon", 36f).dp,
                            batRingThick = intent.getFloatExtra("theme_bat_ring", 12f).dp,
                            alertTitleSize = intent.getFloatExtra("theme_alert_title", 16f).sp,
                            alertMsgSize = intent.getFloatExtra("theme_alert_msg", 14f).sp,
                            hapticStrength = intent.getIntExtra("haptic_strength", 1),
                            chargingStyle = try { ChargingStyle.valueOf(intent.getStringExtra("charging_style") ?: "CUBE") } catch (e: Exception) { ChargingStyle.CUBE },
                            blurIntensity = intent.getFloatExtra("blur_intensity", 16f).dp,
                            hideOnLandscape = intent.getBooleanExtra("hide_landscape", false),
                            isGlassmorphism = intent.getBooleanExtra("glass_mode", true),
                            springDamping = intent.getFloatExtra("spring_damping", 0.85f),
                            springStiffness = intent.getFloatExtra("spring_stiffness", 400f)
                        )
                    }

                    val payload = intent.getStringExtra("gesture_payload")
                    if (payload != null) view.onGestureSettingsUpdated?.invoke(payload)
                }
            }
        }
    }
}