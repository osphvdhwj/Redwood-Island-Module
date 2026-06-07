package com.example.dynamicisland.core.ui.states

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.ui.components.PillSurface
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        backgroundColor = dynamicBackgroundColor ?: com.example.dynamicisland.ui.design.IslandColors.background,
        shadowElevation = 16.dp,
        content = content
    )
}
