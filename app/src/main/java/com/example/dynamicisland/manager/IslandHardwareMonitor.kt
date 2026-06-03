package com.example.dynamicisland.manager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ui.DynamicIslandView

import kotlinx.coroutines.*
import java.io.File
import android.content.Context
import android.app.ActivityManager
import android.provider.Settings
import android.view.Choreographer

class IslandHardwareMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    var onHardwareUpdate: (LiveActivityModel.HardwareMonitor?) -> Unit = {}
) : Choreographer.FrameCallback {
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
        
    var isGamingModeOn = false
        set(value) {
            if (field == value) return
            field = value
            evaluatePolling()
        }

    private var pollJob: Job? = null
    
    // FPS Tracking
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0
    private var startTimeNanos = 0L
    private var currentFps = 0f
    private var lastFrameMs = 0f
    private var jankCount = 0

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos != 0L) {
            val diff = frameTimeNanos - lastFrameTimeNanos
            lastFrameMs = diff / 1_000_000f
            if (lastFrameMs > 17f) jankCount++ // frame dropped (> 60fps threshold)
        }
        lastFrameTimeNanos = frameTimeNanos
        frameCount++
        
        val elapsed = frameTimeNanos - startTimeNanos
        if (elapsed > 1_000_000_000L) { // 1 second
            currentFps = (frameCount * 1_000_000_000L).toFloat() / elapsed
            frameCount = 0
            startTimeNanos = frameTimeNanos
            // jankPct is percent of janky frames in last second
            // we'll reset jankCount here too
            jankCount = 0 
        }
        
        if (isScreenOn && (isDashboardOpen || isGamingModeOn)) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun evaluatePolling() {
        if (isScreenOn && (isDashboardOpen || isGamingModeOn)) {
            startTimeNanos = System.nanoTime()
            Choreographer.getInstance().postFrameCallback(this)
            
            if (pollJob == null || pollJob?.isActive != true) {
                pollJob = scope.launch(Dispatchers.IO) {
                    while(isActive) {
                        val temp = readThermalZone()
                        val freq = readCpuFreq()
                        val ram = getRamFree()
                        val batCycles = getBatteryCycles()
                        val isGaming = isGamingModeOn || isGamingModeActive()

                        withContext(Dispatchers.Main) {
                            onHardwareUpdate(
                                LiveActivityModel.HardwareMonitor(
                                    id = "hw_monitor",
                                    type = ActivityType.HARDWARE,
                                    cpuTempCelsius = temp,
                                    cpuFreqMhz = freq,
                                    isGamingModeOn = isGaming,
                                    fps = currentFps,
                                    frameMs = lastFrameMs,
                                    jankPct = if (frameCount > 0) (jankCount.toFloat() / frameCount) * 100f else 0f,
                                    ramFreeBytes = ram,
                                    batteryCycles = batCycles
                                )
                            )
                        }
                        delay(if (isGaming) 1000 else 2000)
                    }
                }
            }
        } else {
            pollJob?.cancel()
            pollJob = null
            Choreographer.getInstance().removeFrameCallback(this)
            lastFrameTimeNanos = 0L
            onHardwareUpdate(null)
        }
    }

    private fun isGamingModeActive(): Boolean {
        return try {
            val miuiGame = Settings.Secure.getInt(context.contentResolver, "gb_boosting", 0) == 1
            if (miuiGame) return true
            Settings.Secure.getInt(context.contentResolver, "game_mode_active", 0) == 1
        } catch (e: Exception) { false }
    }

    private fun getRamFree(): Long {
        try {
            val mi = ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(mi)
            return mi.availMem
        } catch (e: Exception) { return 0L }
    }

    private fun getBatteryCycles(): Int {
        try {
            val paths = listOf("/sys/class/power_supply/battery/cycle_count", "/sys/class/power_supply/bms/cycle_count")
            for (path in paths) {
                val file = File(path)
                if (file.exists()) return file.readText().trim().toIntOrNull() ?: continue
            }
        } catch (e: Exception) {}
        return 0
    }

    private fun readThermalZone(): Float {
        try {
            val paths = listOf("/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp", "/sys/class/thermal/thermal_zone10/temp", "/sys/devices/virtual/thermal/thermal_zone0/temp")
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
