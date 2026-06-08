package com.example.dynamicisland.satellite

import android.content.Context
import android.os.Bundle

/**
 * 🛰️ GHOST SATELLITE INTERFACE (Omni-Integration)
 *
 * A dependency-free interface for app-specific sensors. 
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
