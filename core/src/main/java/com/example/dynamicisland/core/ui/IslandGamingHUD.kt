package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.shared.model.IslandState   // ← only this import (model.IslandState removed)

// File: app/src/main/java/com/example/dynamicisland/ui/IslandGamingHUD.kt


/**
 * BATCH 6: Gaming HUD
 *
 * Renders in the island's TYPE_1_MINI state during gameplay.
 * Shows a compact 3-column HUD:
 *
 *   [FPS counter] [Thermal bar] [Network ping*]
 *
 * The FPS counter uses a micro-graph showing the last 30 frame durations
 * as a sparkline — spikes indicate jank events visually.
 *
 * Color coding:
 *   FPS: green (≥55), yellow (40–54), red (<40)
 *   Thermal: green (<40°C), yellow (40–50°C), red (>50°C)
 *
 * This is displayed inside the TYPE_1_MINI ring pill — it respects the
 * gaming profile's hapticSuppression and maxExpandState limits.
 */
@Composable
fun DynamicIslandView.GamingHUDMini(
    fps:       Float,
    frameMs:   Float,
    jankPct:   Float,
    cpuTemp:   Float,
    cpuFreqMhz: Int
) {
    val fpsColor = when {
        fps >= 55f -> Color(0xFF66FF66)   // Green — smooth
        fps >= 40f -> Color(0xFFFFCC44)   // Yellow — minor stutter
        else       -> Color(0xFFFF4444)   // Red — struggling
    }

    val tempColor = when {
        cpuTemp < 40f -> Color(0xFF66FF66)
        cpuTemp < 50f -> Color(0xFFFFCC44)
        else          -> Color(0xFFFF4444)
    }

    // Rolling frame history for sparkline (last 30 samples)
    val frameSamples = remember { ArrayDeque<Float>(31) }
    LaunchedEffect(frameMs) {
        frameSamples.addLast(frameMs)
        if (frameSamples.size > 30) frameSamples.removeFirst()
    }

    // Gentle pulse on jank
    val jankPulse by animateFloatAsState(
        targetValue   = if (jankPct > 15f) 0.7f else 0f,
        animationSpec = tween(200),
        label         = "jank_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ── FPS Counter + sparkline ───────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "${fps.toInt()}",
                color      = fpsColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text     = "fps",
                color    = fpsColor.copy(alpha = 0.60f),
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 1.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Sparkline — 30 frame durations as a micro-graph
            Canvas(
                modifier = Modifier
                    .size(width = 32.dp, height = 16.dp)
            ) {
                if (frameSamples.size < 2) return@Canvas
                val samples  = frameSamples.toList()
                val maxSample = samples.maxOrNull()?.coerceAtLeast(8f) ?: 16f
                val minSample = samples.minOrNull() ?: 0f
                val range    = (maxSample - minSample).coerceAtLeast(1f)
                val w        = size.width / (samples.size - 1)

                val path = Path()
                samples.forEachIndexed { i, v ->
                    val x = i * w
                    val y = size.height * (1f - (v - minSample) / range)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path  = path,
                    color = fpsColor.copy(alpha = 0.70f + jankPulse * 0.30f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Mark jank spikes in red
                samples.forEachIndexed { i, v ->
                    if (v > 20f) {   // spike threshold ~50fps
                        val x = i * w
                        val y = size.height * (1f - (v - minSample) / range)
                        drawCircle(Color(0xFFFF4444), radius = 1.5.dp.toPx(), center = Offset(x, y))
                    }
                }
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(Color.White.copy(alpha = 0.20f))
        )

        // ── Thermal ───────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text       = "${cpuTemp.toInt()}°",
                color      = tempColor,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(2.dp))
            // Compact thermal bar
            Canvas(modifier = Modifier.size(width = 18.dp, height = 6.dp)) {
                val ratio = (cpuTemp / 70f).coerceIn(0f, 1f)
                drawRoundRect(
                    color        = Color.White.copy(alpha = 0.15f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                drawRoundRect(
                    color        = tempColor,
                    size         = size.copy(width = size.width * ratio),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp)
                .background(Color.White.copy(alpha = 0.20f))
        )

        // ── CPU frequency (compact) ───────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "${if (cpuFreqMhz >= 1000) "${cpuFreqMhz / 1000}.${(cpuFreqMhz % 1000) / 100}G" else "${cpuFreqMhz}M"}",
                color      = Color.White.copy(alpha = 0.80f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}