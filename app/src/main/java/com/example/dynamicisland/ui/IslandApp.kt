package com.example.dynamicisland.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.ui.components.IslandContainer
import com.example.dynamicisland.ui.state.IslandIntent
import com.example.dynamicisland.ui.state.IslandViewModel

/**
 * Pillar 3: The Root Compose Layout.
 * Strictly observes the StateFlow from IslandViewModel and renders the appropriate UI components.
 */
@Composable
fun IslandApp(viewModel: IslandViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Position the island at the top center
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        IslandContainer(state = uiState) {
            AnimatedContent(
                targetState = uiState.islandState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "IslandContent"
            ) { state ->
                when (state) {
                    IslandState.TYPE_1_MINI -> MiniPill(uiState.activeModel)
                    IslandState.TYPE_2_MID -> MidPill(uiState.activeModel)
                    IslandState.TYPE_3_MAX -> MaxPill(uiState.activeModel)
                    // Add other states as components are implemented
                    else -> Box(modifier = Modifier.size(0.dp))
                }
            }
        }
    }
}

@Composable
fun MiniPill(model: LiveActivityModel?) {
    // Implement Mini view based on model
    // This will eventually call specialized components like MiniMusicView
}

@Composable
fun MidPill(model: LiveActivityModel?) {
    // Implement Mid view based on model
}

@Composable
fun MaxPill(model: LiveActivityModel?) {
    // Implement Max view based on model
}
