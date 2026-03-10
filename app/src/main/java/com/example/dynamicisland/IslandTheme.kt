package com.example.dynamicisland

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 🚀 THE THEME ENGINE: Holds all dynamic properties
data class IslandTheme(
    val cornerRadius: Dp = 50.dp,
    val elementGap: Dp = 8.dp,
    val sidePadding: Dp = 16.dp,
    val ringThickness: Dp = 12.dp,
    val primaryTextSize: TextUnit = 16.sp,
    val secondaryTextSize: TextUnit = 14.sp,
    val progressBarThickness: Dp = 4.dp,
    val buttonSize: Dp = 48.dp,
    val iconSize: Dp = 24.dp,

    // Music Specific
    val musicTitleSize: TextUnit = 16.sp,
    val musicArtistSize: TextUnit = 14.sp,
    val musicSeekerThickness: Dp = 4.dp,
    val musicButtonSize: Dp = 48.dp,

    // Battery Specific
    val batteryCubeTextSize: TextUnit = 16.sp,
    val batteryCubeIconSize: Dp = 36.dp,
    val batteryRingThickness: Dp = 12.dp,

    // Notification Specific
    val alertTitleSize: TextUnit = 16.sp,
    val alertMessageSize: TextUnit = 14.sp
)

// This makes the theme available anywhere in the Compose tree instantly
val LocalIslandTheme = compositionLocalOf { IslandTheme() }
