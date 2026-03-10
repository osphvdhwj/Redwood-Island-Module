package com.example.dynamicisland

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class IslandTheme(
    // 🚀 Text Specifics (Size, Font, X/Y Position)
    val titleSize: TextUnit = 16.sp,
    val titleFont: FontFamily = FontFamily.Default, // Or FontFamily.Serif, FontFamily.Monospace
    val titleOffsetX: Dp = 0.dp,
    val titleOffsetY: Dp = 0.dp,

    val timeTextSize: TextUnit = 12.sp,
    val timeTextOffsetX: Dp = 0.dp,

    // 🚀 Progress Bar Specifics (Thickness, Shape/Style)
    val mediaBarThickness: Dp = 4.dp,
    val mediaBarCap: StrokeCap = StrokeCap.Round, // Can be changed to StrokeCap.Square

    val batteryRingThickness: Dp = 12.dp,
    val batteryRingCap: StrokeCap = StrokeCap.Round,

    // 🚀 Element Sizes
    val cornerRadius: Dp = 50.dp,
    val albumArtSize: Dp = 44.dp,
    val buttonSize: Dp = 48.dp
)

val LocalIslandTheme = compositionLocalOf { IslandTheme() }
