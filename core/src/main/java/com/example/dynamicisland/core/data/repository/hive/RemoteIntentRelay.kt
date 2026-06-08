package com.example.dynamicisland.core.data.repository.hive

import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandController
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import javax.inject.Inject
import com.example.dynamicisland.shared.model.*
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * ⚡ REMOTE INTENT RELAY (Domain Layer)
 *
 * Listens for remote commands (e.g., "Remote Tap") from the Hive network
 * and translates them into local system actions.
 */
@Singleton
class RemoteIntentRelay @Inject constructor(
    private val hiveRepository: HiveRepository,
    private val islandController: IslandController,
    private val dispatchers: DispatcherProvider
) : BackendComponent {

    private val TAG = "RemoteIntentRelay"
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default())

    override fun onStart() {
        RedwoodLogger.i(TAG, "Initializing Remote Intent Relay...")

        scope.launch {
            hiveRepository.incomingIntentFlow.collect { command ->
                RedwoodLogger.i(TAG, "Executing Remote Command: ${command.action} from ${command.sourceDeviceId}")
                
                // Route the remote string action into our local SmartAction engine
                islandController.executeSmartAction(command.action)
            }
        }
    }

    override fun onStop() {
        // Handled by scope cancellation normally
    }
}
