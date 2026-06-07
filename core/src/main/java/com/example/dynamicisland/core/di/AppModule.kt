package com.example.dynamicisland.core.di

import android.content.Context
import com.example.dynamicisland.core.data.repository.BatteryRepository
import com.example.dynamicisland.core.data.repository.GameHubRepository
import com.example.dynamicisland.core.data.repository.HardwareRepository
import com.example.dynamicisland.core.data.repository.cleanup.*
import com.example.dynamicisland.core.data.repository.profiles.*
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.dispatchers.StandardDispatcherProvider
import com.example.dynamicisland.core.domain.state.IslandController
import com.example.dynamicisland.core.domain.state.IslandNeuralCore
import com.example.dynamicisland.core.gesture.MLGestureClassifier
import com.example.dynamicisland.core.intelligence.IslandGenerativeEngine
import com.example.dynamicisland.core.intelligence.IslandPredictionEngine
import com.example.dynamicisland.core.ipc.IslandIPCClient
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.core.util.shell.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.*

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIslandGenerativeEngine(
        @ApplicationContext context: Context,
        neuralCore: IslandNeuralCore,
        controller: IslandController
    ): IslandGenerativeEngine {
        return IslandGenerativeEngine(context, neuralCore, controller)
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
    fun provideStorageScanner(dispatchers: DispatcherProvider): StorageScanner {
        return StorageScanner(dispatchers)
    }

    @Provides
    @Singleton
    fun provideResidualCleaner(rootEngine: RootShellEngine): ResidualCleaner {
        return ResidualCleaner(rootEngine)
    }

    @Provides
    @Singleton
    fun provideAppFreezer(rootEngine: RootShellEngine): AppFreezer {
        return AppFreezer(rootEngine)
    }

    @Provides
    @Singleton
    fun provideUltraBatterySaver(rootEngine: RootShellEngine): UltraBatterySaver {
        return UltraBatterySaver(rootEngine)
    }

    @Provides
    @Singleton
    fun provideThermalEngineBypass(
        rootEngine: RootShellEngine,
        hardwareRepository: HardwareRepository,
        dispatchers: DispatcherProvider,
        neuralCore: IslandNeuralCore
    ): ThermalEngineBypass {
        return ThermalEngineBypass(rootEngine, hardwareRepository, dispatchers, neuralCore)
    }

    @Provides
    @Singleton
    fun provideCleanerManager(
        @ApplicationContext context: Context,
        neuralCore: IslandNeuralCore,
        scanner: StorageScanner,
        cleaner: ResidualCleaner,
        freezer: AppFreezer,
        ultraBatterySaver: UltraBatterySaver,
        thermalBypass: ThermalEngineBypass,
        dispatchers: DispatcherProvider
    ): CleanerManager {
        return CleanerManager(context, neuralCore, scanner, cleaner, freezer, ultraBatterySaver, thermalBypass, dispatchers)
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
        dispatchers: DispatcherProvider,
        neuralCore: IslandNeuralCore
    ): GameHubRepository {
        return GameHubRepository(context, dispatchers, neuralCore)
    }

    @Provides
    @Singleton
    fun provideIslandIPCClient(@ApplicationContext context: Context): IslandIPCClient {
        return IslandIPCClient.get(context)
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

    @Provides
    @Singleton
    fun provideIslandHapticsManager(@ApplicationContext context: Context): IslandHapticsManager {
        return IslandHapticsManager(context)
    }

    @Provides
    @Singleton
    fun provideIslandNetworkMonitor(@ApplicationContext context: Context, dispatchers: DispatcherProvider): IslandNetworkMonitor {
        return IslandNetworkMonitor(context, CoroutineScope(SupervisorJob() + dispatchers.io()))
    }

    @Provides
    @Singleton
    fun provideIslandBackupManager(@ApplicationContext context: Context): IslandBackupManager {
        return IslandBackupManager(context)
    }

    @Provides
    @Singleton
    fun provideIslandLocationManager(@ApplicationContext context: Context): IslandLocationManager {
        return IslandLocationManager(context)
    }
}
