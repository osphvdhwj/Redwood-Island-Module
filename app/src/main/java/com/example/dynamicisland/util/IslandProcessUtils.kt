package com.example.dynamicisland.util

import android.app.ActivityManager
import android.content.Context

object IslandProcessUtils {
    
    /**
     * Returns true if we are running inside our OWN app process.
     * Returns false if we are injected into SystemUI or any other process.
     */
    fun isOwnProcess(context: Context): Boolean {
        return try {
            context.packageName == "com.example.dynamicisland"
        } catch (_: Throwable) {
            false
        }
    }
}