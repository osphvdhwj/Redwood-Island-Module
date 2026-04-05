package com.example.dynamicisland

import android.graphics.Bitmap
import android.media.AudioManager
import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.abs

@OptIn(FlowPreview::class)
@Composable
fun DynamicIslandView.IslandDashboardMax(
    dashboardModel: LiveActivityModel.Dashboard,
    currentMedia: LiveActivityModel.Music?,
    onSliderDrag: (String, Float) -> Unit,
    onQsClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // --- STATE MANAGEMENT ---
    var isVisible by remember { mutableStateOf(false) }
    var overscrollDelta by remember { mutableFloatStateOf(0f) }
    
    // Staggered Animation States
    val headerAlpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(400, delayMillis = 50), label = "h_alpha")
    val coreOffset by animateFloatAsState(if (isVisible) 0f else 50f, spring(dampingRatio = 0.7f, stiffness = 300f), label = "c_offset")
    val dockAlpha by animateFloatAsState(if (isVisible) 1f else 0f, tween(400, delayMillis = 150), label = "d_alpha")

    // Hardware States mirrored locally for Zero-Jank slider updates
    var localVol by remember(hardwareVolume.intValue) { mutableFloatStateOf(hardwareVolume.intValue.toFloat()) }
    var localBrt by remember(hardwareBrightness.intValue) { mutableFloatStateOf(hardwareBrightness.intValue.toFloat()) }

    LaunchedEffect(Unit) {
        isVisible = true
        // Local State Mirroring Debounce (Zero-Jank Threading)
        launch(Dispatchers.IO) {
            snapshotFlow { localVol }.debounce(32).collect { withContext(Dispatchers.Main) { onSliderDrag("VOL", it) } }
        }
        launch(Dispatchers.IO) {
            snapshotFlow { localBrt }.debounce(32).collect { withContext(Dispatchers.Main) { onSliderDrag("BRT", it) } }
        }
    }

    // --- LAYER 0: VISUAL ARCHITECTURE (Frosted Spatial Depth) ---
    val dominantColor = currentMedia?.dominantColor?.let { Color(it) } ?: Color.DarkGray
    val bgBrush = Brush.radialGradient(
        colors = listOf(dominantColor.copy(alpha = 0.2f), Color(0xFF080808)),
        radius = 1200f
    )
    
    // The Elastic Pull Scale
    val bgScale = 1f + (overscrollDelta / 2000f).coerceIn(0f, 0.1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = bgScale; scaleY = bgScale }
            .background(bgBrush)
            .pointerInput(Unit) {
                // The Elastic Pull Gesture
                detectVerticalDragGestures(
                    onDragEnd = { overscrollDelta = 0f },
                    onVerticalDrag = { change, dragAmount -> 
                        if (dragAmount > 0f) overscrollDelta += dragAmount 
                    }
                )
            }
            .windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- ZONE A: STATUS HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = headerAlpha },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                DashboardClock()
                HealthStack()
            }

            // --- ZONE B: ACTION CORE ---
            Column(modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = coreOffset }) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Media Hero
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MediaHeroSquarcle(currentMedia)
                    }
                    
                    // Right: Liquid Sliders
                    Row(
                        modifier = Modifier.width(136.dp).fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LiquidSlider(
                            icon = Icons.Default.VolumeUp,
                            value = localVol,
                            color = Color.White,
                            onValueChange = { localVol = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                        )
                        LiquidSlider(
                            icon = Icons.Default.LightMode,
                            value = localBrt,
                            color = Color(0xFFFFD700),
                            onValueChange = { localBrt = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Utility Grid (2x2)
                UtilityGrid(dashboardModel.activeTiles, onQsClick, haptic)
            }

            // --- ZONE C: PRODUCTIVITY DOCK ---
            Box(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = dockAlpha }) {
                ProductivityDock(dashboardModel.pinnedApps)
            }
        }
    }
}

// =========================================================================================
// LAYER 1 & 2: GLASSMORPHISM COMPONENTS
// =========================================================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = Color(0xFF007AFF),
    content: @Composable BoxScope.() -> Unit
) {
    // Triple-Layer Glassmorphism Logic
    val targetBgColor = if (isActive) activeColor else Color.White.copy(alpha = 0.08f)
    val targetBorderColor = if (isActive) activeColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f)
    val shadowElevation = if (isActive) 16f else 0f

    val bgColor by animateColorAsState(targetBgColor, tween(300), label = "bg")
    val borderColor by animateColorAsState(targetBorderColor, tween(300), label = "border")
    val elevation by animateFloatAsState(shadowElevation, tween(300), label = "shadow")

    Box(
        modifier = modifier
            .graphicsLayer {
                shape = RoundedCornerShape(24.dp)
                clip = true
                this.shadowElevation = elevation
            }
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(24.dp))
            .blur(if (!isActive) 30.dp else 0.dp), // GPU Heavy: Blur only when inactive
        contentAlignment = Alignment.Center,
        content = content
    )
}

// =========================================================================================
// ZONE A: STATUS HEADER
// =========================================================================================

@Composable
fun DashboardClock() {
    var time by remember { mutableStateOf(Calendar.getInstance().time) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000); time = Calendar.getInstance().time
        }
    }
    
    val timeStr = DateFormat.format("HH:mm", time).toString()
    Text(
        text = timeStr,
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        color = Color.White,
        letterSpacing = (-1).sp
    )
}

@Composable
fun HealthStack() {
    val batteryLevel = 45 // Replace with global state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "bat_pulse"
    )

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // CPU Temp
        GlassCard(modifier = Modifier.height(28.dp).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Thermostat, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Text("38°C", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Battery
        val batColor = if (batteryLevel < 20) Color(0xFFFF3B30) else Color(0xFF34C759)
        GlassCard(modifier = Modifier.height(28.dp).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    Icons.Default.BatteryFull, null, 
                    tint = batColor.copy(alpha = if (batteryLevel < 20) pulseAlpha else 1f), 
                    modifier = Modifier.size(14.dp)
                )
                Text("$batteryLevel%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================================
// ZONE B: ACTION CORE
// =========================================================================================

@Composable
fun MediaHeroSquarcle(media: LiveActivityModel.Music?) {
    // Subtle Breathe Animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f, targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "hero_scale"
    )

    GlassCard(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale }) {
        if (media?.albumArt != null) {
            Image(
                bitmap = media.albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay for Controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            // Controls overlay
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Icon(if (media.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp))
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        } else {
            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun LiquidSlider(icon: ImageVector, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    // Spring physics for fill level
    val animatedFill by animateFloatAsState(
        targetValue = value / 100f, 
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "slider_fill"
    )

    GlassCard(modifier = Modifier.width(60.dp).fillMaxHeight()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Friction Sensing UX Logic
                    detectVerticalDragGestures { change, dragAmount ->
                        val isFast = abs(dragAmount) > 15f
                        val multiplier = if (isFast) 10f else 1f // Fast flick vs Precision drag
                        val delta = (dragAmount * multiplier * -0.1f) // Negative because up is negative Y
                        onValueChange((value + delta).coerceIn(0f, 100f))
                    }
                }
        ) {
            // The Liquid Fill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(animatedFill)
                    .background(color)
            )
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (animatedFill > 0.1f) Color.Black else Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(24.dp)
            )
        }
    }
}

@Composable
fun UtilityGrid(tiles: List<QSTileState>, onQsClick: (String) -> Unit, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    val displayTiles = tiles.take(4).padWithEmpty(4)
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        for (row in 0..1) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (col in 0..1) {
                    val tile = displayTiles[row * 2 + col]
                    val isActive = tile?.isActive == true
                    
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable {
                                if (tile != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onQsClick(tile.tileSpec)
                                }
                            },
                        isActive = isActive
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = getIconForSpec(tile?.tileSpec),
                                contentDescription = null,
                                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            if (tile != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = tile.label.take(8),
                                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================================
// ZONE C: PRODUCTIVITY DOCK
// =========================================================================================

@Composable
fun ProductivityDock(apps: List<String>) {
    val context = LocalContext.current
    var appIcons by remember { mutableStateOf<List<Bitmap?>>(emptyList()) }

    // Fetch icons on Background Thread (Zero-Jank constraint)
    LaunchedEffect(apps) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            appIcons = apps.map { pkg ->
                try {
                    val drawable = pm.getApplicationIcon(pkg)
                    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                } catch (e: Exception) { null }
            }
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(appIcons) { index, bmp ->
                // Staggered Pop-in Animation
                var isItemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 50L); isItemVisible = true }
                
                val scale by animateFloatAsState(if (isItemVisible) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 400f), label = "icon_scale")

                if (bmp != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                    ) {
                        // Original Icon
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        // The Reflection Effect
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer { 
                                    scaleY = -1f // Flip vertically
                                    alpha = 0.4f // Lower opacity
                                }
                                .drawWithContent {
                                    drawContent()
                                    // Fade reflection out towards bottom
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            1f to Color.Black
                                        ),
                                        blendMode = BlendMode.DstOut
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================================
// UTILS
// =========================================================================================

private fun List<QSTileState>.padWithEmpty(size: Int): List<QSTileState?> {
    return this + List(maxOf(0, size - this.size)) { null }
}

private fun getIconForSpec(spec: String?): ImageVector {
    return when (spec?.lowercase()) {
        "wifi" -> Icons.Default.Wifi
        "bluetooth" -> Icons.Default.Bluetooth
        "torch" -> Icons.Default.FlashlightOn
        "dnd" -> Icons.Default.DoNotDisturbOn
        "airplane" -> Icons.Default.AirplanemodeActive
        "location" -> Icons.Default.LocationOn
        else -> Icons.Default.Settings
    }
}
