package com.example.dynamicisland.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun BottomAuraPanel() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraPanel")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .graphicsLayer { translationY = 60.dp.toPx() }, // Half-buried
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(1.2f)
                .height(180.dp)
                .blur(60.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00FBFF).copy(alpha = 0.6f), // Cyan
                            Color(0xFF8D27FF).copy(alpha = 0.6f), // Purple
                            Color(0xFFFF00D0).copy(alpha = 0.6f), // Pink
                            Color(0xFF00FBFF).copy(alpha = 0.6f)
                        )
                    )
                )
        )
    }
}
