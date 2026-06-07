package com.example.dynamicisland.core.data.repository.cleanup

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.core.util.shell.RootShellEngine
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🧹 RESIDUAL CLEANER
 *
 * Securely wipes system-level junk using root privileges.
 * Targets: Tombstones, Dropbox logs, and obsolete ART caches.
 */
@Singleton
class ResidualCleaner @Inject constructor(
    private val rootEngine: RootShellEngine
) {
    private val TAG = "ResidualCleaner"

    /**
     * Wipes critical system log directories.
     */
    suspend fun wipeSystemLogs(): Boolean {
        val targets = listOf(
            "/data/tombstones/*",
            "/data/system/dropbox/*",
            "/data/anr/*",
            "/data/log/*"
        )
        RedwoodLogger.d(TAG, "Wiping system logs...")
        return rootEngine.runSequence(targets.map { "rm -rf $it" })
    }

    /**
     * Wipes obsolete Dalvik/ART caches (.vdex).
     */
    suspend fun clearObsoleteCaches(): Boolean {
        RedwoodLogger.d(TAG, "Clearing Dalvik/ART .vdex caches...")
        return rootEngine.runAction("rm -rf /data/dalvik-cache/*/oat/*/*.vdex")
    }

    /**
     * Aggressive cleaning of all app caches.
     * Note: Uses wildcard, requires care.
     */
    suspend fun wipeAllAppCaches(): Boolean {
        RedwoodLogger.d(TAG, "Wiping all app caches (Extreme)...")
        val commands = listOf(
            "rm -rf /data/data/*/cache/*",
            "rm -rf /data/user_de/0/*/cache/*"
        )
        return rootEngine.runSequence(commands)
    }
}
