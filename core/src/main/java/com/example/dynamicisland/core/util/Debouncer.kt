package com.example.dynamicisland.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pillar 5: Performance Optimization
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * Utility to debounce high-frequency events like hardware gauges (CPU temp, FPS).
 * Ensures that the UI is updated at a maximum rate (e.g. 4Hz / 250ms interval)
 * to prevent CPU thrashing and excessive recompositions.
 */
class Debouncer(private val intervalMs: Long = 250L) {
    private var debounceJob: Job? = null

    /**
     * Executes the action only if [intervalMs] has passed since the last invocation.
     * Prevents spamming the UI thread with sensor updates.
     */
    fun process(scope: CoroutineScope, action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(intervalMs)
            action()
        }
    }
}
