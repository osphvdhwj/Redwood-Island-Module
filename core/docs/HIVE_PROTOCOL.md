# 🐝 THE HIVE PROTOCOL

**Version:** 1.0.0
**Security:** AES-256-GCM, Zero-Cloud Local Mesh
**Transport:** Bluetooth LE (Discovery) + Wi-Fi Direct (Data)

## 1. Overview
The Hive Protocol establishes a secure, peer-to-peer (P2P) local mesh network between Redwood-enabled devices. It facilitates sub-100ms synchronization of `LiveActivityModel` states and allows "Remote Tap" interaction sharing without relying on internet connectivity.

## 2. Discovery & Handshake Protocol

### Phase A: BLE Advertising & Discovery
*   **Advertiser:** Devices broadcast a customized BLE payload containing a hashed Redwood ID and capability flags.
*   **Scanner:** Nearby Redwood devices scan for the specific Service UUID.
*   **Zero-Cloud Constraint:** No external routing or cloud STUN/TURN servers are permitted.

### Phase B: Secure Pairing & Wi-Fi Direct Upgrade
1.  **PIN Verification:** User confirms a dynamic 6-digit PIN on both screens (OOB authentication).
2.  **Key Exchange:** Elliptic Curve Diffie-Hellman (ECDH) is used over BLE to derive a shared symmetric key.
3.  **Transport Upgrade:** A Wi-Fi Direct Group is formed. Devices negotiate the P2P Group Owner (GO).
4.  **Secure Socket:** A TCP socket is opened over the Wi-Fi Direct interface, encrypted entirely via the established AES-256-GCM session key.

## 3. Packet Structure & Serialization
Data is compressed to guarantee sub-100ms latency. We utilize JSON (with future support for Protobuf) formatted over the encrypted socket stream.

### Base Packet
```json
{
  "v": 1, 
  "type": "SYNC | INTENT | HEARTBEAT",
  "ts": 1690000000000,
  "payload": "{...}" // Encrypted AES-GCM Payload
}
```

### Decrypted Payload: State Sync (`SYNC`)
```json
{
  "modelId": "music_spotify",
  "activityType": "MESSAGE",
  "data": {
    "title": "Song Title",
    "artist": "Artist Name",
    "isPlaying": true,
    "positionMs": 12000
  }
}
```

### Decrypted Payload: Remote Intent (`INTENT`)
```json
{
  "action": "PLAY_PAUSE",
  "targetDeviceId": "redwood_tablet_01",
  "sourceDeviceId": "redwood_phone_02"
}
```

## 4. Architectural Boundaries
*   `HiveDaemon`: Manages the raw BLE and Wi-Fi Direct hardware state.
*   `HiveSyncBridge`: Serializes local NeuralCore states to the network and deserializes incoming packets.
*   `RemoteIntentRelay`: Translates incoming network commands into local `IslandController.executeSmartAction()` triggers.
