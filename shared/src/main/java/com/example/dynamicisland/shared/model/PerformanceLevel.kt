package com.example.dynamicisland.shared.model

/**
 * Supported hardware performance profiles.
 */
enum class PerformanceLevel {
    /** Minimum clocks, maximum power saving. */
    BATTERY,
    /** Balanced clocks, reactive scaling. */
    BALANCED,
    /** Locked high clocks for stable FPS. */
    PERFORMANCE,
    /** Absolute maximum clocks, background suppression, highest priority. */
    WILD
}
