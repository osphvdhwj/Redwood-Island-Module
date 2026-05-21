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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.R
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.settings.SettingsState

@Composable
fun OtpMid(
    otp: LiveActivityModel.Otp,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val code    = otp.code

    var copied by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue   = if (copied) 1.30f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label         = "otp_copy_scale"
    )
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }

    val displayCode = code.toCharArray().joinToString(" ")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF4285F4).copy(alpha = 0.15f), CircleShape)
                .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Verification Code",
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayCode,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = if (copied) Color(0xFF4CAF50).copy(alpha = 0.25f)
                            else Color(0xFF4285F4).copy(alpha = 0.18f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = if (copied) Color(0xFF4CAF50).copy(alpha = 0.5f)
                            else Color(0xFF4285F4).copy(alpha = 0.4f),
                    shape = CircleShape
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
                    copied = true
                },
            contentAlignment = Alignment.Center
        ) {
            if (copied) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Copy done",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(17.dp).graphicsLayer { scaleX = copyScale; scaleY = copyScale }
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_sync_vector),
                    contentDescription = "Copy OTP",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
