package com.example.dynamicisland.core.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.shared.settings.IconPack

/**
 * HIGH-FIDELITY VISUAL DIALECT (Pillar 2)
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
 * Defines the physical properties and aesthetic "accent" of the Island
 * based on the active IconPack (Dialect).
 */
data class VisualDialect(
    val cornerRadius: Dp = 20.dp,
    val borderWidth: Dp = 1.dp,
    val borderColor: Color = Color.White.copy(alpha = 0.2f),
    val glowColor: Color = Color.Transparent,
    val glowRadius: Dp = 0.dp,
    val backgroundAlpha: Float = 1.0f,
    val accentColor: Color = Color.White,
    val fontScaling: Float = 1.0f,
    val isGlassy: Boolean = false
) {
    companion object {
        fun fromIconPack(pack: IconPack): VisualDialect {
            return when (pack) {
                IconPack.iOS -> VisualDialect(
                    cornerRadius = 24.dp,
                    borderWidth = 0.5.dp,
                    borderColor = Color.White.copy(alpha = 0.3f),
                    backgroundAlpha = 0.1f, // High transparency for glass
                    isGlassy = true,
                    accentColor = Color.White,
                    glowColor = Color.White.copy(alpha = 0.1f),
                    glowRadius = 4.dp
                )
                IconPack.OxygenOS -> VisualDialect(
                    cornerRadius = 18.dp,
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFFFF0000).copy(alpha = 0.4f), // OnePlus Red accent
                    accentColor = Color(0xFFFF0000)
                )
                IconPack.Pixel -> VisualDialect(
                    cornerRadius = 100.dp, // Perfectly circular for mini
                    borderWidth = 1.dp,
                    borderColor = Color.White.copy(alpha = 0.2f),
                    accentColor = Color(0xFF4285F4) // Google Blue
                )
                IconPack.Futuristic -> VisualDialect(
                    cornerRadius = 4.dp, // Cyber-sharp
                    borderWidth = 2.dp,
                    borderColor = Color(0xFF00FFFF), // Cyan
                    glowColor = Color(0xFF00FFFF).copy(alpha = 0.5f),
                    glowRadius = 8.dp,
                    accentColor = Color(0xFF00FFFF)
                )
                IconPack.Bold -> VisualDialect( // Cyberpunk equivalent
                    cornerRadius = 0.dp, // Brutalist
                    borderWidth = 3.dp,
                    borderColor = Color(0xFFFDEE00), // Aureolin / Cyberpunk Yellow
                    glowColor = Color(0xFFFDEE00).copy(alpha = 0.6f),
                    glowRadius = 12.dp,
                    accentColor = Color(0xFFFDEE00),
                    backgroundAlpha = 1.0f
                )
                IconPack.Minimal -> VisualDialect(
                    cornerRadius = 12.dp,
                    borderWidth = 0.dp,
                    borderColor = Color.Transparent,
                    backgroundAlpha = 0.8f,
                    accentColor = Color.White
                )
                IconPack.Outline -> VisualDialect(
                    cornerRadius = 20.dp,
                    borderWidth = 2.dp,
                    borderColor = Color.White,
                    backgroundAlpha = 0.0f, // True outline
                    accentColor = Color.White
                )
                IconPack.Samsung -> VisualDialect(
                    cornerRadius = 22.dp,
                    borderWidth = 1.dp,
                    borderColor = Color.White.copy(alpha = 0.2f),
                    accentColor = Color(0xFF147EFB)
                )
                else -> VisualDialect() 
            }
        }
    }
}
