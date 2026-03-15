package com.example.dynamicisland

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily

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
    
    // 🚀 NEW: Big Pill (Max State) Customizations
    val buttonSize: Dp = 48.dp, 
    val buttonSpacing: Dp = 16.dp,
    val buttonCornerRadius: Dp = 50.dp,
    val actionAnimType: String = "BOUNCE" // "CHECKMARK", "BOUNCE", "PULSE", "NONE"
)

val LocalIslandTheme = staticCompositionLocalOf { IslandTheme() }
