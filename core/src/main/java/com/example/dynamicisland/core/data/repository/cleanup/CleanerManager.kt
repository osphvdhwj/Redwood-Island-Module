package com.example.dynamicisland.core.data.repository.cleanup

import android.content.Context
import android.widget.Toast
import com.example.dynamicisland.core.data.repository.profiles.ThermalEngineBypass
import com.example.dynamicisland.core.data.repository.profiles.UltraBatterySaver
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

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
            neuralCore.intentFlow.collect { intent: IslandIntent ->
                when (intent) {
                    is IslandIntent.CleanupStorage -> performFullCleanup()
                    is IslandIntent.FreezeBackground -> performBackgroundFreeze()
                    is IslandIntent.ToggleUltraBattery -> {
                        ultraBatterySaver.toggle(intent.enable)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ultra Battery Saver: ${if(intent.enable) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is IslandIntent.ToggleThermalBypass -> {
                        val success = thermalBypass.toggleBypass(intent.enable)
                        withContext(Dispatchers.Main) {
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
        cleaner.wipeSystemLogs()
        cleaner.clearObsoleteCaches()
        val largeFiles = scanner.performDeepScan()
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "System Cleanup Complete. Found ${largeFiles.size} large files.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun performBackgroundFreeze() {
        val targets = listOf("com.android.vending", "com.google.android.youtube") 
        val count = freezer.batchFreeze(targets)
        
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Freezer Active: $count background apps suspended.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        scope.cancel()
    }
}
