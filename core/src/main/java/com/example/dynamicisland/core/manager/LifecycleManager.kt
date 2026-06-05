package com.example.dynamicisland.core.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pillar 5: Lifecycle Awareness
 * Manages the screen and power state of the device to optimize battery life.
 * When the screen is off (Doze Mode), this manager broadcasts the state so that
 * heavy rendering and sensor listeners can be paused.
 */
@Singleton
class LifecycleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private val _isPowerSaveMode = MutableStateFlow(false)
    val isPowerSaveMode: StateFlow<Boolean> = _isPowerSaveMode.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> _isScreenOn.value = false
                Intent.ACTION_SCREEN_ON -> _isScreenOn.value = true
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    _isPowerSaveMode.value = powerManager?.isPowerSaveMode ?: false
                }
            }
        }
    }

    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        
        // Initial state sync
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _isPowerSaveMode.value = powerManager?.isPowerSaveMode ?: false
    }

    fun stopMonitoring() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}
