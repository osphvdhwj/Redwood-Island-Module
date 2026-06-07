package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object ConfigBackupManager {

    suspend fun exportConfig(context: Context, prefs: SharedPreferences, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            prefs.all.forEach { (key, value) ->
                when (value) {
                    is Boolean -> json.put(key, value)
                    is Float -> json.put(key, value.toDouble()) // JSON doesn't have float, use double
                    is Int -> json.put(key, value)
                    is Long -> json.put(key, value)
                    is String -> json.put(key, value)
                }
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toString(4).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importConfig(context: Context, prefs: SharedPreferences, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonString.append(line)
                    }
                }
            }

            val json = JSONObject(jsonString.toString())
            val editor = prefs.edit()
            
            // Optional: clear existing preferences before importing
            // editor.clear() 

            json.keys().forEach { key ->
                val value = json.get(key)
                when (value) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
            
            // Trigger a full update
            ConfigManager.sendGestureUpdate(context, prefs)
            ConfigManager.broadcastUpdateSingle(context, prefs, "dashboard")
            ConfigManager.broadcastUpdateSingle(context, prefs, "theme")
            ConfigManager.broadcastUpdateSingle(context, prefs, "layout")
            ConfigManager.broadcastUpdateSingle(context, prefs, "intelligence")
            ConfigManager.broadcastUpdateSingle(context, prefs, "continuity")
            
            // Trigger a reboot broadcast for the main engine to pick up new core settings
            context.sendBroadcast(android.content.Intent("com.example.dynamicisland.RELOAD_PREFS").apply {
                 setPackage("com.android.systemui")
            })

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
