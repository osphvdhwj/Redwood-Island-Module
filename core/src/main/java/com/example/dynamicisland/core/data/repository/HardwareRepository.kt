package com.example.dynamicisland.core.data.repository

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import android.view.Choreographer
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.ActivityType
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.PerformanceLevel
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class HardwareRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) : BackendComponent, Choreographer.FrameCallback {

    private val _hardwareState = MutableStateFlow<LiveActivityModel.HardwareMonitor?>(null)
    val hardwareState: StateFlow<LiveActivityModel.HardwareMonitor?> = _hardwareState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.hardware())
    private var pollJob: Job? = null
    
    var isScreenOn = true
        set(value) {
            field = value
            evaluateState()
        }
    var isDashboardOpen = false
        set(value) {
            field = value
            evaluateState()
        }
    var isGamingModeOn = false
        set(value) {
            field = value
            evaluateState()
        }

    // FPS Internal State
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0
    private var startTimeNanos = 0L
    private var currentFps = 0f
    private var lastFrameMs = 0f
    private var jankCount = 0

    override fun onStart() {
        evaluateState()
    }

    override fun onStop() {
        stopPolling()
        Choreographer.getInstance().removeFrameCallback(this)
    }

    fun setPerformanceLevel(level: PerformanceLevel) {
        // Implementation for root performance tuning
    }

    private fun evaluateState() {
        val shouldPoll = isScreenOn && (isDashboardOpen || isGamingModeOn)
        if (shouldPoll) {
            startPolling()
        } else {
            stopPolling()
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        
        startTimeNanos = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(this)
        
        pollJob = scope.launch {
            while (isActive) {
                val temp = readThermalZone()
                val freq = readCpuFreq()
                val ram = getRamFree()
                val batCycles = getBatteryCycles()
                val isGaming = isGamingModeOn || isGamingModeActive()

                _hardwareState.value = LiveActivityModel.HardwareMonitor(
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
                
                delay(if (isGaming) 1000 else 2000)
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
        Choreographer.getInstance().removeFrameCallback(this)
        lastFrameTimeNanos = 0L
        _hardwareState.value = null
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos != 0L) {
            val diff = frameTimeNanos - lastFrameTimeNanos
            lastFrameMs = diff / 1_000_000f
            if (lastFrameMs > 17f) jankCount++
        }
        lastFrameTimeNanos = frameTimeNanos
        frameCount++
        
        val elapsed = frameTimeNanos - startTimeNanos
        if (elapsed > 1_000_000_000L) {
            currentFps = (frameCount * 1_000_000_000L).toFloat() / elapsed
            frameCount = 0
            startTimeNanos = frameTimeNanos
            jankCount = 0 
        }
        
        if (isScreenOn && (isDashboardOpen || isGamingModeOn)) {
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    // --- System Accessors ---

    private fun isGamingModeActive(): Boolean = try {
        Settings.Secure.getInt(context.contentResolver, "gb_boosting", 0) == 1 ||
        Settings.Secure.getInt(context.contentResolver, "game_mode_active", 0) == 1
    } catch (e: Exception) { false }

    private fun getRamFree(): Long = try {
        val mi = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.getMemoryInfo(mi)
        mi.availMem
    } catch (e: Exception) { 0L }

    private fun getBatteryCycles(): Int = try {
        val paths = listOf("/sys/class/power_supply/battery/cycle_count", "/sys/class/power_supply/bms/cycle_count")
        paths.firstNotNullOfOrNull { path ->
            File(path).takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
        } ?: 0
    } catch (e: Exception) { 0 }

    private fun readThermalZone(): Float = try {
        val paths = listOf("/sys/class/thermal/thermal_zone0/temp", "/sys/class/thermal/thermal_zone1/temp", "/sys/devices/virtual/thermal/thermal_zone0/temp")
        val raw = paths.firstNotNullOfOrNull { path ->
            File(path).takeIf { it.exists() }?.readText()?.trim()?.toFloatOrNull()
        } ?: 35.0f
        if (raw > 1000) raw / 1000f else raw
    } catch (e: Exception) { 35.0f }

    private fun readCpuFreq(): Int = try {
        File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            .takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()?.let { it / 1000 } ?: 0
    } catch (e: Exception) { 0 }
}
