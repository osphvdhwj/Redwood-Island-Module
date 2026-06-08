package com.example.dynamicisland.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FpsGraph(fps: Int) {
    val maxV = 90f
    val barCount = 28
    val bars = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(30f + (Math.random() * 50f).toFloat()) } } }

    LaunchedEffect(fps) {
        while (true) {
            delay(350)
            bars.removeAt(0)
            val newFps = (fps + (Math.random() * 20 - 10)).toFloat().coerceIn(10f, 90f)
            bars.add(newFps)
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Column(
            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).padding(bottom = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(60, 30, 0).forEach { v ->
                Text(text = v.toString(), fontSize = 9.sp, color = Color(0xFF555555), fontFamily = FontFamily.Monospace)
            }
        }

        Row(
            modifier = Modifier.fillMaxHeight().padding(end = 18.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            bars.forEach { v ->
                val barColor = if (v > 55) Color(0xFFFF3B3B) else Color(0xFFFF6B00)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(v / maxV)
                        .defaultMinSize(minHeight = 2.dp)
                        .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                        .background(barColor.copy(alpha = 0.85f))
                )
            }
        }
    }
}

@Composable
fun VerticalSlider(
    value: Int,
    onChange: (Int) -> Unit,
    icon: String,
    label: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(40.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A1A))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newPct = (value / 100f) - (dragAmount.y / size.height)
                        onChange((newPct.coerceIn(0f, 1f) * 100).toInt())
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value / 100f)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.6f))))
            )
            
            Text(
                text = "${value}%",
                fontSize = 8.sp,
                color = Color(0xFF888888),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        Text(
            text = label,
            fontSize = 9.sp,
            color = Color(0xFF888888),
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
