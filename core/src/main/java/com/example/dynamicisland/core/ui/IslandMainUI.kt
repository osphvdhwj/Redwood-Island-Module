package com.example.dynamicisland.core.ui
import com.example.dynamicisland.core.model.IslandUiState

import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dynamicisland.core.R
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.gesture.IslandGesture
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.performance.eliteFluidSurface
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.core.manager.updateIslandTransition
import com.example.dynamicisland.shared.settings.IconPack
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView.IslandUI(state: IslandState) {
    val view = this
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val settings = view.controller?.settingsState ?: com.example.dynamicisland.shared.settings.SettingsState()
    val isCyberpunk = settings.iconPack == IconPack.Futuristic
    val isLiquidGlass = settings.aestheticStyle == AestheticStyle.LIQUID_GLASS
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing)),
        label = "time"
    )

    val animValues = updateIslandTransition(
        targetState = state,
        isCyberpunk = isCyberpunk,
        physicsStyle = settings.physicsStyle,
        miniWidth = if (state == IslandState.TYPE_2_MID) view.midW.value else if (state == IslandState.TYPE_CUBE) view.cubeW.value else view.miniW.value,
        miniHeight = if (state == IslandState.TYPE_2_MID) view.midH.value else if (state == IslandState.TYPE_CUBE) view.cubeH.value else view.miniH.value,
        maxWidth = view.maxW.value,
        maxHeight = if (view.activeModel.value is LiveActivityModel.Music) (view.maxH.value * 0.70f) else view.maxH.value,
        ringWidth = view.ringW.value,
        ringHeight = view.ringH.value,
        miniRadius = if (state == IslandState.TYPE_2_MID) view.midR.value else if (state == IslandState.TYPE_CUBE) view.cubeR.value else view.miniR.value,
        maxRadius = view.maxR.value,
        ringRadius = view.ringR.value
    )
        ringRadius = view.ringR.value
    )

    var isSquished by remember { mutableStateOf(false) }
    val touchScale by animateFloatAsState(
        targetValue = if (isSquished) (1f - (0.08f * settings.squishIntensity)) else 1f, 
        animationSpec = tween(200),
        label = "squish"
    )
    
    val minSafeWidth = view.displayCutoutWidth.floatValue + 4f
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val maxSafeWidth = (screenWidthDp - 16f).coerceAtLeast(minSafeWidth)

    val animatedWidth = animValues.width.coerceIn(minSafeWidth.dp, maxSafeWidth.dp)
    val animatedHeight = animValues.height
    val animatedRadius = animValues.cornerRadius

    val model = view.activeModel.value
    
    val bgColor by animateColorAsState(
        targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else {
            if (settings.aestheticStyle == AestheticStyle.VOID_BLACK) Color.Black
            else if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) {
                val dColor = model.dominantColor
                if (dColor != null) Color(dColor) else Color.Transparent
            }
            else Color.Black
        },
        animationSpec = tween(200),
        label = "bgColor"
    )

    val borderColor = if (settings.aestheticStyle == AestheticStyle.VOID_BLACK) Color.White else animValues.borderColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(view.maxH.value.dp),
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(animatedWidth) 
                .height(animatedHeight)
                .graphicsLayer { 
                    val s = view.elasticScale.value * touchScale * animValues.scale
                    scaleX = s; scaleY = s; alpha = animValues.alpha
                    rotationZ = animValues.rotation
                    transformOrigin = TransformOrigin(0.5f, 0.5f) 
                }
                .eliteFluidSurface(
                    pill1 = android.graphics.Rect(0, 0, with(density) { animatedWidth.toPx().toInt() }, with(density) { animatedHeight.toPx().toInt() }),
                    velocity = view.currentVelocity.value,
                    time = time,
                    color = bgColor,
                    liquidMode = isLiquidGlass,
                    gpuLoad = view.gpuLoad.value
                )
                .geminiAura(enabled = settings.geminiAuraEnabled && (model?.id?.contains("assistant") == true || model?.id?.contains("ai") == true))
                .pointerInput(Unit) {
                    awaitEachGesture { 
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        isSquished = true
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        isSquished = false 
                    }
                }
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); view.onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                        onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); view.onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) },
                        onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); view.onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                    )
                }
                .pointerInput(state) {
                    var dragOffsetY = 0f
                    var dragOffsetX = 0f
                    detectDragGestures(
                        onDragStart = { view.isBridgeDragging.value = true },
                        onDragEnd = {
                            view.isBridgeDragging.value = false
                            view.currentVelocity.value = Offset.Zero
                            if (dragOffsetY > 100f) view.onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) 
                            else if (dragOffsetY < -40f) view.onGestureEvent?.invoke(IslandGesture.SWIPE_UP)
                            else if (abs(dragOffsetX) > 60f) {
                                if (dragOffsetX > 0) view.onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT)
                                else view.onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT)
                            }
                            scope.launch { view.elasticScale.animateTo(1f, tween(200)) }
                        },
                        onDrag = { change, dragAmount -> 
                            change.consume()
                            dragOffsetY += dragAmount.y; dragOffsetX += dragAmount.x
                            view.currentVelocity.value = Offset(dragAmount.x, dragAmount.y)
                            if (settings.velocitySquishEnabled) {
                                scope.launch { view.elasticScale.snapTo(1f + (dragOffsetY / 1500f).coerceIn(-0.1f, 0.2f)) }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f)).togetherWith(fadeOut(tween(200)))
                    },
                    label = "Content"
                ) { s ->
                    when (s) {
                        IslandState.TYPE_3_MAX -> PillRouter(s, model) { view.controller?.executeSmartAction(it.toString()) }
                        IslandState.TYPE_2_MID -> PillRouter(s, model) { view.controller?.executeSmartAction(it.toString()) }
                        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> PillRouter(s, model) { view.controller?.executeSmartAction(it.toString()) }
                        IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                        else -> {}
                    }
                }
            }
        }
        SplitCubeUI(state, animatedHeight, borderColor, animValues.xOffset)
    }
}
