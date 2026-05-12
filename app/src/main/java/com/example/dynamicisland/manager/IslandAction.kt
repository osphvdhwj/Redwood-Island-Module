package com.example.dynamicisland.gesture

data class IslandGesture(val name: String) {
    companion object {
        val SWIPE_LEFT = IslandGesture("SWIPE_LEFT")
        val SWIPE_RIGHT = IslandGesture("SWIPE_RIGHT")
        val TAP = IslandGesture("TAP")
        val LONG_PRESS = IslandGesture("LONG_PRESS")
        val FORCE_TOUCH = IslandGesture("FORCE_TOUCH")
    }
}