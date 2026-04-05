package com.example.dynamicisland.ui
import com.example.dynamicisland.model.*
import com.example.dynamicisland.manager.*

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val haptic = LocalHapticFeedback.current
    val isRinging = model.state == "RINGING"
    val ringPulse by rememberInfiniteTransition(label="ring").animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label="alpha")
    val pillAlpha = if (isRinging) ringPulse else 1f

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF00C853).copy(alpha = pillAlpha), RoundedCornerShape(50))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isRinging) onOpenCallUI?.invoke() else setState(IslandState.TYPE_2_MID) 
                    },
                    onLongPress = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenCallUI?.invoke() 
                    }
                )
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        if (isRinging) {
            Text(text = "Incoming", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        } else {
            IsolatedTimerText(startTime = model.startTime, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DynamicIslandView.CallMid(model: LiveActivityModel.Call) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val theme = LocalIslandTheme.current
    
    var isMicMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    var isSpeakerOn by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }

    AlertMidSlot(
        islandState = islandState.value,
        swipeAction = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onOpenCallUI?.invoke()
        },
        iconContent = {
            val pulseScale by rememberInfiniteTransition(label="pulse").animateFloat(initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label="scale")
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX=pulseScale; scaleY=pulseScale }.background(Color(0xFF333333), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(theme.batIconSize * 0.65f))
            }
        },
        title = model.callerName,
        titleColor = Color.White,
        subtitleContent = {
            IsolatedTimerText(startTime = model.startTime, color = Color(0xFF00C853), fontSize = theme.alertMsgSize, fontWeight = FontWeight.SemiBold)
        },
        rightContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickCircleBtn(icon = if(isMicMuted) Icons.Default.MicOff else Icons.Default.Mic, isActive = isMicMuted, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onMicToggle?.invoke(); isMicMuted = !isMicMuted 
                }
                QuickCircleBtn(icon = Icons.Default.VolumeUp, isActive = isSpeakerOn, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSpeakerToggle?.invoke(); isSpeakerOn = !isSpeakerOn 
                }
                QuickCircleBtn(icon = Icons.Default.CallEnd, isActive = true, activeColor = Color(0xFFFF3B30), inactiveColor = Color(0xFFFF3B30)) { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress); onEndCallClick?.invoke() 
                }
            }
        }
    )
}
