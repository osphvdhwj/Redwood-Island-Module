package com.example.dynamicisland.ui.animations

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.settings.PhysicsStyle

enum class IslandUiState {
    COMPACT,
    MAX_PILL,
    SPLIT_PILL,
    NOTIFICATION_RING,
    HIDDEN
}

data class IslandAnimationValues(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp,
    val xOffset: Float,
    val borderColor: Color,
    val alpha: Float,
    val scale: Float,
    val rotation: Float
)

@Composable
fun updateIslandTransition(
    targetState: IslandUiState,
    isCyberpunk: Boolean,
    physicsStyle: PhysicsStyle = PhysicsStyle.APPLE,
    miniWidth: Float = 180f,
    miniHeight: Float = 36f,
    maxWidth: Float = 360f,
    maxHeight: Float = 220f,
    ringWidth: Float = 45f,
    ringHeight: Float = 45f,
    miniRadius: Float = 18f,
    maxRadius: Float = 42f,
    ringRadius: Float = 22.5f
): IslandAnimationValues {
    val transition = updateTransition(targetState = targetState, label = "IslandTransition")

    // 💎 PRO-GRADE PHYSICS: High stiffness, Low damping for "Snap & Bounciness"
    val damping = if (physicsStyle == PhysicsStyle.OXYGEN_OS) 0.55f else 0.75f
    val stiffness = if (physicsStyle == PhysicsStyle.OXYGEN_OS) 800f else 450f

    val springSpecDp = spring<Dp>(dampingRatio = damping, stiffness = stiffness)
    val springSpecFloat = spring<Float>(dampingRatio = damping, stiffness = stiffness)
    val springSpecColor = spring<Color>(dampingRatio = damping, stiffness = stiffness)

    val width by transition.animateDp(transitionSpec = { springSpecDp }, label = "width") { state ->
        when (state) {
            IslandUiState.COMPACT -> miniWidth.dp
            IslandUiState.MAX_PILL -> maxWidth.dp
            IslandUiState.SPLIT_PILL -> (miniWidth * 0.7f).dp
            IslandUiState.NOTIFICATION_RING -> ringWidth.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val height by transition.animateDp(transitionSpec = { springSpecDp }, label = "height") { state ->
        when (state) {
            IslandUiState.COMPACT -> miniHeight.dp
            IslandUiState.MAX_PILL -> maxHeight.dp
            IslandUiState.SPLIT_PILL -> miniHeight.dp
            IslandUiState.NOTIFICATION_RING -> ringHeight.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val cornerRadius by transition.animateDp(transitionSpec = { springSpecDp }, label = "cornerRadius") { state ->
        when (state) {
            IslandUiState.MAX_PILL -> maxRadius.dp
            IslandUiState.NOTIFICATION_RING -> ringRadius.dp
            IslandUiState.COMPACT, IslandUiState.SPLIT_PILL -> miniRadius.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val rotation by transition.animateFloat(
        transitionSpec = { 
            // 🌀 WOBBLE EFFECT: Overshoot slightly on change
            keyframes { durationMillis = 400; 0f at 0; 2f at 100; -1f at 250; 0f at 400 }
        },
        label = "rotation"
    ) { 0f }

    val xOffset by transition.animateFloat(transitionSpec = { springSpecFloat }, label = "xOffset") { state ->
        when (state) {
            IslandUiState.SPLIT_PILL -> 24f
            else -> 0f
        }
    }

    val borderColor by transition.animateColor(transitionSpec = { springSpecColor }, label = "borderColor") { state ->
        if (state == IslandUiState.NOTIFICATION_RING && isCyberpunk) Color(0xFF00FFFF) else Color.Transparent
    }

    val alpha by transition.animateFloat(transitionSpec = { tween(300) }, label = "alpha") { state ->
        if (state == IslandUiState.HIDDEN) 0f else 1f
    }

    val scale by transition.animateFloat(
        transitionSpec = { if (targetState == IslandUiState.HIDDEN) tween(350) else springSpecFloat },
        label = "scale"
    ) { state ->
        if (state == IslandUiState.HIDDEN) 0f else 1f
    }

    return IslandAnimationValues(width, height, cornerRadius, xOffset, borderColor, alpha, scale, rotation)
}
