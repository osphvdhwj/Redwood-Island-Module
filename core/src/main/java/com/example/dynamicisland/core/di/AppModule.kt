package com.example.dynamicisland.core.di

import android.content.Context
import com.example.dynamicisland.core.data.repository.HardwareRepository
import com.example.dynamicisland.core.manager.IslandMediaManager
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.core.ipc.IslandIPCClient
import com.example.dynamicisland.core.data.repository.BatteryRepository
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.dispatchers.StandardDispatcherProvider
import com.example.dynamicisland.core.data.repository.GameHubRepository
import com.example.dynamicisland.core.util.shell.ShellExecutor
import com.example.dynamicisland.core.util.shell.AndroidShellExecutor
import dagger.Module

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideShellExecutor(): ShellExecutor {
        return AndroidShellExecutor()
    }

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
