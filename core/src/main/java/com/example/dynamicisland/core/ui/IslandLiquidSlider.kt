package com.example.dynamicisland.core.ui

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
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

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
        targetValue = if (isDragging) 0.95f else 1f, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    val animatedFill by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "fill"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(46.dp) // Sleeker, narrower profile
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp)) // Tighter Apple-style squircle
            .background(Color(0xFF1C1C1E)) // Premium dark mode surface
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
        // The liquid rising fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFill.coerceAtLeast(0.01f))
                .background(activeColor)
        )
        
        // Icon / Number Crossfade
        Box(modifier = Modifier.padding(bottom = 12.dp)) {
            Crossfade(targetState = isDragging, animationSpec = tween(150), label = "overlay") { dragging ->
                if (dragging) {
                    Text(
                        text = "${value.toInt()}", 
                        color = if (animatedFill > 0.2f) Color.Black else Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (animatedFill > 0.2f) Color.Black else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
