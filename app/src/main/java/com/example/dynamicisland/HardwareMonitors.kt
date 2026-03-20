package com.example.dynamicisland

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object HardwareMonitors {

    private fun readCpuTemp(): Float {
        return try {
            val temp = File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toFloat()
            if (temp > 1000) temp / 1000 else temp
        } catch (e: Exception) { 0f }
    }

    private fun readCpuFreq(): Int {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim().toInt() / 1000
        } catch (e: Exception) { 0 }
    }

    // 🚀 NEW: Reads raw charging speed from kernel nodes
    fun readChargingWattage(): Float {
        return try {
            val voltageStr = File("/sys/class/power_supply/battery/voltage_now").readText().trim()
            val currentStr = File("/sys/class/power_supply/battery/current_now").readText().trim()
            
            val voltage = voltageStr.toFloat() / 1_000_000f 
            var current = currentStr.toFloat() / 1_000_000f 
            if (current < 0) current *= -1f // Absolute value
            
            val watts = voltage * current
            if (watts > 120f || watts < 0f) 0f else watts 
        } catch (e: Throwable) { 0f }
    }

    fun startMonitoring(): Flow<LiveActivityModel.HardwareMonitor> = flow {
        while (true) {
            val temp = readCpuTemp()
            val freq = readCpuFreq()
            val isGaming = freq > 2000 
            
            emit(LiveActivityModel.HardwareMonitor(
                id = "hw_monitor",
                type = ActivityType.HARDWARE,
                isTransient = false,
                isCritical = temp > 45f,
                cpuTempCelsius = temp,
                cpuFreqMhz = freq,
                isGamingModeOn = isGaming
            ))
            delay(3000) 
        }
    }.flowOn(Dispatchers.IO)
}
