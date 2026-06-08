package com.example.dynamicisland.core.data.repository.profiles

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.core.util.shell.RootShellEngine
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔋 ULTRA BATTERY SAVER
 *
 * Extreme power conservation profile matching Xiaomi's 5% emergency mode.
 * Forces global settings overrides.
 */
@Singleton
class UltraBatterySaver @Inject constructor(
    private val rootEngine: RootShellEngine
) {
    private val TAG = "UltraBatterySaver"

    private var isEnabled = false

    suspend fun toggle(enable: Boolean): Boolean {
        if (isEnabled == enable) return true
        
        RedwoodLogger.i(TAG, "Toggling Ultra Battery Saver to: $enable")
        val commands = if (enable) {
            listOf(
                // Force 60Hz Max
                "settings put system peak_refresh_rate 60.0",
                "settings put system min_refresh_rate 60.0",
                // Force Dark Mode
                "settings put secure ui_night_mode 2",
                // Disable Auto Sync
                "settings put system auto_sync 0",
                // Restrict Background Data
                "cmd netpolicy set restrict-background true"
            )
        } else {
            listOf(
                // Restore defaults (assuming 120Hz native)
                "settings put system peak_refresh_rate 120.0",
                "settings put system min_refresh_rate 10.0",
                "settings put secure ui_night_mode 1", // Auto/Standard
                "settings put system auto_sync 1",
                "cmd netpolicy set restrict-background false"
            )
        }

        val success = rootEngine.runSequence(commands)
        if (success) isEnabled = enable
        return success
    }
    
    fun isActive() = isEnabled
}
