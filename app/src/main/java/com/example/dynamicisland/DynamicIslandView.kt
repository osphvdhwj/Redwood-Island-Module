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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.*
import androidx.savedstate.*
import de.robv.android.xposed.XSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlin.math.abs

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context) {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

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
    var activeTheme = mutableStateOf(IslandTheme())
    var globalBatteryLevel = mutableIntStateOf(100)
    // Add these next to your other mutable states
    var hardwareVolume = mutableIntStateOf(0)
    var hardwareBrightness = mutableIntStateOf(0)

    // Helper functions for the Controller to push updates
    fun updateHardwareVolume(vol: Int) { hardwareVolume.intValue = vol }
    fun updateHardwareBrightness(bright: Int) { hardwareBrightness.intValue = bright }
    
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
    var onCustomMediaAction: ((String) -> Unit)? = null

    private var flowJob: Job? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val mainPillRect = android.graphics.Rect()
    private val splitCubeRect = android.graphics.Rect()
    
    private var insetsListenerProxy: Any? = null

    private val insetsUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(
        replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    // Extract ComposeView to class level to allow receivers to modify its visibility
    private val composeView = ComposeView(context)

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

    // 🎛️ NEW: Wake the Island up when app changes
    private val appChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.APP_CHANGED") {
                composeView.visibility = View.VISIBLE
            }
        }
    }

    // 🎛️ NEW: Safer Screen Receiver (no Lifecycle choking)
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> composeView.visibility = View.GONE
                Intent.ACTION_SCREEN_ON -> composeView.visibility = View.VISIBLE
            }
        }
    }

    init {
        loadPreferences()

        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        ViewTreeLifecycleOwner.set(this, this)
        ViewTreeViewModelStoreOwner.set(this, this)
        ViewTreeSavedStateRegistryOwner.set(this, this)

        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            insetsListenerProxy = java.lang.reflect.Proxy.newProxyInstance(context.classLoader, arrayOf(listenerClass)) { _, method, args ->
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
            viewTreeObserver.javaClass.getMethod("addOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, insetsListenerProxy)
        } catch (e: Exception) {}

        composeView.apply {
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
        val displayCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { windowManager?.currentWindowMetrics?.windowInsets?.displayCutout } else null
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density

        flowJob = CoroutineScope(AndroidUiDispatcher.CurrentThread).launch {
            insetsUpdateFlow.debounce(50).collect { this@DynamicIslandView.requestLayout() }
        }

        val filter = IntentFilter("com.example.dynamicisland.RELOAD_PREFS")
        val appChangeFilter = IntentFilter("com.example.dynamicisland.APP_CHANGED")
        val securePermission = "com.redwood.permission.SECURE_IPC"
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, securePermission, null, Context.RECEIVER_EXPORTED)
            context.registerReceiver(appChangeReceiver, appChangeFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter, securePermission, null)
            context.registerReceiver(appChangeReceiver, appChangeFilter)
        }

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, screenFilter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    
        try {
            if (insetsListenerProxy != null) {
                val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
                viewTreeObserver.javaClass.getMethod("removeOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, insetsListenerProxy)
            }
        } catch (e: Exception) {}

        flowJob?.cancel()
        flowJob = null
        try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(appChangeReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(screenReceiver) } catch (e: Exception) {}

        BatteryPlugin.stop(context)
        context.sendBroadcast(android.content.Intent("com.example.dynamicisland.RESTORE_CLOCK").setPackage("com.android.systemui"))
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        
        // 📱 LANDSCAPE DETECTOR & BLACK HOLE ANIMATOR
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isEffectivelyHidden = state == IslandState.HIDDEN || isLandscape

        // The "Black Hole" Gulp Animation
        val islandScale by animateFloatAsState(
            targetValue = if (isEffectivelyHidden) 0f else 1f,
            animationSpec = if (isEffectivelyHidden) tween(350, easing = FastOutLinearInEasing) else spring(dampingRatio = 0.65f, stiffness = 300f),
            label = "blackhole_scale"
        )
        val islandAlpha by animateFloatAsState(targetValue = if (isEffectivelyHidden) 0f else 1f, animationSpec = tween(300), label = "blackhole_alpha")

        val haptic = LocalHapticFeedback.current
        val density = LocalDensity.current
        var isSquished by remember { mutableStateOf(false) }
        val touchScale by animateFloatAsState(
            targetValue = if (isSquished) 0.96f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f), label = "squish"
        )
        
        val minSafeWidth = displayCutoutWidth.floatValue + 4f

        val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetWidth = rawTargetWidth.coerceAtLeast(minSafeWidth)
        val model = activeModel.value
        val targetHeight = when (state) { 
            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value
            IslandState.TYPE_2_MID -> midH.value
            IslandState.TYPE_3_MAX -> if (model is LiveActivityModel.Music) (maxH.value * 0.70f) else maxH.value
            IslandState.TYPE_CUBE -> cubeH.value 
            else -> ringH.value 
        }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 250f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, spring<Float>(dampingRatio=0.65f, stiffness=250f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.65f, stiffness=250f), label = "y")
        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        // 🎨 REFINED: True AMOLED Black, refined transparencies, and ultra-subtle micro-borders
        val targetBgColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent 
        else {
            if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) Color(model.dominantColor).copy(alpha = 0.85f) 
            else if (state == IslandState.TYPE_3_MAX) Color(0xFF080808).copy(alpha = 0.85f) 
            else Color.Black // Pure black for seamless AMOLED blend
        }
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.08f), animationSpec = tween(600), label = "borderColor")
        
        // 🚀 THIS RESTORES THE PROPER FULLSCREEN REFLECTION WINDOW BEHAVIOR
        LaunchedEffect(state, model, isLandscape) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect

            if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }

            // 📱 FIXED: explicitly toggle the touch flag so the Island isn't an untouchable ghost!
            if (state == IslandState.HIDDEN || isLandscape) {
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }

            wp.width = WindowManager.LayoutParams.MATCH_PARENT
            wp.height = WindowManager.LayoutParams.MATCH_PARENT
            
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX.dp, y = offsetY.coerceAtLeast(0f).dp)
                .height(maxH.value.dp),
            horizontalArrangement = Arrangement.Center, 
            verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
        ) {
            // 🎛️ FIXED: The Outer Box securely tracks the real layout bounds for the Android WindowManager touches.
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

                        if (abs(mainPillRect.left - globalLeft) > 5 || abs(mainPillRect.bottom - globalBottom) > 5 || mainPillRect.isEmpty) {
                            mainPillRect.set(globalLeft, globalTop, globalRight, globalBottom)
                            insetsUpdateFlow.tryEmit(Unit)
                        }
                    }
                    .width(width).height(height)
            ) {
                // 🎛️ FIXED: The Inner Box handles the visual "Black Hole" graphics scale and the touch inputs.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { 
                            scaleX = touchScale * islandScale
                            scaleY = touchScale * islandScale
                            alpha = islandAlpha
                            transformOrigin = TransformOrigin(0.5f, 0.5f) // Sucks directly into the center
                        }
                        .shadow(elevation = if (state == IslandState.TYPE_0_RING) 0.dp else 16.dp, shape = RoundedCornerShape(rad), spotColor = Color.Black)
                        .clip(RoundedCornerShape(rad))
                        .background(bgColor)
                        .border(0.5.dp, borderColor, RoundedCornerShape(rad))
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
                                onTap = { 
                                     if (state != IslandState.TYPE_3_MAX) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) } 
                                },
                                onDoubleTap = { 
                                     if (state != IslandState.TYPE_3_MAX) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) } 
                                },
                                onLongPress = { 
                                     if (state != IslandState.TYPE_3_MAX) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) } 
                                }
                            )
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
                                if (abs(dragAmount.x) > 5f || abs(dragAmount.y) > 5f) { change.consume() }
                                dragOffsetX += dragAmount.x
                                dragOffsetY += dragAmount.y
                            }
                        },
                    contentAlignment = boxAlignment
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                        
                        if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                            Image(
                                 bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, 
                                modifier = Modifier.fillMaxSize()
                                 .alpha(if (state == IslandState.TYPE_3_MAX) 0.5f else 0.25f)
                                .blur(if (state == IslandState.TYPE_3_MAX) 16.dp else 24.dp)
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
                                                is LiveActivityModel.OngoingTask -> OngoingTaskMid(model)
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

                            // 🎛️ REFINED: Ultra-slim grab handle ONLY for Mid and Max pills
                            if (state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) {
                                Box(
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(10.dp).padding(bottom = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) { Box(modifier = Modifier.width(36.dp).height(4.dp).background(Color.White.copy(alpha=0.25f), CircleShape)) }
                            }
                        }
                        
                        if (state == IslandState.TYPE_0_RING) {
                            val musicModel = model as? LiveActivityModel.Music
                            val isMedia = musicModel != null && musicModel.isPlaying
                            val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.intValue <= 20

                            if (shouldShowRing) {
                                val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                                val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }
                                
                                val batteryLevel = globalBatteryLevel.intValue
                                 val baseColor = if (isMedia) {
                                    musicModel?.dominantColor?.let { Color(it) } ?: Color.White
                                 } else if (globalIsCharging.value) {
                                    Color(0xFF00FF00) // Bright Green
                                } else {
                                     when {
                                        batteryLevel <= 5 -> Color(0xFFFF0000) // Super Red
                                        batteryLevel <= 10 -> Color(0xFFFF3333) // Red
                                        batteryLevel <= 40 -> Color(0xFFFFA500) // Orange
                                        batteryLevel <= 60 -> Color(0xFFFFFF00) // Yellow
                                        else -> Color(0xFF006400) // Dark Green
                                    }
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
                                    val strokeW = ringThickness.value.dp.toPx() 
                                    val inset = strokeW / 2
                                    val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                                    val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                                    val progressPercent = progress.coerceIn(0f, 1f)

                                    val sweepGradient = Brush.sweepGradient(0.0f to progressColor.copy(alpha = 0.4f), 0.8f to progressColor, 1.0f to progressColor.copy(alpha = 0.4f))

                                    // 🎨 REFINED: Smooth rounded caps instead of harsh flat cuts
                                    drawArc(color = baseColor.copy(alpha=0.20f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))
                                    drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = 360f * progressPercent, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Round), alpha = 0.95f)

                                    val markerLength = strokeW * 1.3f
                                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                    val radius = (size.width - strokeW) / 2
                                    
                                    drawLine(color = Color.White, start = androidx.compose.ui.geometry.Offset(center.x, center.y - radius - markerLength/2), end = androidx.compose.ui.geometry.Offset(center.x, center.y - radius + markerLength/2), strokeWidth = 4f)
                                     
                                    val angleRad = Math.toRadians((-90f + 360f * progressPercent).toDouble())
                                    val mStartX = center.x + (radius - markerLength/2) * Math.cos(angleRad).toFloat()
                                    val mStartY = center.y + (radius - markerLength/2) * Math.sin(angleRad).toFloat()
                                    val mEndX = center.x + (radius + markerLength/2) * Math.cos(angleRad).toFloat()
                                    val mEndY = center.y + (radius + markerLength/2) * Math.sin(angleRad).toFloat()
                                    drawLine(color = Color.White, start = androidx.compose.ui.geometry.Offset(mStartX, mStartY), end = androidx.compose.ui.geometry.Offset(mEndX, mEndY), strokeWidth = 4f)
                                }
                            }
                        }
                     }
                }
            }

            AnimatedVisibility(
                visible = state == IslandState.TYPE_SPLIT,
                enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                val sModel = splitModel.value
                
                // 1. Dynamic Backgrounds based on priority/state
                val splitBg = when {
                    sModel is LiveActivityModel.Charging && sModel.isPluggedIn -> Color.Green.copy(alpha = 0.2f)
                    sModel is LiveActivityModel.Charging && sModel.level <= 20 -> Color.Red.copy(alpha = 0.2f)
                    else -> Color(0xFF121212).copy(alpha = 0.75f)
                }

                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(height) // Keeps it perfectly synced with the main pill's dynamic height
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
                                    insetsUpdateFlow.tryEmit(Unit)
                                }
                            }
                            .clip(CircleShape)
                            .background(splitBg)
                            .border(1.dp, borderColor, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSplitPillClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        // 🎛️ FIXED: Exhaustive UI routing for the secondary cube
                        when (sModel) {
                            is LiveActivityModel.Charging -> {
                                val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White
                                Text(
                                    text = "${sModel.level}%",
                                    color = iconColor,
                                    fontSize = 11.sp, // Slightly larger for readability
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is LiveActivityModel.SystemAlert -> {
                                // Renders the specific alert icon (e.g., Ringing, Silent, Do Not Disturb)
                                Icon(
                                    imageVector = sModel.icon, 
                                    contentDescription = sModel.title,
                                    tint = sModel.iconTint ?: Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            is LiveActivityModel.AppTimerWarning -> {
                                // Renders the timer/warning icon
                                Icon(
                                    imageVector = sModel.icon, 
                                    contentDescription = "Timer Warning",
                                    tint = Color(0xFFFFA500), // Standard warning orange
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            is LiveActivityModel.OngoingTask -> {
                                // Renders a tiny circular progress bar inside the right cube!
                                CircularProgressIndicator(
                                    progress = { (sModel.progress.toFloat() / sModel.progressMax.toFloat()).coerceIn(0f, 1f) },
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is LiveActivityModel.General -> {
                                // Fallback for standard notifications
                                Icon(
                                    imageVector = sModel.icon,
                                    contentDescription = "Notification",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            else -> {} // Do nothing if model is null
                        }
                    }
                }
            }
