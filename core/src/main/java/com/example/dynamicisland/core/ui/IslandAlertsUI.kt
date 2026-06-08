package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

@Composable
fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
    val color = Color(alert.alertColor)
    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(color.copy(alpha=0.2f), CircleShape).border(1.dp, color.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                val icon = when(alert.alertType) { "THERMAL" -> Icons.Default.Warning; "ROGUE" -> Icons.Default.Warning; else -> Icons.Default.Info }
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
        },
        title = alert.title,
        titleColor = color,
        subtitle = alert.message,
        subtitleColor = color.copy(alpha=0.8f)
    )
}

@Composable
fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha=0.2f), CircleShape).border(1.dp, Color.White.copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { (task.progress.toFloat() / task.progressMax.toFloat()).coerceIn(0f, 1f) }, color = Color.Cyan, trackColor = Color.White.copy(alpha=0.2f), strokeWidth = 2.dp)
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        },
        title = task.title,
        titleColor = Color.White,
        subtitle = task.text,
        subtitleColor = Color.White.copy(alpha=0.8f)
    )
}

@Composable
fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val alertAlpha by pulseTransition.animateFloat(initialValue = 0.2f, targetValue = 0.6f, animationSpec = infiniteRepeatable(animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alertAlpha")

    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = alertAlpha), CircleShape).border(2.dp, Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                if (model.appIcon != null) { 
                    Image(bitmap = model.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.fillMaxSize(0.7f).clip(CircleShape)) 
                } else { 
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) 
                }
            }
        },
        title = "Time Limit Reached",
        titleColor = Color.Red,
        subtitleContent = {
            IsolatedCountdownText(targetTimeMs = model.targetTimeMs, prefix = "${model.appName} closing in ", suffix = "s", color = Color.White)
        }
    )
}
