package com.example.dynamicisland.core.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.PhysicsStyle
import com.example.dynamicisland.shared.settings.ContentTransitionStyle
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandIntent
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.IslandViewModel
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*

/**
 * Pillar 3: The Root Compose Layout.
 * Strictly observes the StateFlow from IslandViewModel and renders the appropriate UI components.
 */
@Composable
fun DynamicIslandView.IslandApp(viewModel: IslandViewModel) {
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
                PillRouter(
                    state = state,
                    model = uiState.activeModel,
                    onIntent = { viewModel.handleIntent(it) }
                )
            }
        }
    }
}

// Removed legacy stubs in favor of PillRouter
