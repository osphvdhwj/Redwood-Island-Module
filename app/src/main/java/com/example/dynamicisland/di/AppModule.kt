package com.example.dynamicisland.di

import android.content.Context
import com.example.dynamicisland.manager.IslandHardwareMonitor
import com.example.dynamicisland.manager.IslandMediaManager
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.system.hook.AospEventProvider
import com.example.dynamicisland.system.hook.SystemEventProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideSystemEventProvider(): SystemEventProvider {
        return AospEventProvider()
    }

    @Provides
    @Singleton
    fun provideIslandMediaManager(@ApplicationContext context: Context): IslandMediaManager {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        return IslandMediaManager(
            context = context,
            scope = scope,
            onMediaChanged = {},
            onMediaTick = {},
            onPeekRequested = {},
            onPauseFadeRequested = {},
            onUncollapseRequested = {}
        )
    }

    @Provides
    @Singleton
    fun provideIslandHardwareMonitor(): IslandHardwareMonitor {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        return IslandHardwareMonitor(
            scope = scope,
            onHardwareUpdate = {}
        )
    }
}
