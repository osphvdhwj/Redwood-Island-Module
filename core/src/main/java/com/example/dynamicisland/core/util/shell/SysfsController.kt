package com.example.dynamicisland.core.util.shell

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🛠️ SYSFS CONTROLLER
 *
 * Direct interface for kernel-level tuning via sysfs nodes.
 * Bridges high-level Game Turbo logic to raw kernel parameters.
 */
@Singleton
class SysfsController @Inject constructor(
    private val rootEngine: RootShellEngine
) {
    
    // CPU Governor Paths
    private const val CPU_GOVERNOR_PATTERN = "/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor"
    
    // Snapdragon GPU (KGSL) Paths
    private const val GPU_MIN_PWRLEVEL = "/sys/class/kgsl/kgsl-3d0/min_pwrlevel"
    private const val GPU_MAX_PWRLEVEL = "/sys/class/kgsl/kgsl-3d0/max_pwrlevel"
    private const val GPU_GOVERNOR = "/sys/class/kgsl/kgsl-3d0/devfreq/governor"
    private const val GPU_ADRENO_BOOST = "/sys/class/kgsl/kgsl-3d0/devfreq/adreno_boost"
    private const val GPU_BUSY_PCT = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"

    /**
     * Sets CPU scaling governor project-wide.
     */
    suspend fun setCpuGovernor(governor: String): Boolean {
        // Use a wildcard to target all cores at once in shell
        return rootEngine.runAction("for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo $governor > \$i; done")
    }

    /**
     * Set GPU Power Levels (Snapdragon KGSL)
     * Lower pwrlevel = Higher frequency.
     */
    suspend fun setGpuPowerLevels(min: Int, max: Int): Boolean {
        val boost = if (min == 0) "3" else "1"
        return rootEngine.runSequence(listOf(
            "echo $min > $GPU_MIN_PWRLEVEL",
            "echo $max > $GPU_MAX_PWRLEVEL",
            "echo $boost > $GPU_ADRENO_BOOST"
        ))
    }

    /**
     * Force locks GPU to maximum performance.
     */
    suspend fun setGpuGovernor(governor: String): Boolean {
        return rootEngine.runAction("echo $governor > $GPU_GOVERNOR")
    }

    /**
     * Performs kernel memory maintenance.
     */
    suspend fun performMemoryTurbo(): Boolean {
        return rootEngine.runSequence(listOf(
            "echo 3 > /proc/sys/vm/drop_caches",
            "echo 1 > /proc/sys/vm/compact_memory"
        ))
    }
}
