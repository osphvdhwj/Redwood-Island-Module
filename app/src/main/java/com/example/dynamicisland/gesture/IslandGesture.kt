// In app/src/main/java/com/example/dynamicisland/gesture/IslandGesture.kt
package com.example.dynamicisland.gesture

data class IslandGesture(val name: String) {
    companion object {
        // Add the missing gesture constants
        val SINGLE_TAP = IslandGesture("SINGLE_TAP")
        val DOUBLE_TAP = IslandGesture("DOUBLE_TAP")
        val SWIPE_UP = IslandGesture("SWIPE_UP")
        val SWIPE_DOWN = IslandGesture("SWIPE_DOWN")
        // Keep the existing ones
        val SWIPE_LEFT = IslandGesture("SWIPE_LEFT")
        val SWIPE_RIGHT = IslandGesture("SWIPE_RIGHT")
        val TAP = IslandGesture("TAP")
        val LONG_PRESS = IslandGesture("LONG_PRESS")
        val FORCE_TOUCH = IslandGesture("FORCE_TOUCH")
    }
}