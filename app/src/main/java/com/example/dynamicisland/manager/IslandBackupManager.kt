package com.example.dynamicisland.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.dynamicisland.ipc.IslandIPCClient
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class IslandBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ipcClient: IslandIPCClient
) {
    private val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

    fun createBackup(includeNeuralWeights: Boolean = true): String? {
        try {
            val root = JSONObject()
            
            // 1. Settings Backup
            val settings = JSONObject()
            prefs.all.forEach { (k, v) -> 
                if (v is Set<*>) {
                    settings.put(k, v.joinToString(","))
                } else {
                    settings.put(k, v) 
                }
            }
            root.put("settings", settings)
            
            // 2. Neural Weights Backup (Request from SystemUI via IPC)
            if (includeNeuralWeights) {
                val weightsData = ipcClient.getAiWeights()
                if (weightsData != null) {
                    root.put("neural_weights", JSONObject(weightsData))
                }
            }
            
            root.put("backup_date", System.currentTimeMillis())
            root.put("version", 2)
            
            return root.toString(4)
        } catch (e: Exception) {
            return null
        }
    }

    fun restoreBackup(jsonString: String): Boolean {
        try {
            val root = JSONObject(jsonString)
            
            // 1. Restore Settings
            if (root.has("settings")) {
                val settings = root.getJSONObject("settings")
                val editor = prefs.edit()
                editor.clear()
                settings.keys().forEach { key ->
                    val v = settings.get(key)
                    when (v) {
                        is Boolean -> editor.putBoolean(key, v)
                        is Int -> editor.putInt(key, v)
                        is Double -> editor.putFloat(key, v.toFloat())
                        is Float -> editor.putFloat(key, v)
                        is Long -> editor.putLong(key, v)
                        is String -> {
                            if (key.endsWith("_APPS") || key == "HIDE_ISLAND_PER_APP") {
                                editor.putStringSet(key, v.split(",").toSet())
                            } else {
                                editor.putString(key, v)
                            }
                        }
                    }
                }
                editor.apply()
                
                // Sync to SystemUI
                val map = mutableMapOf<String, Any>()
                prefs.all.forEach { (k, v) -> if (v != null) map[k] = v }
                ipcClient.bulkPut(map)
            }
            
            // 2. Restore Neural Weights (Push to SystemUI via IPC)
            if (root.has("neural_weights")) {
                val weights = root.getJSONObject("neural_weights")
                ipcClient.setAiWeights(weights.toString(2))
            }
            
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getAutoBackupDirectory(): File {
        val path = prefs.getString("STASH_STORAGE_PATH", "/sdcard/DynamicIsland/Archive") ?: "/sdcard/DynamicIsland/Archive"
        val dir = File(path, "Backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun performAutoBackup() {
        val enabled = prefs.getBoolean("AUTO_BACKUP_ENABLED", false)
        if (!enabled) return
        
        val lastBackup = prefs.getLong("last_auto_backup", 0L)
        val freqDays = prefs.getInt("AUTO_BACKUP_FREQ_DAYS", 7)
        val intervalMs = freqDays * 24 * 60 * 60 * 1000L
        
        if (System.currentTimeMillis() - lastBackup > intervalMs) {
            val data = createBackup() ?: return
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "autobackup_${sdf.format(Date())}.json"
            try {
                File(getAutoBackupDirectory(), filename).writeText(data)
                prefs.edit().putLong("last_auto_backup", System.currentTimeMillis()).apply()
            } catch (e: Exception) {}
        }
    }
}
