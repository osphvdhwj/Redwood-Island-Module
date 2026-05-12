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

/**
 * SplitPill – Two independent pills that share the island width.
 * Pill A (left) and Pill B (right) with a draggable handle between them.
 * Used when two concurrent live activities are active (e.g., music + call).
 */
@Composable
fun SplitPill(controller: IslandController) {
    val settings = controller.settingsState
    // Mock two concurrent models – in reality these come from IslandPriorityEngine
    val leftModel = controller.activeModels.getOrNull(0)
    val rightModel = controller.activeModels.getOrNull(1)

    if (leftModel == null || rightModel == null) return

    // Divider position as fraction of total width
    var dividerFraction by remember { mutableStateOf(0.5f) }
    val leftWeight = dividerFraction
    val rightWeight = 1f - dividerFraction

    val shape = RoundedCornerShape(settings.pillCornerRadius.dp * 0.5f) // smaller pill for split

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(12.dp, shape)                                // soft outer shadow
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    if (settings.dynamicGradient) listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
                    else listOf(Color.Black.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.85f))
                )
            )
    ) {
        // Left pill
        Box(
            modifier = Modifier
                .weight(leftWeight)
                .fillMaxHeight()
                .clip(shape)
                .background(Color.Transparent)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Reuse existing mini view logic – use a compact representation
            when (leftModel) {
                is LiveActivityModel.Music -> MusicMiniPill(leftModel as LiveActivityModel.Music, controller)
                is LiveActivityModel.Call -> CallMiniPill(leftModel as LiveActivityModel.Call)
                else -> Text(leftModel.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        // Draggable divider
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val totalWidth = size.width.toFloat() // need screen width; we approximate
                        val delta = dragAmount / 500f // normalize
                        dividerFraction = (dividerFraction + delta).coerceIn(0.2f, 0.8f)
                    }
                }
        )

        // Right pill
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
                is LiveActivityModel.Music -> MusicMiniPill(rightModel as LiveActivityModel.Music, controller)
                is LiveActivityModel.Call -> CallMiniPill(rightModel as LiveActivityModel.Call)
                else -> Text(rightModel.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/* ------- Mini composables used inside the split pills ------- */

@Composable
private fun MusicMiniPill(music: LiveActivityModel.Music, controller: IslandController) {
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
            tint = if (call.isIncoming) Color(0xFF34C759) else Color(0xFFFF3B30),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = call.contactName ?: "Unknown",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}