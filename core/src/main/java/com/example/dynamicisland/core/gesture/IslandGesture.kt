package com.example.dynamicisland.core.gesture

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*

/**
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
 * Every recognisable gesture for the Dynamic Island.
import com.example.dynamicisland.shared.settings.*
 * Expanded with multi-finger and custom library support.
 */
import com.example.dynamicisland.shared.model.*
enum class IslandGesture {
    // Basic touch gestures
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    SINGLE_TAP,
    FORCE_PRESS,
    FORCE_TOUCH,

    // Directional swipes (1-finger)
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,

    // Multi-finger gestures
    TWO_FINGER_TAP,
    TWO_FINGER_SWIPE_UP,
    TWO_FINGER_SWIPE_DOWN,
    THREE_FINGER_TAP,
    PINCH_IN,
    PINCH_OUT,

    // Custom / AI Reinforcement slots
    CUSTOM_PATTERN_A,
    CUSTOM_PATTERN_B,

    // Quick‑settings tile clicks
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
