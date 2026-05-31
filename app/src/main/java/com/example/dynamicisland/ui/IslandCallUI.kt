package com.example.dynamicisland.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border

import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.ipc.IslandState

@Composable
fun DynamicIslandView.CallMini(model: LiveActivityModel.Call) {
    val view = this
    val theme = LocalIslandTheme.current
    val isRinging = model.state == "RINGING"

    when (theme.callStyle) {
        com.example.dynamicisland.settings.CallStyle.MINIMAL -> {
            // Tiny dot + Timer
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF34C759), CircleShape))
                if (!isRinging) {
                    IsolatedTimerText(startTime = model.startTime, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("Call", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        else -> {
            // IOS and MODERN style logic (Original)
            val infiniteTransition = rememberInfiniteTransition(label="ring")
            val rippleScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.10f,
                animationSpec = infiniteRepeatable(
                    tween(600, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ), label = "ripple"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    tween(800, easing = LinearOutSlowInEasing),
                    RepeatMode.Reverse
                ), label = "glow"
            )

            val bgColor = if (isRinging) {
                Brush.horizontalGradient(listOf(Color(0xFF34C759), Color(0xFF248A3D)))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF30D158), Color(0xFF34C759)))
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = if (isRinging) rippleScale else 1f
                        scaleX = s; scaleY = s
                    }
                    .background(bgColor, RoundedCornerShape(50))
                    .squishClickable {
                        if (isRinging) view.onOpenCallUI?.invoke() else setState(IslandState.TYPE_2_MID)
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val phoneRotation by infiniteTransition.animateFloat(
                    initialValue = -15f,
                    targetValue = 15f,
                    animationSpec = infiniteRepeatable(
                        tween(200, easing = LinearEasing),
                        RepeatMode.Reverse
                    ), label = "rotation"
                )

                Icon(
                    Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(15.dp)
                        .graphicsLayer { rotationZ = if (isRinging) phoneRotation else 0f }
                )

                if (isRinging) {
                    Text(
                        text = model.callerName.ifEmpty { "Incoming Call" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    IsolatedTimerText(
                        startTime = model.startTime,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
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
    val theme = LocalIslandTheme.current
    
    var isMicMuted by remember { mutableStateOf(audioManager.isMicrophoneMute) }
    var isSpeakerOn by remember { mutableStateOf(audioManager.isSpeakerphoneOn) }

    AlertMidSlot(
        islandState = islandState.value,
        swipeAction = { view.onOpenCallUI?.invoke() },
        iconContent = {
            val pulse by rememberInfiniteTransition(label="p").animateFloat(initialValue = 0.96f, targetValue = 1.04f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label="s")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        },
        title = model.callerName,
        titleColor = Color.White,
        subtitleContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(Color(0xFF30D158), CircleShape))
                Spacer(Modifier.width(6.dp))
                IsolatedTimerText(startTime = model.startTime, color = Color(0xFF30D158), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        },
        rightContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCircleBtn(icon = if(isMicMuted) Icons.Default.Close else Icons.Default.Check, isActive = isMicMuted, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    view.onMicToggle?.invoke(); isMicMuted = !isMicMuted 
                }
                QuickCircleBtn(icon = Icons.Default.Notifications, isActive = isSpeakerOn, activeColor = Color.White, inactiveColor = Color.White.copy(0.15f)) { 
                    view.onSpeakerToggle?.invoke(); isSpeakerOn = !isSpeakerOn 
                }
                QuickCircleBtn(icon = Icons.Default.Call, isActive = true, activeColor = Color(0xFFFF3B30), inactiveColor = Color(0xFFFF3B30)) { 
                    view.onEndCallClick?.invoke() 
                }
            }
        }
    )
}
