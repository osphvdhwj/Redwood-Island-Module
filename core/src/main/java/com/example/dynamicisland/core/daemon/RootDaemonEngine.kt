package com.example.dynamicisland.core.daemon

import android.util.Log
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.model.IslandIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private suspend fun executeInDaemonForResult(cmd: String): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine() ?: ""
            process.waitFor()
            result.trim()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Phase 1: Micro-Latency Audio & Mic Detection
     * We hook logcat for AudioFlinger native events to detect mic state instantly.
     */
    private fun startAudioHook() {
        // Look for AudioRecord (Mic) starting or stopping natively - Android 15 AIDL compatible
        val hookCmd = "logcat -b all -v raw -s AudioFlinger | grep -iE 'IAfRecordThread|AudioRecord.*start|AudioRecord.*stop'"
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
        // Custom Kernel Mapping for Redwood: 74=CPU, 90=Battery, 55=GPU
        val hookCmd = "while true; do " +
                "cpu=\$(cat /sys/class/thermal/thermal_zone74/temp 2>/dev/null); " +
                "batt=\$(cat /sys/class/thermal/thermal_zone90/temp 2>/dev/null); " +
                "gpu=\$(cat /sys/class/thermal/thermal_zone55/temp 2>/dev/null); " +
                "if [ ! -z \"\$cpu\" ]; then echo \"DAEMON_EVENT: THERMAL \$cpu \$batt \$gpu\"; fi; " +
                "sleep 5; done &"
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
        // current_now on Redwood is microamperes (uA). Convert to milliamps (mA).
        // Plus, parse batterystats usage summary for top drainer.
        val hookCmd = "while true; do " +
                "uA=\$(cat /sys/class/power_supply/battery/current_now 2>/dev/null); " +
                "if [ ! -z \"\$uA\" ]; then " +
                "  mA=\$((uA / 1000)); " +
                "  echo \"DAEMON_EVENT: BATTERY_RATE \$mA\"; " +
                "fi; " +
                "topApp=\$(dumpsys batterystats --usage | grep 'UID u0a' | sort -rn -k 2 | head -n 1 | awk '{print \$2}'); " +
                "if [ ! -z \"\$topApp\" ]; then echo \"DAEMON_EVENT: TOP_POWER_UID \$topApp\"; fi; " +
                "sleep 10; done &"
        executeInDaemon(hookCmd)
    }

    private fun startNetworkStatsHook() {
        // Parse /proc/net/dev for wlan0 and rmnet TX/RX bytes
        val hookCmd = "while true; do " +
                "wifi=\$(grep wlan0 /proc/net/dev); " +
                "data=\$(grep -E 'rmnet|rmnet_data0' /proc/net/dev | head -n 1); " +
                "rx=0; tx=0; " +
                "if [ ! -z \"\$wifi\" ]; then rx=\$((rx + \$(echo \$wifi | awk '{print \$2}'))); tx=\$((tx + \$(echo \$wifi | awk '{print \$10}'))); fi; " +
                "if [ ! -z \"\$data\" ]; then rx=\$((rx + \$(echo \$data | awk '{print \$2}'))); tx=\$((tx + \$(echo \$data | awk '{print \$10}'))); fi; " +
                "echo \"DAEMON_EVENT: NETWORK_STATS \$rx \$tx\"; " +
                "sleep 2; done &"
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
                    is IslandIntent.UpdateVolume -> setSystemVolume(intent.volume)
                    else -> {}
                }
            }
        }
    }

    /**
     * Set system media volume via shell.
     */
    fun setSystemVolume(value: Int) {
        // volume level usually 0-15 or 0-25
        val maxVol = 15 // Default for most AOSP
        val volLevel = ((value / 100f) * maxVol).toInt()
        executeInDaemon("service call audio 3 i32 3 i32 $volLevel i32 1") // setStreamVolume(STREAM_MUSIC)
    }

    /**
     * Phase 4: Low Touch Latency Implementation
     * Tweaks kernel-level input responsiveness using custom kernel nodes.
     */
    private fun applyLowLatencyTouch(enable: Boolean) {
        Log.i(TAG, "Applying Low Latency Touch Profile: $enable")
        if (enable) {
            // Schedutil RTG Boost (Custom Kernel specific)
            executeInDaemon("for i in /sys/devices/system/cpu/cpufreq/policy*/schedutil/rtg_boost_freq; do echo 1 > \$i; done")
            // System-level touch speed hint
            executeInDaemon("settings put system touch_responsiveness_mode 1")
        } else {
            executeInDaemon("for i in /sys/devices/system/cpu/cpufreq/policy*/schedutil/rtg_boost_freq; do echo 0 > \$i; done")
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
                    
                    // Heuristic for Games: check standard game paths or categories
                    // For now, we'll verify via shell if the app is CATEGORY_GAME
                    daemonScope.launch {
                        val isGame = executeInDaemonForResult("dumpsys package $pkg | grep -q 'category=GAME' && echo 'true' || echo 'false'")
                        if (isGame == "true") {
                            Log.d(TAG, "Daemon: Game Launch Detected: $pkg")
                            neuralCore.dispatch(IslandIntent.UpdateNotificationState(isActive = false)) // Reset shrink
                        }
                    }
                }
            }
            line.startsWith("DAEMON_EVENT: APP_VOL_ACTIVE") -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(true))
            }
            line.startsWith("DAEMON_EVENT: APP_VOL_INACTIVE") -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(false))
            }
            line.startsWith("DAEMON_EVENT: THERMAL") -> {
                val parts = line.removePrefix("DAEMON_EVENT: THERMAL ").trim().split(" ")
                if (parts.isNotEmpty()) {
                    val cpuRaw = parts[0].toFloatOrNull()
                    if (cpuRaw != null) {
                        val cpuTemp = if (cpuRaw > 1000) cpuRaw / 1000f else cpuRaw
                        neuralCore.dispatch(IslandIntent.UpdateThermalState(cpuTemp))
                    }
                    if (parts.size >= 3) {
                        val gpuRaw = parts[2].toFloatOrNull()
                        if (gpuRaw != null) {
                            val gpuTemp = if (gpuRaw > 1000) gpuRaw / 1000f else gpuRaw
                            // Could dispatch a specific GPU intent here if needed
                        }
                    }
                }
            }
            line.startsWith("DAEMON_EVENT: TOP_POWER_UID") -> {
                val uid = line.removePrefix("DAEMON_EVENT: TOP_POWER_UID ").trim()
                if (uid.isNotEmpty()) {
                    // Extract package name via dumpsys if needed, or dispatch UID
                    neuralCore.dispatch(IslandIntent.UpdateTopPowerDrainer(uid))
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