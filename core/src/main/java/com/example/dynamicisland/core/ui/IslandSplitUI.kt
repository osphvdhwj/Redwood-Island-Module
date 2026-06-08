package com.example.dynamicisland.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.ipc.*
import kotlin.math.abs

@Composable
fun DynamicIslandView.SplitCubeUI(state: IslandState, animatedHeight: androidx.compose.ui.unit.Dp, borderColor: Color, xOffset: Float) {
    val view = this
    AnimatedVisibility(
        visible = state == IslandState.TYPE_SPLIT,
        enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        val sModel = view.splitModel.value

        val splitBg = when {
            sModel is LiveActivityModel.Charging && sModel.isPluggedIn -> Color.Green.copy(alpha = 0.2f)
            sModel is LiveActivityModel.Charging && sModel.level <= 20 -> Color.Red.copy(alpha = 0.2f)
            else -> Color(0xFF121212).copy(alpha = 0.75f)
        }

        Row {
            Spacer(modifier = Modifier.width(xOffset.dp))
            Box(

                modifier = Modifier 
                    .size(animatedHeight) 
                    .onGloballyPositioned { coordinates ->
                        val bounds = coordinates.boundsInWindow()
                        val newLeft = bounds.left.toInt()
                        val newTop = bounds.top.toInt()
                        val newRight = bounds.right.toInt()
                        val newBottom = bounds.bottom.toInt()
                    
                        // 🧠 THE LOOP BREAKER
                        if (view.splitCubeRect.value.left != newLeft || view.splitCubeRect.value.top != newTop || view.splitCubeRect.value.right != newRight || view.splitCubeRect.value.bottom != newBottom) {
                            view.splitCubeRect.value.set(newLeft, newTop, newRight, newBottom)
                            view.insetsUpdateFlow.tryEmit(Unit)
                        }
                    }
                    .clip(CircleShape)
                    .background(splitBg)
                    .border(1.dp, borderColor, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { view.onSplitPillClick?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                when (sModel) {
                    is LiveActivityModel.Charging -> {
                        val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White
                        Text(
                            text = "${sModel.level}%",
                            color = iconColor,
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    is LiveActivityModel.SystemAlert -> {
                        Icon(
                            imageVector = Icons.Default.Notifications, 
                            contentDescription = sModel.title,
                            tint = Color(sModel.alertColor),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    is LiveActivityModel.AppTimerWarning -> {
                        Icon(
                            imageVector = Icons.Default.Warning, 
                            contentDescription = "Timer Warning",
                            tint = Color(0xFFFFA500), 
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    is LiveActivityModel.OngoingTask -> {
                        CircularProgressIndicator(
                            progress = { (sModel.progress.toFloat() / sModel.progressMax.toFloat()).coerceIn(0f, 1f) },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is LiveActivityModel.General -> {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notification",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    else -> {} 
                }
            }
        }
    }
}
