package com.example.dynamicisland.core.domain.dispatchers

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * THREADING ISOLATION ENGINE
 *
 * Provides specialized dispatchers for different workloads.
 * Prevents heavy computations or I/O from stuttering the Island animations.
 */
interface DispatcherProvider {
    fun main(): CoroutineDispatcher = Dispatchers.Main
    fun io(): CoroutineDispatcher = Dispatchers.IO
    fun default(): CoroutineDispatcher = Dispatchers.Default
    fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined
    
    /** Dedicated thread for high-frequency hardware polling (FPS, Battery). */
    fun hardware(): CoroutineDispatcher = Dispatchers.IO
    
    /** Dedicated thread for state reduction and persistence. */
    fun state(): CoroutineDispatcher = Dispatchers.Default
}

class StandardDispatcherProvider : DispatcherProvider
