package com.example.dynamicisland.core.util

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * STAFF-LEVEL DIAGNOSTIC ENGINE
 * 
 * Provides high-performance, structured logging with prioritized output
 * and automated crash dumping for the Core App daemon.
 */
object RedwoodLogger {
    private const val TAG = "RedwoodCore"
    private const val LOG_DIR = "/sdcard/Redwood/logs"
    
    enum class Priority(val prefix: String) {
        DEBUG("🔍 DEBUG"),
        INFO("ℹ️ INFO"),
        WARN("⚠️ WARN"),
        ERROR("❌ ERROR"),
        FATAL("💀 FATAL")
    }

    fun d(message: String) = log(Priority.DEBUG, message)
    fun d(tag: String, message: String) = log(Priority.DEBUG, "[$tag] $message")
    
    fun i(message: String) = log(Priority.INFO, message)
    fun i(tag: String, message: String) = log(Priority.INFO, "[$tag] $message")
    
    fun w(message: String) = log(Priority.WARN, message)
    fun w(tag: String, message: String) = log(Priority.WARN, "[$tag] $message")
    
    fun e(message: String, throwable: Throwable? = null) = log(Priority.ERROR, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(Priority.ERROR, "[$tag] $message", throwable)
    
    fun f(message: String, throwable: Throwable? = null) = log(Priority.FATAL, message, throwable)

    private fun log(priority: Priority, message: String, throwable: Throwable? = null) {
        when (priority) {
            Priority.DEBUG -> Log.d(TAG, message)
            Priority.INFO -> Log.i(TAG, message)
            Priority.WARN -> Log.w(TAG, message)
            Priority.ERROR -> Log.e(TAG, message, throwable)
            Priority.FATAL -> Log.e(TAG, "FATAL: $message", throwable)
        }

        if (priority == Priority.FATAL) {
            dumpToDisk(message, throwable)
        }
    }

    private fun dumpToDisk(message: String, throwable: Throwable?) {
        try {
            val dir = File(LOG_DIR)
            if (!dir.exists()) dir.mkdirs()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "core_crash_$timestamp.txt")
            file.writeText("MESSAGE: $message\nSTACKTRACE: ${Log.getStackTraceString(throwable)}")
        } catch (_: Exception) {}
    }
}
