package com.example.dynamicisland.ui.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A central event bus for Dynamic Island intents.
 * This allows singletons like IslandController to communicate with 
 * UI-scoped components like IslandViewModel.
 */
@Singleton
class IslandEventBus @Inject constructor() {
    private val _intents = MutableSharedFlow<IslandIntent>(extraBufferCapacity = 64)
    val intents: SharedFlow<IslandIntent> = _intents.asSharedFlow()

    fun emit(intent: IslandIntent) {
        _intents.tryEmit(intent)
    }
}
