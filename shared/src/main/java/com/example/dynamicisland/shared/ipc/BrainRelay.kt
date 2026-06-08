package com.example.dynamicisland.shared.ipc

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * 🛰️ BRAIN RELAY (Satellite-side)
 * 
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
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
