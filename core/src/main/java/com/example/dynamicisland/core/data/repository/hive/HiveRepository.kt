package com.example.dynamicisland.core.data.repository.hive

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.flow.SharedFlow

/**
 * 🐝 HIVE REPOSITORY INTERFACE
 *
 * Defines the contract for the Zero-Cloud P2P Mesh Network.
 * Adheres to Clean Architecture by abstracting network and socket realities
 * from the Domain and UI layers.
 */
interface HiveRepository {
    /**
     * Observable stream of incoming remote activities.
     */
    val incomingStateFlow: SharedFlow<LiveActivityModel>

    /**
     * Observable stream of incoming remote commands/intents.
     */
    val incomingIntentFlow: SharedFlow<RemoteCommand>

    /**
     * Initializes the Hive Daemon (Starts BLE Advertising/Scanning).
     */
    suspend fun startDiscovery()

    /**
     * Tears down sockets, Wi-Fi Direct, and BLE.
     */
    suspend fun stopDiscovery()

    /**
     * Transmits a local LiveActivityModel state to all connected peers.
     * Guaranteed under 100ms delivery via Wi-Fi Direct AES-GCM sockets.
     */
    suspend fun broadcastState(model: LiveActivityModel)

    /**
     * Sends a remote action (Remote Tap) to a specific connected device.
     */
    suspend fun sendRemoteAction(targetDeviceId: String, action: String)
}

/**
 * Represents an actionable command received from a paired device.
 */
data class RemoteCommand(
    val sourceDeviceId: String,
    val action: String
)
