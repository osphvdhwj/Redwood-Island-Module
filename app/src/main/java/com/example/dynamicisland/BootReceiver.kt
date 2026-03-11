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
                // Fix SELinux File Permissions
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (prefsDir.exists()) { prefsDir.setExecutable(true, false); prefsDir.setReadable(true, false) }
                val prefsFile = File(prefsDir, "island_prefs.xml")
                if (prefsFile.exists()) prefsFile.setReadable(true, false)

                val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                @android.annotation.SuppressLint("WrongConstant")
                val updateIntent = Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
                    addFlags(0x01000000)
                    setPackage("com.android.systemui") // 🚀 Target SystemUI explicitly
                }

                val matrix = JSONObject()
                prefs.all.forEach { (key, value) ->
                    // Pack Gestures into JSON
                    if (key.startsWith("TYPE_") && value is String) {
                        matrix.put(key, value)
                    }
                    // Pack Themes as Float Extras natively
                    if ((key.startsWith("theme_") || key.startsWith("tweak_")) && value is Float) {
                        updateIntent.putExtra(key, value)
                    }
                }
                updateIntent.putExtra("gesture_payload", matrix.toString())

                context.sendBroadcast(updateIntent)
            } catch (e: Exception) {}
        }
    }
}
