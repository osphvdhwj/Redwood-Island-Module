package com.example.dynamicisland

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

object HardwareMonitors {

    // Usually thermal_zone0 is the primary CPU, but some custom ROMs map it to zone1 or zone2.
    // We check the standard path for Poco X5 Pro / Snapdragon.
    private const val CPU_TEMP_PATH = "/sys/class/thermal/thermal_zone0/temp"
    private const val BATTERY_TEMP_PATH = "/sys/class/power_supply/battery/temp"
    private const val CPU_FREQ_PATH = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"

    /**
     * Emits a continuous stream of hardware data every 2 seconds.
     * Runs securely on the IO dispatcher to prevent SystemUI thread blocking.
     */
    fun startMonitoring(): Flow<LiveActivityModel.HardwareMonitor> = flow {
        while (true) {
            val cpuTemp = readSystemFileAsFloat(CPU_TEMP_PATH, divider = 1000f) // Millicelsius to Celsius
            val batTemp = readSystemFileAsFloat(BATTERY_TEMP_PATH, divider = 10f) // Decicelsius to Celsius
            val cpuFreq = readSystemFileAsInt(CPU_FREQ_PATH, divider = 1000) // KHz to MHz

            val isGaming = cpuTemp > 45f && cpuFreq > 2000 // Basic threshold logic for "Gaming Mode"

            emit(
                LiveActivityModel.HardwareMonitor(
                    cpuTempCelsius = cpuTemp,
                    batteryTempCelsius = batTemp,
                    cpuFreqMhz = cpuFreq,
                    isGamingModeOn = isGaming
                )
            )

            // Poll every 2 seconds
            delay(2000L)
        }
    }.flowOn(Dispatchers.IO)

    private fun readSystemFileAsFloat(path: String, divider: Float): Float {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.readText().trim().toFloatOrNull()?.let { it / divider } ?: 0f
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun readSystemFileAsInt(path: String, divider: Int): Int {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.readText().trim().toIntOrNull()?.let { it / divider } ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}
