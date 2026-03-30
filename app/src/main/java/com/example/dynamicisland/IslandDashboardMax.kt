package com.example.dynamicisland

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun IslandDashboardMax(
    dashboardModel: LiveActivityModel.Dashboard,
    currentMedia: LiveActivityModel.Music?,
    onSliderDrag: (String, Float) -> Unit, // "VOL" or "BRIGHT", percentage 0f-1f
    onQsClick: (String) -> Unit
) {
    // The entire dashboard is wrapped in a massive column with generous padding
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ZONE 1: The Apex (Hardware Stats flanking the camera)
        ApexZone()

        // ZONE 2: Consolidated Media Core (Only shows if music is playing)
        if (currentMedia != null) {
            MediaCoreZone(currentMedia)
        }

        Spacer(modifier = Modifier.weight(1f))

        // ZONE 3: Liquid Sliders
        LiquidSlidersZone(onSliderDrag)

        // ZONE 4: The Void Grid (QS Toggles)
        VoidGridZone(dashboardModel.activeTiles, onQsClick)

        // ZONE 5: App Dock
        AppDockZone(dashboardModel.pinnedApps)
    }
}

@Composable
private fun ApexZone() {
    Row(
        modifier = Modifier.fillMaxWidth().height(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Battery & Clock
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("100%", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("12:00", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        // Space for physical camera cutout in the middle
        Spacer(modifier = Modifier.width(100.dp))

        // Right: Connections / Speeds
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LTE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(painterResource(id = android.R.drawable.stat_sys_data_bluetooth), contentDescription = "BT", tint = Color(0xFF0082FC), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun MediaCoreZone(media: LiveActivityModel.Music) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = media.title, color = Color.White, fontSize = 18.sp, 
            fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            text = media.artist, color = Color.Gray, fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        
        // The Wavy Progress Bar (Physics-driven sine wave)
        WavyProgressBar(isPlaying = media.isPlaying)
    }
}

@Composable
private fun WavyProgressBar(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "wavePhase"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(20.dp).padding(vertical = 8.dp)) {
        val path = Path()
        val waveWidth = size.width
        val waveHeight = size.height
        val frequency = 0.05f

        path.moveTo(0f, waveHeight / 2)
        for (x in 0 until waveWidth.toInt() step 5) {
            // Flatten the wave if paused
            val amplitude = if (isPlaying) (waveHeight / 2) else 1f
            val y = (sin((x * frequency) + phase) * amplitude) + (waveHeight / 2)
            path.lineTo(x.toFloat(), y.toFloat())
        }

        drawPath(
            path = path, 
            color = if (isPlaying) Color.White else Color.DarkGray, 
            style = Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
private fun LiquidSlidersZone(onDrag: (String, Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // We will pass in hardcoded '50f' for preview purposes right now. 
        // Later we will link this to the actual hardwareManager volume/brightness flows.
        IslandLiquidSlider(
            value = 50f, 
            onValueChange = { pct -> onDrag("VOL", pct) },
            activeColor = Color(0xFF00FFFF) // Cyan Volume
        )
        
        IslandLiquidSlider(
            value = 75f, 
            onValueChange = { pct -> onDrag("BRIGHT", pct) },
            activeColor = Color(0xFFFFD700) // Sun Yellow Brightness
        )
    }
}

@Composable
private fun SliderPlaceholder(color: Color, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A)) // Dark void background
    ) {
        // Filled portion
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.7f).background(color))
    }
}

@Composable
private fun VoidGridZone(tiles: List<QSTileState>, onClick: (String) -> Unit) {
    // 2x2 Grid of Massive Black Buttons
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            VoidButton(modifier = Modifier.weight(1f), iconRes = android.R.drawable.ic_menu_compass, activeColor = Color(0xFF0082FC), isActive = true)
            VoidButton(modifier = Modifier.weight(1f), iconRes = android.R.drawable.ic_menu_mylocation, activeColor = Color.Green, isActive = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            VoidButton(modifier = Modifier.weight(1f), iconRes = android.R.drawable.ic_menu_camera, activeColor = Color.Yellow, isActive = false)
            VoidButton(modifier = Modifier.weight(1f), iconRes = android.R.drawable.ic_menu_info_details, activeColor = Color(0xFF8A2BE2), isActive = true)
        }
    }
}

@Composable
private fun VoidButton(modifier: Modifier, iconRes: Int, activeColor: Color, isActive: Boolean) {
    // The Void Button physics
    val scale = remember { Animatable(1f) }
    
    Box(
        modifier = modifier
            .aspectRatio(1.5f) // Wide rectangle
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black) // Pure black
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Kill the default Android ripple
            ) {
                // We will add the haptic + scale down/up Coroutine launch here
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (isActive) activeColor else Color.DarkGray,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun AppDockZone(apps: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder App Icons
        repeat(5) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF1A1A1A)))
        }
    }
}
