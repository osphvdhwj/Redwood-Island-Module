package com.example.dynamicisland.core.manager

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.settings.PhysicsStyle

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
    targetState: IslandState,
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

    val springSpecDp = tween<Dp>(durationMillis = 200)
    val springSpecFloat = tween<Float>(durationMillis = 200)
    val springSpecColor = tween<Color>(durationMillis = 200)

    val width by transition.animateDp(transitionSpec = { springSpecDp }, label = "width") { state ->
        when (state) {
            IslandState.TYPE_1_MINI -> miniWidth.dp
            IslandState.TYPE_2_MID -> (miniWidth * 1.5f).dp
            IslandState.TYPE_3_MAX -> maxWidth.dp
            IslandState.TYPE_SPLIT -> (miniWidth * 0.7f).dp
            IslandState.TYPE_0_RING, IslandState.TYPE_ORBITAL -> ringWidth.dp
            IslandState.HIDDEN -> 0.dp
            else -> miniWidth.dp
        }
    }

    val height by transition.animateDp(transitionSpec = { springSpecDp }, label = "height") { state ->
        when (state) {
            IslandState.TYPE_1_MINI -> miniHeight.dp
            IslandState.TYPE_2_MID -> (miniHeight * 1.2f).dp
            IslandState.TYPE_3_MAX -> maxHeight.dp
            IslandState.TYPE_SPLIT -> miniHeight.dp
            IslandState.TYPE_0_RING, IslandState.TYPE_ORBITAL -> ringHeight.dp
            IslandState.HIDDEN -> 0.dp
            else -> miniHeight.dp
        }
    }

    val cornerRadius by transition.animateDp(transitionSpec = { springSpecDp }, label = "cornerRadius") { state ->
        when (state) {
            IslandState.TYPE_3_MAX -> maxRadius.dp
            IslandState.TYPE_0_RING, IslandState.TYPE_ORBITAL -> ringRadius.dp
            IslandState.TYPE_1_MINI, IslandState.TYPE_2_MID, IslandState.TYPE_SPLIT -> miniRadius.dp
            IslandState.HIDDEN -> 0.dp
            else -> miniRadius.dp
        }
    }

    val rotation by transition.animateFloat(
        transitionSpec = { 
            if (targetState == IslandState.TYPE_3_MAX) {
                keyframes { durationMillis = 400; 0f at 0; 1.5f at 150; 0f at 400 }
            } else {
                tween(durationMillis = 200)
            }
        },
        label = "rotation"
    ) { 0f }

    val xOffset by transition.animateFloat(transitionSpec = { springSpecFloat }, label = "xOffset") { state ->
        when (state) {
            IslandState.TYPE_SPLIT -> 24f
            else -> 0f
        }
    }

    val borderColor by transition.animateColor(transitionSpec = { springSpecColor }, label = "borderColor") { state ->
        if ((state == IslandState.TYPE_0_RING || state == IslandState.TYPE_ORBITAL) && isCyberpunk) Color(0xFF00FFFF).copy(alpha=0.6f) else Color.White.copy(alpha=0.08f)
    }

    val alpha by transition.animateFloat(transitionSpec = { tween(200, easing = LinearOutSlowInEasing) }, label = "alpha") { state ->
        if (state == IslandState.HIDDEN) 0f else 1f
    }

    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 200) },
        label = "scale"
    ) { state ->
        if (state == IslandState.HIDDEN) 0.85f else 1f
    }
    
    val glowIntensity by animateFloatAsState(
        targetValue = if (targetState == IslandState.TYPE_0_RING || targetState == IslandState.TYPE_ORBITAL) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = SineOverShoot), RepeatMode.Reverse),
        label = "glow"
    )

    return IslandAnimationValues(width, height, cornerRadius, xOffset, borderColor, alpha, scale, rotation, glowIntensity)
}

private val SineOverShoot = Easing { x -> 
    val c4 = (2 * Math.PI) / 3
    if (x == 0f) 0f else if (x == 1f) 1f else Math.pow(2.0, -10 * x.toDouble()).toFloat() * Math.sin((x * 10 - 0.75) * c4).toFloat() + 1f
}
