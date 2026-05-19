// File: app/src/main/java/com/example/dynamicisland/gesture/IslandGesture.kt
package com.example.dynamicisland.gesture

/**
 * Every recognisable gesture for the Dynamic Island.
 * Used by IslandController for mapping and by ConfigScreens for settings UI.
 */
enum class IslandGesture {
    // Basic touch gestures
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SINGLE_TAP,       // same as TAP but kept for backwards compatibility
    FORCE_PRESS,      // force / 3D touch
    FORCE_TOUCH,      // alternative name

    // Directional swipes
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,

    // Quick‑settings tile clicks (prefix QS_CLICK_)
    // These match the tile specs sent by SystemUI
    QS_CLICK_WIFI,
    QS_CLICK_BLUETOOTH,
    QS_CLICK_FLASHLIGHT,
    QS_CLICK_DND,
    QS_CLICK_BATTERY_SAVER,
    QS_CLICK_LOCATION,
    QS_CLICK_AIRPLANE,
    QS_CLICK_NFC,
    QS_CLICK_HOTSPOT,
    QS_CLICK_CAST
}