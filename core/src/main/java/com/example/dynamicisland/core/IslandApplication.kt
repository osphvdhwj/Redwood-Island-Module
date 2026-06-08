package com.example.dynamicisland.core

import android.app.Application
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.intelligence.IslandGenerativeEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*

@HiltAndroidApp
class IslandApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BrainEntryPoint {
        fun neuralCore(): IslandNeuralCore
        fun generativeEngine(): IslandGenerativeEngine
    }
}
