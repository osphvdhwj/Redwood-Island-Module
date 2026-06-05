# Redwood Island & Nav Island: Master Project Blueprint
**DO NOT DELETE OR MODIFY THIS FILE.**
**AUTHORITATIVE SOURCE FOR AGENT CONTEXT.**

## Project Core Philosophy
Dual-island system utility for rooted Android (AOSP/HyperOS).
- **Redwood Island (Top):** Dynamic Island system.
- **Nav Island (Bottom):** Navigation Pill enhancement.
- **Game Hub (Synergy):** Integration with Game Hub Version 15 (`io.chaldeaprjkt.gamespace`) featuring an AOSP OEM-Level Engine for extreme gaming performance.

---

## 🎮 Game Hub & OEM Engine (Version 15 Integration)
The module now incorporates deep system-level optimizations previously exclusive to premium gaming phones (Xiaomi, ASUS, OnePlus).

### 1. System Optimizer (Backend)
- **Kernel Memory Boosting**: Forceful background process termination plus `drop_caches` and `compact_memory` kernel commands to maximize physical RAM.
- **Filesystem Junk Cleaner**: Securely wipes app caches, system logs, tombstones, and obsolete Dalvik/ART .vdex files.
- **Async Deep Scan**: Non-blocking I/O scan to identify large files (>50MB) and unused APKs.

### 2. Hardware Controller (Security Backend)
- **Clock Locking**: Snapdragon GPU clock locking and CPU governor switching (Battery, Balanced, Performance, Wild modes).
- **Prop Spoofing**: Spoofs MIUI environment and Xiaomi product build to unlock 90/120 FPS in restricted games.

### 3. Visual Dashboard (UI)
- **Xiaomi 6.0 Style**: A sleek, minimal dashboard integrated into the Island overlay with live vitals and feature grid buttons.

---

## 🛠️ Technical Specifications & Requirements

### 1. Redwood Island (Top)
- **Battery Ring:** A fully customizable ring showing battery percentage.
  - **Location:** Can be placed anywhere (distinguished as 'top' for naming).
  - **Dynamics:** The Dynamic Island grows *out* of this ring.
  - **Aesthetics:** Color changes based on battery levels (user-definable).
  - **Visibility:** Must not block views; includes a per-app blacklist to disable.
  - **Animation:** Supports "Breathing" and "Charging" effects.

### 2. Nav Island (Bottom)
- **Pill Hooking:** Directly modifies the `navigation_bar_item` / Pill.
- **Battery Progress:** The Pill functions as a progress bar. 
  - **Color:** Dynamic colors matching the Battery Ring configuration.
  - **Visibility:** Uses a permanent "Dull Color" background so the Pill is never lost, plus a vertical pipe separator (`|`) to indicate the current level exactly.
- **Button Nav:** If user switches to 3-button navigation, Nav Island must **disappear**. (Pending idea for buttons).
- **Expansion:** Expands into large units (Music bar/Dashboard) like Google Gemini/Lens (1x5, 2x6, etc.).
  - **Music Bar Morph:** The Pill "stretches" to become the background of the Music Bar, positioned slightly above the default nav area.

### 3. General Logic & Interaction
- **Synergy:** When battery percentage changes (e.g., 15% -> 14%), both islands pulse in sync for exactly 1 second.
- **Gestures:** Whole gesture system is fully customizable via settings (Double-tap, Swipe, Long-press).
- **Animations:** Charging animations (Glow, Gradient, Particles) are selectable from settings.
- **Haptics:** Dedicated "Haptics" settings category. Sync pulse vibration is OFF by default but togglable.
- **Touch Safety:** Must avoid full-screen window spans to prevent Android 12+ Touch Occlusion.
- **Thermal Management:** Option to "throttle" (stop) animations if device temperature exceeds thresholds.
- **Panic Button:** Quick Settings (QS) Tile to instantly kill/toggle both Islands.

---

## Core Synergy & Intelligence
- **Camera Auto-Detection**: Uses `WindowInsets.displayCutout` to automatically align the Redwood Ring with the device's physical camera punch-hole.
- **Status Bar Synergy**: Automatically fades out the system clock and notification icons when the Island expands to prevent visual overlap.
- **Nav Mode Awareness**: Automatically detects Gesture vs 3-Button navigation and adjusts the Nav Island logic to maintain system stability.

