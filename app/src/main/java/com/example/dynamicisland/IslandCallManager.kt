package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

class IslandCallManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val onCallStateChanged: (LiveActivityModel.Call?) -> Unit
) {
    private var currentCall: LiveActivityModel.Call? = null
    private val callHandler = Handler(Looper.getMainLooper())
    
    // 🎛️ Kernel-Safe Universal Call Detector
    fun startMonitoring(isScreenOn: () -> Boolean) {
        val callCheckRunnable = object : Runnable {
            override fun run() {
                try {
                    val mode = audioManager.mode
                    val isRinging = mode == AudioManager.MODE_RINGTONE
                    val isInCall = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION

                    if (isRinging) {
                        if (currentCall?.state != "RINGING") {
                            currentCall = LiveActivityModel.Call(state = "RINGING", startTime = 0L)
                            onCallStateChanged(currentCall)
                        }
                    } else if (isInCall) {
                        if (currentCall?.state != "ONGOING") {
                            currentCall = LiveActivityModel.Call(state = "ONGOING", startTime = System.currentTimeMillis())
                            onCallStateChanged(currentCall)
                        }
                    } else {
                        if (currentCall != null) {
                            currentCall = null
                            onCallStateChanged(null)
                        }
                    }
                } catch (e: Exception) {}

                // Re-queue the check safely
                if (isScreenOn()) {
                    callHandler.postDelayed(this, 1000)
                } else {
                    callHandler.postDelayed(this, 5000) // Super efficient while screen is off
                }
            }
        }
        callHandler.post(callCheckRunnable)
    }

    fun openNativeCallUI(islandView: DynamicIslandView?) {
        try {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            tm.showInCallScreen(false) 
            islandView?.setState(IslandState.TYPE_1_MINI)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            islandView?.setState(IslandState.TYPE_1_MINI)
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
}
