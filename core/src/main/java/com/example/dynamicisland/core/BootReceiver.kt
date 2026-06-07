package com.example.dynamicisland.core
import com.example.dynamicisland.core.manager.ConfigManager
import com.example.dynamicisland.core.manager.NewConfigManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.MainScope

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // 🏓 PONG: When SystemUI boots, updates, or explicitly asks for data, send it instantly!
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "com.example.dynamicisland.REQUEST_PREFS") {
            
            // Migrate if needed
            NewConfigManager.migrateFromSharedPreferences(context)
            
            val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
            val scope = MainScope()

            // Block 1: Sync Gestures (Isolated)
            try {
                NewConfigManager.sendGestureUpdate(context, prefs)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            
            // Block 2: Sync Dashboard & Pinned Apps (Isolated)
            try {
                NewConfigManager.broadcastUpdateSingle(context, prefs, "dashboard")
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            // Block 3: Sync Layout Shapes & Themes (Isolated)
            try {
                listOf("ring", "mini", "mid", "max", "cube").forEach { prefix ->
                    val w = prefs.getFloat("${prefix}_w", NewConfigManager.getDefaultWidth(prefix))
                    val h = prefs.getFloat("${prefix}_h", NewConfigManager.getDefaultHeight(prefix))
                    val x = prefs.getFloat("${prefix}_x", 0f)
                    val y = prefs.getFloat("${prefix}_y", 48f)
                    val ringT = prefs.getFloat("ring_thickness", 6f)
                    val r = prefs.getFloat("${prefix}_r", NewConfigManager.getDefaultRadius(prefix))
                    
                    NewConfigManager.saveAndBroadcast(prefs, scope, context, prefix, w, h, x, y, r, ringT)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
