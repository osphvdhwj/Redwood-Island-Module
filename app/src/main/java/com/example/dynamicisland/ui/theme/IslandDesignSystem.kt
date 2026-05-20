package com.example.dynamicisland.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class IslandColors(
    val background: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF121212),
    val border: Color = Color.White.copy(alpha = 0.1f),
    val textPrimary: Color = Color.White,
    val textSecondary: Color = Color.White.copy(alpha = 0.6f),
    val accentDefault: Color = Color(0xFF4285F4),
    val success: Color = Color(0xFF34A853),
    val warning: Color = Color(0xFFFBBC05),
    val error: Color = Color(0xFFEA4335)
)

@Immutable
data class IslandShapes(
    val ring: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val mini: RoundedCornerShape = RoundedCornerShape(percent = 50),
    val mid: RoundedCornerShape = RoundedCornerShape(24.dp),
    val max: RoundedCornerShape = RoundedCornerShape(42.dp)
)

val LocalIslandColors = staticCompositionLocalOf { IslandColors() }
val LocalIslandShapes = staticCompositionLocalOf { IslandShapes() }

object IslandTheme {
    val colors: IslandColors
        @Composable get() = LocalIslandColors.current
    val shapes: IslandShapes
        @Composable get() = LocalIslandShapes.current
    
    // Quick typography access
    val typography = Typography(
        titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = Color.White),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
    )
}

@Composable
fun RedwoodIslandTheme(
    dynamicColor: Color? = null,
    isGlassmorphism: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseAlpha = if (isGlassmorphism) 0.65f else 1.0f
    val currentColors = IslandColors(
        background = dynamicColor?.copy(alpha = baseAlpha) ?: Color.Black.copy(alpha = baseAlpha)
    )

    CompositionLocalProvider(
        LocalIslandColors provides currentColors,
        LocalIslandShapes provides IslandShapes()
    ) {
        content()
    }
}