package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@Composable
fun IslandLiquidSlider(
    value: Float, // 0f to 100f
    onValueChange: (Float) -> Unit,
    activeColor: Color
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var touchX by remember { mutableFloatStateOf(0f) }
    var sliderWidth by remember { mutableFloatStateOf(1f) }

    // Spring physics for the bulge impact
    val bulgeScale by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "bulge"
    )

    // Smoothly animate the fill level so it doesn't jump instantly
    val animatedValue by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f),
        label = "fill"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Thick, iOS 18 style height
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    touchX = down.position.x
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    
                    val newPct = (touchX / sliderWidth).coerceIn(0f, 1f)
                    onValueChange(newPct * 100f)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            touchX = change.position.x
                            val dragPct = (touchX / sliderWidth).coerceIn(0f, 1f)
                            onValueChange(dragPct * 100f)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    isDragging = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    ) {
        sliderWidth = size.width
        val cornerRad = size.height / 2f
        val fillWidth = animatedValue * sliderWidth

        // 1. Draw the Void Background (Dark Gray Track)
        drawRoundRect(
            color = Color(0xFF1A1A1A),
            size = size,
            cornerRadius = CornerRadius(cornerRad, cornerRad)
        )

        // 2. Build the Liquid Fill Path
        val fillPath = Path().apply {
            // Standard rounded rectangle for the base fill
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, fillWidth, size.height),
                    topLeft = CornerRadius(cornerRad, cornerRad),
                    bottomLeft = CornerRadius(cornerRad, cornerRad),
                    topRight = CornerRadius(if (animatedValue > 0.95f) cornerRad else 0f),
                    bottomRight = CornerRadius(if (animatedValue > 0.95f) cornerRad else 0f)
                )
            )

            // The Physical Bulge Math
            if (bulgeScale > 0.01f) {
                val bulgeCenter = touchX.coerceIn(cornerRad, sliderWidth - cornerRad)
                val bulgeWidth = 120f // How wide the distortion spreads
                val bulgeHeight = 12f * bulgeScale // How high it pushes up and down
                
                // Add an overlapping cubic bezier curve to simulate liquid surface tension
                moveTo(bulgeCenter - bulgeWidth, 0f)
                cubicTo(
                    bulgeCenter - (bulgeWidth / 3), 0f,
                    bulgeCenter - (bulgeWidth / 3), -bulgeHeight,
                    bulgeCenter, -bulgeHeight
                )
                cubicTo(
                    bulgeCenter + (bulgeWidth / 3), -bulgeHeight,
                    bulgeCenter + (bulgeWidth / 3), 0f,
                    bulgeCenter + bulgeWidth, 0f
                )
                
                // Bottom bulge
                moveTo(bulgeCenter - bulgeWidth, size.height)
                cubicTo(
                    bulgeCenter - (bulgeWidth / 3), size.height,
                    bulgeCenter - (bulgeWidth / 3), size.height + bulgeHeight,
                    bulgeCenter, size.height + bulgeHeight
                )
                cubicTo(
                    bulgeCenter + (bulgeWidth / 3), size.height + bulgeHeight,
                    bulgeCenter + (bulgeWidth / 3), size.height,
                    bulgeCenter + bulgeWidth, size.height
                )
            }
        }

        // 3. Clip the bulge to the main pill bounds so it doesn't break the container shape,
        // unless you want the bulge to actually break outside the bounds (which looks awesome).
        // For a clean iOS look, we clip it inside the main rounded rect bounds.
        clipPath(Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, size.width, size.height), CornerRadius(cornerRad, cornerRad))) }) {
            drawPath(path = fillPath, color = activeColor)
        }
    }
}
