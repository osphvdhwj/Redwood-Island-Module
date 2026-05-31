// File: app/src/main/java/com/example/dynamicisland/ui/IslandMainUI.kt
package com.example.dynamicisland.ui

import com.example.dynamicisland.settings.IconPack

import com.example.dynamicisland.R
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.settings.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.dynamicisland.ui.design.geminiAura

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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView.IslandUI(state: IslandState) {
    val view = this
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val settings = view.controller?.settingsState ?: com.example.dynamicisland.settings.SettingsState()
    val isCyberpunk = settings.iconPack is IconPack.AmoledCyberpunk
    val isLowLatency = settings.enableLowLatencyMode

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

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isSquished by remember { mutableStateOf(false) }
    val touchScale by animateFloatAsState(
        targetValue = if (isSquished) 0.94f else 1f, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "squish"
    )
    
    val minSafeWidth = view.displayCutoutWidth.floatValue + 4f
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val maxSafeWidth = (screenWidthDp - 16f).coerceAtLeast(minSafeWidth)

    val animatedWidth = animValues.width.coerceIn(minSafeWidth.dp, maxSafeWidth.dp)
    val animatedHeight = animValues.height
    val animatedRadius = animValues.cornerRadius.coerceAtLeast(0.dp)
    val offsetX = when (state) {
        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> view.miniX.value
        IslandState.TYPE_2_MID -> view.midX.value
        IslandState.TYPE_3_MAX -> view.maxX.value
        IslandState.TYPE_CUBE -> view.cubeX.value
        else -> view.ringX.value
    }
    val offsetY = when (state) {
        IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> view.miniY.value
        IslandState.TYPE_2_MID -> view.midY.value
        IslandState.TYPE_3_MAX -> view.maxY.value
        IslandState.TYPE_CUBE -> view.cubeY.value
        else -> view.ringY.value
    }

    val model = view.activeModel.value
    
    val bgColor by animateColorAsState(
        targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else {
            if (settings.aestheticStyle == AestheticStyle.VOID_BLACK) {
                Color.Black
            } else if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) {
                Color(model.dominantColor).copy(alpha = 0.85f)
            } else {
                Color.Black
            }
        },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "bgColor"
    )

    val borderColor = if (settings.aestheticStyle == AestheticStyle.VOID_BLACK) Color.White.copy(alpha = 0.1f) else animValues.borderColor.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset { 
                val bX = offsetX.dp.roundToPx()
                val bY = offsetY.dp.roundToPx()
                if (settings.liveBridgeEnabled) {
                    androidx.compose.ui.unit.IntOffset(
                        (bX + view.bridgeOffsetX.floatValue).toInt(),
                        (bY + view.bridgeOffsetY.floatValue).toInt()
                    )
                } else {
                    androidx.compose.ui.unit.IntOffset(bX, bY)
                }
            }
            .height(view.maxH.value.dp),
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.Top
    ) {
        val shadowElevation by animateDpAsState(
            targetValue = if (isSquished) 4.dp else (if (state == IslandState.TYPE_0_RING || isLowLatency) 0.dp else 16.dp),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)
        )
        
        Box(
            modifier = Modifier
                .width(animatedWidth) 
                .height(animatedHeight)
                .graphicsLayer { 
                    val s = view.elasticScale.value * touchScale
                    scaleX = s; scaleY = s; alpha = animValues.alpha
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
                    if (settings.designLanguage == com.example.dynamicisland.settings.DesignLanguage.APPLE_LIQUID_GLASS || settings.aestheticStyle == AestheticStyle.GLASS) {
                        Modifier.glassBackground(blurRadius = settings.blurIntensity.dp, isLowLatency = isLowLatency)
                    } else {
                        Modifier.background(bgColor)
                    }
                )
                .geminiAura(enabled = settings.geminiAuraEnabled && (model?.id?.contains("assistant") == true || model?.id?.contains("ai") == true))
                .border(0.5.dp, borderColor, RoundedCornerShape(animatedRadius))
                .pointerInput(Unit) {
                    awaitEachGesture { awaitFirstDown(pass = PointerEventPass.Initial); isSquished = true; waitForUpOrCancellation(pass = PointerEventPass.Initial); isSquished = false }
                }
                .pointerInput(state) {
                    if (state != IslandState.TYPE_3_MAX) {
                        detectTapGestures(
                            onTap = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                view.onGestureEvent?.invoke(IslandGesture.SINGLE_TAP)
                            },
                            onDoubleTap = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                view.onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) 
                            },
                            onLongPress = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                view.onGestureEvent?.invoke(IslandGesture.LONG_PRESS) 
                            }
                        )
                    }
                }
                .pointerInput(state) {
                    if (state != IslandState.TYPE_3_MAX) {
                        var dragOffsetY = 0f
                        var dragOffsetX = 0f
                        detectDragGestures(
                            onDragStart = { view.isBridgeDragging.value = true },
                            onDragEnd = {
                                view.isBridgeDragging.value = false
                                
                                // 🧲 MAGNETIC SNAPPING
                                if (settings.liveBridgeEnabled && settings.magneticEdgeDocking) {
                                    val screenWidth = configuration.screenWidthDp.toFloat()
                                    val currentGlobalX = view.bridgeOffsetX.floatValue
                                    val targetX = if (currentGlobalX > 0) 140f else -140f 
                                    scope.launch {
                                        Animatable(view.bridgeOffsetX.floatValue).animateTo(targetX, spring(dampingRatio = 0.55f)) { view.bridgeOffsetX.floatValue = value }
                                    }
                                }

                                if (dragOffsetY > 100f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    view.onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) 
                                } else if (dragOffsetY < -40f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    view.onGestureEvent?.invoke(IslandGesture.SWIPE_UP)
                                }
                                scope.launch { view.elasticScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 300f)) }
                            },
                            onDragCancel = { 
                                view.isBridgeDragging.value = false
                                dragOffsetY = 0f
                                scope.launch { view.elasticScale.snapTo(1f) }
                            },
                            onDrag = { change, dragAmount -> 
                                if (abs(dragAmount.y) > 2f || abs(dragAmount.x) > 2f) { change.consume() }
                                dragOffsetY += dragAmount.y 
                                dragOffsetX += dragAmount.x
                                
                                if (settings.liveBridgeEnabled) {
                                    view.bridgeOffsetX.floatValue += dragAmount.x
                                    view.bridgeOffsetY.floatValue += dragAmount.y
                                }

                                if (settings.velocitySquishEnabled) {
                                    val squishAmount = (dragOffsetY / 1000f).coerceIn(-0.15f, 0.25f)
                                    scope.launch { view.elasticScale.snapTo(1f + squishAmount) }
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(start = view.padL.value.dp, top = view.padT.value.dp, end = view.padR.value.dp, bottom = view.padB.value.dp)) {
                
                if (!isLowLatency && (state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
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
                        
                        // Metaball Shader Overlay
                        if (!isLowLatency && settings.enableMetaballTear && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
                            state == IslandState.TYPE_SPLIT && view.metaballTearProgress.value > 0.01f) {
                            
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
                                        pill1 = view.mainPillRect.value,
                                        pill2Center = splitCenter,
                                        pill2Radius = with(density) { (animatedHeight / 2).toPx() },
                                        blobiness = 0.5f * (1f - view.metaballTearProgress.value),
                                        color = Color.Black
                                    )
                            )
                        }

                        AnimatedContent(
                            targetState = state,
                            transitionSpec = {
                                when (settings.contentTransitionStyle) {
                                    ContentTransitionStyle.FADE_SCALE -> {
                                        fadeIn(tween(300)) + scaleIn(initialScale = 0.9f) togetherWith fadeOut(tween(200)) + scaleOut(targetScale = 0.9f)
                                    }
                                    ContentTransitionStyle.FLIP -> {
                                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                                    }
                                    else -> { // SLIDE
                                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                    }
                                }
                            },
                            label = "UI Transition"
                        ) { s ->
                            when (s) {
                                IslandState.TYPE_3_MAX -> { 
                                    when (model) { 
                                        is LiveActivityModel.Dashboard -> DashboardMax(model, view.controller!!)
                                        is LiveActivityModel.Music -> MusicMax(model) 
                                        is LiveActivityModel.Charging -> ChargingMax(model)
                                        is LiveActivityModel.VolumeMixer -> VolumeMixerMax(model)
                                        is LiveActivityModel.NotificationStack -> NotificationStackMax(model)
                                        is LiveActivityModel.QuickNote -> NoteEditorMax(view.controller?.settingsState?.allowedNotesApps?.firstOrNull()) { view.controller?.evaluatePriority() }
                                        else -> {} 
                                    } 
                                }
                                IslandState.TYPE_2_MID -> {
                                    when (model) {
                                        is LiveActivityModel.Dashboard       -> DashboardMid(model)
                                        is LiveActivityModel.Call            -> CallMid(model)
                                        is LiveActivityModel.Music           -> MusicMid(model)
                                        is LiveActivityModel.Charging        -> ChargingMid(model)
                                        is LiveActivityModel.OngoingTask     -> OngoingTaskMid(model)
                                        is LiveActivityModel.SystemAlert     -> SystemAlertMid(model)
                                        is LiveActivityModel.General         -> GeneralMid(model)
                                        else -> {}
                                    }
                                }
                                IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { 
                                    when (model) { 
                                        is LiveActivityModel.Call -> CallMini(model)
                                        is LiveActivityModel.Music -> MusicMini(model)
                                        is LiveActivityModel.General -> GeneralMini(model)
                                        is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
                                        else -> {} 
                                    } 
                                }
                                IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                                else -> {} 
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

                // 📎 Clipboard Paperclip Indicator
                if (settings.enableClipboardPaperclip && view.clipboardStashCount.intValue > 0 && state != IslandState.HIDDEN) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(14.dp)
                            .background(Color.White.copy(0.2f), CircleShape)
                            .border(1.dp, Color.White.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.AttachFile, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
        SplitCubeUI(state, animatedHeight, borderColor, animValues.xOffset)
    }
}
