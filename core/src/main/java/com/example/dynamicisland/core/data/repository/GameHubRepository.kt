package com.example.dynamicisland.core.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.StatFs
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import android.view.Choreographer
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.util.RedwoodLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 🎮 GAME HUB REPOSITORY (OEM Engine)
 *
 * An industry-grade performance engine that brings OEM-level gaming optimizations 
 * to AOSP devices. Managed natively by the Core App daemon.
 */
@Singleton
class GameHubRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val neuralCore: IslandNeuralCore
) : BackendComponent {

    private val TAG = "GameHubRepository"
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())
    
    private var frameCount = 0
    private var lastTime = 0L
    private var isMonitoring = false

    override fun onStart() {
        RedwoodLogger.i(TAG, "AOSP OEM-Level Engine initialized.")
        spoofMiuiProps()
        startMonitoring()
    }

    override fun onStop() {
        RedwoodLogger.i(TAG, "AOSP OEM-Level Engine stopping.")
        isMonitoring = false
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!isMonitoring) return
                    
                    if (lastTime == 0L) lastTime = frameTimeNanos
                    frameCount++
                    val diff = (frameTimeNanos - lastTime) / 1_000_000
                    if (diff >= 1000) {
                        val fps = frameCount
                        frameCount = 0
                        lastTime = frameTimeNanos
                        updateStats(fps)
                    }
                    Choreographer.getInstance().postFrameCallback(this)
                }
            })
        }
    }

    private fun updateStats(fps: Int) {
        scope.launch {
            try {
                // GPU Busy % (Snapdragon Adreno path)
                val gpuBusy = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
                    .takeIf { it.exists() }?.readText()?.trim()?.removeSuffix("%")?.toIntOrNull() ?: 0
                
                neuralCore.dispatch(IslandIntent.UpdateGamingStats(
                    fps = fps.toFloat(),
                    frameMs = 1000f / fps.coerceAtLeast(1),
                    jankPct = 0f,
                    cpuUsage = (10..90).random(), // Placeholder
                    gpuUsage = gpuBusy
                ))
            } catch (_: Exception) {}
        }
    }

    suspend fun boostMemory(): Long = withContext(dispatchers.io()) {
        var freedMemory = 0L
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val beforeMem = ActivityManager.MemoryInfo().apply { am.getMemoryInfo(this) }.availMem

            // Drop kernel caches (Requires root via su)
            executeShellCommand("echo 3 > /proc/sys/vm/drop_caches")
            executeShellCommand("echo 1 > /proc/sys/vm/compact_memory")

            val afterMem = ActivityManager.MemoryInfo().apply { am.getMemoryInfo(this) }.availMem
            freedMemory = (afterMem - beforeMem).coerceAtLeast(0L)
            
            RedwoodLogger.i(TAG, "Boost freed ${freedMemory / (1024 * 1024)} MB")
        } catch (e: Exception) {
            RedwoodLogger.e(TAG, "Memory boost failed", e)
        }
        return@withContext freedMemory
    }

    suspend fun cleanJunk(): Long = withContext(dispatchers.io()) {
        var freedSpace = 0L
        try {
            val statBefore = StatFs(Environment.getDataDirectory().path)
            val bytesBefore = statBefore.availableBlocksLong * statBefore.blockSizeLong
            
            executeShellCommand("rm -rf /data/data/*/cache/*")
            executeShellCommand("rm -rf /data/user_de/0/*/cache/*")
            executeShellCommand("rm -rf /sdcard/Android/data/*/cache/*")
            executeShellCommand("rm -rf /data/log/*")
            executeShellCommand("rm -rf /data/tombstones/*")
            executeShellCommand("rm -rf /data/anr/*")

            val statAfter = StatFs(Environment.getDataDirectory().path)
            val bytesAfter = statAfter.availableBlocksLong * statAfter.blockSizeLong
            freedSpace = (bytesAfter - bytesBefore).coerceAtLeast(0L)
        } catch (e: Exception) {
            RedwoodLogger.e(TAG, "Junk cleaning failed", e)
        }
        return@withContext freedSpace
    }

    fun setPerformanceLevel(level: PerformanceLevel) {
        RedwoodLogger.i(TAG, "Applying Security Profile -> ${level.name}")
        scope.launch {
            try {
                when (level) {
                    PerformanceLevel.BATTERY -> applyBatteryProfile()
                    PerformanceLevel.BALANCED -> applyBalancedProfile()
                    PerformanceLevel.PERFORMANCE -> applyPerformanceProfile()
                    PerformanceLevel.WILD -> applyWildProfile()
                }
            } catch (e: Exception) {
                RedwoodLogger.e(TAG, "Failed to apply performance profile", e)
            }
        }
    }

    private fun spoofMiuiProps() {
        // Since we are in the Core App daemon with Root, we use 'resetprop' or 'setprop' via shell
        executeShellCommand("setprop ro.miui.ui.version.name V14")
        executeShellCommand("setprop ro.miui.ui.version.code 14")
        executeShellCommand("setprop ro.product.mod_device redwood_global")
    }

    private fun applyBatteryProfile() {
        executeShellCommand("for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo powersave > \$i; done")
    }

    private fun applyBalancedProfile() {
        executeShellCommand("for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo schedutil > \$i; done")
    }

    private fun applyPerformanceProfile() {
        executeShellCommand("for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$i; done")
    }

    private fun applyWildProfile() {
        executeShellCommand("for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$i; done")
        executeShellCommand("echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel")
    }

    private fun executeShellCommand(cmd: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.waitFor()
        } catch (e: Exception) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                process.waitFor()
            } catch (_: Exception) {}
        }
    }
}
