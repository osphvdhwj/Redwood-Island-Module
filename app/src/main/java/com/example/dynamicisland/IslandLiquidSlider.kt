package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun VerticalLiquidSlider(
    value: Float, // 0f to 100f
    iconRes: Int,
    activeColor: Color,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    // Scale down the whole slider slightly when pressed (Physical squish effect)
    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 0.92f else 1f, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    // Smooth fill animation
    val animatedFill by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
        label = "fill"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(64.dp) // Thick, chunky iOS style
            .scale(pressScale)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A)) // Dark void background
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    
                    val sliderHeight = size.height.toFloat()
                    var touchY = down.position.y
                    
                    // Invert Y: 0 is top (100%), height is bottom (0%)
                    val newPct = (1f - (touchY / sliderHeight)).coerceIn(0f, 1f)
                    onValueChange(newPct * 100f)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            touchY = change.position.y
                            val dragPct = (1f - (touchY / sliderHeight)).coerceIn(0f, 1f)
                            onValueChange(dragPct * 100f)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    isDragging = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // The expanding liquid fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFill)
                .background(activeColor)
        )
        
        // The hardware icon (always at the bottom)
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (animatedFill > 0.15f) Color.Black else Color.Gray,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(24.dp)
        )
    }
}
