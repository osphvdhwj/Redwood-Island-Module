package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppAppMD3Theme
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
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
            com.example.dynamicisland.shared.settings.ChargingStyle.RING -> {
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
            com.example.dynamicisland.shared.settings.ChargingStyle.WAVE -> {
                val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "wave")
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = androidx.compose.ui.graphics.Path()
                    val h = size.height
                    val w = size.width
                    path.moveTo(0f, h)
                    for (i in 0..w.toInt()) {
                        val y = h - (model.level / 100f * h) + sin((i.toFloat() / w * 2 * PI.toFloat() + waveOffset * 2 * PI.toFloat()).toDouble()).toFloat() * 5f
                        path.lineTo(i.toFloat(), y)
                    }
                    path.lineTo(w, h)
                    path.close()
                    drawPath(path, color.copy(alpha = 0.3f))
                }
            }
            com.example.dynamicisland.shared.settings.ChargingStyle.CUBE -> {
                com.example.dynamicisland.core.ui.components.NeuralCubeUI(color = color, settings = controller?.settingsState ?: com.example.dynamicisland.shared.settings.SettingsState(), modifier = Modifier.size(40.dp))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val logicalIcon = if (model.isPluggedIn) IconProvider.LogicalIcon.BATTERY_CHARGING else if (isLow) IconProvider.LogicalIcon.BATTERY_LOW else IconProvider.LogicalIcon.BATTERY_FULL
            Icon(imageVector = IconProvider.getIcon(logicalIcon, LocalIconPack.current), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            RollingNumberText(value = "${model.level}%", style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
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
            val eased = 1f - (1f - progress).pow(3f)
            displayedLevel = (eased * target).toInt()
            kotlinx.coroutines.delay(16)
        }
        displayedLevel = target
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Aesthetic Radial Glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .background(Brush.radialGradient(listOf(batteryColor.copy(alpha = 0.25f), Color.Transparent)))
        )

        Row(
            modifier = Modifier.fillMaxWidth().scale(scaleAnim.value),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Animated Pulse Icon
            Box(contentAlignment = Alignment.Center) {
                val infiniteGlow = rememberInfiniteTransition(label = "bg").animateFloat(0.1f, 0.4f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "g")
                Box(modifier = Modifier.size(64.dp).background(batteryColor.copy(alpha = infiniteGlow.value), CircleShape).blur(20.dp))
                Icon(
                    imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.BATTERY_CHARGING, LocalIconPack.current),
                    contentDescription = "Charging",
                    tint = batteryColor,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Center: Large Rolling Percentage
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingNumberText(
                    value = "${displayedLevel}",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 56.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    ),
                    modifier = Modifier.alpha(alphaAnim.value)
                )
                Text(
                    text = "PERCENT CHARGED",
                    color = batteryColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // Right: High-Fidelity Liquid Battery
            Box(modifier = Modifier.scale(1.4f)) { 
                LiquidBatteryCanvas(level = charging.level, color = batteryColor, isCharging = true, useWarpEffect = true)
            }
        }
    }
}

@Composable
fun LiquidBatteryCanvas(level: Int, color: Color, isCharging: Boolean, useWarpEffect: Boolean = false) {
    val targetFill = level / 100f
    val animatedFill by animateFloatAsState(targetValue = targetFill, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "fill")
    
    val infiniteTransition = rememberInfiniteTransition(label = "liquid")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if(useWarpEffect) 1200 else 2500, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(48.dp).height(26.dp).border(2.5.dp, Color.White.copy(alpha=0.35f), RoundedCornerShape(7.dp)).padding(3.5.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    )
                }
                clipPath(path) {
                    // Liquid Wave Logic
                    val wPath = androidx.compose.ui.graphics.Path()
                    val h = size.height
                    val w = size.width
                    val fillW = w * animatedFill
                    
                    wPath.moveTo(0f, h)
                    for (i in 0..fillW.toInt()) {
                        val waveHeight = if(useWarpEffect) 4f else 2f
                        val y = h - (h * (i.toFloat() / fillW).coerceIn(0f, 1f) * 0.05f) - (h * 0.95f * animatedFill) + 
                                sin((i.toFloat() / 15f + waveOffset * 2 * PI.toFloat()).toDouble()).toFloat() * waveHeight
                        wPath.lineTo(i.toFloat(), y)
                    }
                    wPath.lineTo(fillW, h)
                    wPath.close()
                    
                    drawPath(
                        path = wPath,
                        brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.7f)))
                    )
                }
            }
            if (isCharging) {
                Icon(
                    imageVector = IconProvider.getIcon(IconProvider.LogicalIcon.ADD, LocalIconPack.current), 
                    contentDescription=null, 
                    tint=Color.Black.copy(alpha=0.5f), 
                    modifier = Modifier.align(Alignment.Center).size(15.dp)
                )
            }
        }
        Box(modifier = Modifier.width(4.5.dp).height(11.dp).background(Color.White.copy(alpha=0.35f), RoundedCornerShape(topEnd = 3.5.dp, bottomEnd = 3.5.dp)))
    }
}
