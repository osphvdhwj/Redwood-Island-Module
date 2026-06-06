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

import com.example.dynamicisland.core.intelligence.IslandPredictionEngine
import com.example.dynamicisland.core.gesture.MLGestureClassifier

import com.example.dynamicisland.core.intelligence.IslandGenerativeEngine

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIslandGenerativeEngine(neuralCore: IslandNeuralCore): IslandGenerativeEngine {
        return IslandGenerativeEngine(neuralCore)
    }

    @Provides
    @Singleton
    fun provideIslandPredictionEngine(@ApplicationContext context: Context): IslandPredictionEngine {
        return IslandPredictionEngine.get(context)
    }

    @Provides
    @Singleton
    fun provideMLGestureClassifier(@ApplicationContext context: Context): MLGestureClassifier {
        return MLGestureClassifier(context)
    }

    @Provides
    @Singleton
    fun provideShellExecutor(): ShellExecutor {
        return AndroidShellExecutor()
    }

    @Provides
    @Singleton
    fun provideRootShellEngine(dispatchers: DispatcherProvider): RootShellEngine {
        return RootShellEngine(dispatchers)
    }

    @Provides
    @Singleton
    fun provideSysfsController(rootEngine: RootShellEngine): SysfsController {
        return SysfsController(rootEngine)
    }

    @Provides
    @Singleton
    fun provideStorageScanner(dispatchers: DispatcherProvider): com.example.dynamicisland.core.data.repository.cleanup.StorageScanner {
        return com.example.dynamicisland.core.data.repository.cleanup.StorageScanner(dispatchers)
    }

    @Provides
    @Singleton
    fun provideResidualCleaner(rootEngine: RootShellEngine): com.example.dynamicisland.core.data.repository.cleanup.ResidualCleaner {
        return com.example.dynamicisland.core.data.repository.cleanup.ResidualCleaner(rootEngine)
    }

    @Provides
    @Singleton
    fun provideAppFreezer(rootEngine: RootShellEngine): com.example.dynamicisland.core.data.repository.cleanup.AppFreezer {
        return com.example.dynamicisland.core.data.repository.cleanup.AppFreezer(rootEngine)
    }

    @Provides
    @Singleton
    fun provideUltraBatterySaver(rootEngine: RootShellEngine): com.example.dynamicisland.core.data.repository.profiles.UltraBatterySaver {
        return com.example.dynamicisland.core.data.repository.profiles.UltraBatterySaver(rootEngine)
    }

    @Provides
    @Singleton
    fun provideThermalEngineBypass(
        rootEngine: RootShellEngine,
        hardwareRepository: com.example.dynamicisland.core.data.repository.HardwareRepository,
        dispatchers: DispatcherProvider,
        neuralCore: com.example.dynamicisland.core.domain.state.IslandNeuralCore
    ): com.example.dynamicisland.core.data.repository.profiles.ThermalEngineBypass {
        return com.example.dynamicisland.core.data.repository.profiles.ThermalEngineBypass(rootEngine, hardwareRepository, dispatchers, neuralCore)
    }

    @Provides
    @Singleton
    fun provideCleanerManager(
        @ApplicationContext context: Context,
        neuralCore: com.example.dynamicisland.core.domain.state.IslandNeuralCore,
        scanner: com.example.dynamicisland.core.data.repository.cleanup.StorageScanner,
        cleaner: com.example.dynamicisland.core.data.repository.cleanup.ResidualCleaner,
        freezer: com.example.dynamicisland.core.data.repository.cleanup.AppFreezer,
        ultraBatterySaver: com.example.dynamicisland.core.data.repository.profiles.UltraBatterySaver,
        thermalBypass: com.example.dynamicisland.core.data.repository.profiles.ThermalEngineBypass,
        dispatchers: com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
    ): com.example.dynamicisland.core.data.repository.cleanup.CleanerManager {
        return com.example.dynamicisland.core.data.repository.cleanup.CleanerManager(context, neuralCore, scanner, cleaner, freezer, ultraBatterySaver, thermalBypass, dispatchers)
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
