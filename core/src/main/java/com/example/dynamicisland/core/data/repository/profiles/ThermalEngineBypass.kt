package com.example.dynamicisland.core.data.repository.profiles

import com.example.dynamicisland.core.data.repository.HardwareRepository
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.core.util.shell.RootShellEngine
import com.example.dynamicisland.shared.ipc.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.PerformanceLevel

/**
 * 🔥 THERMAL ENGINE BYPASS (WILD MODE)
 *
 * Disables system thermal throttling for maximum sustained performance.
 * CONTAINS HARDCODED SAFETY LIMITS.
 */
@Singleton
class ThermalEngineBypass @Inject constructor(
    private val rootEngine: RootShellEngine,
    private val hardwareRepository: HardwareRepository,
    private val dispatchers: DispatcherProvider,
    private val neuralCore: IslandNeuralCore // To dispatch fallback intents if we overheat
) {
    companion object {
        private const val TAG = "ThermalBypass"
        // CRITICAL DOMAIN CONSTRAINT: Hardcoded Safety Limit
        private const val SAFETY_TEMP_LIMIT_CELSIUS = 45.0f
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())
    private var isBypassed = false

    init {
        // 🛡️ SAFETY MONITORING LOOP
        scope.launch {
            hardwareRepository.hardwareState.collect { hwState ->
                if (hwState != null && isBypassed) {
                    if (hwState.cpuTempCelsius >= SAFETY_TEMP_LIMIT_CELSIUS) {
                        RedwoodLogger.e(TAG, "CRITICAL: Temperature reached ${hwState.cpuTempCelsius}°C. Exceeds limit of $SAFETY_TEMP_LIMIT_CELSIUS°C. Executing emergency thermal restore.")
                        emergencyRestore()
                    }
                }
            }
        }
    }

    suspend fun toggleBypass(enable: Boolean): Boolean {
        if (enable && hardwareRepository.hardwareState.value?.let { it.cpuTempCelsius >= SAFETY_TEMP_LIMIT_CELSIUS } == true) {
            RedwoodLogger.w(TAG, "Cannot enable Wild Mode. Device already too hot.")
            return false
        }

        RedwoodLogger.i(TAG, "Toggling Thermal Bypass to: $enable")
        val commands = if (enable) {
            listOf(
                "stop thermald",
                "stop mi_thermald",
                "stop thermal-engine"
            )
        } else {
            listOf(
                "start thermald",
                "start mi_thermald",
                "start thermal-engine"
            )
        }

        val success = rootEngine.runSequence(commands)
        if (success) {
            isBypassed = enable
            RedwoodLogger.w(TAG, if (enable) "WARNING: Thermal protections disabled. Monitor temperatures closely." else "Thermal protections restored.")
        }
        return success
    }

    private suspend fun emergencyRestore() {
        val success = toggleBypass(false)
        if (success) {
            // Force performance level back to balanced via Intent
            neuralCore.dispatch(IslandIntent.UpdatePerformanceLevel(com.example.dynamicisland.shared.model.PerformanceLevel.BALANCED))
            
            // Optionally dispatch a system alert to the Island UI
            // neuralCore.dispatch(IslandIntent.NewActivity(LiveActivityModel.SystemAlert(...)))
        } else {
             RedwoodLogger.e(TAG, "FATAL: Failed to restore thermal engine during emergency!")
             // In a real scenario, we might want to shut down the device here if it fails to restore.
        }
    }
}
