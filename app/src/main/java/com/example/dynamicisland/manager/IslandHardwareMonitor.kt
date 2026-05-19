package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import kotlinx.coroutines.*
import java.io.File

class IslandHardwareMonitor(
    private val scope: CoroutineScope,
    private val onHardwareUpdate: (LiveActivityModel.HardwareMonitor?) -> Unit
) {
    var isScreenOn = true
        set(value) { field = value; evaluatePolling() }
        
    var isDashboardOpen = false
        set(value) { field = value; evaluatePolling() }

    private var pollJob: Job? = null

    private fun evaluatePolling() {
        // 🛑 TIER 3 SLEEP PROTOCOL: Only poll CPU when Screen is ON and Dashboard is OPEN.
        // Otherwise, completely destroy the loop to save battery.
        if (isScreenOn && isDashboardOpen) {
            if (pollJob == null || pollJob?.isActive != true) {
                pollJob = scope.launch(Dispatchers.IO) {
                    while(isActive) {
                        val temp = readThermalZone()
                        val freq = readCpuFreq()
                        withContext(Dispatchers.Main) {
                            onHardwareUpdate(LiveActivityModel.HardwareMonitor("hw_monitor", ActivityType.HARDWARE, temp, freq, false))
                        }
                        delay(2000)
                    }
                }
            }
        } else {
            pollJob?.cancel()
            pollJob = null
            onHardwareUpdate(null) // Clear state from memory when dormant
        }
    }

    private fun readThermalZone(): Float {
        try {
            val paths = listOf("/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp")
            for (path in paths) {
                val file = File(path)
                if (file.exists()) {
                    val temp = file.readText().trim().toFloatOrNull() ?: continue
                    return if (temp > 1000) temp / 1000f else temp
                }
            }
        } catch (e: Exception) {}
        return 35.0f
    }

    private fun readCpuFreq(): Int {
        try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            if (file.exists()) {
                val freq = file.readText().trim().toIntOrNull() ?: return 0
                return freq / 1000
            }
        } catch (e: Exception) {}
        return 0
    }
}
