package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.SharedPreferences

/**
 * 🛠️ LEGACY CONFIG MANAGER (For Backup Compatibility)
 */
object ConfigManager {
    fun getPrefs(context: Context): SharedPreferences = 
        context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
}
