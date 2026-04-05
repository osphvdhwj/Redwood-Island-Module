package com.example.dynamicisland

import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView.IslandUI(state: IslandState) {
    val view = this
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isEffectivelyHidden = state == IslandState.HIDDEN || isLandscape

    val islandScale by animateFloatAsState(targetValue = if (isEffectivelyHidden) 0f else 1f, animationSpec = if (isEffectivelyHidden) tween(350, easing = FastOutLinearInEasing) else spring(dampingRatio = 0.65f, stiffness = 300f), label = "blackhole_scale")

    val theme = LocalIslandTheme.current
    val dpPhysicsSpec = spring<Dp>(dampingRatio = theme.springDamping, stiffness = theme.springStiffness)
    val floatPhysicsSpec = spring<Float>(dampingRatio = theme.springDamping, stiffness = theme.springStiffness)
    
    val haptic = LocalHapticFeedback.current
    var isSquished by remember { mutableStateOf(false) }
    val touchScale by animateFloatAsState(targetValue = if (isSquished) 0.96f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "squish")
    
    val minSafeWidth = displayCutoutWidth.floatValue + 4f
    val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
    val targetWidth = rawTargetWidth.coerceAtLeast(minSafeWidth)
    
    // We get both the active model and the split model to pass to the Dashboard
    val model = activeModel.value
    val splitModelValue = splitModel.value
    val activeMedia = (splitModelValue as? LiveActivityModel.Music) ?: (model as? LiveActivityModel.Music)

    val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> if (model is LiveActivityModel.Music) (maxH.value * 0.70f) else maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
    val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
    val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

    val animatedWidth by animateDpAsState(targetWidth.dp, dpPhysicsSpec, label = "width")
    val animatedHeight by animateDpAsState(targetHeight.dp, dpPhysicsSpec, label = "height")
    val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
    val animatedRadius by animateDpAsState(radTarget, dpPhysicsSpec, label = "rad")
    
    val offsetX by animateFloatAsState(targetX, floatPhysicsSpec, label = "x")
    val offsetY by animateFloatAsState(targetY, floatPhysicsSpec, label = "y")
    val islandAlpha by animateFloatAsState(targetValue = if (isEffectivelyHidden) 0f else 1f, animationSpec = tween(300), label = "blackhole_alpha")

    val targetBgColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else {
        val baseAlpha = if (theme.isGlassmorphism) 0.65f else 1.0f
        if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) { Color(model.dominantColor).copy(alpha = baseAlpha) } else if (state == IslandState.TYPE_3_MAX) { Color(0xFF080808).copy(alpha = baseAlpha) } else { Color.Black.copy(alpha = baseAlpha) }
    }
    val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
    val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.08f), animationSpec = tween(600), label = "borderColor")

    val densityPx = LocalDensity.current.density
    var anchorLeft by remember { mutableIntStateOf(0) }
    var anchorTop by remember { mutableIntStateOf(0) }
    var anchorWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetWidth, targetHeight, anchorLeft, anchorTop, anchorWidth) {
        if (anchorWidth == 0) return@LaunchedEffect
        val wPx = (targetWidth * densityPx).toInt()
        val hPx = (targetHeight * densityPx).toInt()
        val centerX = anchorLeft + (anchorWidth / 2)
        mainPillRect.set(centerX - (wPx / 2), anchorTop, centerX + (wPx / 2), anchorTop + hPx)
        insetsUpdateFlow.tryEmit(Unit)
    }
    
    LaunchedEffect(state, model, isLandscape) {
        if (!isAttachedToWindow) return@LaunchedEffect
        val wp = windowParams ?: return@LaunchedEffect
        val wm = windowManager ?: return@LaunchedEffect

        if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }
        if (state == IslandState.HIDDEN || isLandscape) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() }

        if (model?.isCritical == true) {
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            try {
                val privateFlagsField = WindowManager.LayoutParams::class.java.getField("privateFlags")
                privateFlagsField.setInt(wp, privateFlagsField.getInt(wp) or 0x00000040)
            } catch (e: Exception) {}
        }
        wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = WindowManager.LayoutParams.MATCH_PARENT
        try { wm.updateViewLayout(view, wp) } catch (e: Exception) {}
    }

    val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { 
                androidx.compose.ui.unit.IntOffset(
                    offsetX.dp.roundToPx(), 
                    offsetY.coerceAtLeast(0f).dp.roundToPx()
                ) 
            }
            .height(maxH.value.dp),
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(animatedWidth) 
                .height(animatedHeight)
                .onGloballyPositioned { coordinates ->
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    val bounds = coordinates.boundsInRoot()
                    anchorLeft = location[0] + bounds.left.toInt()
                    anchorTop = location[1] + bounds.top.toInt()
                    anchorWidth = bounds.width.toInt()
                }
                .graphicsLayer { scaleX = touchScale * islandScale; scaleY = touchScale * islandScale; alpha = islandAlpha; transformOrigin = TransformOrigin(0.5f, 0.5f) }
                .shadow(elevation = if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp, shape = RoundedCornerShape(animatedRadius), spotColor = Color.Black)
                .clip(RoundedCornerShape(animatedRadius))
                .background(bgColor)
                .border(0.5.dp, borderColor, RoundedCornerShape(animatedRadius))
                .pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial); isSquished = true; waitForUpOrCancellation(pass = PointerEventPass.Initial); isSquished = false }
                }
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = { 
                            if (state != IslandState.TYPE_3_MAX) { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // 🚀 HARCODED FALLBACK: If gesture matrix is missing, default to EXPAND
                                onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) ?: run { onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) }
                            } 
                        },
                        onDoubleTap = { if (state != IslandState.TYPE_3_MAX) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) } },
                        onLongPress = { if (state != IslandState.TYPE_3_MAX) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) } }
                    )
                }
                .pointerInput(state) {
                    var dragOffsetX = 0f; var dragOffsetY = 0f 
                    detectDragGestures(
                        onDragEnd = {
                            if (abs(dragOffsetX) > abs(dragOffsetY)) {
                                if (dragOffsetX > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) else if (dragOffsetX < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT)
                            } else {
                                if (dragOffsetY > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) else if (dragOffsetY < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_UP)
                            }
                            dragOffsetX = 0f; dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount -> if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) { change.consume() }; dragOffsetX += dragAmount.x; dragOffsetY += dragAmount.y }
                    )
                },
            contentAlignment = boxAlignment
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                
                if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                    val bgBitmap = if (theme.blurIntensity > 0.dp && model.blurredAlbumArt != null) { model.blurredAlbumArt.asImageBitmap() } else model.albumArt.asImageBitmap()
                    Image(bitmap = bgBitmap, contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(if (state == IslandState.TYPE_3_MAX) 0.5f else 0.25f).blur(if (state == IslandState.TYPE_3_MAX) theme.blurIntensity else (theme.blurIntensity + 8.dp)))
                }

                if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING) {
                    val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                        AnimatedContent(
                            targetState = state,
                            transitionSpec = { (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90))) togetherWith fadeOut(animationSpec = tween(90)) },
                            label = "UI Transition"
                        ) { s ->
                            when (s) {
                                IslandState.TYPE_3_MAX -> { 
                                    when (model) { 
                                        is LiveActivityModel.Dashboard -> IslandDashboardMax(
                                            dashboardModel = model,
                                            currentMedia = activeMedia,
                                            onSliderDrag = { type, pct -> 
                                                if (type == "VOL") onVolumeDrag?.invoke(pct.toInt()) else onBrightnessDrag?.invoke(pct.toInt()) 
                                            },
                                            onQsClick = { tileSpec -> onQsTileClick?.invoke(tileSpec) }
                                        )
                                        is LiveActivityModel.Music -> MusicMax(model) 
                                        is LiveActivityModel.Charging -> ChargingMax(model) // 🚀 ADDED ROUTE HERE
                                        else -> {} 
                                    } 
                                }
                                IslandState.TYPE_2_MID -> { when (model) { is LiveActivityModel.Dashboard -> DashboardMid(model); is LiveActivityModel.Call -> CallMid(model); is LiveActivityModel.Music -> MusicMid(model); is LiveActivityModel.General -> GeneralMid(model); is LiveActivityModel.Charging -> ChargingMid(model); is LiveActivityModel.SystemAlert -> SystemAlertMid(model); is LiveActivityModel.AppTimerWarning -> AppTimerWarningMid(model); is LiveActivityModel.OngoingTask -> OngoingTaskMid(model); else -> {} } }
                                IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { when (model) { is LiveActivityModel.Call -> CallMini(model); is LiveActivityModel.Music -> MusicMini(model); is LiveActivityModel.General -> GeneralMini(model); is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model); is LiveActivityModel.RealityPill -> RealityPillMini(model); else -> {} } }
                                IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                                else -> {} 
                            }
                        }
                     }

                    if (state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(10.dp).padding(bottom = 4.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha=0.25f), CircleShape)) }
                    }
                }
                if (state == IslandState.TYPE_0_RING) { RingUI(model) }
             }
        }
        SplitCubeUI(state, animatedHeight, borderColor)
    }
}
