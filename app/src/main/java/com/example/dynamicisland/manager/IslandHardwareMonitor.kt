package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import kotlinx.coroutines.*
import java.io.File

class IslandHardwareMonitor(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    var onHardwareUpdate: (LiveActivityModel.HardwareMonitor?) -> Unit = {}
) {
    var isScreenOn = true
        set(value) {
            if (field == value) return
            field = value
            evaluatePolling()
        }
        
    var isDashboardOpen = false
        set(value) {
            if (field == value) return
            field = value
            evaluatePolling()
        }

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
                        val ram = getRamFree()
                        val batCycles = getBatteryCycles()
                        
                        withContext(Dispatchers.Main) {
                            onHardwareUpdate(
                                LiveActivityModel.HardwareMonitor(
                                    id = "hw_monitor",
                                    type = ActivityType.HARDWARE,
                                    cpuTempCelsius = temp,
                                    cpuFreqMhz = freq,
                                    isGamingModeOn = false,
                                    ramFreeBytes = ram,
                                    batteryCycles = batCycles
                                )
                            )
                        }
                        delay(2000)
                    }
                }
...
    private fun getRamFree(): Long {
        try {
            val mi = android.app.ActivityManager.MemoryInfo()
            val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.getMemoryInfo(mi)
            return mi.availMem
        } catch (e: Exception) { return 0L }
    }

    private fun getBatteryCycles(): Int {
        try {
            val file = File("/sys/class/power_supply/battery/cycle_count")
            if (file.exists()) {
                return file.readText().trim().toIntOrNull() ?: 0
            }
        } catch (e: Exception) {}
        return 0
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
