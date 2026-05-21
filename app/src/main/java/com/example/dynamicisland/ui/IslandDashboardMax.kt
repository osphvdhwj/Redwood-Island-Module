package com.example.dynamicisland.ui

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

@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard, controller: IslandController) {
    val context = LocalContext.current
    val theme = LocalIslandTheme.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* Action handled by onGestureEvent if needed */ }) {
                Icon(Icons.Default.Settings, null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Quick Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardTile(
                modifier = Modifier.weight(1f),
                title = "Volume",
                value = "${hardwareVolume.intValue}%",
                icon = painterResource(R.drawable.ic_play_vector), // Placeholder
                color = Color(0xFF4FC3F7)
            ) {
                // Volume adjust
            }
            DashboardTile(
                modifier = Modifier.weight(1f),
                title = "Brightness",
                value = "Auto",
                icon = Icons.Default.Search, // Placeholder
                color = Color(0xFFFFD54F)
            ) {
                // Brightness adjust
            }
        }

        Spacer(Modifier.height(12.dp))

        // System Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${gamingFps.floatValue.toInt()} FPS", color = Color.White, fontSize = 14.sp)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_battery_full_vector), null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${globalBatteryLevel.intValue}%", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Media Mini Controller (if playing)
        val media = activeModel.value as? LiveActivityModel.Music
        if (media != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(media.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(media.artist, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1)
                    }
                    Row {
                        IconButton(onClick = { onPrevClick?.invoke() }) {
                            Icon(painterResource(R.drawable.ic_prev_vector), null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
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
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_play_vector), null, tint = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Settings Grid
        Text("Quick Settings", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(qsTiles) { tile ->
                QSCircleTile(tile, onQsTileClick)
            }
        }
    }
}

@Composable
fun DashboardTile(
    modifier: Modifier,
    title: String,
    value: String,
    icon: Any, // Icon or Painter
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon is ImageVector) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                } else if (icon is androidx.compose.ui.graphics.painter.Painter) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QSCircleTile(tileSpec: String, onClick: ((String) -> Unit)?) {
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
        modifier = Modifier.clickable { onClick?.invoke(tileSpec) }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon is androidx.compose.ui.graphics.painter.Painter) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            } else if (icon is ImageVector) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
