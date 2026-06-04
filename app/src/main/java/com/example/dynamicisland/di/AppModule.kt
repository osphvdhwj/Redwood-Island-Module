package com.example.dynamicisland.di

import android.content.Context
import com.example.dynamicisland.data.repository.HardwareRepository
import com.example.dynamicisland.manager.IslandMediaManager
import com.example.dynamicisland.settings.SettingsManager
import com.example.dynamicisland.ipc.IslandIPCClient
import com.example.dynamicisland.data.repository.BatteryRepository
import com.example.dynamicisland.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.domain.dispatchers.StandardDispatcherProvider
import com.example.dynamicisland.data.repository.GameHubRepository
import dagger.Module

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return StandardDispatcherProvider()
    }

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideBatteryRepository(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): BatteryRepository {
        return BatteryRepository(context, dispatchers)
    }

    @Provides
    @Singleton
    fun provideGameHubRepository(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): GameHubRepository {
        return GameHubRepository(context, dispatchers)
    }

    @Provides
    @Singleton
    fun provideIslandIPCClient(@ApplicationContext context: Context): IslandIPCClient {
        return IslandIPCClient.get(context)
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
    fun provideHardwareRepository(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): HardwareRepository {
        return HardwareRepository(context, dispatchers)
    }
}
