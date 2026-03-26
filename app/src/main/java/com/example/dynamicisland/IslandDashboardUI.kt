package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
    val theme = LocalIslandTheme.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 🎛️ FIXED: State Decoupling to prevent Auto-Brightness & Volume Jitter
    var isVolumeDragging by remember { mutableStateOf(false) }
    var localVolume by remember { mutableFloatStateOf(0f) }
    val displayVolume = if (isVolumeDragging) localVolume else (hardwareVolume.intValue / 100f)

    var isBrightnessDragging by remember { mutableStateOf(false) }
    var localBrightness by remember { mutableFloatStateOf(0f) }
    val displayBrightness = if (isBrightnessDragging) localBrightness else (hardwareBrightness.intValue / 100f)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        
        // 1. Pinned Apps (Adaptive Grid)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().height(70.dp)
        ) {
            items(4) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val pkg = pinnedApps.getOrNull(index)
                            if (!pkg.isNullOrEmpty()) onGestureEvent?.invoke(IslandGesture.valueOf("LAUNCH_APP_$pkg"))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White.copy(alpha=0.5f), modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 2. The Liquid Sliders
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Brightness Slider
            LiquidSlider(
                modifier = Modifier.weight(1f),
                icon = if (hardwareAutoBrightness.value) Icons.Default.BrightnessAuto else Icons.Default.BrightnessHigh,
                progress = displayBrightness,
                activeColor = Color.White,
                onDragStart = { 
                    isBrightnessDragging = true 
                    localBrightness = hardwareBrightness.intValue / 100f
                },
                onDrag = { delta ->
                    localBrightness = (localBrightness + delta).coerceIn(0f, 1f)
                    onBrightnessDrag?.invoke((localBrightness * 100).toInt())
                },
                onDragEnd = { isBrightnessDragging = false }
            )

            // Volume Slider
            LiquidSlider(
                modifier = Modifier.weight(1f),
                icon = if (hardwareVolume.intValue == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                progress = displayVolume,
                activeColor = Color.White,
                onDragStart = { 
                    isVolumeDragging = true 
                    localVolume = hardwareVolume.intValue / 100f
                },
                onDrag = { delta ->
                    localVolume = (localVolume + delta).coerceIn(0f, 1f)
                    onVolumeDrag?.invoke((localVolume * 100).toInt())
                },
                onDragEnd = { isVolumeDragging = false }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. Quick Settings (Adaptive Grid)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val toggles = listOf(
                Pair(Icons.Default.Wifi, "WiFi"),
                Pair(Icons.Default.Bluetooth, "BT"),
                Pair(Icons.Default.FlashlightOn, "Torch"),
                Pair(Icons.Default.Settings, "Settings")
            )
            
            toggles.forEach { toggle ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when(toggle.second) {
                        "Settings" -> onGestureEvent?.invoke(IslandGesture.valueOf("LAUNCH_SETTINGS"))
                        // Add other toggle intents here
                    }
                }) {
                    Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha=0.15f)), contentAlignment = Alignment.Center) {
                        Icon(toggle.first, contentDescription = toggle.second, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = toggle.second, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// 🎛️ PREMIUM: The Liquid Slider Engine
@Composable
fun LiquidSlider(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    progress: Float,
    activeColor: Color,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    
    // Physics: Slider dynamically expands when grabbed
    val height by animateDpAsState(targetValue = if (isDragging) 48.dp else 36.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "height")
    val corner by animateDpAsState(targetValue = if (isDragging) 24.dp else 18.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "corner")

    // Haptics Engine: Fire a tick every 5% change
    var lastHapticTick by remember { mutableIntStateOf((progress * 20).toInt()) }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(Color.White.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart() 
                    },
                    onDragEnd = { 
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDragEnd() 
                    },
                    onDragCancel = { 
                        isDragging = false
                        onDragEnd() 
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Calculate precise delta based on the physical width of the slider
                    val delta = dragAmount.x / size.width.toFloat()
                    
                    // Rubberband Resistance Math
                    val resistedDelta = if (progress <= 0f && delta < 0) delta * 0.2f 
                                        else if (progress >= 1f && delta > 0) delta * 0.2f 
                                        else delta
                                        
                    onDrag(resistedDelta)

                    // Multi-Frequency Haptics
                    val currentTick = (progress * 20).toInt()
                    if (currentTick != lastHapticTick) {
                        if (currentTick == 0 || currentTick == 20) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Heavy boundary pulse
                        } else {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Light travel tick
                        }
                        lastHapticTick = currentTick
                    }
                }
            }
    ) {
        // The Liquid Fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0.02f, 1f))
                .background(activeColor)
        )
        
        // The Icon
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (progress > 0.15f) Color.Black else Color.White, 
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 12.dp).size(20.dp)
        )
    }
}

// 📱 Dashboard Mid (Quick Toggles)
@Composable
fun DynamicIslandView.DashboardMid(model: LiveActivityModel.Dashboard) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val toggles = listOf(Icons.Default.Wifi, Icons.Default.Bluetooth, Icons.Default.FlashlightOn, Icons.Default.Settings)
        
        toggles.forEach { icon ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Trigger specific intent based on icon
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
