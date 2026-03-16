package com.example.dynamicisland

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class IslandTheme(
    val mediaBarCap: StrokeCap = StrokeCap.Round,
    val mediaBarThickness: Dp = 4.dp,
    val titleOffsetX: Dp = 0.dp,
    val titleOffsetY: Dp = 0.dp,
    val titleSize: TextUnit = 16.sp,
    val titleFont: FontFamily = FontFamily.Default,
    val timeTextSize: TextUnit = 12.sp,
    val timeTextOffsetX: Dp = 0.dp,
    val batteryRingThickness: Dp = 12.dp,
    val cornerRadius: Dp = 50.dp,
    val albumArtSize: Dp = 44.dp,
    val buttonSize: Dp = 48.dp,
    val buttonSpacing: Dp = 16.dp,
    val buttonCornerRadius: Dp = 50.dp,
    val actionAnimType: String = "BOUNCE",
    val handleWidth: Dp = 40.dp,
    val handleHeight: Dp = 5.dp
)

val LocalIslandTheme = compositionLocalOf { IslandTheme() }
