package com.example.dynamicisland.model

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.settings.CallStyle
import com.example.dynamicisland.settings.ChargingStyle
import com.example.dynamicisland.settings.BatteryStyle

enum class IslandShape {
    PILL, CUBE, ORBITAL, BRUTALIST
}

data class IslandTheme(
    val shape: IslandShape = IslandShape.PILL,
    // Original UI Properties
    val mediaBarCap: StrokeCap = StrokeCap.Round,
    val mediaBarThickness: Dp = 4.dp,
    val titleOffsetX: Dp = 0.dp,
    val titleOffsetY: Dp = 0.dp,
    val titleSize: TextUnit = 16.sp,
    val titleFont: FontFamily = FontFamily.Monospace,
    val timeTextSize: TextUnit = 12.sp,
    val timeTextOffsetX: Dp = 0.dp,
    val batteryRingThickness: Dp = 12.dp,
    val cornerRadius: Dp = 50.dp,
    val albumArtSize: Dp = 44.dp,
    val buttonSize: Dp = 48.dp,
    val buttonSpacing: Dp = 16.dp,
    val buttonCornerRadius: Dp = 50.dp,
    val actionAnimType: String = "FLAT",
    val handleWidth: Dp = 40.dp,
    val handleHeight: Dp = 5.dp,
    
    val textPrimary: TextUnit = 16.sp,
    val textSecondary: TextUnit = 14.sp,
    val progressThick: Dp = 4.dp,
    val ringThick: Dp = 12.dp,
    val elementGap: Dp = 8.dp,
    val musicTitleSize: TextUnit = 16.sp,
    val musicArtistSize: TextUnit = 14.sp,
    val musicSeekerThick: Dp = 4.dp,
    val batTextSize: TextUnit = 16.sp,
    val batIconSize: Dp = 36.dp,
    val batRingThick: Dp = 12.dp,
    val alertTitleSize: TextUnit = 16.sp,
    val alertMsgSize: TextUnit = 14.sp,

    // Premium Features
    val hapticStrength: Int = 1, 
    val chargingStyle: ChargingStyle = ChargingStyle.RING, 
    val callStyle: CallStyle = CallStyle.IOS,
    val batteryStyle: BatteryStyle = BatteryStyle.PILL,
    val blurIntensity: Dp = 0.dp,
    val hideOnLandscape: Boolean = false,

    // 🎛️ NEW: The "Shadow Properties" (Physics & Deep Colors)
    val springDamping: Float = 1f, 
    val springStiffness: Float = 0f, 
    val autoCollapseTimeMs: Long = 3000L,
    val isGlassmorphism: Boolean = false, // Toggles deep black vs translucent glass
    val accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    val glowColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Transparent
) {
    companion object {
        val Default = IslandTheme()
        val Brutalist = IslandTheme(
            shape = IslandShape.BRUTALIST,
            cornerRadius = 0.dp,
            mediaBarCap = StrokeCap.Square,
            buttonCornerRadius = 0.dp
        )
        val Orbital = IslandTheme(shape = IslandShape.ORBITAL)
        val Neural = IslandTheme(shape = IslandShape.CUBE)
    }
}

val LocalIslandTheme = compositionLocalOf { IslandTheme() }
