package com.example.dynamicisland.core.ui.design

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.settings.SettingsState

private val DarkColorScheme = darkColorScheme(
    primary = IslandColors.accentCyan,
    secondary = IslandColors.accentPurple,
    tertiary = IslandColors.accentCyan,
    background = IslandColors.background,
    surface = IslandColors.surface,
    onPrimary = IslandColors.textPrimary,
    onSecondary = IslandColors.textPrimary,
    onTertiary = IslandColors.textPrimary,
    onBackground = IslandColors.textPrimary,
    onSurface = IslandColors.textPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = IslandColors.accentCyan,
    secondary = IslandColors.accentPurple,
    tertiary = IslandColors.accentCyan,
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)


@Composable
fun AppMD3Theme(
    settings: SettingsState = SettingsState(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        settings.aestheticStyle == AestheticStyle.LIQUID_GLASS -> VyxelDesignSystem.LiquidGlassBase
        settings.designLanguage == DesignLanguage.VYXEL_EXPRESSIVE -> {
            VyxelDesignSystem.getExpressiveMonet(context, darkTheme)
        }
        settings.designLanguage == DesignLanguage.APPLE_LIQUID_GLASS -> {
            VyxelDesignSystem.LiquidGlassBase
        }
        else -> {
            when {
                settings.dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                darkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
    }
    
    val fontFamily = VyxelDesignSystem.getFontFamily(settings.fontAesthetic)
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp
            ),
            titleLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = (-0.5).sp
            ),
            titleMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            ),
            labelSmall = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        ),
        content = content
    )
}
