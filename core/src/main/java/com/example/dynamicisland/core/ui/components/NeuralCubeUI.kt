package com.example.dynamicisland.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.dynamicisland.shared.settings.SettingsState
import com.example.dynamicisland.shared.settings.AestheticStyle

/**
 * ✨ VYXEL EXPRESSIVE NEURAL CUBE
 * 
 * A 3D-like rotating cube using isometric projection.
 * Supports LiquidGlass transparency and expressive gradient surfaces.
 */
@Composable
fun NeuralCubeUI(color: Color, settings: SettingsState = SettingsState(), modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cube_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier.size(60.dp)) {
        drawNeuralCube(rotation, color, settings.aestheticStyle == AestheticStyle.LIQUID_GLASS)
    }
}

private fun DrawScope.drawNeuralCube(rotationDeg: Float, color: Color, isLiquidGlass: Boolean) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val cubeSize = size.minDimension / 3.5f
    val angle = Math.toRadians(rotationDeg.toDouble()).toFloat()

    // Isometric projection constants
    val isoAngle = PI.toFloat() / 6 // 30 degrees

    fun project(x: Float, y: Float, z: Float): Offset {
        // Simple rotation around Y axis
        val rx = x * cos(angle) + z * sin(angle)
        val rz = -x * sin(angle) + z * cos(angle)
        
        // Isometric projection
        val px = (rx - rz) * cos(isoAngle)
        val py = (rx + rz) * sin(isoAngle) - y
        return Offset(centerX + px * cubeSize, centerY + py * cubeSize)
    }

    val vertices = listOf(
        project(-1f, -1f, -1f), project(1f, -1f, -1f), project(1f, 1f, -1f), project(-1f, 1f, -1f),
        project(-1f, -1f, 1f), project(1f, -1f, 1f), project(1f, 1f, 1f), project(-1f, 1f, 1f)
    )

    val faces = listOf(
        listOf(0, 1, 2, 3), listOf(4, 5, 6, 7), // back, front
        listOf(0, 1, 5, 4), listOf(2, 3, 7, 6), // bottom, top
        listOf(0, 3, 7, 4), listOf(1, 2, 6, 5)  // left, right
    )

    // LiquidGlass uses a gradient brush instead of flat color
    val fillBrush = if (isLiquidGlass) {
        Brush.linearGradient(
            colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f)),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
    } else null

    // Depth sorting: draw faces with alpha
    faces.forEach { face ->
        val path = Path().apply {
            moveTo(vertices[face[0]].x, vertices[face[0]].y)
            lineTo(vertices[face[1]].x, vertices[face[1]].y)
            lineTo(vertices[face[2]].x, vertices[face[2]].y)
            lineTo(vertices[face[3]].x, vertices[face[3]].y)
            close()
        }
        
        if (isLiquidGlass && fillBrush != null) {
            drawPath(path, fillBrush)
            drawPath(path, color.copy(alpha = 0.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
        } else {
            drawPath(path, color.copy(alpha = 0.15f))
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
        }
    }
}
