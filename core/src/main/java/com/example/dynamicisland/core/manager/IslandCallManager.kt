package com.example.dynamicisland.core.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import com.example.dynamicisland.core.ui.DynamicIslandView
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandState
import java.io.InputStream
import kotlinx.coroutines.*

/**
 * 📞 ISLAND CALL MANAGER
 * 
 * Orchestrates telephony and VOIP call states.
 * Listens for system-wide call events and maps them to LiveActivityModels.
 */
class IslandCallManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val scope: CoroutineScope,
    private val onCallStateChanged: (LiveActivityModel.Call?) -> Unit
) {
    private var currentCall: LiveActivityModel.Call? = null
    var userCallingApp: String? = null
    
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val action = if (intent.action == "com.example.dynamicisland.BRAIN_EVENT") {
                intent.getStringExtra("action")
            } else {
                intent.action?.removePrefix("com.example.dynamicisland.")
            }

            if (action == "CALL_STATE_CHANGED") {
                val state = intent.getStringExtra("state") ?: return
                val caller = intent.getStringExtra("caller") ?: "Unknown Caller"
                val number = intent.getStringExtra("number")
                val photoUri = intent.getStringExtra("photoUri")
                val relLabel = intent.getStringExtra("relationLabel")
                val isSpam = intent.getBooleanExtra("isSpam", false)
                val pkg = intent.getStringExtra("pkg")
                
                val source = pkg?.let {
                    when {
                        it.contains("whatsapp") -> "WhatsApp"
                        it.contains("telegram") -> "Telegram"
                        it == userCallingApp -> "Default"
                        else -> null
                    }
                } ?: if (userCallingApp != null) "Default" else null
                
                if (state == "DISCONNECTED") {
                    currentCall?.contactPhoto?.recycle()
                    currentCall = null
                    onCallStateChanged(null)
                    return
                }

                scope.launch {
                    val photoBmp = photoUri?.let { loadContactPhoto(it) }
                    
                    val startTime = if (state == "ONGOING") {
                        if (currentCall?.state == "ONGOING") currentCall!!.startTime else System.currentTimeMillis()
                    } else 0L

                    val newCall = LiveActivityModel.Call(
                        callerName = caller,
                        phoneNumber = number,
                        state = state,
                        startTime = startTime,
                        sourceApp = source,
                        photoUri = photoUri,
                        contactPhoto = photoBmp,
                        relationLabel = relLabel,
                        isSpam = isSpam
                    )
                    
                    currentCall = newCall
                    withContext(Dispatchers.Main) {
                        onCallStateChanged(currentCall)
                    }
                }
            }
        }
    }

    private suspend fun loadContactPhoto(uriStr: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriStr)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction("com.example.dynamicisland.BRAIN_EVENT")
            addAction("com.example.dynamicisland.CALL_STATE_CHANGED")
        }
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
            islandView?.islandState?.value = IslandState.TYPE_1_MINI
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            islandView?.islandState?.value = IslandState.TYPE_1_MINI
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
            currentCall?.contactPhoto?.recycle()
        } catch (e: Exception) {}
    }
}
