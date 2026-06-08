package com.example.dynamicisland.core.data.repository.hive

import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.settings.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 🌉 HIVE SYNC BRIDGE (Domain Layer)
 *
 * Bridges the gap between the Hive Daemon (Network) and the Island Neural Core (State).
 * Serializes local states to the network and injects remote states into the local UI.
 */
@Singleton
class HiveSyncBridge @Inject constructor(
    private val hiveRepository: HiveRepository,
    private val neuralCore: IslandNeuralCore,
    private val dispatchers: DispatcherProvider
) : BackendComponent {

    private val TAG = "HiveSyncBridge"
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default())

    override fun onStart() {
        RedwoodLogger.i(TAG, "Initializing Hive Sync Bridge...")
        
        scope.launch {
            hiveRepository.startDiscovery()
        }

        // 1. Local State -> Network Broadcast
        scope.launch {
            neuralCore.uiState.collect { state ->
                val activeModel = state.activeModel
                if (activeModel != null && !activeModel.isSensitive) {
                    // We don't broadcast sensitive data (like OTPs or Caller ID) over the mesh
                    // unless explicitly permitted.
                    hiveRepository.broadcastState(activeModel)
                }
            }
        }

        // 2. Network State -> Local UI (Remote Island Reflection)
        scope.launch {
            hiveRepository.incomingStateFlow.collect { remoteModel ->
                RedwoodLogger.d(TAG, "Received Remote Model: ${remoteModel.id}")
                neuralCore.dispatch(IslandIntent.NewActivity(remoteModel))
            }
        }
    }

    override fun onStop() {
        scope.launch {
            hiveRepository.stopDiscovery()
        }
    }
}
