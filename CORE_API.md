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

### Audio State Modifiers
*(Usually dispatched internally by the Core Daemon, but can be mocked)*
*   `UpdateMicState(isMicActive: Boolean)`: Signals if `AudioRecord` is actively capturing the microphone.
*   `UpdatePerAppVolumeState(isActive: Boolean)`: Signals if the custom ROM's Per-App Volume feature is actively routing separate streams.

### UI & Activity Lifecycle
*   `NewActivity(model: LiveActivityModel)`: Pushes a new Live Activity (e.g., Music, Timer) to the Core's stack.
*   `RemoveActivity(activityId: String)`: Removes an activity from the stack.
*   `ToggleExpand` / `Collapse` / `DismissActive`: Manages the visual state of the current top-most UI component.

## 📦 Data Models

### `IslandUiState`
The single source of truth broadcasted by the Core. Clients should observe this state to render their UI.
*   `islandState`: Current physical state of the overlay (`HIDDEN`, `TYPE_0_RING`, `TYPE_1_MINI`, etc.)
*   `gamingFps`, `gamingCpuUsage`, `gamingGpuUsage`: Live telemetry data.
*   `isMicActive`: Boolean indicating if the microphone is in use (useful for dynamic volume sliders).

---
*Note: This API is currently unstable and subject to change as the Redwood ecosystem evolves.*