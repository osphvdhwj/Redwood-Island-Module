package com.example.dynamicisland.shared.model

/**
 * Represents the visual state and size configuration of the Dynamic Island.
 */
enum class IslandState {
    /** Island is completely invisible and touch-transparent. */
    HIDDEN,

import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
    /** The idle "pill" or "ring" state when no active events are present. */
    TYPE_0_RING,

    /** Compact horizontal pill for secondary info or ongoing tasks. */
    TYPE_1_MINI,

    /** Medium-sized pill for active interactions (e.g., Music, Charging). */
    TYPE_2_MID,

    /** Fully expanded view for dashboard or critical alerts. */
    TYPE_3_MAX,

    /** Specialized 3D-like rotating cube for minimized album art. */
    TYPE_CUBE,

    /** Reactive ring with rotating particles. */
    TYPE_ORBITAL,

    /** Sharp-edged container with solid borders. */
    TYPE_BRUTALIST,

    /** Split view showing two distinct activities simultaneously. */
    TYPE_SPLIT
}
