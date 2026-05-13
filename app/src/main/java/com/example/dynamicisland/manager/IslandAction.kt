// File: app/src/main/java/com/example/dynamicisland/manager/IslandAction.kt
package com.example.dynamicisland.manager

/**
 * Every possible action the Dynamic Island can perform when triggered by a gesture.
 * Used by the gesture‑mapping UI (ConfigComponents.kt / GestureDropdown) and
 * executed by IslandController.executeSmartAction().
 */
enum class IslandAction {
    // Default / no‑op
    NONE,

    // Media
    PLAY_PAUSE,
    NEXT_TRACK,
    PREV_TRACK,

    // Volume
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE_TOGGLE,
    VOLUME,

    // Brightness (idle swipe by default)
    BRIGHTNESS,

    // Navigation
    PREV_APP,
    OPEN_DASHBOARD,
    OPEN_QUICK_TOGGLES,

    // Island control
    EXPAND,
    COLLAPSE,

    // System
    SCREENSHOT,
    LAUNCH_SETTINGS,
    LAUNCH_CAMERA,

    // App launcher stub (actual package handled elsewhere)
    LAUNCH_APP_STUB
}