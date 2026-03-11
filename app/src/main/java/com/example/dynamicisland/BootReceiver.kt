package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.io.File

class BootReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("WrongConstant")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            try {
                // 1. Fix SELinux File Permissions for Xposed automatically on boot
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (prefsDir.exists()) { prefsDir.setExecutable(true, false); prefsDir.setReadable(true, false) }
                val prefsFile = File(prefsDir, "island_prefs.xml")
                if (prefsFile.exists()) prefsFile.setReadable(true, false)

                // 2. Read the unlocked preferences
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                val matrix = JSONObject()
                
                // 3. Build the intent to wake up the Island in SystemUI
                val payloadIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
                    addFlags(0x01000000)
                    setPackage("com.android.systemui") // 🚀 Target SystemUI directly
                    
                    prefs.all.forEach { (key, value) ->
                        // 🚀 Pack Gestures into JSON (Only TYPE_ keys!)
                        if (key.startsWith("TYPE_") && value is String) {
                            matrix.put(key, value)
                        }
                        // 🚀 Pack Themes & Tweaks as Float Extras (Prevents the JSON Parsing Crash!)
                        if (key.startsWith("theme_") || key.startsWith("tweak_")) {
                            if (value is Float) putExtra(key, value)
                            if (value is Int) putExtra(key, value.toFloat())
                        }
                    }
                    putExtra("gesture_payload", matrix.toString())
                }
                
                context.sendBroadcast(payloadIntent)
            } catch (e: Exception) {}
        }
    }
}
