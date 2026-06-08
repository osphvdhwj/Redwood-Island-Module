package com.example.dynamicisland.core.ui.animations
import com.example.dynamicisland.core.model.IslandUiState

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.PhysicsStyle

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
    val rotation: Float,
    val glowIntensity: Float
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

    // 💎 LIQUID PHYSICS: High-Grade Fluid Motion
    // Apple uses a custom spring with high stiffness (around 1000) and very specific damping (0.8 - 0.9)
    val damping = when (physicsStyle) {
        PhysicsStyle.APPLE -> 0.85f
        PhysicsStyle.OXYGEN_OS -> 0.65f
        else -> 0.75f
    }
    val stiffness = when (physicsStyle) {
        PhysicsStyle.APPLE -> 1200f
        PhysicsStyle.OXYGEN_OS -> 800f
        else -> 450f
    }

    val springSpecDp = tween<Dp>(durationMillis = 200)
    val springSpecFloat = tween<Float>(durationMillis = 200)
    val springSpecColor = tween<Color>(durationMillis = 200)

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
            if (targetState == IslandUiState.MAX_PILL) {
                // 🌪️ Subtle tilt on expansion
                keyframes { durationMillis = 400; 0f at 0; 1.5f at 150; 0f at 400 }
            } else {
                tween(durationMillis = 200)
            }
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
        if (state == IslandUiState.NOTIFICATION_RING && isCyberpunk) Color(0xFF00FFFF).copy(alpha=0.6f) else Color.White.copy(alpha=0.08f)
    }

    val alpha by transition.animateFloat(transitionSpec = { tween(200, easing = LinearOutSlowInEasing) }, label = "alpha") { state ->
        if (state == IslandUiState.HIDDEN) 0f else 1f
    }

    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "scale"
    ) { state ->
        if (state == IslandUiState.HIDDEN) 0.85f else 1f
    }
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (targetState == IslandUiState.NOTIFICATION_RING) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = SineOverShoot), RepeatMode.Reverse),
        label = "glow"
    )

    return IslandAnimationValues(width, height, cornerRadius, xOffset, borderColor, alpha, scale, rotation, glowIntensity)
}

private val SineOverShoot = Easing { x -> 
    val c4 = (2 * Math.PI) / 3
    if (x == 0f) 0f else if (x == 1f) 1f else Math.pow(2.0, -10 * x.toDouble()).toFloat() * Math.sin((x * 10 - 0.75) * c4).toFloat() + 1f
}
