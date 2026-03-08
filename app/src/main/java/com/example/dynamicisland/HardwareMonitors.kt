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

    private fun findThermalPath() {
        try {
            val dir = File("/sys/class/thermal/")
            if (dir.exists()) {
                dir.listFiles()?.forEach { zone ->
                    if (zone.name.startsWith("thermal_zone")) {
                        val typeFile = File(zone, "type")
                        if (typeFile.exists()) {
                            val type = typeFile.readText().lowercase()
                            if (type.contains("cpu") || type.contains("tsens") || type.contains("soc") || type.contains("bcl")) {
                                cpuThermalPath = File(zone, "temp").absolutePath
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        
        // 🚀 SAFE FALLBACK: If no CPU node is found, try the universal battery node
        if (cpuThermalPath == null) {
            val battTemp = File("/sys/class/power_supply/battery/temp")
            if (battTemp.exists()) {
                cpuThermalPath = battTemp.absolutePath
            }
        }
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
        // 🚀 FAIL GRACEFULLY: Return 0f if the hardware doesn't support thermal reading
        val path = cpuThermalPath ?: return 0f
        return try {
            val tempStr = File(path).readText().trim()
            val tempRaw = tempStr.toFloat()
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
