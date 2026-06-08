package com.example.dynamicisland.core.util

import android.app.ActivityManager
import android.content.Context

object IslandProcessUtils {
    
    /**
     * Returns true if we are running inside our OWN app process.
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
     * Returns false if we are injected into SystemUI or any other process.
     */
    fun isOwnProcess(context: Context): Boolean {
        return try {
            context.packageName == "com.example.dynamicisland"
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks if the device has root access available.
     */
    fun isRootAvailable(): Boolean {
        return try {
            val p = Runtime.getRuntime().exec("su -c id")
            p.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}