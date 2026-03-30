package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.*
import java.io.File

object BatteryPlugin {

    // 🚀 FIXED: Added the 'wattage' Float to your callback signature
    var onBatteryChanged: ((level: Int, isCharging: Boolean, color: Int, wattage: Float) -> Unit)? = null
    private var isRegistered = false

    private var lastChargingState: Boolean? = null
    private var lastLevel: Int? = null

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (scale > 0) ((level * 100f) / scale).toInt() else 0

                // Always update our local cache
                lastChargingState = isCharging
                lastLevel = percent
                
                // Fetch the immediate wattage
                val wattage = calculateWattage()
                onBatteryChanged?.invoke(percent, isCharging, getBatteryColor(percent), wattage)
                
                // 🧠 BATTERY SAVER: Only run the 3-second polling loop IF the device is plugged in
                managePollingJob(isCharging)
            }
        }
    }

    private fun managePollingJob(isCharging: Boolean) {
        if (isCharging && job?.isActive != true) {
            job = scope.launch {
                while (isActive && lastChargingState == true) {
                    delay(3000) // Poll the kernel every 3 seconds while charging
                    val wattage = calculateWattage()
                    
                    lastLevel?.let { level ->
                        onBatteryChanged?.invoke(level, true, getBatteryColor(level), wattage)
                    }
                }
            }
        } else if (!isCharging) {
            job?.cancel()
            job = null
        }
    }

    private fun calculateWattage(): Float {
        try {
            // Read raw hardware nodes from the Linux kernel
            val currentFile = File("/sys/class/power_supply/battery/current_now")
            val voltageFile = File("/sys/class/power_supply/battery/voltage_now")
            
            if (currentFile.exists() && voltageFile.exists()) {
                val currentMicroAmps = Math.abs(currentFile.readText().trim().toFloat())
                val voltageMicroVolts = voltageFile.readText().trim().toFloat()
                
                // P = I * V (Convert micro to standard units)
                return (currentMicroAmps / 1_000_000f) * (voltageMicroVolts / 1_000_000f)
            }
        } catch (e: Exception) {}
        return 0f
    }

    fun start(context: Context) {
        if (!isRegistered) {
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isRegistered = true
        }
    }

    fun stop(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
            isRegistered = false
            job?.cancel()
            job = null
        }
    }

    @ColorInt
    private fun getBatteryColor(percent: Int): Int {
        return when {
            percent <= 20 -> Color.RED
            percent <= 50 -> interpolateColor(Color.RED, Color.YELLOW, (percent - 20) / 30f)
            percent <= 100 -> interpolateColor(Color.YELLOW, Color.GREEN, (percent - 50) / 50f)
            else -> Color.GREEN
        }
    }

    @ColorInt
    private fun interpolateColor(@ColorInt startColor: Int, @ColorInt endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        return ColorUtils.blendARGB(startColor, endColor, f)
    }
}
