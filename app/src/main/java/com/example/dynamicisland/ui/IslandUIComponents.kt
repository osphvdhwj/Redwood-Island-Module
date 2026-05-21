package com.example.dynamicisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.model.LocalIslandTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun AlertMidSlot(
    islandState: IslandState,
    iconContent: @Composable () -> Unit,
    title: String,
    titleColor: Color = Color.White,
    subtitle: String? = null,
    subtitleColor: Color = Color.White.copy(alpha=0.7f),
    subtitleContent: @Composable (() -> Unit)? = null,
    rightContent: @Composable (() -> Unit)? = null,
    swipeAction: (() -> Unit)? = null
) {
    val theme = LocalIslandTheme.current
    var modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)

    if (swipeAction != null) {
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                if (dragAmount.y > 20f) swipeAction()
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(theme.batIconSize + 8.dp).height(theme.batIconSize + 8.dp)) { iconContent() }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(text = title, color = titleColor, fontSize = theme.alertTitleSize, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState))
             if (subtitleContent != null) {
                 subtitleContent()
             } else if (subtitle != null) {
                 Text(text = subtitle, color = subtitleColor, fontSize = theme.alertMsgSize, maxLines = 1, modifier = Modifier.safeMarquee(islandState))
             }
        }
        if (rightContent != null) {
            Spacer(Modifier.width(14.dp))
            rightContent()
        }
    }
}

@Composable
fun IsolatedTimerText(startTime: Long, color: Color, fontSize: TextUnit, fontWeight: FontWeight) {
    var elapsedSecs by remember { mutableLongStateOf((System.currentTimeMillis() - startTime) / 1000) }
    LaunchedEffect(startTime) {
        while(isActive) { delay(1000); elapsedSecs = (System.currentTimeMillis() - startTime) / 1000 }
    }
    Text(text = String.format("%02d:%02d", elapsedSecs / 60, elapsedSecs % 60), color = color, fontSize = fontSize, fontWeight = fontWeight)
}

@Composable
fun IsolatedCountdownText(targetTimeMs: Long, prefix: String, suffix: String, color: Color) {
    val theme = LocalIslandTheme.current
    var remaining by remember { mutableIntStateOf(((targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }
    LaunchedEffect(targetTimeMs) {
        while (remaining > 0) { delay(1000); remaining = ((targetTimeMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0) }
    }
    Text(text = "$prefix$remaining$suffix", color = color, fontSize = theme.alertMsgSize, maxLines = 1)
}

@Composable
fun QuickCircleBtn(icon: ImageVector, isActive: Boolean, activeColor: Color, inactiveColor: Color, onClick: () -> Unit) {
    QuickCircleBtnContent(icon = icon, painter = null, isActive = isActive, activeColor = activeColor, inactiveColor = inactiveColor, onClick = onClick)
}

@Composable
fun QuickCircleBtn(painter: Painter, isActive: Boolean, activeColor: Color, inactiveColor: Color, onClick: () -> Unit) {
    QuickCircleBtnContent(icon = null, painter = painter, isActive = isActive, activeColor = activeColor, inactiveColor = inactiveColor, onClick = onClick)
}

@Composable
private fun QuickCircleBtnContent(
    icon: ImageVector?,
    painter: Painter?,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(if (isActive) activeColor else inactiveColor).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, contentDescription=null, tint = if (isActive && activeColor == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
        } else if (painter != null) {
            Icon(painter, contentDescription=null, tint = if (isActive && activeColor == Color.White) Color.Black else Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

fun getIconForType(type: ActivityType): ImageVector {
    return when(type) {
        ActivityType.CALL -> Icons.Default.Phone; ActivityType.NAVIGATION -> Icons.Default.LocationOn;
        ActivityType.TIMER -> Icons.Default.Notifications; ActivityType.MESSAGE -> Icons.Default.Email;
        ActivityType.ALARM -> Icons.Default.Notifications; ActivityType.CHARGING -> Icons.Default.Add;
        ActivityType.BATTERY_LOW -> Icons.Default.Warning; ActivityType.BLUETOOTH -> Icons.Default.Settings;
        ActivityType.WIFI -> Icons.Default.Refresh; ActivityType.HARDWARE -> Icons.Default.Info;
        else -> Icons.Default.Info
    }
}
