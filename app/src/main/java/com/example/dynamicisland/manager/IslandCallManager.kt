package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager

class IslandCallManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onCallStateChanged: (LiveActivityModel.Call?) -> Unit
) {
    private var currentCall: LiveActivityModel.Call? = null
    
    // 🎛️ NEW: Listens to our highly-efficient Framework Hook instead of Polling!
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.CALL_STATE_CHANGED") {
                val state = intent.getStringExtra("state") ?: return
                val caller = intent.getStringExtra("caller") ?: "Unknown Caller"
                
                when (state) {
                    "RINGING" -> {
                        currentCall = LiveActivityModel.Call(state = "RINGING", callerName = caller, startTime = 0L)
                        onCallStateChanged(currentCall)
                    }
                    "ONGOING" -> {
                        // Only reset start time if it wasn't already ongoing
                        val startTime = if (currentCall?.state == "ONGOING") currentCall!!.startTime else System.currentTimeMillis()
                        currentCall = LiveActivityModel.Call(state = "ONGOING", callerName = caller, startTime = startTime)
                        onCallStateChanged(currentCall)
                    }
                    "DISCONNECTED" -> {
                        currentCall = null
                        onCallStateChanged(null)
                    }
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter("com.example.dynamicisland.CALL_STATE_CHANGED")
        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(callReceiver, filter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(callReceiver, filter, securePermission, null)
        }
    }

    fun openNativeCallUI(islandView: DynamicIslandView?) {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            tm.showInCallScreen(false) 
            islandView?.setState(IslandState.TYPE_1_MINI
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            islandView?.setState(IslandState.TYPE_1_MINI
        }
    }

    fun endActiveCall() {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            tm.javaClass.getMethod("endCall").invoke(tm)
        } catch (e: Exception) {
            try {
                val eventDown = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
                val eventUp = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_HEADSETHOOK)
                audioManager.dispatchMediaKeyEvent(eventDown)
                audioManager.dispatchMediaKeyEvent(eventUp)
            } catch (e2: Exception) {}
        }
    }
    
    fun destroy() {
        try {
            context.unregisterReceiver(callReceiver)
        } catch (e: Exception) {}
    }
}
