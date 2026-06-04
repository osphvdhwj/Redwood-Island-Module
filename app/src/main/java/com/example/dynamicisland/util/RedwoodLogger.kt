package com.example.dynamicisland.util

import android.util.Log
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * STAFF-LEVEL DIAGNOSTIC ENGINE
 *
 * Provides high-performance, structured logging with prioritized output
 * and optional local crash dumping for on-device debugging.
 */
object RedwoodLogger {
    private const val TAG = "RedwoodIsland"
    private const val LOG_DIR = "/sdcard/Redwood/logs"
    
    enum class Priority(val prefix: String) {
        DEBUG("🔍 DEBUG"),
        INFO("ℹ️ INFO"),
        WARN("⚠️ WARN"),
        ERROR("❌ ERROR"),
        FATAL("💀 FATAL")
    }

    fun d(message: String) = log(Priority.DEBUG, message)
    fun i(message: String) = log(Priority.INFO, message)
    fun w(message: String) = log(Priority.WARN, message)
    fun e(message: String, throwable: Throwable? = null) = log(Priority.ERROR, message, throwable)
    fun f(message: String, throwable: Throwable? = null) = log(Priority.FATAL, message, throwable)

    private fun log(priority: Priority, message: String, throwable: Throwable? = null) {
        val formattedMessage = "${priority.prefix}: $message"
        
        // 1. Always log to XposedBridge (Industry Standard)
        XposedBridge.log("$TAG $formattedMessage")
        if (throwable != null) {
            XposedBridge.log(throwable)
        }

        // 2. Log to Logcat for real-time monitoring
        when (priority) {
            Priority.DEBUG -> Log.d(TAG, message)
            Priority.INFO -> Log.i(TAG, message)
            Priority.WARN -> Log.w(TAG, message)
            Priority.ERROR, Priority.FATAL -> Log.e(TAG, message, throwable)
        }

        // 3. Conditional Crash Dumping
        if (priority == Priority.FATAL) {
            dumpToDisk(formattedMessage, throwable)
        }
    }

    private fun dumpToDisk(message: String, throwable: Throwable?) {
        try {
            val dir = File(LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "crash_$timestamp.txt")
            
            val content = buildString {
                appendLine("TIMESTAMP: $timestamp")
                appendLine("MESSAGE: $message")
                if (throwable != null) {
                    appendLine("STACKTRACE:")
                    appendLine(Log.getStackTraceString(throwable))
                }
            }
            
            file.writeText(content)
        } catch (e: Exception) {
            XposedBridge.log("$TAG ⚠️ Failed to dump crash to disk: ${e.message}")
        }
    }
}
