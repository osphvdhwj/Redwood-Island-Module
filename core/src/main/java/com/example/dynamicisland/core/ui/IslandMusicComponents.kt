package com.example.dynamicisland.core.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Global flag to prevent local position from fighting the drag
var isDraggingMedia by mutableStateOf(false)

fun formatTime(ms: Long): String { 
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return String.format("%d:%02d", s / 60, s % 60) 
}

@Composable
fun IsolatedCircularProgress(durationMs: Long, posProvider: () -> Long, color: Color) {
    var localPos by remember { mutableLongStateOf(posProvider()) }
    LaunchedEffect(Unit) { while(isActive) { delay(100); localPos = posProvider() } }
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    val currentPosition = localPos.toFloat().coerceAtLeast(0f)
    CircularProgressIndicator(progress = { (currentPosition / safeDuration).coerceIn(0f, 1f) }, color = color, trackColor = color.copy(alpha = 0.2f), strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
}

@Composable
fun InteractiveWavyMediaBar(durationMs: Long, posProvider: () -> Long, isPlaying: Boolean, color: Color, trackColor: Color, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val localPosState = remember { mutableLongStateOf(posProvider()) }
    
    LaunchedEffect(Unit) { 
        while(isActive) { delay(50); if (!isDraggingMedia) localPosState.longValue = posProvider() } 
    }
    
    val safeDuration = if (durationMs <= 0L) 1f else durationMs.toFloat()
    var dragProgress by remember { mutableFloatStateOf(0f) }
    
    // 🚀 120FPS OPTIMIZATION: We keep these as State objects and DO NOT use 'by' here.
    val targetAmplitude = if (isDraggingMedia) 6f else if (isPlaying) 2.5f else 0f
    val amplitudeState = animateFloatAsState(targetValue = targetAmplitude, animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f), label = "amp")
    val phaseShiftState = rememberInfiniteTransition(label = "wave").animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "phase")

    val wavePath = remember { Path() }

    Canvas(
        modifier = modifier.fillMaxWidth().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset -> isDraggingMedia = true; dragProgress = (offset.x / size.width).coerceIn(0f, 1f); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                onDragEnd = { isDraggingMedia = false; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSeek((dragProgress * safeDuration).toLong()) },
                onDragCancel = { isDraggingMedia = false }
            ) { change, _ -> change.consume(); dragProgress = (change.position.x / size.width).coerceIn(0f, 1f) }
        }.pointerInput(Unit) { detectTapGestures(onTap = { offset -> onSeek(((offset.x / size.width).coerceIn(0f, 1f) * safeDuration).toLong()) }) }
    ) {
        // 🚀 We extract the .value INSIDE the Canvas. 
        // This ensures the Compose Compiler ONLY redraws the canvas, saving massive CPU power.
        val currentAmplitude = amplitudeState.value
        val currentPhaseShift = phaseShiftState.value
        val currentPos = localPosState.longValue
        
        val currentProgress = (currentPos / safeDuration).coerceIn(0f, 1f)
        val displayProgress = if (isDraggingMedia) dragProgress else currentProgress

        val midY = size.height / 2
        val activeWidth = size.width * displayProgress
        drawLine(color = trackColor, start = androidx.compose.ui.geometry.Offset(activeWidth, midY), end = androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = size.height, cap = StrokeCap.Round)

        wavePath.rewind() 
        wavePath.moveTo(0f, midY)
        val frequency = 0.08f
        
        for (x in 0..activeWidth.toInt() step 4) {
            val tension = if (isDraggingMedia) (1f - (kotlin.math.abs(x - activeWidth) / size.width)).coerceAtLeast(0.2f) else 1f
            val y = midY + kotlin.math.sin((x * frequency).toDouble() + currentPhaseShift).toFloat() * (currentAmplitude.dp.toPx() * tension)
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(activeWidth, midY)

        drawIntoCanvas { drawPath(path = wavePath, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.height, cap = StrokeCap.Round)) }
        drawCircle(color = Color.White, radius = if(isDraggingMedia) 6.dp.toPx() else 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(activeWidth, midY))
    }
}
