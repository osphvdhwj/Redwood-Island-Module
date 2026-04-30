package com.example.dynamicisland.core.logging

import android.util.Log
import de.robv.android.xposed.XposedBridge

object IslandLogger {
    private const val GLOBAL_TAG = "RedwoodIsland"
    private var isDebug = true // Toggle this for production builds

    fun d(tag: String, message: String) {
        if (isDebug) {
            val fullMsg = "[$tag] $message"
            Log.d(GLOBAL_TAG, fullMsg)
            XposedBridge.log("$GLOBAL_TAG: $fullMsg")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = "[$tag] $message"
        Log.e(GLOBAL_TAG, fullMsg, throwable)
        XposedBridge.log("$GLOBAL_TAG ERROR: $fullMsg")
        throwable?.let { XposedBridge.log(it) }
    }

    fun i(tag: String, message: String) {
        val fullMsg = "[$tag] $message"
        Log.i(GLOBAL_TAG, fullMsg)
        XposedBridge.log("$GLOBAL_TAG: $fullMsg")
    }
}
