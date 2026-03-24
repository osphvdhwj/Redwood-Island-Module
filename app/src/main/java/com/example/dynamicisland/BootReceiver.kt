package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Wake up if the phone boots, the app updates, or SystemUI explicitly asks for settings
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            intent.action == "com.example.dynamicisland.REQUEST_PREFS") {
            
            // 1. Read the storage safely from the app's own context
            val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
            
            // 2. Loop through every shape size and push it to SystemUI
            val prefixes = listOf("ring", "mini", "mid", "max", "cube")
            
            prefixes.forEach { prefix ->
                val syncIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
                    @Suppress("WrongConstant")
                    addFlags(Intent.FLAG_RECEIVER_FOREGROUND or 0x01000000)
                    setPackage("com.android.systemui")

                    putExtra("prefix", prefix)
                    
                    // Fallbacks just in case it's a completely fresh install
                    val defW = when(prefix) { "ring" -> 45f; "mini" -> 180f; "mid" -> 320f; "max" -> 360f; "cube" -> 85f; else -> 0f }
                    val defH = when(prefix) { "ring" -> 45f; "mini" -> 36f; "mid" -> 80f; "max" -> 220f; "cube" -> 85f; else -> 0f }

                    // Package Dimensions
                    putExtra("w", prefs.getFloat("${prefix}_w", defW))
                    putExtra("h", prefs.getFloat("${prefix}_h", defH))
                    putExtra("x", prefs.getFloat("${prefix}_x", 0f))
                    putExtra("y", prefs.getFloat("${prefix}_y", 48f))

                    // Package UI Tweaks & Themes
                    putExtra("ring_thickness", prefs.getFloat("ring_thickness", 6f))
                    putExtra("expand_upwards", prefs.getBoolean("expand_upwards", false))
                    putExtra("pad_t", prefs.getFloat("pad_t", 0f))
                    putExtra("pad_b", prefs.getFloat("pad_b", 0f))
                    putExtra("pad_l", prefs.getFloat("pad_l", 0f))
                    putExtra("pad_r", prefs.getFloat("pad_r", 0f))

                    putExtra("theme_button_size", prefs.getFloat("theme_button_size", 48f))
                    putExtra("theme_button_spacing", prefs.getFloat("theme_button_spacing", 16f))
                    putExtra("theme_button_radius", prefs.getFloat("theme_button_radius", 50f))
                    putExtra("theme_anim_type", prefs.getString("theme_anim_type", "BOUNCE"))

                    // Package Gestures
                    val matrix = JSONObject()
                    prefs.all.forEach { (key, value) -> if (key.startsWith("TYPE_") && value is String) matrix.put(key, value) }
                    putExtra("gesture_payload", matrix.toString())

                    // Package Dashboard Apps
                    for (i in 0..7) putExtra("pinned_app_$i", prefs.getString("pinned_app_$i", ""))
                    val defaultQS = listOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
                    for (i in 0..6) putExtra("qs_tile_$i", prefs.getString("qs_tile_$i", defaultQS[i]))
                }
                // Send the payload securely to SystemUI
                context.sendBroadcast(syncIntent)
            }
        }
    }
}
