package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image 
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    init { savedStateRegistryController.performRestore(Bundle()) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    fun attach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun detach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)
    var cubeW = mutableStateOf(85f); var cubeH = mutableStateOf(85f); var cubeX = mutableStateOf(0f); var cubeY = mutableStateOf(48f)

    var padT = mutableStateOf(0f); var padB = mutableStateOf(0f); var padL = mutableStateOf(0f); var padR = mutableStateOf(0f)
    var ringThickness = mutableStateOf(6f)
    var expandUpwards = mutableStateOf(false)

    var isCubeRotationEnabled = mutableStateOf(true)
    var customOffsetY = mutableFloatStateOf(0f)
    var customBaseWidth = mutableFloatStateOf(100f)
    var activeTheme = mutableStateOf(IslandTheme())
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)
    
    var qsTiles = mutableStateListOf<String>("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")
    var pinnedApps = mutableStateListOf<String>("", "", "", "", "", "", "", "")

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) 

    fun updateTicker(pos: Long) { currentMediaPos.longValue = pos }
    fun updateBattery(level: Int, isCharging: Boolean) { globalBatteryLevel.intValue = level; globalIsCharging.value = isCharging }

    var onGestureEvent: ((IslandGesture) -> Unit)? = null
    var onGestureSettingsUpdated: ((String?) -> Unit)? = null
    var onSplitPillClick: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onAudioOutputClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    private fun loadPreferences() {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable(); pref.reload()
            ringW.value = pref.getFloat("ring_w", 45f); ringH.value = pref.getFloat("ring_h", 45f); ringX.value = pref.getFloat("ring_x", 0f); ringY.value = pref.getFloat("ring_y", 48f)
            miniW.value = pref.getFloat("mini_w", 180f); miniH.value = pref.getFloat("mini_h", 36f); miniX.value = pref.getFloat("mini_x", 0f); miniY.value = pref.getFloat("mini_y", 48f)
            midW.value = pref.getFloat("mid_w", 320f); midH.value = pref.getFloat("mid_h", 80f); midX.value = pref.getFloat("mid_x", 0f); midY.value = pref.getFloat("mid_y", 48f)
            maxW.value = pref.getFloat("max_w", 360f); maxH.value = pref.getFloat("max_h", 220f); maxX.value = pref.getFloat("max_x", 0f); maxY.value = pref.getFloat("max_y", 48f)
            cubeW.value = pref.getFloat("cube_w", 85f); cubeH.value = pref.getFloat("cube_h", 85f); cubeX.value = pref.getFloat("cube_x", 0f); cubeY.value = pref.getFloat("cube_y", 48f)
            padT.value = pref.getFloat("pad_t", 0f); padB.value = pref.getFloat("pad_b", 0f); padL.value = pref.getFloat("pad_l", 0f); padR.value = pref.getFloat("pad_r", 0f)
            ringThickness.value = pref.getFloat("ring_thickness", 6f)
            expandUpwards.value = pref.getBoolean("expand_upwards", false)
            isCubeRotationEnabled.value = pref.getBoolean("rotate_cube", true)
        } catch (e: Throwable) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                val prefix = intent.getStringExtra("prefix")
                if (prefix != null) {
                    val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f); val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                    when (prefix) { "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }; "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }; "mid" -> { midW.value = w; midH.value = h; midX.value = x; midY.value = y }; "max" -> { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y }; "cube" -> { cubeW.value = w; cubeH.value = h; cubeX.value = x; cubeY.value = y } }
                    padT.value = intent.getFloatExtra("pad_t", padT.value); padB.value = intent.getFloatExtra("pad_b", padB.value); padL.value = intent.getFloatExtra("pad_l", padL.value); padR.value = intent.getFloatExtra("pad_r", padR.value)
                    ringThickness.value = intent.getFloatExtra("ring_thickness", ringThickness.value)
                    expandUpwards.value = intent.getBooleanExtra("expand_upwards", expandUpwards.value)
                    
                    for (i in 0..7) { val pkg = intent.getStringExtra("pinned_app_$i"); if (pkg != null) pinnedApps[i] = pkg }
                    for (i in 0..6) { val qs = intent.getStringExtra("qs_tile_$i"); if (qs != null) qsTiles[i] = qs }
                } 

                activeTheme.value = IslandTheme(
                    buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp,
                    buttonSpacing = intent.getFloatExtra("theme_button_spacing", 16f).dp,
                    buttonCornerRadius = intent.getFloatExtra("theme_button_radius", 50f).dp,
                    actionAnimType = intent.getStringExtra("theme_anim_type") ?: "BOUNCE"
                )
                val payload = intent.getStringExtra("gesture_payload")
                if (payload != null) onGestureSettingsUpdated?.invoke(payload) else loadPreferences()
            }
        }
    }

    init {
        loadPreferences()
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { lifecycleOwner.attach() }
                override fun onViewDetachedFromWindow(v: View) { lifecycleOwner.detach() }
            })
            
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CompositionLocalProvider(LocalContext provides moduleContext, LocalIslandTheme provides activeTheme.value) {
                        IslandUI(islandState.value)
                    }
                }
            }
        }
        val coroutineContext = AndroidUiDispatcher.CurrentThread; val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }
        addView(composeView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter("com.example.dynamicisland.RELOAD_PREFS")
        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter, securePermission, null)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { context.unregisterReceiver(receiver) } catch (e: Throwable) {}
        BatteryPlugin.stop(context)
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val haptic = LocalHapticFeedback.current
        
        var isSquished by remember { mutableStateOf(false) }
        val squishX by animateFloatAsState(targetValue = if (isSquished) 1.03f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "sx")
        val squishY by animateFloatAsState(targetValue = if (isSquished) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "sy")

        val targetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.72f, stiffness = 200f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "y")
        
        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        val model = activeModel.value
        val bgColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.Black, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.08f), animationSpec = tween(600), label = "borderColor")

        LaunchedEffect(state, model, expandUpwards.value) {
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect

            if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }

            if (state == IslandState.HIDDEN) {
                wp.width = 0 
                wp.height = 0 
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            }
            
            wp.gravity = if (expandUpwards.value) (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL) else (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            
            try { 
                wm.updateViewLayout(this@DynamicIslandView, wp) 
            } catch (e: Exception) {
                kotlinx.coroutines.yield()
                try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (ignore: Exception) {}
            }
        }

        val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter
        var currentWindowHeight by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = offsetY.coerceAtLeast(0f).dp)
                .padding(bottom = 20.dp)
                .onSizeChanged { size ->
                    if (currentWindowHeight != size.height && size.height > 0) {
                        currentWindowHeight = size.height
                        if (state != IslandState.HIDDEN) {
                            val wp = windowParams ?: return@onSizeChanged
                            val wm = windowManager ?: return@onSizeChanged
                            wp.height = size.height
                            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch(e: Throwable){}
                        }
                    }
                },
            contentAlignment = boxAlignment
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
            ) {
                
                Box { 
                    if (model is LiveActivityModel.SystemAlert || model is LiveActivityModel.RealityPill) {
                        val infiniteTransition = rememberInfiniteTransition(label="glow")
                        val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse), label="glowAlpha")
                        val alertColor = (model as? LiveActivityModel.SystemAlert)?.alertColor?.let { Color(it) } ?: Color(0xFF00FFCC)
                        Box(modifier = Modifier.width(width).height(height).blur(32.dp).background(Brush.radialGradient(colors = listOf(alertColor.copy(alpha = glowAlpha), Color.Transparent))))
                    }

                    Box(
                        modifier = Modifier
                            .width(width).height(height)
                            .graphicsLayer { 
                                scaleX = squishX; scaleY = squishY 
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .clip(RoundedCornerShape(rad))
                            .background(bgColor).border(0.5.dp, borderColor, RoundedCornerShape(rad))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(pass = PointerEventPass.Initial)
                                    isSquished = true
                                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                    isSquished = false
                                }
                            }
                            .pointerInput(state) {
                                if (state != IslandState.TYPE_3_MAX) {
                                    detectTapGestures(
                                        onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                                        onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) },
                                        onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                                    )
                                }
                            }
                            .pointerInput(state) {
                                var dragOffsetX = 0f
                                var dragOffsetY = 0f
                                
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
                                    }
                                ) { change, dragAmount ->
                                    if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) {
                                        change.consume()
                                    }
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                }
                            },
                        contentAlignment = boxAlignment
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.coerceAtLeast(0f).dp, top = padT.value.coerceAtLeast(0f).dp, end = padR.value.coerceAtLeast(0f).dp, bottom = padB.value.coerceAtLeast(0f).dp)) {
                            
                            if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                                Image(
                                    bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, 
                                    modifier = Modifier.fillMaxSize()
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(brush = Brush.horizontalGradient(0.0f to Color.Transparent, 0.8f to Color.Black, 1.0f to Color.Black))
                                    }
                                    .alpha(if (state == IslandState.TYPE_3_MAX) 0.65f else 0.35f).blur(if (state == IslandState.TYPE_3_MAX) 12.dp else 24.dp)
                                )
                            }

                            if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING) {
                                val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")
                                Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                                    AnimatedContent(
                                        targetState = state,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90))) togetherWith fadeOut(animationSpec = tween(90))
                                        },
                                        label = "UI Transition"
                                    ) { s ->
                                        when (s) {
                                            IslandState.TYPE_3_MAX -> { if (model is LiveActivityModel.Dashboard) DashboardMax(model) else if (model is LiveActivityModel.Music) MusicMax(model) }
                                            IslandState.TYPE_2_MID -> { 
                                                when (model) {
                                                    is LiveActivityModel.Dashboard -> DashboardMid(model)
                                                    is LiveActivityModel.Music -> MusicMid(model)
                                                    is LiveActivityModel.General -> GeneralMid(model)
                                                    is LiveActivityModel.Charging -> ChargingMid(model)
                                                    is LiveActivityModel.SystemAlert -> SystemAlertMid(model)
                                                    is LiveActivityModel.AppTimerWarning -> AppTimerWarningMid(model)
                                                    else -> {}
                                                }
                                            }
                                            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> {
                                                when (model) {
                                                    is LiveActivityModel.Music -> MusicMini(model)
                                                    is LiveActivityModel.General -> GeneralMini(model)
                                                    is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
                                                    is LiveActivityModel.RealityPill -> RealityPillMini(model)
                                                    else -> {}
                                                }
                                            }
                                            IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                                            else -> {} 
                                        }
                                    }
                                }

                                if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX || state == IslandState.TYPE_SPLIT) {
                                    Box(
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(when(state) { IslandState.TYPE_3_MAX -> 32.dp; IslandState.TYPE_2_MID -> 20.dp; else -> 16.dp }),
                                        contentAlignment = Alignment.Center
                                    ) { Box(modifier = Modifier.width(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 24.dp else 40.dp).height(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 3.dp else 5.dp).shadow(2.dp, CircleShape).background(Color.White.copy(alpha=0.8f), CircleShape)) }
                                }
                            }
                            
                            if (state == IslandState.TYPE_0_RING) {
                                val musicModel = model as? LiveActivityModel.Music
                                val isMedia = musicModel != null && musicModel.isPlaying
                                val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.intValue <= 20

                                if (shouldShowRing) {
                                    val baseColor = if (isMedia) {
                                        musicModel?.dominantColor?.let { Color(it) } ?: Color.White
                                    } else if (globalIsCharging.value) {
                                        Color.Green
                                    } else if (globalBatteryLevel.intValue <= 20) {
                                        Color.Red
                                    } else {
                                        Color.White
                                    }

                                    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = if (globalIsCharging.value && !isMedia) 0.3f else 1f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                        label = "alpha"
                                    )
                                    val progressColor = baseColor.copy(alpha = pulseAlpha)

                                    Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp).align(Alignment.Center)) {
                                        val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                                        val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }
                                        val strokeW = ringThickness.value.dp.toPx() 
                                        val inset = strokeW / 2
                                        val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                                        val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                                        val progressPercent = progress.coerceIn(0f, 1f)
                                        val capStyle = if (progressPercent >= 0.99f) StrokeCap.Butt else StrokeCap.Round

                                        val sweepGradient = Brush.sweepGradient(0.0f to progressColor.copy(alpha = 0.2f), 0.8f to progressColor, 1.0f to progressColor.copy(alpha = 0.2f))

                                        drawArc(color = baseColor.copy(alpha=0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))
                                        drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = 360f * progressPercent, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW + 6f, cap = capStyle), alpha = 0.4f)
                                        drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = 360f * progressPercent, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = capStyle))
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = state == IslandState.TYPE_SPLIT, enter = scaleIn(spring(dampingRatio=0.8f, stiffness=300f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                    val sModel = splitModel.value
                    val splitBg = if (sModel is LiveActivityModel.Charging) { if (sModel.isPluggedIn) Color.Green.copy(alpha=0.2f) else if (sModel.level <= 20) Color.Red.copy(alpha=0.2f) else Color(0xFF121212).copy(alpha=0.75f) } else Color(0xFF121212).copy(alpha=0.75f)
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(height)
                                .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSplitPillClick?.invoke() },
                            contentAlignment = Alignment.Center) {
                            if (sModel is LiveActivityModel.Charging) { val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White; Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
