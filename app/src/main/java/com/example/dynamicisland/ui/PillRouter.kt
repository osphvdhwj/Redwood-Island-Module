package com.example.dynamicisland.ui

import androidx.compose.runtime.Composable
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Routes a LiveActivityModel to the appropriate specialized Composable view
 * based on the current IslandState.
 */
@Composable
fun PillRouter(
    state: IslandState,
    model: LiveActivityModel?,
    onIntent: (com.example.dynamicisland.ui.state.IslandIntent) -> Unit
) {
    if (model == null) return

    when (state) {
        IslandState.TYPE_1_MINI -> MiniPillRouter(model, onIntent)
        IslandState.TYPE_2_MID -> MidPillRouter(model, onIntent)
        IslandState.TYPE_3_MAX -> MaxPillRouter(model, onIntent)
        else -> {}
    }
}

@Composable
fun MiniPillRouter(model: LiveActivityModel, onIntent: (com.example.dynamicisland.ui.state.IslandIntent) -> Unit) {
    // Standard mini-view routing
    when (model) {
        is LiveActivityModel.Music -> { /* MiniMusicView(model) */ }
        else -> { /* Default Mini view */ }
    }
}

@Composable
fun MidPillRouter(model: LiveActivityModel, onIntent: (com.example.dynamicisland.ui.state.IslandIntent) -> Unit) {
    when (model) {
        is LiveActivityModel.Music -> { /* MusicMid(model) */ }
        is LiveActivityModel.Call -> { /* CallMid(model) */ }
        else -> { /* Default Mid view */ }
    }
}

@Composable
fun MaxPillRouter(model: LiveActivityModel, onIntent: (com.example.dynamicisland.ui.state.IslandIntent) -> Unit) {
    // Routing for expanded MAX view
}
