package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

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

    var onBatteryChanged: ((level: Int, isCharging: Boolean, color: Int, wattage: Float) -> Unit)? = null
    private var isRegistered = false

    private var lastChargingState: Boolean? = null
    private var lastLevel: Int? = null

    private var job: Job? = null
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (scale > 0) ((level * 100f) / scale).toInt() else 0

                lastChargingState = isCharging
                lastLevel = percent
                
                val wattage = calculateWattage()
                scope.launch(Dispatchers.Main) {
                    onBatteryChanged?.invoke(percent, isCharging, getBatteryColor(percent), wattage)
                }
                
                managePollingJob(isCharging)
            }
        }
    }

    private fun managePollingJob(isCharging: Boolean) {
        if (isCharging && job?.isActive != true) {
            job = scope.launch {
                while (isActive && lastChargingState == true) {
                    delay(3000) 
                    val wattage = calculateWattage()
                    lastLevel?.let { level ->
                        withContext(Dispatchers.Main) {
                            onBatteryChanged?.invoke(level, true, getBatteryColor(level), wattage)
                        }
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
            val currentFile = File("/sys/class/power_supply/battery/current_now")
            val voltageFile = File("/sys/class/power_supply/battery/voltage_now")
            
            if (currentFile.exists() && voltageFile.exists()) {
                val currentMicroAmps = Math.abs(currentFile.readText().trim().toFloat())
                val voltageMicroVolts = voltageFile.readText().trim().toFloat()
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
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            isRegistered = false
            job?.cancel()
            job = null
        }
    }

    fun destroy(context: Context) {
        stop(context)
        supervisorJob.cancel()
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
