package com.example.dynamicisland

import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.max
import kotlin.math.sin

@Composable
fun IslandDashboardMax(
    dashboardModel: LiveActivityModel.Dashboard,
    currentMedia: LiveActivityModel.Music?,
    onSliderDrag: (String, Float) -> Unit,
    onQsClick: (String) -> Unit,
    onMediaCommand: (String) -> Unit = {},
    onAppClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // --- TOP AREA: The L-Ribbon, Sliders, and Media Box ---
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(210.dp)) {
            val totalWidthPx = constraints.maxWidth.toFloat()
            val totalHeightPx = constraints.maxHeight.toFloat()
            val density = LocalDensity.current
            
            val sliderWidth = 52.dp
            val gap = 8.dp
            val qsSize = 60.dp
            
            // 1. Sliders (Top Left)
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(130.dp), // Leaves room for the bottom horizontal ribbon
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                VerticalLiquidSlider(value = 75f, iconRes = android.R.drawable.ic_menu_day, activeColor = Color(0xFFFFD700)) { onSliderDrag("BRIGHT", it) }
                VerticalLiquidSlider(value = 50f, iconRes = android.R.drawable.ic_lock_silent_mode_off, activeColor = Color(0xFF00FFFF)) { onSliderDrag("VOL", it) }
            }

            // 2. Media Box (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(with(density) { (totalWidthPx - (sliderWidth.toPx() * 2) - (qsSize.toPx()) - (gap.toPx() * 3)).toDp() })
                    .height(130.dp)
            ) {
                if (currentMedia != null) {
                    MediaControlSquarcle(currentMedia, onMediaCommand)
                } else {
                    EmptyMediaBox()
                }
            }

            // 3. The Custom Snake Ribbon (L-Shape wrapped around Media Box)
            // It sits precisely between the Sliders and Media Box, and under the Media Box.
            val snakeStartX = with(density) { (sliderWidth.toPx() * 2) + (gap.toPx() * 2) }
            QSSnakeRibbon(
                tiles = dashboardModel.activeTiles,
                startX = snakeStartX,
                cornerY = with(density) { 130.dp.toPx() + gap.toPx() }, // Right below the media box
                tileSize = with(density) { qsSize.toPx() },
                gap = with(density) { gap.toPx() },
                onClick = onQsClick
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- BOTTOM ROW: Dock ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222222)))
            Spacer(modifier = Modifier.height(12.dp))
            AppDockZone(
                apps = dashboardModel.pinnedApps.ifEmpty { listOf("com.android.settings", "com.android.chrome", "com.google.android.youtube", "com.google.android.dialer") }, 
                onClick = onAppClick
            )
        }
    }
}

@Composable
private fun QSSnakeRibbon(
    tiles: List<QSTileState>,
    startX: Float,
    cornerY: Float,
    tileSize: Float,
    gap: Float,
    onClick: (String) -> Unit
) {
    // Generate an extended list of mock tiles if the real ones aren't provided
    val activeList = if (tiles.size > 8) tiles else List(12) { i -> QSTileState("QS_$i", "Tile", i % 2 == 0, false) }
    
    // Physics State for the belt
    val scrollState = remember { mutableFloatStateOf(0f) }
    
    // Math: The total length of the path before you run out of icons
    val maxScroll = max(0f, (activeList.size * (tileSize + gap)) - (cornerY + 500f)) 

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Swiping UP (-y) moves items up (increases scroll). 
                    // Swiping LEFT (-x) pulls items around the corner (increases scroll).
                    val delta = -(dragAmount.y + dragAmount.x)
                    scrollState.floatValue = (scrollState.floatValue + delta).coerceIn(0f, maxScroll)
                }
            }
    ) {
        val density = LocalDensity.current
        
        activeList.forEachIndexed { index, tile ->
            // Calculate absolute distance of this tile along the belt
            val d = (index * (tileSize + gap)) - scrollState.floatValue
            
            // Only draw it if it's visible on the path
            if (d > -tileSize && d < cornerY + 1000f) {
                // THE SNAKE ROUTING MATH
                val x: Float
                val y: Float
                
                if (d <= cornerY) {
                    // Vertical segment (falling down)
                    x = startX
                    y = d
                } else {
                    // It hit the corner. Turn 90 degrees right!
                    x = startX + (d - cornerY)
                    y = cornerY
                }
                
                // Colors for mock demonstration
                val activeColor = if (index % 3 == 0) Color(0xFF0082FC) else if (index % 3 == 1) Color.Yellow else Color(0xFF8A2BE2)
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .size(with(density) { tileSize.toDp() })
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (tile.isActive) activeColor else Color(0xFF141414))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick(tile.tileSpec) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera), // Placeholder icon
                        contentDescription = null, 
                        tint = if (tile.isActive) Color.Black else Color.White, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaControlSquarcle(media: LiveActivityModel.Music, onCommand: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414))
    ) {
        if (media.albumArt != null) {
            Image(bitmap = media.albumArt.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.3f))
        }

        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = media.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = media.artist, color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            WavyProgressBar(isPlaying = media.isPlaying)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(26.dp).clickable { onCommand("PREV") })
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White).clickable { onCommand("PLAY_PAUSE") }, contentAlignment = Alignment.Center) {
                    Icon(if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(26.dp).clickable { onCommand("NEXT") })
            }
        }
    }
}

@Composable
private fun EmptyMediaBox() {
    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFF141414)), contentAlignment = Alignment.Center) {
        Text("Not Playing", color = Color(0xFF444444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WavyProgressBar(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "wavePhase")

    Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
        val path = Path()
        val waveWidth = size.width
        val waveHeight = size.height
        val frequency = 0.08f

        path.moveTo(0f, waveHeight / 2)
        for (x in 0 until waveWidth.toInt() step 4) {
            val amplitude = if (isPlaying) (waveHeight / 3) else 0.5f
            val y = (sin((x * frequency) + phase) * amplitude) + (waveHeight / 2)
            path.lineTo(x.toFloat(), y.toFloat())
        }
        drawPath(path = path, color = Color.White, style = Stroke(width = 3f))
    }
}

@Composable
private fun AppDockZone(apps: List<String>, onClick: (String) -> Unit) {
    val pm = LocalContext.current.packageManager
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        apps.take(5).forEach { pkg ->
            val iconBmp = remember(pkg) { try { pm.getApplicationIcon(pkg).toBitmap(120, 120).asImageBitmap() } catch (e: Exception) { null } }
            Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF141414)).clickable { onClick(pkg) }, contentAlignment = Alignment.Center) {
                if (iconBmp != null) Image(bitmap = iconBmp, contentDescription = pkg, modifier = Modifier.fillMaxSize())
                else Text(pkg.take(1).uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
