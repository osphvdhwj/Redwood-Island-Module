package com.example.dynamicisland.satellite

import android.content.Context
import android.os.Bundle

/**
 * 🛰️ GHOST SATELLITE INTERFACE (Omni-Integration)
 *
 * A dependency-free interface for app-specific sensors. 
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * Designed to be loaded into third-party processes (Gboard, Launcher) 
 * without triggering ClassNotFound errors for Hilt, Compose, or ML Kit.
 */
interface SatelliteBase {
    /**
     * Called when the Satellite is injected into the host process.
     */
    fun onInitialize(context: Context, hostPackageName: String)

    /**
     * Standardized event dispatcher to send data to the SystemUI Brain.
     */
    fun dispatchEvent(context: Context, eventType: String, data: Bundle)

    /**
     * Called when the host process is terminating or the hook is removed.
     */
    fun onDestroy()
}
