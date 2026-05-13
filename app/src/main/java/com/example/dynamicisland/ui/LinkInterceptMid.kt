package com.example.dynamicisland.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsState

@Composable
fun LinkInterceptMid(
    link: LiveActivityModel.LinkIntercept,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val accentColor = Color(0xFF64B5F6) // soft blue, adjust with settings if desired

    var copyPressed by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue = if (copyPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "copy_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Link, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // URL text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Link detected",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = link.url,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Open button
        Box(
            modifier = Modifier
                .scale(copyScale)
                .background(accentColor.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    copyPressed = true
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {}
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Open",
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Copy button (additional quick action)
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", link.url))
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }

    LaunchedEffect(copyPressed) {
        if (copyPressed) {
            kotlinx.coroutines.delay(300)
            copyPressed = false
        }
    }
}