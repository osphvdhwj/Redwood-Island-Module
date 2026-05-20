package com.example.dynamicisland.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Split Pill UI – Handles two simultaneous ongoing activities.
 * Standard layout: A larger primary pill on the left (e.g., Music), 
 * and a smaller circular indicator on the right (e.g., Timer or Call).
 */
@Composable
fun SplitPill(
    primaryModel: LiveActivityModel,
    secondaryModel: LiveActivityModel,
    onPrimaryClick: () -> Unit = {},
    onSecondaryClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- PRIMARY PILL (Left) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(percent = 50))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPrimaryClick()
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getIconForModel(primaryModel),
                    contentDescription = null,
                    tint = getAccentForModel(primaryModel),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                // Simple waveform or scrolling text could go here
                Text(
                    text = getLabelForModel(primaryModel),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // --- DIVIDER (Invisible Space) ---
        Spacer(modifier = Modifier.width(8.dp))

        // --- SECONDARY PILL (Right) ---
        Box(
            modifier = Modifier
                .aspectRatio(1f) // Keep it a perfect circle
                .fillMaxHeight()
                .clip(CircleShape)
                .background(getAccentForModel(secondaryModel).copy(alpha = 0.15f))
                .border(1.dp, getAccentForModel(secondaryModel).copy(alpha = 0.3f), CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSecondaryClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForModel(secondaryModel),
                contentDescription = null,
                tint = getAccentForModel(secondaryModel),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Helper methods to extract UI elements from generic LiveActivityModels
private fun getIconForModel(model: LiveActivityModel): ImageVector {
    return when (model) {
        is LiveActivityModel.Call -> Icons.Default.Call
        is LiveActivityModel.Music -> Icons.Default.MusicNote
        is LiveActivityModel.AppTimerWarning -> Icons.Default.Timer
        else -> Icons.Default.Call // Fallback
    }
}

private fun getAccentForModel(model: LiveActivityModel): Color {
    return when (model) {
        is LiveActivityModel.Call -> Color(0xFF4CAF50)
        is LiveActivityModel.Music -> Color(0xFFE040FB)
        is LiveActivityModel.AppTimerWarning -> Color(0xFFFFB74D)
        else -> Color.White // Fallback
    }
}

private fun getLabelForModel(model: LiveActivityModel): String {
    return when (model) {
        is LiveActivityModel.Call -> model.callerName
        is LiveActivityModel.Music -> model.title
        is LiveActivityModel.AppTimerWarning -> "Time Limit"
        else -> "Active" // Fallback
    }
}