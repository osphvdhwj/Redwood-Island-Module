package com.example.dynamicisland

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DynamicIslandView.DashboardMax(model: LiveActivityModel.Dashboard) {
    val haptic = LocalHapticFeedback.current

    var isVolumeDragging by remember { mutableStateOf(false) }
    var localVolume by remember { mutableFloatStateOf(0f) }
    val displayVolume = if (isVolumeDragging) localVolume else (hardwareVolume.intValue / 100f)

    var isBrightnessDragging by remember { mutableStateOf(false) }
    var localBrightness by remember { mutableFloatStateOf(0f) }
    val displayBrightness = if (isBrightnessDragging) localBrightness else (hardwareBrightness.intValue / 100f)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
        
        // 1. Pinned Apps (Adaptive Grid)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().height(70.dp)
        ) {
            items(4) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val pkg = pinnedApps.getOrNull(index)
                            // 🚀 FIXED: Call the local function directly to bypass Security Exception!
                            if (!pkg.isNullOrEmpty()) {
                                onAppPinnedClick?.invoke(pkg)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White.copy(alpha=0.5f), modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 2. The Liquid Sliders
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LiquidSlider(
                modifier = Modifier.weight(1f),
                icon = if (hardwareAutoBrightness.value) Icons.Default.BrightnessAuto else Icons.Default.BrightnessHigh,
                progress = displayBrightness,
                activeColor = Color.White,
                onDragStart = { 
                    isBrightnessDragging = true 
                    localBrightness = hardwareBrightness.intValue / 100f
                },
                onDrag = { delta ->
                    localBrightness = (localBrightness + delta).coerceIn(0f, 1f)
                    onBrightnessDrag?.invoke((localBrightness * 100).toInt())
                },
                onDragEnd = { isBrightnessDragging = false }
            )

            LiquidSlider(
                modifier = Modifier.weight(1f),
                // 🚀 FIXED: Using AutoMirrored icons to resolve the Lint deprecation warnings
                icon = if (hardwareVolume.intValue == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                progress = displayVolume,
                activeColor = Color.White,
                onDragStart = { 
                    isVolumeDragging = true 
                    localVolume = hardwareVolume.intValue / 100f
                },
                onDrag = { delta ->
                    localVolume = (localVolume + delta).coerceIn(0f, 1f)
                    onVolumeDrag?.invoke((localVolume * 100).toInt())
                },
                onDragEnd = { isVolumeDragging = false }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. Quick Settings
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val tilesToShow = model.activeTiles.take(4)
            
            tilesToShow.forEach { tile ->
                val bgColor = if (tile.isActive) Color.White else Color.White.copy(alpha = 0.15f)
                val fgColor = if (tile.isActive) Color.Black else Color.White
                val alpha = if (tile.isUnavailable) 0.5f else 1f

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally, 
                    modifier = Modifier.clickable(enabled = !tile.isUnavailable) { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // 🚀 FIXED: Call the local function directly instead of using Enum Strings!
                        onQsTileClick?.invoke(tile.tileSpec)
                    }.alpha(alpha).weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape).background(bgColor), 
                        contentAlignment = Alignment.Center
                    ) {
                        if (tile.iconBitmap != null) {
                             Image(
                                bitmap = tile.iconBitmap.asImageBitmap(),
                                contentDescription = tile.label,
                                colorFilter = ColorFilter.tint(fgColor),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = fgColor, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = tile.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
