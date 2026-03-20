package com.example.dynamicisland

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.HardwarePropertiesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

object HardwareMonitors {

    private fun getHardwareStats(context: Context): Pair<Int, Int> {
        var batteryTemp = 0
        var cpuTemp = 0

        // Safe Battery Temp Reading
        try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        } catch (e: Exception) { /* Failsafe */ }

        // Safe CPU Temp Reading (Requires DEVICE_POWER permission, often granted to SystemUI)
        try {
            val hwManager = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as HardwarePropertiesManager
            val cpuTemps = hwManager.getDeviceTemperatures(HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU, HardwarePropertiesManager.TEMPERATURE_CURRENT)
            if (cpuTemps.isNotEmpty()) cpuTemp = cpuTemps[0].toInt()
        } catch (e: Exception) { /* Fallback */ }

        // If API fails, default to battery temp as a rough proxy for device warmth
        return Pair(0 /* mock freq */, if (cpuTemp > 0) cpuTemp else (batteryTemp / 10))
    }

    fun startMonitoring(context: Context): Flow<LiveActivityModel.HardwareMonitor> = flow {
        while (true) {
            val (freq, temp) = getHardwareStats(context)
            val isGaming = freq > 2000 
            
            emit(LiveActivityModel.HardwareMonitor(
                id = "hw_monitor",
                type = ActivityType.HARDWARE,
                isTransient = false,
                isCritical = temp > 45,
                cpuTempCelsius = temp,
                cpuFreqMhz = freq,
                isGamingModeOn = isGaming
            ))
            delay(3000) 
        }
    }.flowOn(Dispatchers.IO)
}
