package com.example.dynamicisland.core.accessibility

import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * Manages accessibility announcements for Dynamic Island state changes.
 * Integrates with Android's accessibility service to provide spoken feedback
 * when the island transitions between states.
 */
class IslandAccessibilityManager(context: Context) {

    private val accessibilityManager: AccessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private var isEnabled = false

    /**
     * Starts listening and enables announcements.
     */
    fun start() {
        isEnabled = true
    }

    /**
     * Stops announcements.
     */
    fun stop() {
        isEnabled = false
    }

    /**
     * Announces a state change via TalkBack or other screen readers.
     * Respects the enabled flag and only sends if accessibility is active.
     */
    fun announceStateChange(stateDescription: String) {
        if (!isEnabled || !accessibilityManager.isEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
            event.text.add("Dynamic Island: $stateDescription")
            event.className = this.javaClass.name
            event.packageName = "com.example.dynamicisland"
            accessibilityManager.sendAccessibilityEvent(event)
            event.recycle()
        }
    }

    /**
     * Returns true if any accessibility service (like TalkBack) is currently active.
     */
    fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }
}