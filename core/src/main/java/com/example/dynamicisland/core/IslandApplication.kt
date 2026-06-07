package com.example.dynamicisland.core

import android.app.Application
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class IslandApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BrainEntryPoint {
        fun neuralCore(): IslandNeuralCore
        fun generativeEngine(): IslandGenerativeEngine
    }
}
