package com.example.dynamicisland.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextOverflow
import com.example.dynamicisland.gesture.IslandGesture
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.R
import com.example.dynamicisland.model.LiveActivityModel
import com.example.dynamicisland.manager.IslandController
import com.example.dynamicisland.model.LocalIslandTheme
import com.example.dynamicisland.manager.StashedItem
import com.example.dynamicisland.manager.StashType
import com.example.dynamicisland.model.ActivityType
import com.example.dynamicisland.settings.ShortcutLayout
import com.example.dynamicisland.ui.components.MiniWeatherWidget
import com.example.dynamicisland.ui.components.MiniCalendarWidget

@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard, controller: IslandController) {
    val view = this
    val theme = LocalIslandTheme.current
    val settings = controller.settingsState
    val stashItems by controller.stashHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Smart Dashboard", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("System Vitals & Controls", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .squishClickable { 
                            controller.postTransientNotification(com.example.dynamicisland.model.LiveActivityModel.QuickNote(), -1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .squishClickable { view.onGestureEvent?.invoke(IslandGesture.LONG_PRESS) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // --- Section 1: System Vitals (Togglable) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val hw = controller.currentHardware
            val vitals = mutableListOf<@Composable () -> Unit>()
            
            if (settings.showVitalsRam && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Free RAM", formatRam(hw.ramFreeBytes), (hw.ramFreeBytes / 8589934592f).coerceIn(0f, 1f), Color(0xFF34C759), Icons.Default.Memory) 
            }
            if (settings.showVitalsCpu && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "CPU Temp", "${hw.cpuTempCelsius.toInt()}°C", (hw.cpuTempCelsius / 100f).coerceIn(0f, 1f), Color(0xFFFF9500), Icons.Default.Thermostat) 
            }
            if (settings.showVitalsFps) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Performance", "${gamingFps.floatValue.toInt()} FPS", (gamingFps.floatValue / 120f).coerceIn(0f, 1f), Color(0xFF00FBFF), Icons.Default.Speed) 
            }
            if (settings.showVitalsBatCycles && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Battery Cycles", "${hw.batteryCycles}", (hw.batteryCycles / 1000f).coerceIn(0f, 1f), Color(0xFFAF52DE), Icons.Default.Refresh) 
            }
            
            vitals.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { Box(Modifier.weight(1f)) { it() } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // --- Section 1.5: Widgets (NEW) ---
        if (settings.enableMaxWidgets) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) { MiniWeatherWidget("24°C", "Sunny") }
                Box(Modifier.weight(1f)) { MiniCalendarWidget("Meeting", "14:00") }
            }
        }

        // --- Section 2: Island Stash ---
        if (stashItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Island Stash", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(stashItems) { item -> StashItemCard(item) }
                }
            }
        }

        // --- Section 3: Media Mini ---
        val media = activeModel.value as? LiveActivityModel.Music
        if (media != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Box(Modifier.size(48.dp).background(Color.White.copy(0.1f), RoundedCornerShape(10.dp)))
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(media.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(media.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { onPlayPauseClick?.invoke() }) {
                            Icon(
                                painter = if (media.isPlaying) painterResource(R.drawable.ic_pause_vector) else painterResource(R.drawable.ic_play_vector),
                                null, tint = Color.White, modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { onNextClick?.invoke() }) {
                            Icon(painterResource(R.drawable.ic_next_vector), null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // --- Section 4: Quick Settings Grid/Carousel (NEW) ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Quick Actions", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            
            if (settings.shortcutLayout == ShortcutLayout.CAROUSEL) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(view.qsTiles) { tile ->
                        QSTileModern(tile, view.onQsTileClick)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    view.qsTiles.take(4).forEach { tile -> QSTileModern(tile, view.onQsTileClick) }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    view.qsTiles.drop(4).take(4).forEach { tile -> QSTileModern(tile, view.onQsTileClick) }
                    if (view.qsTiles.size < 8) {
                        repeat(4 - (view.qsTiles.size - 4)) { Spacer(Modifier.size(64.dp)) }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun DynamicIslandView.StashItemCard(item: StashedItem) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .squishClickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                handleStashClick(context, item)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareStashItem(context, item)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            val icon = when (item.type) {
                StashType.TEXT -> Icons.Default.Edit
                StashType.IMAGE -> Icons.Default.Image
                StashType.FILE -> Icons.Default.Build
            }
            Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (item.type == StashType.TEXT) item.content else item.id,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun handleStashClick(context: android.content.Context, item: StashedItem) {
    if (item.type == StashType.TEXT) {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Stashed Text", item.content))
        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
    } else {
        try {
            val file = java.io.File(item.content)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, context.contentResolver.getType(uri))
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Cannot open file", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareStashItem(context: android.content.Context, item: StashedItem) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        if (item.type == StashType.TEXT) {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, item.content)
        } else {
            val file = java.io.File(item.content)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            type = context.contentResolver.getType(uri)
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Stashed Item").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun SystemVitalCard(
    modifier: Modifier,
    label: String,
    value: String,
    progress: Float,
    color: Color,
    icon: Any
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    color = color,
                    trackColor = color.copy(alpha = 0.12f),
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.fillMaxSize()
                )
                val iconModifier = Modifier.size(20.dp)
                if (icon is androidx.compose.ui.graphics.painter.Painter) {
                    Icon(icon, null, tint = color, modifier = iconModifier)
                } else if (icon is ImageVector) {
                    Icon(icon, null, tint = color, modifier = iconModifier)
                }
            }
            Column {
                Text(label, color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
            }
        }
    }
}

private fun formatRam(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return String.format("%.1f GB", gb)
}

@Composable
fun QSTileModern(tileSpec: String, onClick: ((String) -> Unit)?) {
    val (icon, label) = when (tileSpec.lowercase()) {
        "wifi" -> painterResource(R.drawable.ic_wifi_vector) to "Wi-Fi"
        "bluetooth" -> painterResource(R.drawable.ic_bluetooth_vector) to "BT"
        "torch" -> painterResource(R.drawable.ic_torch_vector) to "Torch"
        "airplane" -> Icons.Default.Send to "Air" 
        "location" -> Icons.Default.Place to "GPS"
        else -> Icons.Default.Settings to "Sys"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .squishClickable { onClick?.invoke(tileSpec) },
            contentAlignment = Alignment.Center
        ) {
            if (icon is androidx.compose.ui.graphics.painter.Painter) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            } else if (icon is ImageVector) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
