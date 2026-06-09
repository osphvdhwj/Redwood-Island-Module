# Redwood Island & Core - Project Mandates

This file contains foundational instructions for AI agents operating within the Redwood Island & Core codebase. Adherence to these mandates is non-negotiable to maintain architectural integrity and system stability.

## 🏗️ Architectural Vision
The project follows a **Strict Dual-App Architecture** designed to mimic the seamless, high-performance experience of Apple/iOS.

1.  **Redwood Core (The Brain):**
    *   **Package:** `com.example.dynamicisland.core`
    *   **Role:** Backend System Daemon.
    *   **Logic:** Handles all low-level system observation, hardware control, AI state processing, and root-level hooks.
    *   **Privilege:** MUST utilize `su` (Root Shell) for all hardware-level interactions. No high-latency standard Android APIs for performance-critical tasks.
    *   **State:** The `IslandNeuralCore` is the single source of truth for the entire suite.

2.  **Redwood Island (The UI):**
    *   **Role:** Frontend Presentation Layer.
    *   *Note:* Currently integrated as the UI components within the `core` module but logically distinct.
    *   **Tech Stack:** 100% Native Jetpack Compose. NO WebViews for system overlays.
    *   **Behavior:** Must remain "dumb" and reactive. It dispatches `IslandIntent` to the Core and renders based on `IslandUiState`.

## 🛠️ Implementation Standards

### 1. Root-Level "SU" Hooks
*   Prefer native shell commands (logcat, dumpsys, sysfs) over standard Android SDK listeners.
*   Persistent root processes must be managed via the `RootDaemonEngine`.
*   All new hardware observation logic (Camera, Thermals, Battery mA) must live in the Core Daemon.

### 2. UI & Interaction
*   **Aesthetics:** Dark mode, blurred transparent backgrounds, and fluid "Liquid Physics" animations.
*   **Responsiveness:** Low touch latency is a priority. All UI changes must be triggered by state updates from the Neural Core.
*   **Widget Stacking:** Always support iOS-style stacking. When a new activity arrives, push to the `widgetStack`. When dismissed, restore the previous activity in the stack.

### 3. IPC & Security
*   All communication between components must go through the established `IslandBrainService` using the `com.redwood.permission.SECURE_IPC` permission.
*   Public APIs must be documented in `CORE_API.md`.

### 4. Anti-Cheat Compliance (Hard Mandate)
*   **Zero Visibility:** Never hook or inject into Game Processes.
*   **Passive Only:** Hardware telemetry must be read from Kernel nodes (/sys, /proc), not from app memory.
*   **Stealth UI:** Use standard Android `SYSTEM_ALERT_WINDOW` overlays to ensure the UI is whitelisted by anti-cheat systems.

### 5. Git & Workflow
*   **DO NOT PUSH CODE** to remote repositories unless explicitly instructed by the user.
*   **DO NOT BUILD APKs** unless explicitly requested.
*   Maintain the `README.md` and `CORE_API.md` alongside code changes.

## 📝 Developer Context
The target device is **Redwood (POCO X5 Pro)** running **PixelOS (Android 15 base)**. Always account for custom ROM features like native Per-App Volume and specific kernel nodes for Snapdragon hardware.
