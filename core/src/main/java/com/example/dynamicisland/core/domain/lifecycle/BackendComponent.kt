package com.example.dynamicisland.core.domain.lifecycle

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
