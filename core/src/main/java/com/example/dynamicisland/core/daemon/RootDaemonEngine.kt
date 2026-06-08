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

    /**
     * Phase 3: Hardware Intent Listener
     * Listens for UI intents to change hardware settings via shell.
     */
    private fun startHardwareIntentListener() {
        daemonScope.launch {
            neuralCore.intentFlow.collect { intent ->
                when (intent) {
                    is IslandIntent.UpdateBrightness -> setSystemBrightness(intent.brightness, intent.isAuto)
                    // is IslandIntent.UpdateVolume -> setSystemVolume(intent.volume)
                    else -> {}
                }
            }
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
            line == "DAEMON_EVENT: APP_VOL_ACTIVE" -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(true))
            }
            line == "DAEMON_EVENT: APP_VOL_INACTIVE" -> {
                neuralCore.dispatch(IslandIntent.UpdatePerAppVolumeState(false))
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