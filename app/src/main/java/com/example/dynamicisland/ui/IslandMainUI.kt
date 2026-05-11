package com.example.dynamicisland.ui
import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*

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
import androidx.compose.ui.layout.boundsInWindow
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
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val maxSafeWidth = (screenWidthDp - 16f).coerceAtLeast(minSafeWidth)

    val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
    val targetWidth = rawTargetWidth.coerceIn(minSafeWidth, maxSafeWidth)
    
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
    val bgSpec = when {
    state == IslandState.TYPE_3_MAX -> tween<androidx.compose.ui.graphics.Color>(400, easing = FastOutSlowInEasing)
    state == IslandState.TYPE_0_RING -> tween<androidx.compose.ui.graphics.Color>(250, easing = LinearOutSlowInEasing)
    else -> spring<androidx.compose.ui.graphics.Color>(dampingRatio = 0.85f, stiffness = 300f)
}
val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = bgSpec, label = "bgColor")
    val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.08f), animationSpec = tween(600), label = "borderColor")
    
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
        val shadowElevation by animateDpAsState(
            targetValue = if (isSquished) 4.dp else (if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
        )
        
        Box(
            modifier = Modifier
                .width(animatedWidth) 
                .height(animatedHeight)
                // 🚀 FIX: graphicsLayer MUST be before onGloballyPositioned so touches map correctly!
                .graphicsLayer { scaleX = islandScale; scaleY = islandScale; alpha = islandAlpha; transformOrigin = TransformOrigin(0.5f, 0.5f) }
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val newLeft = bounds.left.toInt()
                    val newTop = bounds.top.toInt()
                    val newRight = bounds.right.toInt()
                    val newBottom = bounds.bottom.toInt()
                    
                    // 🧠 THE LOOP BREAKER: Only emit an update if the pixel boundary actually changed
                    if (mainPillRect.left != newLeft || mainPillRect.top != newTop || mainPillRect.right != newRight || mainPillRect.bottom != newBottom) {
                        mainPillRect.set(newLeft, newTop, newRight, newBottom)
                        insetsUpdateFlow.tryEmit(Unit)
                    }
                }
                .shadow(elevation = shadowElevation, shape = RoundedCornerShape(animatedRadius), spotColor = Color.Black)
                .clip(RoundedCornerShape(animatedRadius))
                .background(bgColor)
                .border(0.5.dp, borderColor, RoundedCornerShape(animatedRadius))
                .pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial); isSquished = true; waitForUpOrCancellation(pass = PointerEventPass.Initial); isSquished = false }
                }
                .pointerInput(state) {
                    // 🚀 FIX: Do not steal taps if the Dashboard is open!
                    if (state != IslandState.TYPE_3_MAX) {
                        detectTapGestures(
                            onTap = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGestureEvent?.invoke(IslandGesture.SINGLE_TAP)
                            },
                            onDoubleTap = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) 
                            },
                            onLongPress = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onGestureEvent?.invoke(IslandGesture.LONG_PRESS) 
                            }
                        )
                    }
                }
                .pointerInput(state) {
                    // 🚀 FIX: Do not steal drags/swipes if the Dashboard is open!
                    if (state != IslandState.TYPE_3_MAX) {
                        var dragOffsetX = 0f; var dragOffsetY = 0f 
                        detectDragGestures(
                            onDragEnd = {
                                if (abs(dragOffsetX) > abs(dragOffsetY)) {
                                    if (dragOffsetX > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) 
                                    else if (dragOffsetX < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT)
                                } else {
                                    if (dragOffsetY > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) 
                                    else if (dragOffsetY < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_UP)
                                }
                                dragOffsetX = 0f; dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount -> 
                                if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) { change.consume() }
                                dragOffsetX += dragAmount.x; dragOffsetY += dragAmount.y 
                            }
                        )
                    }
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
                            transitionSpec = {
                            val expanding = targetState.ordinal > initialState.ordinal
                            if (expanding) {
                                (fadeIn(animationSpec = tween(300, delayMillis = 60)) + scaleIn(
                                    initialScale = 0.88f,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f)
                                )) togetherWith fadeOut(animationSpec = tween(80))
                            } else {
                                (fadeIn(animationSpec = tween(200)) + scaleIn(
                                    initialScale = 1.04f,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f)
                                )) togetherWith (fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.96f))
                            }
                        },
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
                                        is LiveActivityModel.Charging -> ChargingMax(model)
                                        else -> {} 
                                    } 
                                }
                                IslandState.TYPE_2_MID -> {
                                when (model) {
                                is LiveActivityModel.Dashboard       -> DashboardMid(model)
                                is LiveActivityModel.Call            -> CallMid(model)
                                is LiveActivityModel.Music           -> MusicMid(model)
                                is LiveActivityModel.Charging        -> ChargingMid(model)
                                is LiveActivityModel.AppTimerWarning -> AppTimerWarningMid(model)
                                is LiveActivityModel.OngoingTask     -> OngoingTaskMid(model)
                                is LiveActivityModel.ExternalActivity -> ExternalActivityMid(model)
                                is LiveActivityModel.LinkIntercept   -> LinkInterceptMid(model)
                                is LiveActivityModel.SystemAlert     -> {
                                    // OTP gets its own bespoke view; everything else uses the generic alert
                                    if (model.alertType == "OTP_CATCHER") OtpMid(model)
                                    else SystemAlertMid(model)
                                }
                                is LiveActivityModel.General         -> {
                                    // Translation and Barcode results arrive as General models with a
                                    // special id prefix so they can be routed to their own composables.
                                    when {
                                        model.id == "sys_translation" -> {
                                            // Reconstruct a minimal TranslationResult for display.
                                            // The actual TranslationEngine result is the source of truth;
                                            // here we just render the text stored in title/dataText.
                                            TranslationGeneralMid(model)
                                        }
                                        model.id == "sys_barcode"     -> BarcodeGeneralMid(model)
                                        else                          -> GeneralMid(model)
                                    }
                                }
                                else -> {}
                            }
                        }
                                IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { 
                                    // Gaming HUD takes priority in mini when gaming mode is active
                                    if (view.controller?.currentHardware?.isGamingModeOn == true && model !is LiveActivityModel.Call) {
                                        GamingHUDMini(
                                            fps        = gamingFps.floatValue,
                                            frameMs    = gamingFrameMs.floatValue,
                                            jankPct    = gamingJankPct.floatValue,
                                            cpuTemp    = view.controller?.currentHardware?.cpuTempCelsius ?: 0f,
                                            cpuFreqMhz = view.controller?.currentHardware?.cpuFreqMhz    ?: 0
                                        )
                                    } else {
                                        when (model) { 
                                            is LiveActivityModel.Call -> CallMini(model)
                                            is LiveActivityModel.Music -> MusicMini(model)
                                            is LiveActivityModel.General -> GeneralMini(model)
                                            is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
                                            is LiveActivityModel.RealityPill -> RealityPillMini(model)
                                            else -> {} 
                                        } 
                                    }
                                }
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
