package com.example.dynamicisland.ui

import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.pow

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
            val logicalIcon = if (charging.isPluggedIn) IconProvider.LogicalIcon.BATTERY_CHARGING else if (isLow) IconProvider.LogicalIcon.BATTERY_LOW else IconProvider.LogicalIcon.BATTERY_FULL
            Icon(
                imageVector = IconProvider.getIcon(logicalIcon, LocalIconPack.current),
                contentDescription = null,
                tint = batteryColor,
                modifier = Modifier.size(theme.batIconSize * 0.65f)
            )
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
            com.example.dynamicisland.settings.ChargingStyle.RING -> {
                val spinAngle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart), label = "spin")
                if (model.isPluggedIn || isLow) { 
                    Box(modifier = Modifier.size(54.dp).graphicsLayer { rotationZ = spinAngle }) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val brush = Brush.sweepGradient(0f to Color.Transparent, 0.5f to color.copy(alpha=0.5f), 1f to Color.Transparent)
                            drawCircle(brush = brush, radius = size.width/2, style = androidx.compose.ui.graphics.drawscope.Stroke(12f))
                        }
                    }
                }
            }
            com.example.dynamicisland.settings.ChargingStyle.WAVE -> {
                val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "wave")
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = androidx.compose.ui.graphics.Path()
                    val h = size.height
                    val w = size.width
                    path.moveTo(0f, h)
                    for (i in 0..w.toInt()) {
                        val y = h - (model.level / 100f * h) + sin(i.toFloat() / w * 2 * PI.toFloat() + waveOffset * 2 * PI.toFloat()) * 5f
                        path.lineTo(i.toFloat(), y)
                    }
                    path.lineTo(w, h)
                    path.close()
                    drawPath(path, color.copy(alpha = 0.3f))
                }
            }
            com.example.dynamicisland.settings.ChargingStyle.CUBE -> {
                // Classic solid block pulse
                val pulseScale by infiniteTransition.animateFloat(initialValue = 0.9f, targetValue = 1.1f, animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
                Box(modifier = Modifier.size(40.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }.background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(2.dp, color, RoundedCornerShape(12.dp)))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val logicalIcon = if (model.isPluggedIn) IconProvider.LogicalIcon.BATTERY_CHARGING else if (isLow) IconProvider.LogicalIcon.BATTERY_LOW else IconProvider.LogicalIcon.BATTERY_FULL
            Icon(imageVector = IconProvider.getIcon(logicalIcon, LocalIconPack.current), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = "${model.level}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun DynamicIslandView.ChargingMax(charging: LiveActivityModel.Charging) {
    val batteryColor = Color(0xFF00FF55)
    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alphaAnim.animateTo(1f, animationSpec = tween(200))
            scaleAnim.animateTo(1f, animationSpec = spring(dampingRatio = 0.55f, stiffness = 350f))
        }
    }

    var displayedLevel by remember { mutableIntStateOf(0) }
    LaunchedEffect(charging.level) {
        kotlinx.coroutines.delay(200)
        val target = charging.level
        val duration = 800L
        val startTime = System.currentTimeMillis()
        while (displayedLevel < target) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val eased = 1f - (1f - progress).pow(3)
            displayedLevel = (eased * target).toInt()
            kotlinx.coroutines.delay(16)
        }
        displayedLevel = target
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
                    imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.BATTERY_CHARGING, LocalIconPack.current),
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${displayedLevel}%",
                    color = Color.White,
                    fontSize = 48.sp, 
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.alpha(alphaAnim.value)
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
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    )
                }
                clipPath(path) {
                    drawRect(color = color, topLeft = androidx.compose.ui.geometry.Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width * animatedFill, size.height))
                }
            }
            if (isCharging) {
                Icon(imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.ADD, LocalIconPack.current), contentDescription=null, tint=Color.Black.copy(alpha=0.6f), modifier = Modifier.align(Alignment.Center).size(16.dp))
            }
        }
        Box(modifier = Modifier.width(4.dp).height(10.dp).background(Color.White.copy(alpha=0.3f), RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp)))
    }
}
