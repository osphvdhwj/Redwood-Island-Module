package com.example.dynamicisland.core.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * GeminiAuraModifier
 * 
 * Applies a rotating sweep gradient border mimicking the Gemini AI / Siri aura.
 * Highly reactive and aesthetic.
 */
fun Modifier.geminiAura(
    enabled: Boolean,
    thickness: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(100f)
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "AuraRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "angle"
    )

    this.border(
        width = thickness,
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF00FBFF), // Cyan
                Color(0xFF8D27FF), // Purple
                Color(0xFFFF00D0), // Pink
                Color(0xFF00FBFF)  // Cycle back
            )
        ),
        shape = shape
    )
}
