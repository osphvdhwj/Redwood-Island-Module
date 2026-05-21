# 🏝️ Redwood Island

An advanced, high-performance **LSPosed** module that injects a fluid, interactive "Dynamic Island" overlay directly into Android's **SystemUI**. Built with **Jetpack Compose** and a custom **C++ Hardware Blur Engine**.

---

## ✨ Features

*   **Fluid Multi-State Architecture:** Seamlessly transitions between `Ring`, `Mini`, `Mid`, `Max`, `Cube`, and `Split` states based on real-time event priority.
*   **Deep System Integration:**
    *   **Live Activities:** Real-time tracking for Maps, Downloads, and ongoing tasks.
    *   **Telecom Engine:** Deep hooks into the Android Call pipeline with contact photo extraction.
    *   **Hardware Monitoring:** Integrated FPS, CPU temp, and thermal throttling indicators for gaming.
*   **ROM-Specific Optimizations:** Dedicated support for **Infinity X A15**, **CrDroid**, and **Evolution X** specific APIs (Gaming Mode, Thermal Profiles, Edge Lighting).
*   **Intelligent Features:**
    *   **OTP Catcher:** Automatically intercepts and displays verification codes.
    *   **Privacy Indicators:** Real-time visual alerts for Camera and Microphone usage (A15).
    *   **Continuity Camera:** Integrated barcode/QR scanning from system notifications.
*   **Premium UI/UX:**
    *   **Apple-Grade Physics:** Custom spring-based animation engine for organic transitions.
    *   **Hardware Glassmorphism:** Real-time background blurring using Android 12+ `FLAG_BLUR_BEHIND`.
    *   **Dynamic Palette:** Real-time color extraction from album art for themed UI.

---

## 🏗️ Technical Architecture

### 1. Zero-Latency Injection (`MainHook.kt`)
Redwood Island utilizes a multi-strategy injection engine to ensure compatibility across diverse Android distributions. It targets foundational `android.app.Application` and `SystemUI` lifecycle hooks to safely inject a `WindowManager` overlay with `TYPE_NAVIGATION_BAR_PANEL` (Type 2024), bypassing modern tapjacking protections.

### 2. The Omni-Gesture Engine
A custom touch interception layer that uses `OnComputeInternalInsetsListener` to punch dynamic "touch holes" in the system-wide overlay. This allows the Island to remain interactive while permitting 100% of touches to pass through to the underlying OS when the Island is idle.

### 3. IPC & State Management
Utilizes a decentralized broadcast-based IPC mechanism to synchronize state across the `android` (System Server) and `com.android.systemui` processes. This bypasses SELinux restrictions and ensures zero-latency responsiveness to system events.

---

## 🛠️ Build & Installation

### Requirements
*   **Rooted** Android 12-15 device.
*   **LSPosed** Framework installed.
*   Android Studio (AGP 8.7+).

### Steps
1.  Clone the repository and build the Release APK.
2.  Install the APK on your device.
3.  Enable **Redwood Island** in the LSPosed Manager.
4.  Ensure **System UI** is selected in the scope.
5.  **Reboot** the device to initialize the hooks.

---

## ⚖️ License & Disclaimer
This project is for educational and customization purposes only. Use at your own risk.

*Note: Redwood Island is not affiliated with Apple Inc. or any Android ROM distribution.*
