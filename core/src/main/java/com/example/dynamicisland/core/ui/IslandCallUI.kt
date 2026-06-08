package com.example.dynamicisland.core.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.settings.*

@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val view = this
    val theme = LocalIslandTheme.current
    val isRinging = model.state == "RINGING"

    when (theme.callStyle) {
        com.example.dynamicisland.shared.settings.CallStyle.MINIMAL -> {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (model.contactPhoto != null) {
                    Image(
                        bitmap = model.contactPhoto.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.size(8.dp).background(if (model.isSpam) Color.Red else Color(0xFF34C759), CircleShape))
                }
                
                if (!isRinging) {
                    IsolatedTimerText(startTime = model.startTime, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(if (model.isSpam) "Spam" else "Call", color = if (model.isSpam) Color.Red else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        else -> {
            val infiniteTransition = rememberInfiniteTransition(label="ring")
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.10f,
                animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ripple"
            )

            val bgColor = if (model.isSpam) {
                Brush.horizontalGradient(listOf(Color(0xFFFF3B30), Color(0xFF8E0000)))
            } else if (isRinging) {
                Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFF248A3D)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF30D158), Color(0xFF34C759)))
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = if (isRinging) rippleScale else 1f; scaleY = if (isRinging) rippleScale else 1f }
                    .background(bgColor, RoundedCornerShape(50))
                    .squishClickable { if (isRinging || model.isSpam) view.onOpenCallUI?.invoke() else setState(IslandState.TYPE_2_MID) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.contactPhoto != null) {
                    Image(bitmap = model.contactPhoto.asImageBitmap(), contentDescription = null, modifier = Modifier.size(20.dp).clip(CircleShape))
                } else {
                    Icon(if (model.isSpam) Icons.Default.Warning else Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                }

                if (isRinging || model.isSpam) {
                    Text(
                        text = if (model.isSpam) "Potential Spam" else model.callerName.ifEmpty { "Incoming Call" },
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, modifier = Modifier.weight(1f)
                    )
                } else {
                    IsolatedTimerText(startTime = model.startTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun DynamicIslandView.CallMid(model: LiveActivityModel.Call) {
    val view = this
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    
    var isMicMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    var isSpeakerOn by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }

    AlertMidSlot(
        islandState = islandState.value,
        swipeAction = { view.onOpenCallUI?.invoke() },
        iconContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (model.contactPhoto != null) {
                    Image(bitmap = model.contactPhoto.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                } else {
                    Icon(if (model.isSpam) Icons.Default.Warning else Icons.Default.Person, contentDescription = null, tint = if (model.isSpam) Color.Red else Color.White, modifier = Modifier.size(26.dp))
                }
            }
        },
        title = if (model.isSpam) "Spam Caller" else model.callerName,
        titleColor = if (model.isSpam) Color.Red else Color.White,
        subtitleContent = {
            Column {
                if (model.relationLabel != null) {
                    Text(text = model.relationLabel, color = Color.White.copy(alpha=0.7f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(if (model.isSpam) Color.Red else Color(0xFF30D158), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    if (model.state == "RINGING") {
                        Text(text = model.phoneNumber ?: "Unknown Number", color = Color.White.copy(alpha=0.7f), fontSize = 13.sp)
                    } else {
                        IsolatedTimerText(startTime = model.startTime, color = Color(0xFF30D158), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        rightContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (model.state == "RINGING") {
                    QuickCircleBtn(icon = Icons.Default.Call, isActive = true, activeColor = Color(0xFF34C759), inactiveColor = Color(0xFF34C759)) { view.onOpenCallUI?.invoke() }
                } else {
                    QuickCircleBtn(icon = if(isMicMuted) Icons.Default.Close else Icons.Default.Check, isActive = isMicMuted, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                        view.onMicToggle?.invoke(); isMicMuted = !isMicMuted 
                    }
                }
                QuickCircleBtn(icon = Icons.Default.Call, isActive = true, activeColor = Color(0xFFFF3B30), inactiveColor = Color(0xFFFF3B30)) { 
                    view.onEndCallClick?.invoke() 
                }
            }
        }
    )
}
