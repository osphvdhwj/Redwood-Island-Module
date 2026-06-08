package com.example.dynamicisland.core.data.repository.cleanup

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.core.util.shell.RootShellEngine
import com.example.dynamicisland.shared.ipc.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ❄️ APP FREEZER
 *
 * Suspends rogue background processes using 'pm suspend'.
 * Implements strict whitelisting for system stability.
 */
@Singleton
class AppFreezer @Inject constructor(
    private val rootEngine: RootShellEngine
) {
    private val TAG = "AppFreezer"

    private val SYSTEM_WHITELIST = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.example.dynamicisland.core",
        "com.example.dynamicisland",
        "com.google.android.gms",
        "com.android.vending",
        "com.android.launcher3",
        "com.miui.home" // HyperOS/MIUI support
    )

    /**
     * Suspends an app by package name.
     */
    suspend fun freezeApp(packageName: String): Boolean {
        if (SYSTEM_WHITELIST.contains(packageName)) {
            RedwoodLogger.w(TAG, "Attempted to freeze whitelisted app: $packageName. Aborting.")
            return false
        }
        
        RedwoodLogger.d(TAG, "Freezing app: $packageName")
        return rootEngine.runAction("pm suspend $packageName")
    }

    /**
     * Resumes a suspended app.
     */
    suspend fun unfreezeApp(packageName: String): Boolean {
        RedwoodLogger.d(TAG, "Unfreezing app: $packageName")
        return rootEngine.runAction("pm unsuspend $packageName")
    }

    /**
     * Suspends a list of apps sequentially.
     */
    suspend fun batchFreeze(packageNames: List<String>): Int {
        var count = 0
        packageNames.forEach { pkg ->
            if (freezeApp(pkg)) count++
        }
        return count
    }
}
