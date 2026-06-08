package com.example.dynamicisland.core.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
/**
 * Organic Bottom Aura
 * Mimics the premium "fluid glow" of the new Gemini interface.
 */
@Composable
fun BottomAuraPanel() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraPanel")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val scaleX by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scaleX"
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "scaleY"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer { translationY = 80.dp.toPx() }, 
        contentAlignment = Alignment.BottomCenter
    ) {
        // Multi-layer gradient for depth
        Box(
            modifier = Modifier
                .fillMaxWidth(1.4f)
                .height(200.dp)
                .graphicsLayer { 
                    this.rotationZ = rotation
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                }
                .blur(80.dp)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF00FBFF).copy(alpha = 0.4f), // Cyan
                            Color(0xFF8D27FF).copy(alpha = 0.5f), // Purple
                            Color(0xFFFF00D0).copy(alpha = 0.4f), // Pink
                            Color(0xFF00FBFF).copy(alpha = 0.4f)
                        )
                    )
                )
        )
        
        // Inner intense glow core
                .fillMaxWidth(0.8f)
                .height(100.dp)
                    this.rotationZ = -rotation * 0.5f
                    this.scaleX = scaleY
                    this.scaleY = scaleX
                .blur(40.dp)
                    Brush.radialGradient(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent
    }
}
