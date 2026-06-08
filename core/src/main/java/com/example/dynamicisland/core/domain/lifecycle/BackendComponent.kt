package com.example.dynamicisland.core.domain.lifecycle

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*

/**
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
 * REWOOD LIFECYCLE PROTOCOL
import com.example.dynamicisland.shared.settings.*
 *
 * Defines the standard lifecycle for all backend managers.
import com.example.dynamicisland.shared.model.*
 * Ensures consistent startup and cleanup to prevent memory leaks.
 */
interface BackendComponent {
    /** Called when the host process (SystemUI) is ready. */
    fun onStart()
    
    /** Called when the hook is being destroyed or restarted. */
    fun onStop()
}
