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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlin.math.abs

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun start() { savedStateRegistryController.performRestore(android.os.Bundle()); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    fun destroy() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) }
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
    
    var displayCutoutWidth = mutableFloatStateOf(0f)
    var pinnedApps = mutableStateListOf<String>("", "", "", "", "", "", "", "")
    var qsTiles = mutableStateListOf<String>("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")

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

    private var flowJob: Job? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val mainPillRect = android.graphics.Rect()
    private val splitCubeRect = android.graphics.Rect()

    private val insetsUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

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

                customOffsetY.floatValue = intent.getFloatExtra("tweak_offset_y", customOffsetY.floatValue)
                customBaseWidth.floatValue = intent.getFloatExtra("tweak_base_width", customBaseWidth.floatValue)

                val capString = intent.getStringExtra("theme_media_cap") ?: "Round"
                val parsedCap = if (capString == "Square") StrokeCap.Square else StrokeCap.Round

                activeTheme.value = IslandTheme(
                    mediaBarCap = parsedCap,
                    mediaBarThickness = intent.getFloatExtra("theme_media_thick", 4f).dp,
                    titleOffsetX = intent.getFloatExtra("theme_title_x", 0f).dp,
                    titleOffsetY = intent.getFloatExtra("theme_title_y", 0f).dp,
                    titleSize = intent.getFloatExtra("theme_title_size", 16f).sp,
                    timeTextSize = intent.getFloatExtra("theme_time_size", 12f).sp,
                    timeTextOffsetX = intent.getFloatExtra("theme_time_x", 0f).dp,
                    batteryRingThickness = intent.getFloatExtra("theme_bat_ring", 12f).dp,
                    cornerRadius = intent.getFloatExtra("theme_corner_radius", 50f).dp,
                    albumArtSize = intent.getFloatExtra("theme_album_art_size", 44f).dp,
                    buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp
                )

                val payload = intent.getStringExtra("gesture_payload")
                if (payload != null) onGestureSettingsUpdated?.invoke(payload) else loadPreferences()
            }
        }
    }

    init {
        loadPreferences()
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
        } catch (e: Throwable) {}

        val composeView = ComposeView(context).apply {
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
        lifecycleOwner.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val displayCutout = try { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { windowManager?.currentWindowMetrics?.windowInsets?.displayCutout } else null } catch(e:Throwable){null}
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density

        flowJob = CoroutineScope(AndroidUiDispatcher.CurrentThread).launch {
            insetsUpdateFlow.debounce(50).collect {
                try { this@DynamicIslandView.requestLayout() } catch(e:Throwable){}
            }
        }

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
        flowJob?.cancel()
        flowJob = null
        lifecycleOwner.destroy()
        try { context.unregisterReceiver(receiver) } catch (e: Throwable) {}

        BatteryPlugin.stop(context)
        try { context.sendBroadcast(android.content.Intent("com.example.dynamicisland.RESTORE_CLOCK").setPackage("com.android.systemui")) } catch(e:Throwable){}
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val haptic = LocalHapticFeedback.current
        var isSquished by remember { mutableStateOf(false) }
        val touchScale by animateFloatAsState(
            targetValue = if (isSquished) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "squish"
        )
        val minSafeWidth = displayCutoutWidth.floatValue + 4f

        val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetWidth = rawTargetWidth.coerceAtLeast(minSafeWidth)
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

        LaunchedEffect(state, model) {
            if (!isAttachedToWindow || windowToken == null) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }

            if (state == IslandState.HIDDEN) {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = 0 
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            } else {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = ((maxH.value + 150) * density).toInt(); wp.x = 0; wp.y = 0
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
            }
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Throwable) {}
        }

        val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter

        Row(
            modifier = Modifier.fillMaxWidth().offset(x = offsetX.dp, y = offsetY.dp).height(maxH.value.dp), 
            horizontalArrangement = Arrangement.Center, 
            verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        try {
                            if (!isAttachedToWindow) return@onGloballyPositioned
                            val location = IntArray(2)
                            this@DynamicIslandView.getLocationOnScreen(location)
                            val bounds = coordinates.boundsInRoot()
                            val globalLeft = location[0] + bounds.left.toInt()
                            val globalTop = location[1] + bounds.top.toInt()
                            val globalRight = location[0] + bounds.right.toInt()
                            val globalBottom = location[1] + bounds.bottom.toInt()

                            if (abs(mainPillRect.left - globalLeft) > 5 || abs(mainPillRect.bottom - globalBottom) > 5 || mainPillRect.isEmpty) {
                                mainPillRect.set(globalLeft, globalTop, globalRight, globalBottom)
                                insetsUpdateFlow.tryEmit(Unit)
                            }
                        } catch(e: Throwable) {}
                    }
                    .width(width).height(height)
                    .graphicsLayer { scaleX = touchScale; scaleY = touchScale }
                    .clip(RoundedCornerShape(rad))
                    .background(bgColor).border(1.dp, borderColor, RoundedCornerShape(rad))
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
                Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                    
                    if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                        Image(bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(if (state == IslandState.TYPE_3_MAX) 0.65f else 0.35f).blur(if (state == IslandState.TYPE_3_MAX) 12.dp else 24.dp))
                    }

                    if (state != IslandState.HIDDEN) {
                        val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                            AnimatedContent(
                                targetState = state,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                     scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                                    .togetherWith(fadeOut(animationSpec = tween(90)))
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
                            val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                            val progress = if (isMedia) (currentMediaPos.longValue.toFloat() / safeDur) else globalBatteryLevel.intValue / 100f
                            
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

                            // 🚀 DYNAMIC RING REDESIGN: Apple-Style glowing continuous stroke
                            Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp).align(Alignment.Center)) {
                                val strokeW = ringThickness.value.dp.toPx() 
                                val inset = strokeW / 2
                                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                                val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)

                                drawArc(color = baseColor.copy(alpha=0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))

                                val progressPercent = progress.coerceIn(0f, 1f)
                                val capStyle = if (progressPercent >= 0.99f) StrokeCap.Butt else StrokeCap.Round
                                drawArc(
                                    color = progressColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progressPercent,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(strokeW, cap = capStyle)
                                )
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
                                try {
                                    if (!isAttachedToWindow) return@onGloballyPositioned
                                    val location = IntArray(2)
                                    this@DynamicIslandView.getLocationOnScreen(location)
                                    val bounds = coordinates.boundsInRoot()
                                    val globalLeft = location[0] + bounds.left.toInt()
                                    val globalTop = location[1] + bounds.top.toInt()
                                    val globalRight = location[0] + bounds.right.toInt()
                                    val globalBottom = location[1] + bounds.bottom.toInt()
                                    if (abs(splitCubeRect.left - globalLeft) > 5 || splitCubeRect.isEmpty) {
                                        splitCubeRect.set(globalLeft, globalTop, globalRight, globalBottom)
                                        insetsUpdateFlow.tryEmit(Unit)
                                    }
                                } catch(e: Throwable) {}
                            }
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
