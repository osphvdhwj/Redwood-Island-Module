package com.example.dynamicisland.core.ui.design

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
@Composable
fun IslandPreviewCard(
    modifier: Modifier = Modifier,
    liveWidth: Float = 0f,
    liveHeight: Float = 0f,
    liveX: Float = 0f,
    liveY: Float = 0f,
    liveRadius: Float = 100f,
    liveRingT: Float = 6f,
    isLivePreview: Boolean = false,
    previewState: String = ""
) {
    val states = listOf("Music", "Call", "Charging")
    var currentState by remember { mutableStateOf(states[0]) }
    
    // Auto-cycling for non-live mode
    LaunchedEffect(isLivePreview) {
        if (!isLivePreview) {
            while (true) {
                kotlinx.coroutines.delay(4000)
                val currentIndex = states.indexOf(currentState)
                currentState = states[(currentIndex + 1) % states.size]
            }
        }
    }
    // Determine target dimensions
    val targetWidth = if (isLivePreview) liveWidth.dp else when (previewState) {
        "call" -> 160.dp
        "charging" -> 190.dp
        "music" -> 220.dp
        else -> when (currentState) {
            "Music" -> 220.dp
            "Call" -> 160.dp
            "Charging" -> 190.dp
            else -> 120.dp
    val targetHeight = if (isLivePreview) liveHeight.dp else when (previewState) {
        "call" -> 40.dp
        "charging" -> 44.dp
        "music" -> 44.dp
        else -> 44.dp
    val pillWidth by animateDpAsState(targetWidth, label = "w")
    val pillHeight by animateDpAsState(targetHeight, label = "h")
    val pillRadius by animateDpAsState(if (isLivePreview) liveRadius.dp else 100.dp, label = "r")
    val pillOffsetX by animateDpAsState(if (isLivePreview) liveX.dp else 0.dp, label = "x")
    val pillOffsetY by animateDpAsState(if (isLivePreview) liveY.dp else 32.dp, label = "y")
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1C1E), Color.Black)), RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        ) {
            // --- Screen Simulation ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Punch hole
                drawCircle(Color.Black, radius = 8.dp.toPx(), center = Offset(size.width / 2f, 32.dp.toPx()))
                
                // Content Boundaries
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
                    topLeft = Offset(16.dp.toPx(), 16.dp.toPx()),
                    size = Size(size.width - 32.dp.toPx(), size.height - 32.dp.toPx()),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            // --- The Island Pill ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = pillOffsetX, y = pillOffsetY)
                    .width(pillWidth)
                    .height(pillHeight)
                    .clip(RoundedCornerShape(pillRadius))
                    .background(if (isLivePreview && previewState == "ring") Color.Transparent else Color.Black)
                    .then(
                        if (isLivePreview) {
                            if (previewState == "ring") {
                                Modifier.border(liveRingT.dp, IslandColors.accentCyan, RoundedCornerShape(pillRadius))
                            } else {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(pillRadius))
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Mock Content based on state
                Crossfade(targetState = if (isLivePreview) previewState else currentState, label = "content") { state ->
                    when (state.lowercase()) {
                        "music" -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                Icon(Icons.Default.MusicNote, null, tint = IslandColors.accentCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Now Playing", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("Redwood Audio Engine", color = Color.Gray, fontSize = 8.sp)
                                }
                        }
                        "call" -> {
                                Icon(Icons.Default.Call, null, tint = Color.Green, modifier = Modifier.size(18.dp))
                                Text("04:20", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        "charging" -> {
                                Icon(Icons.Default.BatteryChargingFull, null, tint = Color.Green, modifier = Modifier.size(18.dp))
                                Text("88%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
}
