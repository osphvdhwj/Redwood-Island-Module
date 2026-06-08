# Redwood Island & Core 🏝️

The ultimate high-performance UI customization and game optimization suite for rooted AOSP/HyperOS devices.

## 🌟 Key Features

### 🔴 Redwood Island (The Brain)
*   **Game Turbo UI:** Native Jetpack Compose implementation of a Xiaomi-style Game Space overlay, featuring an animated FPS graph, vertical brightness/volume sliders, and performance mode toggles.
*   **Performance Engine:** Real-time hardware monitoring (CPU/GPU/FPS) via AOSP-level shell access, capable of dynamically applying CPU scaling governors (Battery, Balanced, Wild).
*   **Top Island (Battery Ring & Stacking):** Persistent battery percentage ring with priority-weighted notification stacking and fluid liquid physics.
*   **Nav Island:** System Pill hooking to turn the bottom navigation pill into a battery progress bar or music control.

### 💊 Redwood Core (The Ghost)
*   **LSPosed Injection:** A lightweight, UI-less Xposed module that intercepts system events directly from the Android framework (SystemUI, ActivityManager) and securely forwards them to Redwood Island via `IslandContentProvider`.

## 🛠️ Technical Highlights
*   **Dual-App Architecture:** Clean separation between the `core` (UI and logic) and `app` (Satellite/Hook) modules.
*   **Root Shell Executor:** Bypasses standard Android APIs to directly communicate with kernel sysfs paths (`/sys/devices/system/cpu/...` and `/sys/class/kgsl/...`) for true system-level control.
*   **Native Compose Overlay:** The Game Space overlay is built entirely in Jetpack Compose, avoiding heavy WebViews while achieving 60+ FPS animated charts and blurred backgrounds.
*   **Dual-Window Architecture:** Uses independent top/bottom windows to bypass Android 12+ Touch Occlusion (No touch blocking).

## 🏗️ Technical Architecture

### 1. IPC & State Management
Utilizes a decentralized broadcast and ContentProvider-based IPC mechanism to synchronize state across the `android` (System Server) and `com.example.dynamicisland.core` processes. This bypasses SELinux restrictions and ensures zero-latency responsiveness to system events.

### 2. The Omni-Gesture Engine
A custom touch interception layer that uses `OnComputeInternalInsetsListener` to punch dynamic "touch holes" in the system-wide overlay. This allows overlays to remain interactive while permitting 100% of touches to pass through to the underlying OS when idle.

---

## 📝 Project Blueprint
Detailed technical requirements and future roadmap can be found in `MASTER_PROJECT_SNAPSHOT.md`. **Do not modify that file.**

---
## ⚖️ License & Disclaimer
This project is for educational and customization purposes only. Use at your own risk.

*Note: Redwood Island is not affiliated with Apple Inc. or any Android ROM distribution.*
