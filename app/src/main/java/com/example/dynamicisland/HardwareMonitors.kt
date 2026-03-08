package com.example.dynamicisland

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

object HardwareMonitors {

    private var cpuThermalPath: String? = null

    init {
        findThermalPath()
    }

    // 🚀 NEW: Dynamically scan the Linux Kernel for the exact CPU thermal sensor
    private fun findThermalPath() {
        try {
            val dir = File("/sys/class/thermal/")
            if (dir.exists()) {
                dir.listFiles()?.forEach { zone ->
                    if (zone.name.startsWith("thermal_zone")) {
                        val typeFile = File(zone, "type")
                        if (typeFile.exists()) {
                            val type = typeFile.readText().lowercase()
                            // Look for Qualcomm (tsens), MediaTek/Exynos (cpu), or generic SOC sensors
                            if (type.contains("cpu") || type.contains("tsens") || type.contains("soc") || type.contains("bcl")) {
                                cpuThermalPath = File(zone, "temp").absolutePath
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        // Failsafe fallback
        if (cpuThermalPath == null) cpuThermalPath = "/sys/class/thermal/thermal_zone0/temp"
    }

    fun startMonitoring(): Flow<LiveActivityModel.HardwareMonitor> = flow {
        while (true) {
            val temp = readCpuTemp()
            val freq = readCpuFreq()
            // Simple heuristic: If freq is consistently maxed out, assume Gaming Mode
            val isGaming = freq > 2000 
            
            emit(LiveActivityModel.HardwareMonitor(
                cpuTempCelsius = temp,
                cpuFreqMhz = freq,
                isGamingModeOn = isGaming
            ))
            delay(3000) // Update every 3 seconds to save battery
        }
    }

    private fun readCpuTemp(): Float {
        return try {
            val tempStr = File(cpuThermalPath!!).readText().trim()
            val tempRaw = tempStr.toFloat()
            // Some kernels report 45000 for 45.0C
            if (tempRaw > 1000) tempRaw / 1000f else tempRaw
        } catch (e: Exception) {
            0f
        }
    }

    private fun readCpuFreq(): Int {
        return try {
            val freqStr = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim()
            freqStr.toInt() / 1000
        } catch (e: Exception) {
            0
        }
    }
}
