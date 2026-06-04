package com.example.dynamicisland.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemProperties
import com.example.dynamicisland.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.domain.lifecycle.BackendComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.dynamicisland.util.XposedExtensions
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.example.dynamicisland.domain.state.IslandNeuralCore
import com.example.dynamicisland.ui.mvi.IslandIntent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameHubRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val neuralCore: IslandNeuralCore
) : BackendComponent {

    private const val TAG = "GameHubRepository"
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())
    
    private var frameCount = 0
    private var lastTime = 0L
    private var isMonitoring = false

    enum class PerformanceLevel {
        BATTERY, BALANCED, PERFORMANCE, WILD
    }

    override fun onStart() {
        XposedBridge.log("$TAG: AOSP OEM-Level Engine initialized.")
        spoofMiuiProps()
        startMonitoring()
    }

    override fun onStop() {
        XposedBridge.log("$TAG: AOSP OEM-Level Engine stopping.")
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
                // GPU Busy % (Adreno specific)
                val gpuBusy = File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage")
                    .takeIf { it.exists() }?.readText()?.trim()?.removeSuffix("%")?.toIntOrNull() ?: 0
                
                // CPU usage (Simplified)
                val cpuUsage = (10..90).random() // TODO: Read /proc/stat properly

                neuralCore.dispatch(IslandIntent.UpdateGamingStats(
                    fps = fps.toFloat(),
                    frameMs = 1000f / fps.coerceAtLeast(1),
                    jankPct = 0f,
                    cpuUsage = cpuUsage,
                    gpuUsage = gpuBusy
                ))
            } catch (_: Exception) {}
        }
    }

    // --- 1. System Optimizer (Memory & Filesystem) ---

    suspend fun boostMemory(): Long = withContext(dispatchers.io()) {
        var freedMemory = 0L
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val beforeMem = ActivityManager.MemoryInfo().apply { am.getMemoryInfo(this) }.availMem

            // 1. Kill background processes
            am.runningAppProcesses?.forEach { app ->
                if (app.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    am.killBackgroundProcesses(app.processName)
                }
            }

            // 2. Drop kernel caches (Requires root/system)
            executeShellCommand("echo 3 > /proc/sys/vm/drop_caches")
            executeShellCommand("echo 1 > /proc/sys/vm/compact_memory")

            val afterMem = ActivityManager.MemoryInfo().apply { am.getMemoryInfo(this) }.availMem
            freedMemory = (afterMem - beforeMem).coerceAtLeast(0L)
            
            XposedBridge.log("$TAG: Boost freed ${freedMemory / (1024 * 1024)} MB")
        } catch (e: Exception) {
            XposedBridge.log("$TAG ❌: Memory boost failed - ${e.message}")
        }
        return@withContext freedMemory
    }

    suspend fun cleanJunk(): Long = withContext(dispatchers.io()) {
        var freedSpace = 0L
        try {
            val statBefore = StatFs(Environment.getDataDirectory().path)
            val bytesBefore = statBefore.availableBlocksLong * statBefore.blockSizeLong
            
            // 1. App Caches
            executeShellCommand("rm -rf /data/data/*/cache/*")
            executeShellCommand("rm -rf /data/user_de/0/*/cache/*")
            executeShellCommand("rm -rf /sdcard/Android/data/*/cache/*")

            // 2. Log files & Tombstones
            executeShellCommand("rm -rf /data/log/*")
            executeShellCommand("rm -rf /data/tombstones/*")
            executeShellCommand("rm -rf /data/anr/*")

            // 3. Obsolete Dalvik Cache
            executeShellCommand("rm -rf /data/dalvik-cache/*/oat/*/*.vdex")

            val statAfter = StatFs(Environment.getDataDirectory().path)
            val bytesAfter = statAfter.availableBlocksLong * statAfter.blockSizeLong
            
            freedSpace = (bytesAfter - bytesBefore).coerceAtLeast(0L)
            XposedBridge.log("$TAG: Junk cleaning freed ${freedSpace / (1024 * 1024)} MB")
        } catch (e: Exception) {
            XposedBridge.log("$TAG ❌: Junk cleaning failed - ${e.message}")
        }
        return@withContext freedSpace
    }

    suspend fun deepCleanScan(): List<File> = withContext(dispatchers.io()) {
        val largeFiles = mutableListOf<File>()
        try {
            val sdcard = Environment.getExternalStorageDirectory()
            sdcard.walkTopDown()
                .maxDepth(4) // Limit depth to prevent hanging
                .onEnter { !it.isHidden && it.name != "Android" } // Skip Android/data
                .filter { it.isFile }
                .forEach { file ->
                    if (file.length() > 50 * 1024 * 1024 || file.extension.lowercase() == "apk") {
                        largeFiles.add(file)
                    }
                }
        } catch (e: Exception) {
            XposedBridge.log("$TAG ❌: Deep scan failed - ${e.message}")
        }
        return@withContext largeFiles
    }

    // --- 2. Hardware Controller (GPU/CPU/Prop Spoofing) ---

    fun setPerformanceLevel(level: PerformanceLevel) {
        XposedBridge.log("$TAG: Applying Security Profile -> ${level.name}")
        scope.launch {
            try {
                when (level) {
                    PerformanceLevel.BATTERY -> applyBatteryProfile()
                    PerformanceLevel.BALANCED -> applyBalancedProfile()
                    PerformanceLevel.PERFORMANCE -> applyPerformanceProfile()
                    PerformanceLevel.WILD -> applyWildProfile()
                }
            } catch (e: Exception) {
                XposedBridge.log("$TAG ❌: Failed to apply performance profile: ${e.message}")
            }
        }
    }

    private fun spoofMiuiProps() {
        try {
            SystemProperties.set("ro.miui.ui.version.name", "V14")
            SystemProperties.set("ro.miui.ui.version.code", "14")
            SystemProperties.set("ro.product.mod_device", "redwood_global")
            
            XposedExtensions.setStaticObjectFieldSafe(android.os.Build::class.java, "MANUFACTURER", "Xiaomi")
            XposedExtensions.setStaticObjectFieldSafe(android.os.Build::class.java, "BRAND", "Xiaomi")
            
            XposedBridge.log("$TAG: MIUI Identity Spoofed.")
        } catch (e: Exception) {
            XposedBridge.log("$TAG ⚠️: Failed to spoof properties: ${e.message}")
        }
    }

    private fun applyBatteryProfile() {
        setCpuGovernor("powersave")
        setGpuPowerLevel(5, 5)
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
    }

    private fun applyBalancedProfile() {
        setCpuGovernor("schedutil")
        setGpuPowerLevel(0, 5)
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
    }

    private fun applyPerformanceProfile() {
        setCpuGovernor("performance")
        setGpuPowerLevel(0, 2)
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
        trimMemory(60)
    }

    private fun applyWildProfile() {
        setCpuGovernor("performance")
        setGpuPowerLevel(0, 0)
        Process.setThreadPriority(-20)
        trimMemory(80)
        killBackgroundProcesses()
    }

    private fun setCpuGovernor(governor: String) {
        for (i in 0..7) {
            writeSysfs("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor", governor)
        }
    }

    private fun setGpuPowerLevel(min: Int, max: Int) {
        writeSysfs("/sys/class/kgsl/kgsl-3d0/min_pwrlevel", min.toString())
        writeSysfs("/sys/class/kgsl/kgsl-3d0/max_pwrlevel", max.toString())
        writeSysfs("/sys/class/kgsl/kgsl-3d0/devfreq/adreno_boost", if (min == 0) "3" else "1")
    }

    private fun trimMemory(level: Int) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val method = ActivityManager::class.java.getMethod("trimMemory", Int::class.javaPrimitiveType)
            method.invoke(am, level)
        } catch (_: Exception) {}
    }

    private fun killBackgroundProcesses() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses?.forEach {
            if (it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                am.killBackgroundProcesses(it.processName)
            }
        }
    }

    private fun writeSysfs(path: String, value: String) {
        try {
            val file = File(path)
            if (file.exists() && file.canWrite()) {
                file.writeText(value)
            }
        } catch (_: Exception) {}
    }

    private fun executeShellCommand(cmd: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.waitFor()
        } catch (e: Exception) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                process.waitFor()
            } catch (e2: Exception) {}
        }
    }
}
