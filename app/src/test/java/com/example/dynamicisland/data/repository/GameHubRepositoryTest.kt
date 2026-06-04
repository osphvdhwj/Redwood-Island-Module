package com.example.dynamicisland.data.repository

import android.app.ActivityManager
import android.content.Context
import com.example.dynamicisland.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.domain.state.IslandNeuralCore
import com.example.dynamicisland.util.shell.ShellExecutor
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameHubRepositoryTest {

    private val context: Context = mockk(relaxed = true)
    private val dispatchers: DispatcherProvider = mockk()
    private val neuralCore: IslandNeuralCore = mockk(relaxed = true)
    private val shell: ShellExecutor = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: GameHubRepository

    @BeforeEach
    fun setup() {
        every { dispatchers.io() } returns testDispatcher
        repository = GameHubRepository(context, dispatchers, neuralCore, shell)
    }

    @Test
    fun `boostMemory should execute kernel commands`() = runTest(testDispatcher) {
        // Arrange
        val am: ActivityManager = mockk(relaxed = true)
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns am

        // Act
        repository.boostMemory()

        // Assert
        verify { shell.executeRoot("echo 3 > /proc/sys/vm/drop_caches") }
        verify { shell.executeRoot("echo 1 > /proc/sys/vm/compact_memory") }
    }

    @Test
    fun `cleanJunk should execute filesystem cleaning commands`() = runTest(testDispatcher) {
        // Act
        repository.cleanJunk()

        // Assert
        verify { shell.executeRoot("rm -rf /data/data/*/cache/*") }
        verify { shell.executeRoot("rm -rf /data/log/*") }
    }
}
