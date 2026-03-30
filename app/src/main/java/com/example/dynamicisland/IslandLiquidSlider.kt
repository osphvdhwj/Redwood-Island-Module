package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun LiquidSlider(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    progress: Float,
    activeColor: Color,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    // Physics: Slider dynamically expands when grabbed
    val height by animateDpAsState(targetValue = if (isDragging) 48.dp else 36.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "height")
    val corner by animateDpAsState(targetValue = if (isDragging) 24.dp else 18.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "corner")

    // Haptics Engine: Fire a tick every 5% change
    var lastHapticTick by remember { mutableIntStateOf((progress * 20).toInt()) }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart() 
                    },
                    onDragEnd = { 
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDragEnd() 
                    },
                    onDragCancel = { 
                        isDragging = false
                        onDragEnd() 
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Calculate precise delta based on the physical width of the slider
                    val delta = dragAmount.x / size.width.toFloat()
                    
                    // Rubberband Resistance Math
                    val resistedDelta = if (progress <= 0f && delta < 0) delta * 0.2f 
                                        else if (progress >= 1f && delta > 0) delta * 0.2f 
                                        else delta
                                        
                    onDrag(resistedDelta)

                    // Multi-Frequency Haptics
                    val currentTick = (progress * 20).toInt()
                    if (currentTick != lastHapticTick) {
                        if (currentTick == 0 || currentTick == 20) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Heavy boundary pulse
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Light travel tick
                        }
                        lastHapticTick = currentTick
                    }
                }
            }
    ) {
        // The Liquid Fill
        // 🚀 120FPS OPTIMIZATION: Use Modifier.fillMaxWidth(fraction) inside a derived context
        // OR better yet, animate the width using graphicsLayer so layout is skipped.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth() // Take up 100% of the parent layout width
                .graphicsLayer {
                    // We change the scaleX instead of the layout width!
                    // This skips Phase 1 (Composition) and Phase 2 (Layout) entirely.
                    scaleX = progress.coerceIn(0.02f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) // Scale from the left edge
                }
                .background(activeColor)
        )
        
        // The Icon
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (progress > 0.15f) Color.Black else Color.White, 
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(20.dp)
        )
    }
}
