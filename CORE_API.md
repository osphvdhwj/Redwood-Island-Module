# Redwood Core - Developer API 🧠

Redwood Core operates as a root-level system daemon. It manages low-level hardware interactions, system state observation (via Xposed/SU), and performance tuning. 

Frontend clients (like **Redwood Island** or third-party customization apps) can interact with Redwood Core using the IPC mechanisms defined below.

## 📡 IPC Connection
Clients must bind to the `IslandBrainService` exposed by `com.example.dynamicisland.core`.

```xml
<!-- Required Permission -->
<uses-permission android:name="com.redwood.permission.SECURE_IPC" />
```

## ⚡ Supported Intents (`IslandIntent.kt`)

Redwood Core accepts the following unified intents to command hardware or update the system state.

### Hardware & System Control
*   `UpdateBrightness(brightness: Int, isAuto: Boolean)`: Overrides system brightness instantly using the root daemon.
*   `UpdateVolume(volume: Int)`: Updates media volume.
*   `UpdatePerformanceLevel(level: PerformanceLevel)`: Adjusts CPU scaling governors (`BATTERY`, `BALANCED`, `PERFORMANCE`, `WILD`).
*   `FreezeBackground`: Suspends cached apps in memory to free up RAM.
*   `CleanupStorage`: Clears system cache, tombstones, and logs.
*   `ToggleThermalBypass(enable: Boolean)`: Bypasses thermal throttling limits (requires supported kernel).

### Daemon State Modifiers (Dispatched internally by RootDaemonEngine)
*   `UpdateMicState(isMicActive: Boolean)`: Detects if `AudioRecord` is actively capturing the microphone.
*   `UpdatePerAppVolumeState(isActive: Boolean)`: Detects if the custom ROM's Per-App Volume feature is actively routing separate streams.
*   `UpdateCameraState(isActive: Boolean)`: Detects if the Camera is actively streaming.
*   `UpdateThermalState(cpuTemp: Float)`: Updates live CPU temperature.
*   `UpdateForegroundApp(pkg: String)`: Updates the current active app on screen.
*   `UpdateRefreshRate(hz: Int)`: Updates current display refresh rate.
*   `UpdateBatteryStats(dischargeRate: Int)`: Updates live battery discharge rate (mA).
*   `UpdateNetworkStats(txSpeed: Long, rxSpeed: Long)`: Updates live network throughput.

### UI & Activity Lifecycle
*   `NewActivity(model: LiveActivityModel)`: Pushes a new Live Activity (e.g., Music, Timer) to the Core's stack.
*   `RemoveActivity(activityId: String)`: Removes an activity from the stack.
*   `ToggleExpand` / `Collapse` / `DismissActive`: Manages the visual state of the current top-most UI component.

## 📦 Data Models

### `IslandUiState`
The single source of truth broadcasted by the Core. Clients should observe this state to render their UI.
*   `islandState`: Current physical state of the overlay (`HIDDEN`, `TYPE_0_RING`, `TYPE_1_MINI`, etc.)
*   `gamingFps`, `gamingCpuUsage`, `gamingGpuUsage`: Live telemetry data.
*   `isMicActive`, `isCameraActive`: Hardware usage booleans.
*   `cpuTemperature`, `currentRefreshRate`, `batteryDischargeRate`: System telemetry.
*   `currentForegroundApp`: Current app context.

---

## 🛠️ Tutorial: Hooking into Redwood Core

Third-party developers can utilize the Redwood Core daemon to build custom overlays or optimization dashboards.

### 1. Declare Permissions
Add the IPC permission to your app's `AndroidManifest.xml`:
```xml
<uses-permission android:name="com.redwood.permission.SECURE_IPC" />
```

### 2. Bind to the Brain Service
Bind your application to the `IslandBrainService`:

```kotlin
val intent = Intent("com.example.dynamicisland.BRAIN_SERVICE").apply {
    setPackage("com.example.dynamicisland.core")
}

context.bindService(intent, object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val brainService = IIslandBrain.Stub.asInterface(service)
        
        // Example: Command the daemon to freeze background apps
        val extras = Bundle()
        brainService.dispatch("FREEZE_BACKGROUND", extras)
    }

    override fun onServiceDisconnected(name: ComponentName?) {}
}, Context.BIND_AUTO_CREATE)
```

### 3. Subscribing to State (Receiver)
To receive live telemetry (like thermals or refresh rate), register a BroadcastReceiver matching the core's output stream:

```kotlin
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val cpuTemp = intent?.getFloatExtra("cpu_temp", 0f)
        val foregroundApp = intent?.getStringExtra("foreground_app")
        
        Log.d("CustomUI", "Current Temp: \$cpuTemp, App: \$foregroundApp")
    }
}
val filter = IntentFilter("com.example.dynamicisland.BRAIN_EVENT")
context.registerReceiver(receiver, filter, "com.redwood.permission.SECURE_IPC", null)
```

*Note: This API is currently unstable and subject to change as the Redwood ecosystem evolves.*