package com.example.dynamicisland.core.ui.states

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.ui.components.PillSurface
import com.example.dynamicisland.shared.ipc.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
@Composable
fun RingPill(
    modifier: Modifier = Modifier,
    hasActiveBackgroundEvent: Boolean = false,
    accentColor: Color = com.example.dynamicisland.core.ui.design.IslandColors.accentCyan
) {
    // Subtle breathing animation for the ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring_breathe")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (hasActiveBackgroundEvent) 0.6f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_alpha"
    )
    PillSurface(
        modifier = modifier.size(width = 100.dp, height = 32.dp), // Adjust to your physical cutout size
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        backgroundColor = Color.Black,
        interactive = true
    ) {
        // Draw the glowing ring border
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = accentColor.copy(alpha = ringAlpha),
                size = size,
                style = Stroke(width = 3.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2)
            )
        }
        // Feature 192: Ambient floating particles if an event is active
        if (hasActiveBackgroundEvent) {
            AmbientParticles(accentColor)
        }
    }
}
private fun AmbientParticles(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)),
        label = "particle_rotation"
    )
    val particles = remember { List(4) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val radiusX = size.width / 2 + 10f
        val radiusY = size.height / 2 + 10f
        particles.forEachIndexed { index, seed ->
            val angle = Math.toRadians((rotation + (index * 90) + (seed.x * 30)).toDouble())
            val x = center.x + (radiusX * cos(angle)).toFloat()
            val y = center.y + (radiusY * sin(angle)).toFloat()
            
            drawCircle(
                color = color.copy(alpha = 0.5f * seed.y),
                radius = 2f + (seed.x * 2f),
                center = Offset(x, y)
