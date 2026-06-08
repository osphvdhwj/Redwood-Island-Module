package com.example.dynamicisland.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import androidx.compose.foundation.background
import com.example.dynamicisland.shared.model.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.gesture.IslandGesture
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.core.ui.design.*
import com.example.dynamicisland.shared.ipc.*

@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard, controller: IslandController) {
    val view = this
    val theme = LocalIslandTheme.current
    val pack = LocalIconPack.current
    val dialect = VisualDialect.fromIconPack(pack)
    val settings = controller.settingsState
    val stashItems = model.stashHistory

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
                            controller.postTransientNotification(LiveActivityModel.QuickNote, -1)
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

        // --- Section 1: System Vitals (Elite Dialect) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val hw = currentHardware.value
            val vitals = mutableListOf<@Composable () -> Unit>()
            
            if (settings.showVitalsRam && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Free RAM", formatRam(hw.ramFreeBytes), (hw.ramFreeBytes / 8589934592f).coerceIn(0f, 1f), Color(0xFF34C759), IconProvider.LogicalIcon.RAM, pack, dialect) 
            }
            if (settings.showVitalsCpu && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "CPU Temp", "${hw.cpuTempCelsius.toInt()}°C", (hw.cpuTempCelsius / 100f).coerceIn(0f, 1f), Color(0xFFFF9500), IconProvider.LogicalIcon.CPU, pack, dialect) 
            }
            if (settings.showVitalsFps && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Performance", "${hw.fps.toInt()} FPS", (hw.fps / 120f).coerceIn(0f, 1f), Color(0xFF00FBFF), IconProvider.LogicalIcon.SPEED, pack, dialect) 
            }
            if (settings.showVitalsBatCycles && hw != null) vitals.add { 
                SystemVitalCard(Modifier.fillMaxWidth(), "Battery Cycles", "${hw.batteryCycles}", (hw.batteryCycles / 1000f).coerceIn(0f, 1f), Color(0xFFAF52DE), IconProvider.LogicalIcon.SYNC, pack, dialect) 
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
                // Box(Modifier.weight(1f)) { MiniWeatherWidget("24°C", "Sunny") }
                // Box(Modifier.weight(1f)) { MiniCalendarWidget("Meeting", "14:00") }
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
                    val art = media.albumArt
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
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

        // --- Section 4: Quick Settings (Elite Grid) ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Quick Actions", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            
            if (settings.shortcutLayout == ShortcutLayout.CAROUSEL) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(view.qsTiles) { tile ->
                        QSTileModern(tile, view.onQsTileClick, pack, dialect)
                    }
                }
            } else {
                val chunkedTiles = view.qsTiles.chunked(4)
                chunkedTiles.forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowTiles.forEach { tile ->
                            QSTileModern(tile, view.onQsTileClick, pack, dialect)
                        }
                        if (rowTiles.size < 4) {
                            repeat(4 - rowTiles.size) { Spacer(Modifier.width(64.dp)) }
                        }
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
                StashType.LINK -> Icons.Default.Link
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
    logicalIcon: IconProvider.LogicalIcon,
    pack: IconPack,
    dialect: VisualDialect
) {
    val icon = IconProvider.getIcon(logicalIcon, pack)
    
    Box(
        modifier = modifier
            .height(84.dp)
            .shadow(
                elevation = dialect.glowRadius,
                shape = RoundedCornerShape(dialect.cornerRadius),
                ambientColor = dialect.glowColor,
                spotColor = dialect.glowColor
            )
            .background(
                Color.White.copy(alpha = if (dialect.isGlassy) 0.12f else 0.05f), 
                RoundedCornerShape(dialect.cornerRadius)
            )
            .border(dialect.borderWidth, dialect.borderColor, RoundedCornerShape(dialect.cornerRadius))
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
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
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
fun QSTileModern(tileSpec: String, onClick: ((String) -> Unit)?, pack: IconPack, dialect: VisualDialect) {
    val logicalIcon = when (tileSpec.lowercase()) {
        "wifi" -> IconProvider.LogicalIcon.WIFI
        "bluetooth" -> IconProvider.LogicalIcon.BLUETOOTH
        "torch" -> IconProvider.LogicalIcon.TORCH
        "airplane" -> IconProvider.LogicalIcon.SYNC
        "location" -> IconProvider.LogicalIcon.MAP
        "data" -> IconProvider.LogicalIcon.DATA
        "hotspot" -> IconProvider.LogicalIcon.HOTSPOT
        else -> IconProvider.LogicalIcon.SETTINGS
    }
    
    val icon = IconProvider.getIcon(logicalIcon, pack)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = dialect.glowRadius / 2,
                    shape = CircleShape,
                    ambientColor = dialect.glowColor,
                    spotColor = dialect.glowColor
                )
                .background(Color.White.copy(alpha = if (dialect.isGlassy) 0.15f else 0.08f), CircleShape)
                .border(dialect.borderWidth, dialect.borderColor, CircleShape)
                .squishClickable { onClick?.invoke(tileSpec) },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        val label = when (tileSpec.lowercase()) {
            "wifi" -> "Wi-Fi"
            "bluetooth" -> "BT"
            "torch" -> "Torch"
            "airplane" -> "Air"
            "location" -> "GPS"
            else -> tileSpec.take(4).uppercase()
        }
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
