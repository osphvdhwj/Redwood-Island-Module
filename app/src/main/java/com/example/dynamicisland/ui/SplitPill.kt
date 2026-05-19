package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.manager.IslandController
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsState

@Composable
fun SplitPill(controller: IslandController) {
    val settings = controller.settingsState ?: SettingsState()
    // In real implementation, get two active models from controller
    val leftModel = null // replace with actual
    val rightModel = null

    if (leftModel == null || rightModel == null) return

    var dividerFraction by remember { mutableStateOf(0.5f) }
    val leftWeight = dividerFraction
    val rightWeight = 1f - dividerFraction

    val shape = RoundedCornerShape(settings.pillCornerRadius.dp * 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(12.dp, shape)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (settings.dynamicGradient) listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                    else listOf(Color.Black.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.85f))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight()
                .clip(shape)
                .background(Color.Transparent)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (leftModel) {
                is LiveActivityModel.Music -> MusicMiniPill(leftModel)
                is LiveActivityModel.Call -> CallMiniPill(leftModel)
                else -> Text(leftModel.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val delta = dragAmount / 500f
                        dividerFraction = (dividerFraction + delta).coerceIn(0.2f, 0.8f)
                    }
                }
        )

        Box(
            modifier = Modifier
                .weight(rightWeight)
                .fillMaxHeight()
                .clip(shape)
                .background(Color.Transparent)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            when (rightModel) {
                is LiveActivityModel.Music -> MusicMiniPill(rightModel)
                is LiveActivityModel.Call -> CallMiniPill(rightModel)
                else -> Text(rightModel.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun MusicMiniPill(music: LiveActivityModel.Music) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.MusicNote, null,
            tint = Color(0xFF1DB954),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "${music.artist} · ${music.title}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

@Composable
private fun CallMiniPill(call: LiveActivityModel.Call) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.Phone, null,
            tint = if (call.state == "RINGING") Color(0xFF34C759) else Color(0xFFFF3B30),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = call.callerName ?: "Unknown",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}