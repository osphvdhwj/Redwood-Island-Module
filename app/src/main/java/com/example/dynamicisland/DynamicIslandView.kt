package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.channels.BufferOverflow

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun start() { savedStateRegistryController.performRestore(android.os.Bundle()); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
}

@OptIn(kotlinx.coroutines.FlowPreview::class) // 🚀 FIX: Opt-in to debounce preview
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
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) 

    // 🚀 FIX: Direct update endpoints (Replaces Broadcasts)
    fun updateTicker(pos: Long) {
        currentMediaPos.longValue = pos
    }

    fun updateBattery(level: Int, isCharging: Boolean) {
        globalBatteryLevel.intValue = level
        globalIsCharging.value = isCharging
    }

    // 🚀 THE UNIFIED EVENT SINK
    var onGestureEvent: ((IslandGesture) -> Unit)? = null
    var onGestureSettingsUpdated: ((String?) -> Unit)? = null

    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onAudioOutputClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()
    private val mainPillRect = android.graphics.Rect()
    private val splitCubeRect = android.graphics.Rect()

    // 🚀 NEW: The Debouncer Flow
    private val boundsUpdateFlow = MutableSharedFlow<android.graphics.Rect>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

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
        } catch (e: Exception) {}
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
                } 
                val payload = intent.getStringExtra("gesture_payload")
                if (payload != null) onGestureSettingsUpdated?.invoke(payload) else loadPreferences()
            }
        }
    }

    init {
        loadPreferences()
        // 🚀 FIX: Only listen for RELOAD_PREFS
        val filter = IntentFilter("com.example.dynamicisland.RELOAD_PREFS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        setViewTreeLifecycleOwner(lifecycleOwner); setViewTreeSavedStateRegistryOwner(lifecycleOwner); setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val listener = java.lang.reflect.Proxy.newProxyInstance(context.classLoader, arrayOf(listenerClass)) { _, method, args ->
                if (method.name == "onComputeInternalInsets") {
                    val info = args[0]
                    val touchableInsetsRegion = info.javaClass.getField("TOUCHABLE_INSETS_REGION").getInt(null)
                    info.javaClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType).invoke(info, touchableInsetsRegion)
                    val region = info.javaClass.getField("touchableRegion").get(info) as android.graphics.Region
                    region.setEmpty()
                    if (islandState.value != IslandState.HIDDEN) {
                        if (!mainPillRect.isEmpty) region.op(mainPillRect, android.graphics.Region.Op.UNION)
                        if (islandState.value == IslandState.TYPE_SPLIT && !splitCubeRect.isEmpty) region.op(splitCubeRect, android.graphics.Region.Op.UNION)
                    }
                }
                null
            }
            viewTreeObserver.javaClass.getMethod("addOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, listener)
        } catch (e: Exception) {}

        val composeView = ComposeView(context).apply { setContent { MaterialTheme(colorScheme = darkColorScheme()) { CompositionLocalProvider(LocalContext provides moduleContext) { IslandUI(islandState.value) } } } }
        val coroutineContext = AndroidUiDispatcher.CurrentThread; val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }

        // 🚀 NEW: Safely debounce layout requests to save the Choreographer
        CoroutineScope(coroutineContext).launch {
            boundsUpdateFlow.debounce(50).collect { rect ->
                mainPillRect.set(rect)
                composeView.requestLayout() // Much lighter than dispatchOnGlobalLayout!
            }
        }

        addView(composeView); lifecycleOwner.start()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val targetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.82f, stiffness = 350f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "y")
        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        val model = activeModel.value

        val targetBgColor = if (state == IslandState.HIDDEN) Color.Transparent 
        else if (state == IslandState.TYPE_0_RING) Color.Black.copy(alpha = 0.01f) 
        else {
            if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) Color(model.dominantColor).copy(alpha = 0.65f) 
            else if (state == IslandState.TYPE_3_MAX) Color(0xFF121212).copy(alpha = 0.4f) 
            else Color(0xFF121212).copy(alpha = 0.75f) 
        }
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.15f), animationSpec = tween(600), label = "borderColor")

        LaunchedEffect(state) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) {
                wp.width = 0; wp.height = 0; wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            } else if (state == IslandState.TYPE_CUBE || state == IslandState.TYPE_SPLIT) {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = WindowManager.LayoutParams.MATCH_PARENT; wp.x = 0; wp.y = 0
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND; wp.blurBehindRadius = 45 }
            } else {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = ((maxH.value + 150) * density).toInt(); wp.x = 0; wp.y = 0
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            }
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        // 🚀 THE OMNI-GESTURE TRACKER
        var dragOffsetX by remember { mutableFloatStateOf(0f) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter

        Row(
            modifier = Modifier.fillMaxWidth().offset(x = offsetX.dp, y = offsetY.dp).height(maxH.value.dp), 
            horizontalArrangement = Arrangement.Center, 
            verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val location = IntArray(2)
                        this@DynamicIslandView.getLocationOnScreen(location)
                        val bounds = coordinates.boundsInRoot()
                        
                        val globalLeft = location[0] + bounds.left.toInt()
                        val globalTop = location[1] + bounds.top.toInt()
                        val globalRight = location[0] + bounds.right.toInt()
                        val globalBottom = location[1] + bounds.bottom.toInt()

                        // 🚀 CHOREOGRAPHER FIX: Only fire if physical bounds change significantly
                        if (abs(mainPillRect.left - globalLeft) > 5 || abs(mainPillRect.bottom - globalBottom) > 5 || mainPillRect.isEmpty) {
                            mainPillRect.set(globalLeft, globalTop, globalRight, globalBottom)
                            viewTreeObserver.dispatchOnGlobalLayout()
                        }
                    }
                    .width(width).height(height).clip(RoundedCornerShape(rad))
                    .background(bgColor).border(1.dp, borderColor, RoundedCornerShape(rad))
                    // 🚀 UNIFIED TAP & DRAG ENGINE
                    .pointerInput(Unit) { 
                        detectTapGestures(
                            onTap = { onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) }, 
                            onDoubleTap = { onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) }, 
                            onLongPress = { onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                        ) 
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (abs(dragOffsetX) > abs(dragOffsetY)) {
                                    if (abs(dragOffsetX) > 40f) onGestureEvent?.invoke(if (dragOffsetX > 0) IslandGesture.SWIPE_RIGHT else IslandGesture.SWIPE_LEFT)
                                } else {
                                    if (abs(dragOffsetY) > 40f) onGestureEvent?.invoke(if (dragOffsetY > 0) IslandGesture.SWIPE_DOWN else IslandGesture.SWIPE_UP)
                                }
                                dragOffsetX = 0f; dragOffsetY = 0f
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x; dragOffsetY += dragAmount.y
                        }
                    },
                contentAlignment = boxAlignment
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                    
                    if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                        Image(bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(if (state == IslandState.TYPE_3_MAX) 0.65f else 0.35f).blur(if (state == IslandState.TYPE_3_MAX) 12.dp else 24.dp))
                    }

                    if (state != IslandState.HIDDEN) {
                        val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                            AnimatedContent(targetState = state, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "UI Transition") { s ->
                                when (s) {
                                    IslandState.TYPE_3_MAX -> { if (model is LiveActivityModel.Dashboard) DashboardMax(model) else if (model is LiveActivityModel.Music) MusicMax(model) }
                                    IslandState.TYPE_2_MID -> { 
                                        if (model is LiveActivityModel.Dashboard) DashboardMid(model) 
                                        else if (model is LiveActivityModel.Music) MusicMid(model) 
                                        else if (model is LiveActivityModel.General) GeneralMid(model) 
                                        else if (model is LiveActivityModel.Charging) ChargingMid(model)
                                        else if (model is LiveActivityModel.SystemAlert) SystemAlertMid(model) // 🚀 ADD THIS LINE
                                    }
                                    IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> {
                                        when (model) {
                                            is LiveActivityModel.Music -> MusicMini(model)
                                            is LiveActivityModel.General -> GeneralMini(model)
                                            is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
                                            is LiveActivityModel.RealityPill -> RealityPillMini(model) // 🚀 RENDER PATH ADDED
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
                        val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.value <= 20
                        if (shouldShowRing) {
                            val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                            val progress = if (isMedia) (currentMediaPos.longValue.toFloat() / safeDur) else globalBatteryLevel.value / 100f
                            val progressColor = if (isMedia) Color.White else if (globalIsCharging.value) Color.Green else if (globalBatteryLevel.value <= 20) Color.Red else Color.White
                            
                            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp).align(Alignment.Center)) {
                                val strokeW = ringThickness.value.dp.toPx() 
                                drawArc(color = progressColor.copy(alpha=0.2f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeW))
                                drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, style = Stroke(strokeW, cap = StrokeCap.Round))
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
                            .onGloballyPositioned { coordinates ->
                                val location = IntArray(2)
                                this@DynamicIslandView.getLocationOnScreen(location)
                                val bounds = coordinates.boundsInRoot()
                                val globalLeft = location[0] + bounds.left.toInt()
                                val globalTop = location[1] + bounds.top.toInt()
                                val globalRight = location[0] + bounds.right.toInt()
                                val globalBottom = location[1] + bounds.bottom.toInt()
                                if (abs(splitCubeRect.left - globalLeft) > 5 || splitCubeRect.isEmpty) {
                                    splitCubeRect.set(globalLeft, globalTop, globalRight, globalBottom)
                                    viewTreeObserver.dispatchOnGlobalLayout()
                                }
                            }
                            .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape), 
                        contentAlignment = Alignment.Center) {
                        if (sModel is LiveActivityModel.Charging) { val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White; Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
