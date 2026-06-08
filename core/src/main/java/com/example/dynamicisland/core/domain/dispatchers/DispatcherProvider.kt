package com.example.dynamicisland.core.domain.dispatchers

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme

import com.example.dynamicisland.shared.settings.*
/**
 * THREADING ISOLATION ENGINE
import com.example.dynamicisland.shared.model.*
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
