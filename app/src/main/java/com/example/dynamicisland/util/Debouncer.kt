package com.example.dynamicisland.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pillar 5: Performance Optimization
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
