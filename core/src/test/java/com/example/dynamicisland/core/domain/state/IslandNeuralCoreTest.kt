package com.example.dynamicisland.core.domain.state

import android.content.Context
import io.mockk.*
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import com.example.dynamicisland.shared.model.*
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
