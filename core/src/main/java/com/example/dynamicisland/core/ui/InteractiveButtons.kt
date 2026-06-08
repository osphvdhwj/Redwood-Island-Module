package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.delay

@Composable
fun InteractiveIconButton(logicalIcon: IconProvider.LogicalIcon, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    val icon = IconProvider.getIcon(logicalIcon, LocalIconPack.current)
    InteractiveIconButtonContent(icon = icon, painter = null, tint = tint, baseSize = baseSize, bgAlpha = bgAlpha, onClick = onClick)
}

@Composable
fun InteractiveIconButton(icon: ImageVector, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    InteractiveIconButtonContent(icon = icon, painter = null, tint = tint, baseSize = baseSize, bgAlpha = bgAlpha, onClick = onClick)
}

@Composable
fun InteractiveIconButton(painter: Painter, tint: Color, baseSize: Dp, bgAlpha: Float = 0f, onClick: () -> Unit) {
    InteractiveIconButtonContent(icon = null, painter = painter, tint = tint, baseSize = baseSize, bgAlpha = bgAlpha, onClick = onClick)
}

@Composable
private fun InteractiveIconButtonContent(
    icon: ImageVector?,
    painter: Painter?,
    tint: Color,
    baseSize: Dp,
    bgAlpha: Float = 0f,
    onClick: () -> Unit
) {
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
    
    val showCheck = isClicked && theme.actionAnimType == "CHECKMARK"
    val currentTint = if (showCheck) Color.Green else tint

    Box(
        modifier = Modifier
            .size(baseSize)
            .clip(RoundedCornerShape(theme.buttonCornerRadius))
            .background(currentTint.copy(alpha = bgAlpha))
            .squishClickable(haptic = HapticFeedbackType.TextHandleMove) { 
                isClicked = true
                onClick() 
            },
        contentAlignment = Alignment.Center
    ) {
        if (showCheck) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = currentTint.copy(alpha = alpha),
                modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = currentTint.copy(alpha = alpha),
                modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = currentTint.copy(alpha = alpha),
                modifier = Modifier.size(baseSize * 0.55f).graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}
