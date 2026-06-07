package com.example.dynamicisland.shared.ipc

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * 🛰️ BRAIN RELAY (Satellite-side)
 * 
 * A lightweight utility to send events from Ghost Satellites to the Core Brain.
 * Uses optimized broadcasts to ensure compatibility across all injected processes.
 */
object BrainRelay {
    private const val ACTION_EVENT = "com.example.dynamicisland.BRAIN_EVENT"
    private const val PERMISSION = "com.redwood.permission.SECURE_IPC"

    fun dispatch(context: Context, action: String, extras: Bundle = Bundle()) {
        val intent = Intent(ACTION_EVENT).apply {
            setPackage("com.example.dynamicisland.core")
            putExtra("action", action)
            putExtras(extras)
        }
        context.sendBroadcast(intent, PERMISSION)
    }
}
