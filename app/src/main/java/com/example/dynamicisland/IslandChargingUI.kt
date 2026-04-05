package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
    val theme = LocalIslandTheme.current
    val isLow = charging.level <= 20 && !charging.isPluggedIn
    val batteryColor = if (charging.isPluggedIn) Color(0xFF00FF55) else if (isLow) Color(0xFFFF0033) else Color.White
    
    val infiniteTransition = rememberInfiniteTransition(label = "charging_glow")
    val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 0.8f, animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")

    AlertMidSlot(
        islandState = islandState.value,
        iconContent = {
            Box(modifier = Modifier.fillMaxSize().background(batteryColor.copy(alpha = if (charging.isPluggedIn || isLow) glowAlpha else 0.1f), CircleShape).blur(if (charging.isPluggedIn || isLow) 12.dp else 0.dp))
            Icon(imageVector = if (charging.isPluggedIn) Icons.Default.Add else if (isLow) Icons.Default.Warning else Icons.Default.BatteryFull, contentDescription = null, tint = batteryColor, modifier = Modifier.size(theme.batIconSize * 0.65f))
        },
        title = if (charging.isPluggedIn) "Charging" else if (isLow) "Low Battery" else "Battery",
        titleColor = if (isLow) batteryColor else Color.White,
        subtitle = "${charging.level}% Remaining",
        subtitleColor = Color.White.copy(alpha=0.7f),
        rightContent = { LiquidBatteryCanvas(level = charging.level, color = batteryColor, isCharging = charging.isPluggedIn) }
    )
}

@Composable
fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
    val theme = LocalIslandTheme.current
    val isLow = model.level <= 20 && !model.isPluggedIn
    val color = if (model.isPluggedIn) Color(0xFF00FF55) else if (isLow) Color(0xFFFF0033) else Color.White
    val infiniteTransition = rememberInfiniteTransition(label = "anim")
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (theme.chargingStyle) {
            "APPLE" -> {
                val fillProgress by animateFloatAsState(targetValue = model.level / 100f, animationSpec = tween(1500), label = "fill")
                Canvas(modifier = Modifier.size(54.dp)) {
                    drawArc(color = Color.White.copy(alpha=0.1f), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(10f))
                    drawArc(color = color, startAngle = -90f, sweepAngle = 360f * fillProgress, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(10f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }
            "HYPEROS" -> {
                val glowAlpha by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 0.6f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "glow")
                Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp).background(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = glowAlpha)))))
            }
            else -> {
                val pulseScale by infiniteTransition.animateFloat(initialValue = 0.85f, targetValue = 1.15f, animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "scale")
                val spinAngle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "spin")
                if (model.isPluggedIn || isLow) { 
                    Box(modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = spinAngle; scaleX = pulseScale; scaleY = pulseScale }) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val brush = Brush.sweepGradient(0f to Color.Transparent, 0.5f to color.copy(alpha=0.5f), 1f to Color.Transparent)
                            drawCircle(brush = brush, radius = size.width/2, style = androidx.compose.ui.graphics.drawscope.Stroke(12f))
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val icon = if (model.isPluggedIn) Icons.Default.Add else if (isLow) Icons.Default.Warning else Icons.Default.BatteryFull
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = "${model.level}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// 🚀 NEW: The Massive iOS-Style Charging Expansion
@Composable
fun DynamicIslandView.ChargingMax(charging: LiveActivityModel.Charging) {
    val batteryColor = Color(0xFF00FF55)
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)) }
        launch { alphaAnim.animateTo(1f, animationSpec = tween(300)) }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(30.dp)
                .background(Brush.radialGradient(listOf(batteryColor.copy(alpha = 0.2f), Color.Transparent)))
        )

        Row(
            modifier = Modifier.fillMaxWidth().scale(scaleAnim.value),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(48.dp).background(batteryColor.copy(alpha = 0.2f), CircleShape).blur(12.dp))
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${charging.level}%",
                    color = Color.White,
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.graphicsLayer { alpha = alphaAnim.value }
                )
                Text(
                    text = "Charging",
                    color = batteryColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            Box(modifier = Modifier.scale(1.3f)) { 
                LiquidBatteryCanvas(level = charging.level, color = batteryColor, isCharging = true)
            }
        }
    }
}

@Composable
fun LiquidBatteryCanvas(level: Int, color: Color, isCharging: Boolean) {
    val targetFill = level / 100f
    val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(46.dp).height(24.dp).border(2.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(6.dp)).padding(3.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val clipPath = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))) }
                clipPath(clipPath) {
                    drawRect(color = color, topLeft = Offset.Zero, size = Size(size.width * animatedFill, size.height))
                }
            }
            if (isCharging) {
                Icon(Icons.Default.Add, contentDescription=null, tint=Color.Black.copy(alpha=0.6f), modifier = Modifier.align(Alignment.Center).size(16.dp))
            }
        }
        Box(modifier = Modifier.width(4.dp).height(10.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)))
    }
}
