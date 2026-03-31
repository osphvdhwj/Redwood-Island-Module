package com.example.dynamicisland

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalLiquidSlider(
    value: Float, // 0f to 100f
    iconRes: Int,
    activeColor: Color,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 0.94f else 1f, 
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "pressScale"
    )

    val animatedFill by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "fill"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(52.dp) // Sleek, slightly narrower
            .scale(pressScale)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141414)) // Deep void
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isDragging = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    
                    val sliderHeight = size.height.toFloat()
                    var touchY = down.position.y
                    
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
        // Liquid Fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFill)
                .background(activeColor)
        )
        
        // Smart Overlay: Crossfades Icon <-> Number
        Box(modifier = Modifier.padding(bottom = 12.dp)) {
            Crossfade(targetState = isDragging, animationSpec = tween(150), label = "slider_overlay") { dragging ->
                if (dragging) {
                    Text(
                        text = "${value.toInt()}", 
                        color = if (animatedFill > 0.15f) Color.Black else Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (animatedFill > 0.15f) Color.Black else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
