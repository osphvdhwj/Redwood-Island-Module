package com.example.dynamicisland.core.data.repository.cleanup

import android.content.Context
import android.widget.Toast
import com.example.dynamicisland.core.data.repository.profiles.ThermalEngineBypass
import com.example.dynamicisland.core.data.repository.profiles.UltraBatterySaver
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 🛠️ CLEANER MANAGER
 *
 * Orchestrates the execution of cleanup and freezing tasks based on UDF intents.
 * Observes the NeuralCore intent flow and triggers hardware actions.
 */
@Singleton
class CleanerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val neuralCore: IslandNeuralCore,
    private val scanner: StorageScanner,
    private val cleaner: ResidualCleaner,
    private val freezer: AppFreezer,
    private val ultraBatterySaver: UltraBatterySaver,
    private val thermalBypass: ThermalEngineBypass,
    private val dispatchers: DispatcherProvider
) : BackendComponent {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())

    override fun onStart() {
        scope.launch {
            neuralCore.intentFlow.collect { intent ->
                when (intent) {
                    is IslandIntent.CleanupStorage -> performFullCleanup()
                    is IslandIntent.FreezeBackground -> performBackgroundFreeze()
                    is IslandIntent.ToggleUltraBattery -> {
                        ultraBatterySaver.toggle(intent.enable)
                        launch(dispatchers.main()) {
                            Toast.makeText(context, "Ultra Battery Saver: ${if(intent.enable) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is IslandIntent.ToggleThermalBypass -> {
                        val success = thermalBypass.toggleBypass(intent.enable)
                        launch(dispatchers.main()) {
                            if (success) {
                                Toast.makeText(context, "Thermal Bypass: ${if(intent.enable) "DANGER ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Thermal Bypass failed. Device may be too hot.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun performFullCleanup() {
        // 1. Wipe system logs
        cleaner.wipeSystemLogs()
        // 2. Clear ART caches
        cleaner.clearObsoleteCaches()
        // 3. Scan for large files (Internal analytics)
        val largeFiles = scanner.performDeepScan()
        
        launch(dispatchers.main()) {
            Toast.makeText(context, "System Cleanup Complete. Found ${largeFiles.size} large files.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun performBackgroundFreeze() {
        // For Phase 3, we simulate catching background hogs. 
        // In a production build, this would use UsageStatsManager to find non-foreground apps.
        val targets = listOf("com.android.vending", "com.google.android.youtube") // Examples
        val count = freezer.batchFreeze(targets)
        
        launch(dispatchers.main()) {
            Toast.makeText(context, "Freezer Active: $count background apps suspended.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        // Cleanup scope if needed
    }
}
