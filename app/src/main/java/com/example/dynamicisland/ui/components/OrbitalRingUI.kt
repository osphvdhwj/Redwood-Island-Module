package com.example.dynamicisland.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Orbital Ring UI
 * 
 * A reactive ring with rotating particles.
 * Speed reacts to hardware stats (CPU/RAM load).
 */
@Composable
fun OrbitalRingUI(color: Color, speedFactor: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    
    // Base duration is 3000ms, speedFactor increases speed (decreases duration)
    val duration = (3000 / speedFactor.coerceAtLeast(0.1f)).toInt().coerceIn(500, 10000)
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension / 2 - 8.dp.toPx()

        // Draw main ring track
        drawCircle(
            color = color.copy(alpha = 0.15f),
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        // Draw orbital particles
        val particleCount = 3
        for (i in 0 until particleCount) {
            val angle = Math.toRadians((rotation + i * (360 / particleCount)).toDouble())
            val px = centerX + radius * cos(angle).toFloat()
            val py = centerY + radius * sin(angle).toFloat()
            
            // Particle core
            drawCircle(
                color = color,
                radius = 3.5.dp.toPx(),
                center = Offset(px, py)
            )
            
            // Particle glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(px, py),
                    radius = 10.dp.toPx()
                ),
                radius = 10.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}
