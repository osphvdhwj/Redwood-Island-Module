package com.example.dynamicisland.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.LiveActivityModel
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.SettingsState

@Composable
fun OtpMid(
    otp: LiveActivityModel.Otp,
    settings: SettingsState,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val code    = otp.code

    var copied by remember { mutableStateOf(false) }
    val copyScale by animateFloatAsState(
        targetValue   = if (copied) 1.25f else 1f,
        animationSpec = IslandPhysics.springFloat,
        label         = "copy"
    )
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1200); copied = false }
    }

    val displayCode = code.toCharArray().joinToString("  ")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Icon Slot ---
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0xFF4285F4).copy(alpha = 0.12f), CircleShape)
                .border(1.dp, Color(0xFF4285F4).copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, null, tint = Color(0xFF4285F4), modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        // --- Text Content ---
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "VERIFICATION CODE",
                color = Color(0xFF4285F4),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                displayCode,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // --- Copy Button ---
        Box(
            modifier = Modifier
                .size(42.dp)
                .graphicsLayer { scaleX = copyScale; scaleY = copyScale }
                .background(if (copied) Color(0xFF34C759).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), CircleShape)
                .squishClickable {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
                    copied = true
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (copied) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (copied) Color(0xFF34C759) else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
