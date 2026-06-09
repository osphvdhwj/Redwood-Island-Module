package com.example.dynamicisland.core.daemon

import android.util.Log
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.model.IslandIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REDWOOD ROOT DAEMON ENGINE
 * 
 * Instead of relying on Android App Service lifecycles or standard API callbacks,
 * this engine establishes a persistent, detached `su` process that runs as a true daemon.
 * It uses low-level native pipes to intercept system events (Audio, Brightness, Packages)
 * in real-time with micro-latency.
 */
@Singleton
class RootDaemonEngine @Inject constructor(
    private val neuralCore: IslandNeuralCore
) {
    private val TAG = "RootDaemonEngine"
    private val daemonScope = CoroutineScope(Dispatchers.IO + Job())
    private var suProcess: Process? = null
    private var suOut: OutputStreamWriter? = null
    private var suIn: BufferedReader? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.i(TAG, "Starting Redwood Root Daemon...")

        daemonScope.launch {
            try {
                // Launch persistent root shell
                suProcess = Runtime.getRuntime().exec("su")
                suOut = OutputStreamWriter(suProcess!!.outputStream)
                suIn = BufferedReader(InputStreamReader(suProcess!!.inputStream))

                // Start hook listeners in background
                startAudioHook()
                startPerAppVolumeHook()
                startCameraHook()
                startThermalHook()
                startForegroundAppHook()
                startRefreshRateHook()
                startBatteryStatsHook()
                startNetworkStatsHook()
                startHardwareIntentListener()

                // Read continuous stream from daemon
                while (isRunning && suProcess?.isAlive == true) {
                    val line = suIn?.readLine() ?: break
                    processDaemonEvent(line)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Daemon crash: ${e.message}")
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            suOut?.write("exit\n")
            suOut?.flush()
            suProcess?.destroy()
        } catch (e: Exception) {}
    }

    private fun executeInDaemon(cmd: String) {
        daemonScope.launch {
            try {
                suOut?.write("$cmd\n")
                suOut?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to daemon: $cmd")
            }
        }
    }

    /**
     * Phase 1: Micro-Latency Audio & Mic Detection
     * We hook logcat for AudioFlinger native events to detect mic state instantly.
     */
    private fun startAudioHook() {
        // Look for AudioRecord (Mic) starting or stopping natively
        val hookCmd = "logcat -b all -v raw -s AudioFlinger | grep -E 'AudioRecord.*start|AudioRecord.*stop'"
        executeInDaemon("$hookCmd &")
    }

    /**
     * Phase 2: Per App Volume Integration
     * Hook into AudioManager events or dumpsys to detect per-app volume enablement
     */
    private fun startPerAppVolumeHook() {
        // Poll dumpsys audio for active app volumes (since Custom ROM broadcasts are hidden)
        // We will run this via a sleep loop inside the shell
        val hookCmd = "while true; do dumpsys audio | grep -qi 'AppVolume' && echo 'DAEMON_EVENT: APP_VOL_ACTIVE' || echo 'DAEMON_EVENT: APP_VOL_INACTIVE'; sleep 2; done &"
        executeInDaemon(hookCmd)
    }

    private fun startCameraHook() {
        val hookCmd = "logcat -b all -v raw -s CameraService | grep -E 'connect|disconnect'"
        executeInDaemon("$hookCmd &")
    }

    private fun startThermalHook() {
        val hookCmd = "while true; do temp=\$(cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null); if [ ! -z \"\$temp\" ]; then echo \"DAEMON_EVENT: THERMAL \$temp\"; fi; sleep 5; done &"
        executeInDaemon(hookCmd)
    }

    private fun startForegroundAppHook() {
        val hookCmd = "logcat -b events -v raw -s am_wm_activity_launch | grep 'am_wm_activity_launch'"
        executeInDaemon("$hookCmd &")
    }

    private fun startRefreshRateHook() {
        val hookCmd = "while true; do fps=\$(dumpsys SurfaceFlinger | grep -o 'refresh-rate [0-9]*' | grep -o '[0-9]*' | head -n 1); if [ ! -z \"\$fps\" ]; then echo \"DAEMON_EVENT: REFRESH_RATE \$fps\"; fi; sleep 2; done &"
        executeInDaemon(hookCmd)
    }

    private fun startBatteryStatsHook() {
        val hookCmd = "while true; do rate=\$(cat /sys/class/power_supply/battery/current_now 2>/dev/null); if [ ! -z \"\$rate\" ]; then echo \"DAEMON_EVENT: BATTERY_RATE \$rate\"; fi; sleep 5; done &"
        executeInDaemon(hookCmd)
    }

    private fun startNetworkStatsHook() {
        val hookCmd = "while true; do stats=\$(grep wlan0 /proc/net/dev | awk '{print \$2,\$10}'); if [ ! -z \"\$stats\" ]; then echo \"DAEMON_EVENT: NETWORK_STATS \$stats\"; fi; sleep 2; done &"
        executeInDaemon(hookCmd)
    }

    /**
     * Phase 3: Hardware Intent Listener
     * Listens for UI intents to change hardware settings via shell.
     */
    private fun startHardwareIntentListener() {
        daemonScope.launch {
            neuralCore.intentFlow.collect { intent ->
                when (intent) {
                    is IslandIntent.UpdateBrightness -> setSystemBrightness(intent.brightness, intent.isAuto)
                    is IslandIntent.ToggleLowLatency -> applyLowLatencyTouch(intent.enable)
                    // is IslandIntent.UpdateVolume -> setSystemVolume(intent.volume)
                    else -> {}
                }
            }
        }
    }

    /**
     * Phase 4: Low Touch Latency Implementation
     * Tweaks kernel-level input responsiveness and touch boost.
     */
    private fun applyLowLatencyTouch(enable: Boolean) {
        Log.i(TAG, "Applying Low Latency Touch Profile: $enable")
        if (enable) {
            // Force performance governor on input devices (if available)
            executeInDaemon("for i in /sys/devices/system/cpu/cpufreq/policy*/touch_boost; do echo 1 > \$i; done")
            executeInDaemon("for i in /sys/devices/system/cpu/cpufreq/policy*/input_boost; do echo 1 > \$i; done")
            // System-level touch speed hint
            executeInDaemon("settings put system touch_responsiveness_mode 1")
        } else {
            executeInDaemon("for i in /sys/devices/system/cpu/cpufreq/policy*/touch_boost; do echo 0 > \$i; done")
            executeInDaemon("settings put system touch_responsiveness_mode 0")
        }
    }

    /**
     * Processes output streaming directly from the root shell.
     */
    private fun processDaemonEvent(line: String) {
        if (line.isBlank()) return

        when {
            line.contains("AudioRecord") && line.contains("start") -> {
                Log.d(TAG, "Daemon: Mic Active Hook triggered")
                neuralCore.dispatch(IslandIntent.UpdateMicState(true))
            }
            line.contains("AudioRecord") && line.contains("stop") -> {
                Log.d(TAG, "Daemon: Mic Inactive Hook triggered")
                neuralCore.dispatch(IslandIntent.UpdateMicState(false))
            }
            line.contains("CameraService") && line.contains("connect") -> {
                neuralCore.dispatch(IslandIntent.UpdateCameraState(true))
            }
            line.contains("CameraService") && line.contains("disconnect") -> {
                neuralCore.dispatch(IslandIntent.UpdateCameraState(false))
            }
            line.contains("am_wm_activity_launch") -> {
                // Extract package name from am_wm_activity_launch log
                // Format: [userId, component, ... ]
                val parts = line.split(",")
                if (parts.size > 1) {
                    val component = parts[1].trim()
                    val pkg = component.split("/")[0]
                    neuralCore.dispatch(IslandIntent.UpdateForegroundApp(pkg))
                }
            }
            line.startsWith("DAEMON_EVENT: APP_VOL_ACTIVE") -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(true))
            }
            line.startsWith("DAEMON_EVENT: APP_VOL_INACTIVE") -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(false))
            }
            line.startsWith("DAEMON_EVENT: THERMAL") -> {
                val tempRaw = line.removePrefix("DAEMON_EVENT: THERMAL ").trim().toFloatOrNull()
                if (tempRaw != null) {
                    // Usually in millidegrees Celsius
                    val temp = if (tempRaw > 1000) tempRaw / 1000f else tempRaw
                    neuralCore.dispatch(IslandIntent.UpdateThermalState(temp))
                }
            }
            line.startsWith("DAEMON_EVENT: REFRESH_RATE") -> {
                val fps = line.removePrefix("DAEMON_EVENT: REFRESH_RATE ").trim().toIntOrNull()
                if (fps != null) {
                    neuralCore.dispatch(IslandIntent.UpdateRefreshRate(fps))
                }
            }
            line.startsWith("DAEMON_EVENT: BATTERY_RATE") -> {
                val rate = line.removePrefix("DAEMON_EVENT: BATTERY_RATE ").trim().toIntOrNull()
                if (rate != null) {
                    neuralCore.dispatch(IslandIntent.UpdateBatteryStats(rate))
                }
            }
            line.startsWith("DAEMON_EVENT: NETWORK_STATS") -> {
                val parts = line.removePrefix("DAEMON_EVENT: NETWORK_STATS ").trim().split(" ")
                if (parts.size >= 2) {
                    val rx = parts[0].toLongOrNull() ?: 0L
                    val tx = parts[1].toLongOrNull() ?: 0L
                    neuralCore.dispatch(IslandIntent.UpdateNetworkStats(tx, rx))
                }
            }
        }
    }

    /**
     * Execute hardware-level brightness change instantly via Daemon.
     */
    fun setSystemBrightness(value: Int, isAuto: Boolean) {
        executeInDaemon("settings put system screen_brightness_mode ${if (isAuto) 1 else 0}")
        if (!isAuto) {
            // Android brightness is typically 0-255
            val sysValue = ((value / 100f) * 255).toInt()
            executeInDaemon("settings put system screen_brightness $sysValue")
        }
    }
}