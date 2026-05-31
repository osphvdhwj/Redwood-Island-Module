// File: app/src/main/java/com/example/dynamicisland/ui/IslandMainUI.kt
package com.example.dynamicisland.ui

import com.example.dynamicisland.settings.IconPack

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.gesture.IslandGesture
import com.example.dynamicisland.ui.animations.IslandUiState
import com.example.dynamicisland.ui.animations.updateIslandTransition
import androidx.compose.foundation.Canvas

import com.example.dynamicisland.performance.metaballFluid

@Composable
fun PrivacyDotUI(op: String?) {
    if (op == null) return
    
    val color = if (op == "CAMERA") Color.Green else Color(0xFFFFA500) // Orange for MIC
    
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape)
            .shadow(4.dp, CircleShape)
    )
}

@Composable
fun EdgeLightUI(isActive: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = if (isActive) tween(300) else tween(800),
        label = "edge_light_alpha"
    )
    
    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Cyan, Color.Magenta, Color.Yellow, Color.Cyan
                        )
                    ),
                    shape = RoundedCornerShape(100f) // Removed invalid pseudo-code
                )
                .blur(4.dp)
                .alpha(alpha)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView.IslandUI(state: IslandState) {
    android.util.Log.d("IslandMainUI", "Composing IslandUI -> State: $state")
    val view = this
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val settings = view.controller?.settingsState ?: com.example.dynamicisland.settings.SettingsState()
    val isCyberpunk = settings.iconPack is IconPack.AmoledCyberpunk

    val uiState = when (state) {
        IslandState.HIDDEN -> IslandUiState.HIDDEN
        IslandState.TYPE_0_RING -> IslandUiState.NOTIFICATION_RING
        IslandState.TYPE_1_MINI, IslandState.TYPE_2_MID, IslandState.TYPE_CUBE -> IslandUiState.COMPACT
        IslandState.TYPE_3_MAX -> IslandUiState.MAX_PILL
        IslandState.TYPE_SPLIT -> IslandUiState.SPLIT_PILL
    }

    val animValues = updateIslandTransition(
        targetState = uiState,
        isCyberpunk = isCyberpunk,
        miniWidth = if (state == IslandState.TYPE_2_MID) midW.value else if (state == IslandState.TYPE_CUBE) cubeW.value else miniW.value,
        miniHeight = if (state == IslandState.TYPE_2_MID) midH.value else if (state == IslandState.TYPE_CUBE) cubeH.value else miniH.value,
        maxWidth = maxW.value,
        maxHeight = if (activeModel.value is LiveActivityModel.Music) (maxH.value * 0.70f) else maxH.value,
        ringWidth = ringW.value,
        ringHeight = ringH.value,
        miniRadius = if (state == IslandState.TYPE_2_MID) midR.value else if (state == IslandState.TYPE_CUBE) cubeR.value else miniR.value,
        maxRadius = maxR.value,
        ringRadius = ringR.value
    )

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isSquished by remember { mutableStateOf(false) }
    val touchScale by animateFloatAsState(
        targetValue = if (isSquished) 0.94f else 1f, 
        animationSpec = IslandPhysics.springFloat,
        label = "squish"
    )
    
    val minSafeWidth = view.displayCutoutWidth.floatValue + 4f
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val maxSafeWidth = (screenWidthDp - 16f).coerceAtLeast(minSafeWidth)

    val animatedWidth = animValues.width.coerceIn(minSafeWidth.dp, maxSafeWidth.dp)
    val animatedHeight = animValues.height
    val animatedRadius = animValues.cornerRadius.coerceAtLeast(0.dp)
    val offsetX = when (state) {
        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value
        IslandState.TYPE_2_MID -> midX.value
        IslandState.TYPE_3_MAX -> maxX.value
        IslandState.TYPE_CUBE -> cubeX.value
        else -> ringX.value
    }
    val offsetY = when (state) {
        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value
        IslandState.TYPE_2_MID -> midY.value
        IslandState.TYPE_3_MAX -> maxY.value
        IslandState.TYPE_CUBE -> cubeY.value
        else -> ringY.value
    }

    val islandScale = animValues.scale
    val islandAlpha = animValues.alpha

    val model = activeModel.value
    
    val bgColor by animateColorAsState(
        targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else {
            if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) {
                Color(model.dominantColor).copy(alpha = 0.85f)
            } else {
                Color.Black // True AMOLED Black
            }
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bgColor"
    )

    val borderColor = animValues.borderColor.copy(alpha = 0.8f)

    
    // particle burst
    var particleTrigger by remember { mutableStateOf(0L) }
    val particles = remember { mutableStateListOf<Particle>() }
    
    LaunchedEffect(state) {
        if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING) {
            particleTrigger = System.currentTimeMillis()
            repeat(12) { index ->
                val angle = Random.nextFloat() * 360f
                val speed = Random.nextFloat() * 400f + 200f
                particles.add(Particle(angle, speed, particleTrigger))
            }
            delay(1500)
            particles.clear()
        }
    }
    
    LaunchedEffect(state, model, isLandscape) {
        if (!isAttachedToWindow) return@LaunchedEffect
        val wp = windowParams ?: return@LaunchedEffect
        val wm = windowManager ?: return@LaunchedEffect

        if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } 
        else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }
        if (state == IslandState.HIDDEN || isLandscape) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE } 
        else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() }

        if (model?.isCritical == true) {
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            try {
                val privateFlagsField = WindowManager.LayoutParams::class.java.getField("privateFlags")
                privateFlagsField.setInt(wp, privateFlagsField.getInt(wp) or 0x00000040)
            } catch (e: Exception) {}
        }
        wp.width = WindowManager.LayoutParams.MATCH_PARENT
        wp.height = WindowManager.LayoutParams.MATCH_PARENT
        try { wm.updateViewLayout(view, wp) } catch (e: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { 
                androidx.compose.ui.unit.IntOffset(
                    offsetX.dp.roundToPx(), 
                    offsetY.dp.roundToPx() // Allow full freedom on Y-axis
                ) 
            }
            .height(maxH.value.dp),
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.Top
    ) {
        val shadowElevation by animateDpAsState(
            targetValue = if (isSquished) 4.dp else (if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
        )
        
        Box(
            modifier = Modifier
                .width(animatedWidth) 
                .height(animatedHeight)
                .graphicsLayer { 
                    scaleX = islandScale; scaleY = islandScale; alpha = islandAlpha; 
                    transformOrigin = TransformOrigin(0.5f, 0.5f) 
                }
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInWindow()
                    val newLeft = bounds.left.toInt()
                    val newTop = bounds.top.toInt()
                    val newRight = bounds.right.toInt()
                    val newBottom = bounds.bottom.toInt()
                    if (view.mainPillRect.value.left != newLeft || view.mainPillRect.value.top != newTop || 
                        view.mainPillRect.value.right != newRight || view.mainPillRect.value.bottom != newBottom) {
                        view.mainPillRect.value.set(newLeft, newTop, newRight, newBottom)
                        view.insetsUpdateFlow.tryEmit(Unit)
                    }
                }
                .shadow(elevation = shadowElevation, shape = RoundedCornerShape(animatedRadius), spotColor = Color.Black)
                .clip(RoundedCornerShape(animatedRadius))
                .then(
                    if (settings.designLanguage == com.example.dynamicisland.settings.DesignLanguage.APPLE_LIQUID_GLASS) {
                        Modifier.glassBackground(blurRadius = settings.blurIntensity.dp)
                    } else if (settings.dynamicGradient && model is LiveActivityModel.Music) {
                        val gradientColors: List<Color> = view.controller?.currentGradientColors ?: listOf(Color.DarkGray, Color.Black)
                        Modifier.background(Brush.verticalGradient(gradientColors))
                    } else if (state == IslandState.TYPE_2_MID && model is LiveActivityModel.Music && model.dominantColor != null) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(model.dominantColor).copy(alpha = 0.85f),
                                    Color(model.dominantColor).copy(alpha = 0.5f)
                                )
                            )
                        )
                    } else {
                        Modifier.background(bgColor)
                    }
                )
                .border(0.5.dp, borderColor, RoundedCornerShape(animatedRadius))
                .pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial); isSquished = true; waitForUpOrCancellation(pass = PointerEventPass.Initial); isSquished = false }
                }
                .pointerInput(state) {
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
                                // Reset elastic scale
                                scope.launch { elasticScale.animateTo(1f, spring()) }
                            },
                            onDragCancel = { 
                                dragOffsetX = 0f; dragOffsetY = 0f
                                scope.launch { elasticScale.snapTo(1f) }
                            },
                            onDrag = { change, dragAmount -> 
                                if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) { change.consume() }
                                dragOffsetX += dragAmount.x; dragOffsetY += dragAmount.y 
                                // Apply elastic scale based on vertical drag
                                val newScale = (elasticScale.value + dragAmount.y * 0.001f).coerceIn(0.85f, 1.15f)
                                scope.launch { elasticScale.snapTo(newScale) }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Privacy Dot Placement
            if (view.activePrivacyOp.value != null && state != IslandState.HIDDEN) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                ) {
                    PrivacyDotUI(view.activePrivacyOp.value)
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                
                if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                    // FIXED: Safe fallback for blur
                    val bgBitmap = if (model.blurredAlbumArt != null) model.blurredAlbumArt.asImageBitmap() else model.albumArt.asImageBitmap()
                    Image(
                        bitmap = bgBitmap,
                        contentDescription = "Cinematic BG",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (state == IslandState.TYPE_3_MAX) 0.5f else 0.25f)
                            .blur(if (state == IslandState.TYPE_3_MAX) 16.dp else 24.dp)
                    )
                }

                // Infinity X Edge Light Overlay
                val edgeLightAlpha by animateFloatAsState(
                    targetValue = if (view.edgeLightActive.value) 1f else 0f,
                    animationSpec = if (view.edgeLightActive.value) tween(300) else tween(1000),
                    label = "edge_light_alpha"
                )
                if (edgeLightAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Cyan)
                                ),
                                shape = RoundedCornerShape(animatedRadius)
                            )
                            .blur(4.dp)
                            .alpha(edgeLightAlpha)
                    )
                }

                if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING) {
                    val bottomPadding by animateDpAsState(
                        targetValue = when(state) { 
                            IslandState.TYPE_3_MAX -> 24.dp
                            IslandState.TYPE_2_MID -> 16.dp
                            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp
                            else -> 0.dp 
                        },
                        label = "bottomPadding"
                    )
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                        
                        // Metaball Shader Overlay (Android 13+)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
                            state == IslandState.TYPE_SPLIT && metaballTearProgress.value > 0.01f) {
                            
                            val density = LocalDensity.current
                            val splitCenter = with(density) {
                                androidx.compose.ui.geometry.Offset(
                                    x = (animValues.width / 2 + animValues.xOffset.dp).toPx(),
                                    y = (animValues.height / 2).toPx()
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .metaballFluid(
                                        pill1 = mainPillRect,
                                        pill2Center = splitCenter,
                                        pill2Radius = with(density) { (animatedHeight / 2).toPx() },
                                        blobiness = 0.5f * (1f - metaballTearProgress.value),
                                        color = Color.Black
                                    )
                            )
                        }

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
                                        is LiveActivityModel.Dashboard -> DashboardMax(
                                            model = model,
                                            controller = view.controller!!
                                        )
                                        is LiveActivityModel.Music -> MusicMax(model) 
                                        is LiveActivityModel.Charging -> ChargingMax(model)
                                        is LiveActivityModel.VolumeMixer -> VolumeMixerMax(model)
                                        is LiveActivityModel.NotificationStack -> NotificationStackMax(model)
                                        is LiveActivityModel.QuickNote -> NoteEditorMax(
                                            targetNotesApp = view.controller?.settingsState?.allowedNotesApps?.firstOrNull(),
                                            onSave = { view.controller?.evaluatePriority() }
                                        )
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
                                        is LiveActivityModel.Barcode         -> BarcodeMid(model, settings)
                                        is LiveActivityModel.SystemAlert     -> {
                                            if (model.alertType == "OTP_CATCHER") {
                                                val otpModel = LiveActivityModel.Otp(code = model.message)
                                                OtpMid(otpModel, settings = settings)
                                            }
                                            else SystemAlertMid(model)
                                        }
                                        is LiveActivityModel.General         -> {
                                            when {
                                                model.id == "sys_translation"  -> TranslationMid(model)
                                                model.id == "sys_navigation"
                                                    || (model.type == ActivityType.MESSAGE
                                                        && model.accentColor == android.graphics.Color.parseColor("#34A853"))
                                                -> {
                                                    val navModel = LiveActivityModel.Navigation(
                                                        instruction = model.dataText.ifEmpty { model.title },
                                                        distance = ""
                                                    )
                                                    NavigationMid(navModel)
                                                }
                                                else                           -> GeneralMid(model)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                                IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { 
                                    if (view.controller?.currentHardware?.isGamingModeOn == true && model !is LiveActivityModel.Call) {
                                        GamingHUDMini(
                                            fps        = view.gamingFps.floatValue,
                                            frameMs    = view.gamingFrameMs.floatValue,
                                            jankPct    = view.gamingJankPct.floatValue,
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
                                            is LiveActivityModel.NotificationStack -> {
                                                if (model.totalCount > 1) {
                                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                        Text(
                                                            text = "+${model.totalCount}",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                } else {
                                                    GeneralMini(LiveActivityModel.General(id = model.id, title = model.notifications.firstOrNull()?.title ?: "", dataText = model.notifications.firstOrNull()?.text ?: ""))
                                                }
                                            }
                                            else -> {} 
                                        } 
                                    }
                                }
                                IslandState.TYPE_CUBE -> { 
                                    if (model is LiveActivityModel.Charging) ChargingCube(model) 
                                }
                                else -> {} 
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (particles.isNotEmpty()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val now = System.currentTimeMillis()
                                particles.forEach { p ->
                                    val elapsed = now - p.birthTime
                                    val progress = (elapsed / 1500f).coerceIn(0f, 1f)
                                    val radius = progress * p.speed
                                    val alpha = ((1f - progress) * 0.8f).coerceAtLeast(0f)
                                    val offset = Offset(
                                        center.x + radius * cos(Math.toRadians(p.angle.toDouble())).toFloat(),
                                        center.y + radius * sin(Math.toRadians(p.angle.toDouble())).toFloat()
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = alpha),
                                        radius = 3f,
                                        center = offset
                                    )
                                }
                            }
                        }
                    }

                    if (state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(10.dp)
                                .padding(bottom = 4.dp),
                            contentAlignment = Alignment.Center
                        ) { 
                            Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha=0.25f), CircleShape)) 
                        }
                    }
                }
                if (state == IslandState.TYPE_0_RING) { RingUI(model) }
            }
        }
        SplitCubeUI(state, animatedHeight, borderColor, animValues.xOffset)
    }
}

private data class Particle(
    val angle: Float,
    val speed: Float,
    val birthTime: Long
)