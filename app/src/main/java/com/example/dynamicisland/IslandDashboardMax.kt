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
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        
        // --- TOP AREA: Sliders, Media, and L-Ribbon ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { constraints.maxWidth.toFloat() }
            
            val gap = 12.dp
            val qsSize = 58.dp
            
            val mediaBoxWidth = 180.dp
            val mediaBoxHeight = 140.dp
            
            val sliderBoxWidth = with(density) { (constraints.maxWidth.toDp() - mediaBoxWidth - gap - qsSize) }

            // PART 2: Top Left - Sliders (Column)
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(sliderBoxWidth)
                    .height(mediaBoxHeight),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                HorizontalLiquidSlider(
                    value = 75f, 
                    icon = Icons.Rounded.BrightnessMedium, 
                    activeColor = Color(0xFFFACC15)
                ) { onSliderDrag("BRIGHT", it) }
                
                HorizontalLiquidSlider(
                    value = 50f, 
                    icon = Icons.Rounded.VolumeUp, 
                    activeColor = Color(0xFF06B6D4)
                ) { onSliderDrag("VOL", it) }
            }

            // PART 4: Top Right - Media Box 
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(mediaBoxWidth)
                    .height(mediaBoxHeight)
            ) {
                if (currentMedia != null) {
                    MediaControlSquarcle(currentMedia, onMediaCommand)
                } else {
                    EmptyMediaBox()
                }
            }

            // PART 3: The Custom L-Shaped QS Ribbon
            // Calculates path to flow underneath sliders & media, and then up the left side of the media box
            val cornerX = maxWidthPx - with(density) { mediaBoxWidth.toPx() } - with(density) { qsSize.toPx() } - with(density) { gap.toPx() }
            val cornerY = with(density) { mediaBoxHeight.toPx() } + with(density) { gap.toPx() }

            QSSnakeRibbon(
                tiles = dashboardModel.activeTiles,
                cornerX = cornerX,
                cornerY = cornerY,
                tileSize = with(density) { qsSize.toPx() },
                gap = with(density) { gap.toPx() },
                onClick = onQsClick
            )
        }

        // --- PART 1: BOTTOM ROW: App Dock ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2E))) // Divider
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
    cornerX: Float,
    cornerY: Float,
    tileSize: Float,
    gap: Float,
    onClick: (String) -> Unit
) {
    // Generate fallback tiles if not enough are active to demonstrate the ribbon effect
    val activeList = if (tiles.isNotEmpty()) tiles else List(12) { i -> 
        val specs = listOf("wifi", "bt", "dnd", "flashlight", "airplane", "location", "battery", "hotspot")
        QSTileState(specs[i % specs.size], "Tile", i % 2 == 0, false) 
    }
    
    val scrollState = remember { mutableFloatStateOf(0f) }
    val maxScroll = max(0f, (activeList.size * (tileSize + gap)) - (cornerX + 500f)) 

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Both X and Y dragging apply to the shared ribbon pull effect
                    val delta = -(dragAmount.y + dragAmount.x)
                    scrollState.floatValue = (scrollState.floatValue + delta).coerceIn(0f, maxScroll)
                }
            }
    ) {
        val density = LocalDensity.current
        
        activeList.forEachIndexed { index, tile ->
            // Current position of this specific tile along the unbent ribbon
            val d = (index * (tileSize + gap)) - scrollState.floatValue
            
            // Only draw if within bounds
            if (d > -tileSize && d < cornerX + 1000f) {
                val x: Float
                val y: Float
                
                // Determine layout on the L-Shape
                if (d <= cornerX) {
                    // Segment 1: Horizontal part (Moves left to right)
                    x = d
                    y = cornerY
                } else {
                    // Segment 2: Vertical part (Hits the corner and moves UP)
                    x = cornerX
                    y = cornerY - (d - cornerX) 
                }
                
                val activeColor = if (index % 3 == 0) Color(0xFF3B82F6) else if (index % 3 == 1) Color(0xFFF59E0B) else Color(0xFF8B5CF6)
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(x.toInt(), y.toInt()) }
                        .size(with(density) { tileSize.toDp() })
                        .clip(RoundedCornerShape(18.dp)) 
                        .background(if (tile.isActive) activeColor else Color(0xFF1C1C1E))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick(tile.tileSpec) },
                    contentAlignment = Alignment.Center
                ) {
                    // Dynamic Icons for "All Icons" requirement
                    Icon(
                        imageVector = getQSIcon(tile.tileSpec),
                        contentDescription = tile.label, 
                        tint = if (tile.isActive) Color.Black else Color.White, 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Maps System Tile Specs to actual Icons rather than just repeating one stock icon
@Composable
private fun getQSIcon(spec: String): ImageVector {
    return when (spec.lowercase()) {
        "wifi" -> Icons.Rounded.Wifi
        "bt", "bluetooth" -> Icons.Rounded.Bluetooth
        "cell", "mobile" -> Icons.Rounded.Phone
        "dnd" -> Icons.Rounded.DoNotDisturbOn
        "flashlight", "torch" -> Icons.Rounded.FlashlightOn
        "airplane" -> Icons.Rounded.AirplanemodeActive
        "location" -> Icons.Rounded.LocationOn
        "battery" -> Icons.Rounded.BatteryFull
        "rotation" -> Icons.Rounded.ScreenRotation
        "hotspot" -> Icons.Rounded.WifiTethering
        else -> Icons.Rounded.Settings
    }
}

@Composable
private fun HorizontalLiquidSlider(
    value: Float,
    icon: ImageVector,
    activeColor: Color,
    onDrag: (Float) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(value) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val width = size.width.toFloat()
                    val delta = (dragAmount.x / width) * 100f
                    sliderValue = (sliderValue + delta).coerceIn(0f, 100f)
                    onDrag(sliderValue)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Active Fill Bar
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(sliderValue / 100f)
                .background(activeColor)
        )
        // Leading Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (sliderValue > 15f) Color.Black else Color.White,
            modifier = Modifier.padding(start = 16.dp).size(24.dp)
        )
    }
}

@Composable
private fun MediaControlSquarcle(media: LiveActivityModel.Music, onCommand: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E))
    ) {
        if (media.albumArt != null) {
            Image(bitmap = media.albumArt.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.4f))
        }

        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = media.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = media.artist, color = Color(0xFFAAAAAA), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            WavyProgressBar(isPlaying = media.isPlaying)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onCommand("PREV") })
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onCommand("PLAY_PAUSE") }, contentAlignment = Alignment.Center) {
                    Icon(if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onCommand("NEXT") })
            }
        }
    }
}

@Composable
private fun EmptyMediaBox() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFF444446), modifier = Modifier.size(32.dp))
            Text("Not Playing", color = Color(0xFF666668), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun WavyProgressBar(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "wavePhase")

    Canvas(modifier = Modifier.fillMaxWidth().height(14.dp)) {
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
        drawPath(path = path, color = Color.White, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
private fun AppDockZone(apps: List<String>, onClick: (String) -> Unit) {
    val pm = LocalContext.current.packageManager
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        apps.take(5).forEach { pkg ->
            val iconBmp = remember(pkg) { try { pm.getApplicationIcon(pkg).toBitmap(140, 140).asImageBitmap() } catch (e: Exception) { null } }
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF1C1C1E)).clickable { onClick(pkg) }, contentAlignment = Alignment.Center) {
                if (iconBmp != null) Image(bitmap = iconBmp, contentDescription = pkg, modifier = Modifier.fillMaxSize())
                else Text(pkg.take(1).uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
