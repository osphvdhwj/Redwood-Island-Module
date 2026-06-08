package com.example.dynamicisland.shared.model

/**
 * Supported hardware performance profiles.
 */
enum class PerformanceLevel {
    /** Minimum clocks, maximum power saving. */
    BATTERY,
    /** Balanced clocks, reactive scaling. */
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
    BALANCED,
    /** Locked high clocks for stable FPS. */
    PERFORMANCE,
    /** Absolute maximum clocks, background suppression, highest priority. */
    WILD
}
