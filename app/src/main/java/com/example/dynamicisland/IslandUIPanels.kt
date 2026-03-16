package com.example.dynamicisland

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 🚀 ADAPTIVE MINI PILL: Intrinsic centering, no overlapping
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMini(music: LiveActivityModel.Music) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Pushes icon and text to edges safely
    ) {
        if (music.albumArt != null) {
            Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape))
        } else {
            Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))
        }
        
        Spacer(Modifier.width(12.dp))
        
        Text(
            text = "${music.title} • ${music.artist}", 
            color = Color.White, 
            fontSize = 14.sp, 
            fontWeight = FontWeight.Medium, 
            maxLines = 1, 
            modifier = Modifier.weight(1f).safeMarquee(islandState.value) 
        )
        
        // Animated Equalizer/Waveform indicator instead of overlapping progress
        if (music.isPlaying) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(music.dominantColor ?: android.graphics.Color.WHITE), modifier = Modifier.size(16.dp))
        }
    }
}

// 🚀 ADAPTIVE MID PILL: Strict weight ratios
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMid(music: LiveActivityModel.Music) {
    val dynamicColor = Color(music.titleTextColor).copy(alpha = 0.9f)
    
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // TOP 70%: Content
        Row(modifier = Modifier.weight(0.7f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (music.albumArt != null) {
                Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(text = music.title, color = dynamicColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = dynamicColor.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
            
            val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
            Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = dynamicColor, modifier = Modifier.size(32.dp))
        }
        
        // BOTTOM 30%: Handle and Timestamps
        Row(modifier = Modifier.weight(0.3f).fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            IsolatedTimeText(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, textColor = dynamicColor.copy(alpha=0.6f), fontSize = 11.sp)
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(dynamicColor.copy(alpha = 0.3f))) // Clean Handle
            Text(text = formatTime(music.durationMs), color = dynamicColor.copy(alpha=0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
        }
    }
}

// 🚀 ADAPTIVE MAX PILL: Strict flex layout
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandView.MusicMax(music: LiveActivityModel.Music) {
    val theme = LocalIslandTheme.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // TOP 15%: App Icon & Routing
        Row(modifier = Modifier.weight(0.15f).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            if (music.appIcon != null) Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App", modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))) 
            else Box(Modifier.size(24.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(6.dp)))
            Icon(Icons.Default.Smartphone, contentDescription = "Output", tint = Color.White.copy(0.7f), modifier = Modifier.size(20.dp))
        }
        
        // MIDDLE 40%: Huge Art & Text
        Row(modifier = Modifier.weight(0.4f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = music.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.safeMarquee(islandState.value))
                Text(text = music.artist, color = Color.White.copy(alpha=0.75f), fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.safeMarquee(islandState.value))
            }
        }
        
        // BOTTOM 45%: Seekbar & Controls
        Column(modifier = Modifier.weight(0.45f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
            IsolatedMediaSlider(durationMs = music.durationMs, posProvider = { currentMediaPos.longValue }, dynamicTextColor = Color.White, onSeek = { onSeekTo?.invoke(it) })
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                Icon(playIcon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(48.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
            }
        }
    }
}

// 🚀 REFINED DASHBOARD (Clean Grid)
@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceEvenly) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DashboardToggle(Icons.Default.Wifi, true)
            DashboardToggle(Icons.Default.Bluetooth, false)
            DashboardToggle(Icons.Default.AirplanemodeActive, false)
            DashboardToggle(Icons.Default.FlashlightOn, false)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DashboardToggle(Icons.Default.DoNotDisturbOn, false)
            DashboardToggle(Icons.Default.LocationOn, true)
            DashboardToggle(Icons.Default.Settings, true)
            DashboardToggle(Icons.Default.Build, false)
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        }
    }
}

@Composable
fun DashboardToggle(icon: ImageVector, active: Boolean) {
    val bgColor = if (active) Color.Cyan else Color.White.copy(alpha = 0.15f)
    val tint = if (active) Color.Black else Color.White
    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// 🚀 OTP CATCHER (Beautiful layout)
@Composable
fun DynamicIslandView.OtpMid(model: LiveActivityModel.Otp) {
    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).background(Color(0xFF4285F4).copy(alpha=0.2f), CircleShape).border(1.dp, Color(0xFF4285F4).copy(alpha=0.5f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
             Text(text = "Code from ${model.sourceApp}", color = Color.Gray, fontSize = 12.sp)
             Text(text = model.code, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
        }
        Box(modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
             Icon(Icons.Default.ContentCopy, contentDescription="Copy", tint=Color.Cyan, modifier = Modifier.size(24.dp))
        }
    }
}

// UTILITY COMPONENTS
@Composable
fun DynamicIslandView.ChargingCube(model: LiveActivityModel.Charging) {
    val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = "${model.level}%", color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun DynamicIslandView.ChargingMid(charging: LiveActivityModel.Charging) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Green.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = Color.Green) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Charging", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "${charging.level}%", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.SystemAlertMid(alert: LiveActivityModel.SystemAlert) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color(alert.alertColor).copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, null, tint = Color(alert.alertColor)) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = alert.title, color = Color(alert.alertColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = alert.message, color = Color(alert.alertColor).copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.OngoingTaskMid(task: LiveActivityModel.OngoingTask) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Cyan.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = Color.Cyan) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = task.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = task.text, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.AppTimerWarningMid(model: LiveActivityModel.AppTimerWarning) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color.Red.copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, null, tint = Color.Red) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Time Limit Reached", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = model.appName, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

@Composable
fun DynamicIslandView.RealityPillMini(model: LiveActivityModel.RealityPill) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${model.appName} • ${model.sessionMinutes}m", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DynamicIslandView.HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DynamicIslandView.GeneralMini(general: LiveActivityModel.General) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = general.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun DynamicIslandView.GeneralMid(general: LiveActivityModel.General) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Box(modifier = Modifier.size(44.dp).background(Color(general.accentColor).copy(0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, null, tint = Color(general.accentColor)) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = general.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = general.dataText, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}

fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

@Composable
fun IsolatedTimeText(durationMs: Long, posProvider: () -> Long, textColor: Color, fontSize: androidx.compose.ui.unit.TextUnit = 12.sp) {
    val timeStr by remember(durationMs) { derivedStateOf { formatTime(posProvider()) } }
    Text(text = timeStr, color = textColor, fontSize = fontSize, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedMediaSlider(durationMs: Long, posProvider: () -> Long, dynamicTextColor: Color, onSeek: (Long) -> Unit) {
    var localPosition by remember { mutableFloatStateOf(0f) }
    Slider(
        value = (posProvider().toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
        onValueChange = { localPosition = it * durationMs; onSeek(localPosition.toLong()) },
        colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.25f), thumbColor = dynamicTextColor),
        modifier = Modifier.fillMaxWidth().height(24.dp)
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun androidx.compose.ui.Modifier.safeMarquee(state: IslandState): androidx.compose.ui.Modifier {
    return if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING && state != IslandState.TYPE_CUBE) this.basicMarquee() else this
}
