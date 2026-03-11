package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import java.io.File

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            try {
                // 1. Fix SELinux File Permissions for Xposed automatically on boot
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (prefsDir.exists()) { prefsDir.setExecutable(true, false); prefsDir.setReadable(true, false) }
                val prefsFile = File(prefsDir, "island_prefs.xml")
                if (prefsFile.exists()) prefsFile.setReadable(true, false)

                // 2. Broadcast to the Island to wake up and load prefs
                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                @android.annotation.SuppressLint("WrongConstant")
                val updateIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
                    addFlags(0x01000000)
                    setPackage("com.android.systemui") // 🚀 Target SystemUI directly to prevent intent dropping
                }

                val matrix = JSONObject()
                // Package all gesture and theme settings
                prefs.all.forEach { (key, value) ->
                    if ((key.contains("TYPE_") || key.contains("theme_")) && value is String) {
                        matrix.put(key, value)
                    }
                }
                updateIntent.putExtra("gesture_payload", matrix.toString())

                context.sendBroadcast(updateIntent)
            } catch (e: Exception) {}
        }
    }
}