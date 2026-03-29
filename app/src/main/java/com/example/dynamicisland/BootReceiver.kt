package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.MainScope

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // 🏓 PING-PONG: When SystemUI boots, updates, or requests data, send it instantly!
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "com.example.dynamicisland.REQUEST_PREFS") {
            
            try {
                // 1. Read the storage safely from the app's own context
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                
                // 2. Instantly blast ALL Premium Theme Data, Toggles, and Gestures
                ConfigManager.sendGestureUpdate(context, prefs)
                
                // 3. Loop through every shape size and push it to SystemUI correctly
                val scope = MainScope()
                listOf("ring", "mini", "mid", "max", "cube").forEach { prefix ->
                    val w = prefs.getFloat("${prefix}_w", ConfigManager.getDefaultWidth(prefix))
                    val h = prefs.getFloat("${prefix}_h", ConfigManager.getDefaultHeight(prefix))
                    val x = prefs.getFloat("${prefix}_x", 0f)
                    val y = prefs.getFloat("${prefix}_y", 48f)
                    val ringT = prefs.getFloat("ring_thickness", 6f)
                    val expandUp = prefs.getBoolean("expand_upwards", false)
                    
                    // The Manager handles packing and broadcasting securely
                    ConfigManager.saveAndBroadcast(prefs, scope, context, prefix, w, h, x, y, ringT, expandUp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
