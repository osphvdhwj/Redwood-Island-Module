package com.example.dynamicisland

import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.sin

@Composable
fun IslandDashboardMax(
    dashboardModel: LiveActivityModel.Dashboard,
    currentMedia: LiveActivityModel.Music?,
    onSliderDrag: (String, Float) -> Unit, 
    onQsClick: (String) -> Unit,
    onMediaCommand: (String) -> Unit = {}, // Default empty so it doesn't break IslandMainUI
    onAppClick: (String) -> Unit = {}      // Default empty
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // --- TOP ROW: Media Player (Left) & Vertical Sliders (Right) ---
        Row(
            modifier = Modifier.fillMaxWidth().height(140.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Left: Media Box
            Box(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                if (currentMedia != null) {
                    MediaControlSquarcle(currentMedia, onMediaCommand)
                } else {
                    // Empty state if no music is playing
                    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                        Text("Not Playing", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }

            // Top Right: Vertical Sliders
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brightness Slider (Left)
                VerticalLiquidSlider(
                    value = 75f, // Placeholder, wire to actual brightness later
                    iconRes = android.R.drawable.ic_menu_day, 
                    activeColor = Color(0xFFFFD700), // Sun Yellow
                    onValueChange = { pct -> onSliderDrag("BRIGHT", pct) }
                )
                // Volume Slider (Right)
                VerticalLiquidSlider(
                    value = 50f, // Placeholder, wire to actual volume later
                    iconRes = android.R.drawable.ic_lock_silent_mode_off, 
                    activeColor = Color(0xFF00FFFF), // Cyan
                    onValueChange = { pct -> onSliderDrag("VOL", pct) }
                )
            }
        }

        // --- MIDDLE ROW: Quick Settings Grid ---
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            VoidGridZone(dashboardModel.activeTiles, onQsClick)
        }

        // --- BOTTOM ROW: Horizontal Line & App Dock ---
        Column(modifier = Modifier.fillMaxWidth()) {
            // The Horizontal Separator
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF333333)))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // The App Dock (Fetching actual icons!)
            AppDockZone(
                apps = dashboardModel.pinnedApps.ifEmpty { listOf("com.android.settings", "com.android.chrome", "com.google.android.youtube", "com.google.android.dialer") }, 
                onClick = onAppClick
            )
        }
    }
}

@Composable
private fun MediaControlSquarcle(media: LiveActivityModel.Music, onCommand: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A))
    ) {
        // Blurred Album Art Background
        if (media.albumArt != null) {
            Image(
                bitmap = media.albumArt.asImageBitmap(),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.3f)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Track Info
            Column {
                Text(text = media.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = media.artist, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            // The Wavy Line (Replaces standard progress bar)
            WavyProgressBar(isPlaying = media.isPlaying)

            // Playback Controls
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onCommand("PREV") })
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).clickable { onCommand("PLAY_PAUSE") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (media.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "PlayPause", tint = Color.Black, modifier = Modifier.size(24.dp))
                }
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onCommand("NEXT") })
            }
        }
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
private fun VoidGridZone(tiles: List<QSTileState>, onClick: (String) -> Unit) {
    // 4x2 Grid of Circular/Squarcle Quick Settings
    // Using hardcoded placeholders for the visual layout, wire them to the tiles list later
    val icons = listOf(
        Pair(android.R.drawable.ic_menu_camera, Color.Yellow),
        Pair(android.R.drawable.ic_menu_mylocation, Color.Green),
        Pair(android.R.drawable.ic_menu_compass, Color(0xFF0082FC)),
        Pair(android.R.drawable.ic_lock_idle_alarm, Color(0xFF8A2BE2)),
        Pair(android.R.drawable.ic_menu_info_details, Color.Cyan),
        Pair(android.R.drawable.ic_menu_share, Color.Magenta),
        Pair(android.R.drawable.ic_menu_manage, Color.White),
        Pair(android.R.drawable.ic_menu_gallery, Color(0xFFFFA500))
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 0..3) { VoidButton(iconRes = icons[i].first, activeColor = icons[i].second, isActive = i % 2 == 0, onClick = { onClick("QS_$i") }) }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 4..7) { VoidButton(iconRes = icons[i].first, activeColor = icons[i].second, isActive = i % 2 != 0, onClick = { onClick("QS_$i") }) }
        }
    }
}

@Composable
private fun VoidButton(iconRes: Int, activeColor: Color, isActive: Boolean, onClick: () -> Unit) {
    val scale = remember { Animatable(1f) }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp)) // Squarcle
            .background(if (isActive) activeColor else Color(0xFF1A1A1A))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = if (isActive) Color.Black else Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AppDockZone(apps: List<String>, onClick: (String) -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.take(5).forEach { pkg ->
            // Safely fetch the app icon from the Android OS
            val iconBmp = remember(pkg) {
                try {
                    val drawable = pm.getApplicationIcon(pkg)
                    drawable.toBitmap(width = 120, height = 120).asImageBitmap()
                } catch (e: PackageManager.NameNotFoundException) { null }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .clickable { onClick(pkg) },
                contentAlignment = Alignment.Center
            ) {
                if (iconBmp != null) {
                    Image(bitmap = iconBmp, contentDescription = pkg, modifier = Modifier.fillMaxSize())
                } else {
                    // Fallback letter if icon isn't found
                    Text(pkg.take(1).uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
