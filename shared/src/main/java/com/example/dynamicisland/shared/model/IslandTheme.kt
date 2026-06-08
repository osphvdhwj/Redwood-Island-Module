package com.example.dynamicisland.shared.model

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.shared.settings.*

enum class IslandShape {
    PILL, CUBE, ORBITAL, BRUTALIST
}

/**
 * 🎨 HIGH-FIDELITY ISLAND THEME
 */
data class IslandTheme(
    val cornerRadius: Dp = 24.dp,
    val borderWidth: Dp = 0.5.dp,
    val borderColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
    val glowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent,
    val glowRadius: Dp = 0.dp,
    val backgroundAlpha: Float = 1.0f,
    val accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val fontScaling: Float = 1.0f,
    val isGlassy: Boolean = false,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val mainFont: FontFamily = FontFamily.Default,
    val accentFont: FontFamily = FontFamily.Default,
    val titleSize: TextUnit = 14.sp,
    val bodySize: TextUnit = 12.sp,
    val captionSize: TextUnit = 10.sp,
    
    // Core Dimensions
    val batIconSize: Dp = 18.dp,
    val alertTitleSize: TextUnit = 15.sp,
    val alertMsgSize: TextUnit = 13.sp,
    val buttonCornerRadius: Dp = 16.dp,
    val actionAnimType: String = "SQUISH"
)

val LocalIslandTheme = compositionLocalOf { IslandTheme() }
