package com.example.dynamicisland.core.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

/**
 * Extracts two prominent colors from an album art bitmap to create a dynamic gradient.
 * Uses the dominant swatch, vibrant swatch, and light vibrant swatch.
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * Falls back to a default dark gradient if no colors are extracted.
 */
fun extractGradientColors(bitmap: Bitmap): List<Color> {
    val palette = Palette.from(bitmap).generate()
    val colors = mutableListOf<Color>()

    palette.dominantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.vibrantSwatch?.rgb?.let { colors.add(Color(it)) }
    palette.lightVibrantSwatch?.rgb?.let { colors.add(Color(it)) }

    if (colors.isEmpty()) {
        colors.add(Color.DarkGray)
        colors.add(Color.Black)
    }

    return colors.distinct().take(2)
}