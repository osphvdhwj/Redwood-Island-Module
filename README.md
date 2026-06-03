# Redwood Island 🏝️

The ultimate high-performance Dynamic Island & Navigation enhancement module for rooted AOSP/HyperOS devices.

## 🌟 Key Features

### 🔴 Redwood Island (Top)
*   **Battery Ring:** Persistent battery percentage ring (hole-punch or custom position).
*   **Adaptive Expansion:** The island grows fluidly out of the Battery Ring.
*   **Smart Stacking:** Manages multiple notifications with priority-weighted scoring.
*   **Liquid Physics:** High-fidelity animations with customizable "Tactile Squish".
*   **Dashboard Max:** Expandable 2x6 / 1x5 units for music, hardware stats, and shortcuts.

### 💊 Nav Island (Bottom)
*   **System Pill Hook:** Directly modifies the Android Navigation Pill—not just an overlay.
*   **Battery Progress:** The pill itself acts as a sleek battery progress bar.
*   **Dynamic Morphing:** Expands into a "Music Bar" or "Shortcuts Bar" (Google Lens/Gemini style).
*   **Gesture Synergy:** Synchronized pulses with the Top Island during battery state changes.

## 🛠️ Technical Highlights
*   **Dual-Window Architecture:** Uses independent top/bottom windows to bypass Android 12+ Touch Occlusion (No touch blocking).
*   **Hardware Blur:** Real-time AGSL/Palette extraction for adaptive wallpaper-based theming.
*   **Zero Leak Lifecycle:** Aggressive bitmap recycling and memory hardening for 4+ day uptime.
*   **LSPosed Powered:** Deep hooks into SystemUI for native performance.

## 🏗️ Technical Architecture

### 1. Unified Controller (`IslandController.kt`)
Redwood Island utilizes a decentralized controller that manages two distinct overlay layers. It uses an internal `KnowledgeBase` (LruCache) to synchronize battery states and gesture pulses across the Top and Bottom islands without redundant sensor polling.

### 2. The Omni-Gesture Engine
A custom touch interception layer that uses `OnComputeInternalInsetsListener` to punch dynamic "touch holes" in the system-wide overlay. This allows the Island to remain interactive while permitting 100% of touches to pass through to the underlying OS when the Island is idle.

### 3. IPC & State Management
Utilizes a decentralized broadcast-based IPC mechanism to synchronize state across the `android` (System Server) and `com.android.systemui` processes. This bypasses SELinux restrictions and ensures zero-latency responsiveness to system events.

---

## 📝 Project Blueprint
Detailed technical requirements and future roadmap can be found in `MASTER_PROJECT_SNAPSHOT.md`. **Do not modify that file.**

---
## ⚖️ License & Disclaimer
This project is for educational and customization purposes only. Use at your own risk.

*Note: Redwood Island is not affiliated with Apple Inc. or any Android ROM distribution.*
