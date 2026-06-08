package com.example.dynamicisland.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * The core physical container for all Island states.
 * Handles background blur, dynamic borders, shadows, and the "Squish" touch physics.
 */
@Composable
fun PillSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    backgroundColor: Color = IslandColors.background,
    borderColor: Color = IslandColors.border,
    shadowElevation: Dp = 16.dp,
    interactive: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    var isSquished by remember { mutableStateOf(false) }
    val touchScale by animateFloatAsState(
        targetValue = if (isSquished) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pill_squish"
    )

    Box(
        modifier = modifier
            .graphicsLayer { 
                scaleX = touchScale
                scaleY = touchScale
            }
            .shadow(
                elevation = if (isSquished) 4.dp else shadowElevation, 
                shape = shape, 
                spotColor = Color.Black
            )
            .clip(shape)
            .background(backgroundColor)
            .border(0.5.dp, borderColor, shape)
            .then(
                if (interactive) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Initial)
                            isSquished = true
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                            isSquished = false
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
