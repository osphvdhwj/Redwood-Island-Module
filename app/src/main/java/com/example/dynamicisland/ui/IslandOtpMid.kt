package com.example.dynamicisland.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.model.LiveActivityModel

/**
 * Mid-state view for OTP codes.
 *
 * Layout:
 *   [🔒 icon]  Verification Code     [Copy ✓]
 *              1 2 3 4 5 6  (mono, large, spaced)
 *
 * Tapping the copy button copies to clipboard and shows a check mark
 * for 1.5 s with a spring-scale animation.
 */
@Composable
fun DynamicIslandView.OtpMid(alert: LiveActivityModel.SystemAlert) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val theme   = LocalIslandTheme.current
    val code    = alert.message   // the raw digit string

    var copied by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue   = if (copied) 1.30f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "copy_scale"
    )
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }

    // Space out individual digits for readability
    val displayCode = code.toCharArray().joinToString(" ")

    Row(
        modifier          = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Icon ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(theme.batIconSize + 4.dp)
                .background(Color(0xFF4285F4).copy(alpha = 0.15f), CircleShape)
                .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Lock,
                contentDescription = null,
                tint               = Color(0xFF4285F4),
                modifier           = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        // ── Code + label ─────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Verification Code",
                color      = Color.White.copy(alpha = 0.55f),
                fontSize   = theme.alertMsgSize,
                fontWeight = FontWeight.Medium
            )
            Text(
                text       = displayCode,
                color      = Color.White,
                fontSize   = (theme.alertTitleSize.value + 4f).sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.width(8.dp))

        // ── Copy button ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = if (copied) Color(0xFF4CAF50).copy(0.25f)
                            else Color(0xFF4285F4).copy(alpha = 0.18f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (copied) Color(0xFF4CAF50).copy(0.50f)
                            else Color(0xFF4285F4).copy(alpha = 0.40f),
                    shape = CircleShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("otp", code))
                    copied = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy code",
                tint               = if (copied) Color(0xFF4CAF50) else Color(0xFF4285F4),
                modifier           = Modifier
                    .size(17.dp)
                    .graphicsLayer { scaleX = copyScale; scaleY = copyScale }
            )
        }
    }
}