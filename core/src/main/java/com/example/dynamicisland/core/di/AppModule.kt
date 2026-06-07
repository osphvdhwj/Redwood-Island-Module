package com.example.dynamicisland.core.di

import android.content.Context
import com.example.dynamicisland.core.data.repository.*
import com.example.dynamicisland.core.data.repository.cleanup.*
import com.example.dynamicisland.core.data.repository.profiles.*
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.dispatchers.StandardDispatcherProvider
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.gesture.*
import com.example.dynamicisland.core.intelligence.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.core.settings.SettingsManager
import com.example.dynamicisland.core.ipc.IslandIPCClient
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
        controller: dagger.Lazy<IslandController>
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
}
