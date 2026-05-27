package com.example.dynamicisland.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun IslandPreviewCard(
    modifier: Modifier = Modifier,
    liveWidth: Float = 0f,
    liveHeight: Float = 0f,
    liveX: Float = 0f,
    liveY: Float = 0f,
    liveRingT: Float = 6f,
    isLivePreview: Boolean = false,
    previewState: String = "",
    expandUpwards: Boolean = false
) {
    val states = listOf("Music", "Call", "Charging")
    var currentState by remember { mutableStateOf(states[0]) }
    var isAutoPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(isAutoPlaying, isLivePreview) {
        if (isAutoPlaying && !isLivePreview) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val currentIndex = states.indexOf(currentState)
                val nextIndex = (currentIndex + 1) % states.size
                currentState = states[nextIndex]
            }
        }
    }

    val pillWidth by animateDpAsState(
        targetValue = if (isLivePreview) liveWidth.dp else when (currentState) {
            "Music" -> 220.dp
            "Call" -> 160.dp
            "Charging" -> 190.dp
            else -> 120.dp
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillWidth"
    )

    val pillHeight by animateDpAsState(
        targetValue = if (isLivePreview) liveHeight.dp else when (currentState) {
            "Music" -> 44.dp
            "Call" -> 40.dp
            "Charging" -> 44.dp
            else -> 40.dp
        },
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillHeight"
    )

    val pillOffsetX by animateDpAsState(
        targetValue = if (isLivePreview) liveX.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillOffsetX"
    )

    val pillOffsetY by animateDpAsState(
        targetValue = if (isLivePreview) liveY.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "pillOffsetY"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // 200.dp Canvas Preview Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF0A1628), Color.Black)))
                .border(1.dp, IslandColors.border, RoundedCornerShape(24.dp))
        ) {
            // Background Canvas (Phone Silhouette or 1:1 Screen Preview)
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isLivePreview) {
                    // 1:1 Scale Screen Preview
                    val punchHoleRadius = 8.dp.toPx()
                    // Typical centered punch-hole camera position
                    val punchHoleY = 32.dp.toPx()
                    
                    // Draw a simulated camera cutout (punch hole)
                    drawCircle(
                        color = Color.Black,
                        radius = punchHoleRadius,
                        center = Offset(size.width / 2f, punchHoleY)
                    )
                    
                    // Subtle inner shadow to make the punch hole look like hardware
                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = punchHoleRadius,
                        center = Offset(size.width / 2f, punchHoleY),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Draw a subtle border to represent the edges of the device screen
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.5f),
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        style = Stroke(width = 6.dp.toPx())
                    )
                } else {
                    // Miniature Realistic Phone Silhouette
                    val phoneW = 260.dp.toPx()
                    val phoneH = 400.dp.toPx()
                    val phoneT = 24.dp.toPx()
                    val cr = 36.dp.toPx()

                    // Outer metallic bezel
                    drawRoundRect(
                        color = Color(0xFF222222),
                        topLeft = Offset(size.width / 2 - phoneW / 2, phoneT),
                        size = Size(phoneW, phoneH),
                        cornerRadius = CornerRadius(cr, cr),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    
                    // Inner screen boundary
                    drawRoundRect(
                        color = Color(0xFF0D0D0D),
                        topLeft = Offset(size.width / 2 - phoneW / 2 + 10f, phoneT + 10f),
                        size = Size(phoneW - 20f, phoneH - 20f),
                        cornerRadius = CornerRadius(cr - 5f, cr - 5f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            if (!isLivePreview) {
                // Infinite Pulsing Ambient Background Ring
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseScale"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseAlpha"
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = pillOffsetX, y = pillOffsetY)
                        .width(pillWidth)
                        .height(pillHeight)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                        .border(2.dp, IslandColors.accentCyan, RoundedCornerShape(50))
                )
            }

            val alignment = if (expandUpwards) Alignment.BottomCenter else Alignment.TopCenter

            // Dynamic Island Pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = pillOffsetX, y = pillOffsetY)
                    .width(pillWidth)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(50))
                    .background(if (isLivePreview && previewState == "ring") Color.Transparent else Color.Black)
                    .then(
                        if (isLivePreview && previewState == "ring") {
                            Modifier.border(liveRingT.dp, IslandColors.accentCyan, RoundedCornerShape(50))
                        } else Modifier
                    )
                    .padding(horizontal = if (isLivePreview) 0.dp else 12.dp),
                contentAlignment = alignment
            ) {
                if (!isLivePreview) {
                    // Real content rendering swaps
                    Crossfade(targetState = currentState, label = "pillContent") { state ->
                    when (state) {
                        "Music" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Brush.linearGradient(listOf(IslandColors.accentPurple, IslandColors.accentCyan))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cyber Track", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                // Simulated Equalizer Visualizer
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(3.dp, 12.dp).background(IslandColors.accentCyan, CircleShape))
                                    Box(modifier = Modifier.size(3.dp, 18.dp).background(IslandColors.accentPurple, CircleShape))
                                    Box(modifier = Modifier.size(3.dp, 8.dp).background(IslandColors.accentCyan, CircleShape))
                                }
                            }
                        }
                        "Call" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("0:42", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF44336)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 135f })
                                }
                            }
                        }
                        "Charging" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = IslandColors.accentCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("85% Super Charge", color = IslandColors.accentCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        if (!isLivePreview) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Horizontal LazyRow of state selector chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(states) { state ->
                    val isSelected = currentState == state
                    val chipBg = if (isSelected) IslandColors.accentCyan.copy(alpha = 0.15f) else IslandColors.surface
                    val chipBorder = if (isSelected) IslandColors.accentCyan else IslandColors.border
                    val chipTextColor = if (isSelected) IslandColors.accentCyan else IslandColors.textSecondary
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(50))
                            .clickable { 
                                currentState = state 
                                isAutoPlaying = false
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state,
                            color = chipTextColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
