package com.example.dynamicisland.ui.animations

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.settings.IconPack

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
    val scale: Float
)

@Composable
fun updateIslandTransition(
    targetState: IslandUiState,
    isCyberpunk: Boolean,
    miniWidth: Float = 180f,
    miniHeight: Float = 36f,
    maxWidth: Float = 360f,
    maxHeight: Float = 220f,
    ringWidth: Float = 45f,
    ringHeight: Float = 45f,
    // Per-state corner radius
    miniRadius: Float = 18f,
    maxRadius: Float = 42f,
    ringRadius: Float = 22.5f
): IslandAnimationValues {
    val transition = updateTransition(targetState = targetState, label = "IslandTransition")

    val springSpecDp = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val springSpecFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val springSpecColor = spring<Color>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val width by transition.animateDp(
        transitionSpec = { springSpecDp },
        label = "width"
    ) { state ->
        when (state) {
            IslandUiState.COMPACT -> miniWidth.dp
            IslandUiState.MAX_PILL -> maxWidth.dp
            IslandUiState.SPLIT_PILL -> (miniWidth * 0.7f).dp
            IslandUiState.NOTIFICATION_RING -> ringWidth.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val height by transition.animateDp(
        transitionSpec = { springSpecDp },
        label = "height"
    ) { state ->
        when (state) {
            IslandUiState.COMPACT -> miniHeight.dp
            IslandUiState.MAX_PILL -> maxHeight.dp
            IslandUiState.SPLIT_PILL -> miniHeight.dp
            IslandUiState.NOTIFICATION_RING -> ringHeight.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val cornerRadius by transition.animateDp(
        transitionSpec = { springSpecDp },
        label = "cornerRadius"
    ) { state ->
        when (state) {
            IslandUiState.MAX_PILL -> maxRadius.dp
            IslandUiState.NOTIFICATION_RING -> ringRadius.dp
            IslandUiState.COMPACT, IslandUiState.SPLIT_PILL -> miniRadius.dp
            IslandUiState.HIDDEN -> 0.dp
        }
    }

    val xOffset by transition.animateFloat(
        transitionSpec = { springSpecFloat },
        label = "xOffset"
    ) { state ->
        when (state) {
            IslandUiState.SPLIT_PILL -> 24f // Push away from cutout in Split Pill
            else -> 0f
        }
    }

    val borderColor by transition.animateColor(
        transitionSpec = { springSpecColor },
        label = "borderColor"
    ) { state ->
        if (state == IslandUiState.NOTIFICATION_RING && isCyberpunk) {
            Color(0xFF00FFFF) // Neon Cyan
        } else {
            Color.Transparent
        }
    }

    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) },
        label = "alpha"
    ) { state ->
        if (state == IslandUiState.HIDDEN) 0f else 1f
    }

    val scale by transition.animateFloat(
        transitionSpec = { 
            if (targetState == IslandUiState.HIDDEN) {
                tween(durationMillis = 350, easing = FastOutLinearInEasing)
            } else {
                springSpecFloat
            }
        },
        label = "scale"
    ) { state ->
        if (state == IslandUiState.HIDDEN) 0f else 1f
    }

    return IslandAnimationValues(
        width = width,
        height = height,
        cornerRadius = cornerRadius,
        xOffset = xOffset,
        borderColor = borderColor,
        alpha = alpha,
        scale = scale
    )
}
