package com.example.dynamicisland.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Standard container for all Mid-sized states (MusicMid, OtpMid, NavigationMid).
 */
@Composable
fun MidPill(
    modifier: Modifier = Modifier,
    dynamicBackgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    PillSurface(
        modifier = modifier
            .fillMaxWidth(0.9f) // 90% of screen width
            .height(72.dp),     // Standard Mid height
        shape = IslandTheme.shapes.mid,
        backgroundColor = dynamicBackgroundColor ?: IslandTheme.colors.background,
        shadowElevation = 16.dp,
        content = content
    )
}