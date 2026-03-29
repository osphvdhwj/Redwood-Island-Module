package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay

@Composable
fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    val theme = LocalIslandTheme.current
    var isClicked by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isClicked) {
        if (isClicked) {
            delay(if(theme.actionAnimType == "CHECKMARK") 1000 else 300)
            isClicked = false
        }
    }

    val scale by animateFloatAsState(if (isClicked && theme.actionAnimType == "BOUNCE") 1.3f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label="scale")
    val alpha by animateFloatAsState(if (isClicked && theme.actionAnimType == "PULSE") 0.3f else 1f, tween(150), label="alpha")
    val currentIcon = if (isClicked && theme.actionAnimType == "CHECKMARK") Icons.Default.Check else icon
    val currentTint = if (isClicked && theme.actionAnimType == "CHECKMARK") Color.Green else tint

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(RoundedCornerShape(theme.buttonCornerRadius))
            .background(currentTint.copy(alpha = bgAlpha))
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                isClicked = true
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = currentIcon,
            contentDescription = null,
            tint = currentTint.copy(alpha = alpha),
            modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
        )
    }
}
