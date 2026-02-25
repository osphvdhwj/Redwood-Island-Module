package com.example.dynamicisland

import java.io.File

object HardwareMonitors {

    // Reads the primary thermal zone (usually SoC/Battery on Poco devices)
    // For Snapdragon 778G (Poco X5 Pro), thermal_zone0 is often 'tsens_tz_sensor0' or similar
    // We try 0 first, but it might need tuning if user reports 0.0
    fun getCpuTemp(): Float {
        return try {
            val tempStr = File("/sys/class/thermal/thermal_zone0/temp").readText().trim()
            tempStr.toFloat() / 1000f // Convert millidegree Celsius to Celsius
        } catch (e: Exception) {
            0f
        }
    }

    // Reads the current scaling frequency of CPU Core 0 (Little cluster) or Core 4 (Big)
    // We'll read cpu0 for general activity
    fun getCpuFreq(): String {
        return try {
            val freqStr = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").readText().trim()
            val mhz = freqStr.toInt() / 1000
            "${mhz} MHz"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
