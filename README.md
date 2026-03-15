
# 🏝️ Redwood Island Module

An advanced, highly customizable, and hardware-accelerated **Dynamic Island** module for Android, built using **LSPosed** and **Jetpack Compose**.

Redwood Island injects directly into Android's `SystemUI`, bypassing standard WindowManager limitations to create a seamless, OS-level overlay. It features a fully interactive gesture engine, real-time media extraction, dynamic typography, and deep hardware glassmorphism.

## ✨ Core Features

* **4 Fluid States:** Seamlessly transitions between **Ring**, **Mini**, **Mid**, and **Max** states based on priority and user gestures.
* **Pro-Tier Media Engine:** * Integrates with Android's `MediaSessionManager` to capture playback states globally.
* **Dynamic Palette:** Uses the Google Palette API to extract a dominant background color and a mathematically perfect, high-contrast text color from the current Album Art.
* **Wavy Audio Visualizer:** A custom trigonometric canvas path that ripples around the camera hole in the "Ring" state.
* **Rotating Media Cube:** A spinning 20dp 3D-like album art cube in the Mini state.
* **Cinematic Backgrounds:** Heavy 24dp blurred album art used as the physical background for the Mid and Max pills.
* **Circular Progress:** Real-time track progress wraps the circular album thumbnail.


* **Omni-Directional Gesture Engine:**
* **Swipe Up/Down:** Travel up and down the Island size ladder manually.
* **Swipe Left/Right:** Skip to Next/Previous tracks.
* **Single Tap:** Play/Pause.
* **Double Tap:** Instantly collapse Max to Mini.


* **Hardware Glassmorphism:** Uses Android 12+ `FLAG_BLUR_BEHIND` for true physical background blurring, combined with dynamic transparency.
* **Live Customizer:** A companion app (`ConfigActivity`) to independently adjust the Width, Height, X, and Y coordinates of every single state in real-time.

---

## 🏗️ Architecture & Technical Milestones (For Future Devs)

Building a Compose UI inside a SystemUI Xposed hook comes with severe Android security limitations. Here is how we bypassed them:

### 1. The SELinux & Settings Sync Bypass (RAM Payload)

**The Problem:** Android 15's strict SELinux policies prevent `SystemUI` from reading the shared preference XML files of other apps while running. Standard `XSharedPreferences` only works on the initial boot.
**The Solution:** When a user adjusts a slider in the Config App, we don't just save it to disk. We pack the exact slider coordinates into an `Intent` and beam the data directly through the device's RAM.

```kotlin
// Pack values into the Intent and broadcast it
val intent = Intent("com.example.dynamicisland.RELOAD_PREFS")
intent.putExtra("w", w) // ...
sendBroadcast(intent)

```

### 2. The Broadcast Visibility Fix (Android 11+)

**The Problem:** Because of modern background restrictions, standard Intents are silently dropped before they reach the background `SystemUI` process. `Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND` is hidden (`@hide`) in the standard SDK.
**The Solution:** We force the system to wake up the background receiver by passing the raw hex value of the hidden flag:

```kotlin
intent.addFlags(0x01000000) // Raw hex for FLAG_RECEIVER_INCLUDE_BACKGROUND

```

### 3. Bypassing the Camera Punch Hole (Y-Axis Limits)

**The Problem:** Android's WindowManager prevents overlays from drawing over or above the camera cutout safe zone.
**The Solution:** We force the `WindowManager.LayoutParams` to ignore screen bounds and display cutouts entirely:

```kotlin
flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

```

### 4. APK Shrinking vs. LSPosed Crashes

**The Problem:** Using Jetpack Compose's `material-icons-extended` inflates the APK to 40MB+. However, enabling ProGuard/R8 to shrink the app causes LSPosed Manager to crash (`Resources$NotFoundException: Resource ID #0x7f0f0000`) because it deletes the `xposed_description` string.
**The Solution:** 1. `isMinifyEnabled = true` (To tree-shake the 40MB of Kotlin-based Compose icons).
2. `isShrinkResources = false` (To protect the XML strings LSPosed needs to boot).
3. Result: A perfectly stable **~2MB** APK.

*Note: Never use an XML Vector (`<mipmap>`) as the main app icon, or the LSPosed Manager UI will crash trying to parse it.*

---

## 🛠️ File Structure

* `MainHook.kt` - The LSPosed entry point. Hooks into `com.android.systemui` and injects the Compose View.
* `IslandController.kt` - The Brain. Handles `MediaSessionManager`, Hardware Monitoring, State Priorities, Palette Color Extraction, and the Omni-Gesture routing.
* `DynamicIslandView.kt` - The UI Engine. A massive, heavily optimized Jetpack Compose file handling animations, spring physics, canvas drawing, hardware blurring, and touch interception.
* `LiveActivityModel.kt` - Sealed classes representing the data flowing into the Island (Music, Hardware, Dashboard, Charging).
* `ConfigActivity.kt` - The frontend companion app for user customization.

## 🚀 Installation & Build

1. **Requirements:**
* A rooted Android device with **LSPosed** installed.
* Android Studio with AGP 8.5.2+ (Targeting SDK 35).


2. **Build:**
* Build the Release APK (ProGuard rules are already configured to protect Xposed entry points).


3. **Deploy:**
* Install the APK.
* Open LSPosed Manager -> Modules -> Enable **Redwood Island**.
* Check **System UI** in the scope list.
* Reboot SystemUI (or device).

---

# Redwood Dynamic Island Module

An advanced, high-performance LSPosed module that injects a fluid, interactive "Dynamic Island" overlay directly into Android 14's SystemUI. 

**New Updates:**
This module features Live Activities (Maps, Downloads), OTP Catching, Hardware Monitoring, Apple-grade spring physics, and dynamic pill resizing.

---

## ⚠️ CRITICAL DEVELOPER ARCHITECTURE NOTES ⚠️
**DO NOT ALTER THE CORE INJECTION LOGIC.** This module was specifically engineered to bypass strict Android 14 tapjacking protections and custom ROM (e.g., CrDroid) initialization obfuscations. Changing these rules will cause the module to become invisible or fail to hook.

### 1. The Injection Target (`MainHook.kt`)
* **DO NOT** hook `com.android.systemui.SystemUIApplication`. Custom ROMs often bypass or obfuscate this class during boot, causing the module to fail silently.
* **MUST USE:** Hook the base `android.app.Application` class's `onCreate` method, then check if `app.packageName == "com.android.systemui"`. This is un-bypassable by the OS.
* **Delay:** The Compose View injection must be delayed by at least `3000ms` using `Handler(Looper.getMainLooper()).postDelayed`. Injecting instantly will cause SystemUI to overwrite or crash the view during its own status bar initialization.

### 2. The WindowManager Parameters (`MainHook.kt`)
* **Window Type:** MUST use `2024` (`TYPE_NAVIGATION_BAR_PANEL`). Using `TYPE_APPLICATION_OVERLAY` will be blocked by Android 12+ Tapjacking/Secure Window protections.
* **Window Dimensions:** MUST be `MATCH_PARENT` for both Width and Height.
* **DO NOT** use `WRAP_CONTENT` with Type 2024. The strict system WindowManager will reject dynamic coordinate resizing and silently collapse the window to `0x0` pixels (making the island invisible).

### 3. Touch Passthrough Engine (`DynamicIslandView.kt`)
Because the WindowManager layout is `MATCH_PARENT` (covering the entire screen), it blocks all touches to the phone. 
* **The Solution:** We use Java Reflection to attach an `OnComputeInternalInsetsListener` to the `viewTreeObserver`.
* **How it works:** As the Compose UI animates, `onGloballyPositioned` tracks the exact `Rect` coordinates of the Island. The internal insets listener reads this `Rect` and dynamically punches a "touch hole" through the rest of the screen, allowing normal phone usage while keeping the Island interactive.
* **Memory Leak Warning:** The `insetsListenerProxy` MUST be explicitly removed in `onDetachedFromWindow()` via reflection. Failing to do so will leak the entire Compose ViewTree and eventually crash `system_server`.

### 4. Layout Translations (60+ FPS Rule)
* **DO NOT** use `Modifier.offset(x, y)` to animate the Island's X/Y coordinates. This forces a full Layout Phase recomposition on every frame and drops the frame rate.
* **MUST USE:** `Modifier.graphicsLayer { translationX = ... ; translationY = ... }`. This pushes the spatial movement entirely to the GPU render phase, keeping animations buttery smooth.

### 5. System Server Hooks (Live Activities & App Tracking)
To track what apps are open and catch incoming ongoing notifications (for Live Activities), the module also hooks the `android` package (System Server).
* Hooks `ActivityTaskManagerService` to broadcast `APP_CHANGED` (powers the Gaming Blacklist).
* Hooks `NotificationManagerService` to intercept `FLAG_ONGOING_EVENT` and OTP messages before they reach the status bar.

---

## 🛠️ Features
* **Smart Track Peek:** Temporarily expands the island to show track changes before hiding again.
* **Gaming Blacklist:** Automatically hides the island when specific packages (like Free Fire Max) are brought to the foreground.
* **Battery Wellbeing Integration:** Long-pressing the charging cube deep-links into custom battery management apps.
* **Luminance Clamping:** Dynamically guarantees ring and text visibility even when extracting colors from pitch-black album art.

## 📦 Build Instructions
1. Clone the repository.
2. Open in Android Studio or compile via Gradle (`./gradlew assembleRelease`).
3. Install the APK and enable in LSPosed Manager.
4. **Important:** After installing or updating, you must perform a **Hard Reboot** of the device to clear the SystemUI and LSPosed cache. Simply restarting SystemUI via terminal is not enough for structural changes.
---
