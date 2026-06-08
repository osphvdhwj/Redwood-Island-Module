package com.example.dynamicisland.core.ui.design

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * Unified Design System for Redwood Dynamic Island.
 * Features deep blacks, neon accents, and premium glassmorphism.
 */
@Immutable
data class RedwoodColors(
    val background: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF0D0D0D),
    val surfaceVariant: Color = Color(0xFF111111),
    val border: Color = Color(0x14FFFFFF),
    val accentCyan: Color = Color(0xFF00E5FF),
    val accentPurple: Color = Color(0xFF7C4DFF),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xB3FFFFFF), // Increased from 0x8C
    val error: Color = Color(0xFFFF4D4D),
    val success: Color = Color(0xFF00E676)
)

val LocalRedwoodColors = staticCompositionLocalOf { RedwoodColors() }

object IslandColors {
    val background = Color(0xFF000000)
    val surface = Color(0xFF0D0D0D)
    val surfaceVariant = Color(0xFF111111)
    val border = Color(0x14FFFFFF)
    val accentCyan = Color(0xFF00E5FF)
    val accentPurple = Color(0xFF7C4DFF)
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xB3FFFFFF) // Increased from 0x8C
}

object RedwoodDesignSystem {
    val colors: RedwoodColors
        @Composable get() = LocalRedwoodColors.current

    val typography = Typography(
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = (-0.5).sp
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    )
}

@Composable
fun RedwoodTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRedwoodColors provides RedwoodColors()
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                background = IslandColors.background,
                surface = IslandColors.surface,
                primary = IslandColors.accentCyan
            ),
            typography = RedwoodDesignSystem.typography,
            content = content
        )
    }
}

/**
 * Modern Glassmorphism modifier.
 * Applies background blur, semi-transparent background, and a subtle glow border.
 */
fun Modifier.glassmorphicCard(
    cornerRadius: Dp = 24.dp,
    glowColor: Color = Color.Transparent,
    glowRadius: Dp = 0.dp
): Modifier = this
    .composed {
        val colors = RedwoodDesignSystem.colors
        this
            .then(
                if (glowRadius > 0.dp) {
                    Modifier.drawBehind {
                        val paint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(glowRadius.toPx(), 0f, 0f, glowColor.toArgb())
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRoundRect(
                                0f, 0f, size.width, size.height, cornerRadius.toPx(), cornerRadius.toPx(), paint
                            )
                        }
                    }
                } else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(colors.surface.copy(alpha = 0.88f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(colors.border, Color.Transparent),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    }

/**
 * Premium squish effect for interactive elements.
 */
fun Modifier.premiumClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "premiumClickableScale"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            enabled = enabled,
            interactionSource = actualInteractionSource,
            indication = null,
            onClick = onClick
        )
}
