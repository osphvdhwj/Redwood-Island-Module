package com.example.dynamicisland.core.domain.lifecycle

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * REWOOD LIFECYCLE PROTOCOL
 *
 * Defines the standard lifecycle for all backend managers.
 * Ensures consistent startup and cleanup to prevent memory leaks.
 */
interface BackendComponent {
    /** Called when the host process (SystemUI) is ready. */
    fun onStart()
    
    /** Called when the hook is being destroyed or restarted. */
    fun onStop()
}
