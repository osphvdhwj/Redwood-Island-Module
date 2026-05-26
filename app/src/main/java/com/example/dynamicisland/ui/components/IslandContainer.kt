package com.example.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.ui.mvi.IslandUiState

/**
 * The base container for the Dynamic Island.
 * Handles the shape, size, and animation transitions between different states.
 */
@Composable
fun IslandContainer(
    state: IslandUiState,
    content: @Composable BoxScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    
    // Physics tokens from IslandTheme
    val springDamping = state.theme.springDamping
    val springStiffness = state.theme.springStiffness
    
    val dpPhysicsSpec = spring<Dp>(dampingRatio = springDamping, stiffness = springStiffness)
    
    // Determine target dimensions based on state
    val targetWidth = when (state.islandState) {
        IslandState.TYPE_0_RING -> 45f
        IslandState.TYPE_1_MINI -> 180f
        IslandState.TYPE_2_MID -> 320f
        IslandState.TYPE_3_MAX -> 360f
        IslandState.TYPE_SPLIT -> 180f
        IslandState.TYPE_CUBE -> 85f
        else -> 45f
    }.coerceIn(state.displayCutoutWidth + 4f, screenWidthDp - 24f)

    val targetHeight = when (state.islandState) {
        IslandState.TYPE_0_RING -> 45f
        IslandState.TYPE_1_MINI -> 36f
        IslandState.TYPE_2_MID -> 80f
        IslandState.TYPE_3_MAX -> 220f
        IslandState.TYPE_SPLIT -> 36f
        IslandState.TYPE_CUBE -> 85f
        else -> 45f
    }

    val targetRadius = (when (state.islandState) {
        IslandState.TYPE_3_MAX -> 42.dp
        IslandState.TYPE_2_MID -> 16.dp
        IslandState.TYPE_CUBE -> 24.dp
        else -> (targetHeight / 2).dp
    }).coerceAtLeast(0.dp)

    val animatedWidth by animateDpAsState(targetWidth.dp, dpPhysicsSpec, label = "width")
    val animatedHeight by animateDpAsState(targetHeight.dp, dpPhysicsSpec, label = "height")
    val animatedRadius by animateDpAsState(targetRadius, dpPhysicsSpec, label = "radius")

    val isHidden = state.islandState == IslandState.HIDDEN
    val alpha by animateFloatAsState(if (isHidden) 0f else 1f, tween(300), label = "alpha")

    // Pillar 5: Strict Lifecycle Scopes. If screen is off, do not render to save battery.
    if (!isHidden && state.isScreenOn) {
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .shadow(16.dp, RoundedCornerShape(animatedRadius), spotColor = Color.Black)
                .clip(RoundedCornerShape(animatedRadius))
                .background(Color.Black.copy(alpha = 0.9f))
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(animatedRadius)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
