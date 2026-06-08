package com.example.dynamicisland.core.domain.state

import android.content.Context
import io.mockk.*
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IslandNeuralCoreTest {

    private lateinit var neuralCore: IslandNeuralCore
    private val context: Context = mockk(relaxed = true)
    private val filesDir = File("/tmp")

    @BeforeEach
    fun setup() {
        every { context.filesDir } returns filesDir
        neuralCore = IslandNeuralCore(context)
    }

    @Test
    fun `dispatch SyncState should update uiState correctly`() = runTest {
        // Arrange
        val testState = IslandState.TYPE_3_MAX
        val intent = IslandIntent.SyncState(testState, null, null)

        // Act
        neuralCore.dispatch(intent)

        // Assert
        assertEquals(testState, neuralCore.uiState.value.islandState)
    }

    @Test
    fun `reduce logic should handle BatteryPulse intent`() = runTest {
        // Arrange
        val intent = IslandIntent.BatteryPulse(85)

        // Act
        neuralCore.dispatch(intent)

        // Assert
        assertEquals(85, neuralCore.uiState.value.batteryLevel)
    }
}
