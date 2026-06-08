package com.example.dynamicisland.core.ui.design

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.PhysicsStyle
import com.example.dynamicisland.shared.settings.ContentTransitionStyle
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.shared.model.LocalIslandTheme
import com.example.dynamicisland.shared.model.IslandTheme
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.ui.text.font.FontFamily
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.FontAesthetic
/**
 * ✨ VYXEL EXPRESSIVE DESIGN SYSTEM
 * 
 * Ported and improved from Vyxel Apps.
 * Features ultra-vibrant tonal mappings and support for the LiquidGlass aesthetic.
 */
object VyxelDesignSystem {
    // --- Font Loader ---
    fun getFontFamily(aesthetic: FontAesthetic): FontFamily = when (aesthetic) {
        FontAesthetic.KILO -> FontFamily(Font(R.font.kilo))
        FontAesthetic.CHOCOCOOKY -> FontFamily(Font(R.font.chococooky))
        FontAesthetic.MONOSPACE -> FontFamily.Monospace
        else -> FontFamily.Default
    }
    // --- LiquidGlass Palette ---
    val LiquidGlassBase = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF1C1B1F).copy(alpha = 0.6f), // Translucent for glass
        surface = Color(0xFF1C1B1F).copy(alpha = 0.8f),
        onPrimary = Color(0xFF381E72),
        onSecondary = Color(0xFF332D41),
        onTertiary = Color(0xFF492532),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5)
    )
    /**
     * Extracts a full Monet (Material You) palette with Vyxel-style expressive weights.
     */
    fun getExpressiveMonet(context: Context, isDark: Boolean): ColorScheme {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return if (isDark) darkColorScheme() else lightColorScheme()
        }
        val res = context.resources
        fun getColor(id: Int) = Color(res.getColor(id, context.theme))
        val primary = getColor(android.R.color.system_accent1_200)
        val secondary = getColor(android.R.color.system_accent2_200)
        val tertiary = getColor(android.R.color.system_accent3_200)
        return if (isDark) {
            darkColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                primaryContainer = getColor(android.R.color.system_accent1_700),
                onPrimaryContainer = getColor(android.R.color.system_accent1_100),
                surface = getColor(android.R.color.system_neutral1_900),
                background = getColor(android.R.color.system_neutral1_900)
            )
        } else {
            lightColorScheme(
                primary = getColor(android.R.color.system_accent1_600),
                secondary = getColor(android.R.color.system_accent2_600),
                tertiary = getColor(android.R.color.system_accent3_600),
                primaryContainer = getColor(android.R.color.system_accent1_100),
                onPrimaryContainer = getColor(android.R.color.system_accent1_900),
                surface = getColor(android.R.color.system_neutral1_50),
                background = getColor(android.R.color.system_neutral1_50)
}
