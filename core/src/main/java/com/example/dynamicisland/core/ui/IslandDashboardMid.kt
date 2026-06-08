package com.example.dynamicisland.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.model.QSTileState
import com.example.dynamicisland.shared.settings.*

// File: app/src/main/java/com.example.dynamicisland.core.ui/IslandDashboardMid.kt


@Composable
fun DynamicIslandView.DashboardMid(model: LiveActivityModel.Dashboard) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tilesToShow = model.activeTiles.take(4)

        tilesToShow.forEach { tile ->
            val bgColor = if (tile.isActive) Color.White else Color.White.copy(alpha = 0.15f)
            val fgColor = if (tile.isActive) Color.Black else Color.White
            val alpha = if (tile.isUnavailable) 0.5f else 1f

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .alpha(alpha)
                    .clickable(enabled = !tile.isUnavailable) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onQsTileClick?.invoke(tile.tileName)   // 👈 now uses tileName
                    },
                contentAlignment = Alignment.Center
            ) {
                // Use the resource ID to show icon; fallback to default Settings icon
                if (tile.iconRes != 0) {
                    Icon(
                        painter = painterResource(id = tile.iconRes),
                        contentDescription = tile.tileName,     // 👈 tileName as label
                        tint = fgColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = tile.tileName,
                        tint = fgColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
