package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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
    init { savedStateRegistryController.performRestore(Bundle()) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun attach() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    fun detach() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) }
}

data class IslandTheme(
    val mediaBarCap: StrokeCap = StrokeCap.Round,
    val mediaBarThickness: Dp = 4.dp,
    val titleSize: TextUnit = 16.sp,
    val titleFont: FontFamily = FontFamily.Default,
    val timeTextSize: TextUnit = 12.sp,
    val buttonSize: Dp = 48.dp,
    val buttonSpacing: Dp = 16.dp,
    val buttonCornerRadius: Dp = 50.dp,
    val actionAnimType: String = "BOUNCE"
)

val LocalIslandTheme = compositionLocalOf { IslandTheme() }
val LocalIslandFont = compositionLocalOf { FontFamily.Default }

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)
    var mediaMidW = mutableStateOf(320f); var mediaMidH = mutableStateOf(80f); var mediaMidX = mutableStateOf(0f); var mediaMidY = mutableStateOf(48f)
    var mediaMaxW = mutableStateOf(360f); var mediaMaxH = mutableStateOf(200f); var mediaMaxX = mutableStateOf(0f); var mediaMaxY = mutableStateOf(48f)
    var cubeW = mutableStateOf(85f); var cubeH = mutableStateOf(85f); var cubeX = mutableStateOf(0f); var cubeY = mutableStateOf(48f)

    var ringThickness = mutableStateOf(6f)
    var expandUpwards = mutableStateOf(false)
    var isCubeRotationEnabled = mutableStateOf(true)
    var useSystemFont = mutableStateOf(true) 
    var activeTheme = mutableStateOf(IslandTheme())
    
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)
    var displayCutoutWidth = mutableFloatStateOf(0f)

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
    private var insetsListenerProxy: Any? = null
    private val insetsUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    private fun loadPreferences() {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable(); pref.reload()
            ringW.value = pref.getFloat("ring_w", 45f); ringH.value = pref.getFloat("ring_h", 45f); ringX.value = pref.getFloat("ring_x", 0f); ringY.value = pref.getFloat("ring_y", 48f)
            miniW.value = pref.getFloat("mini_w", 180f); miniH.value = pref.getFloat("mini_h", 36f); miniX.value = pref.getFloat("mini_x", 0f); miniY.value = pref.getFloat("mini_y", 48f)
            midW.value = pref.getFloat("mid_w", 320f); midH.value = pref.getFloat("mid_h", 80f); midX.value = pref.getFloat("mid_x", 0f); midY.value = pref.getFloat("mid_y", 48f)
            maxW.value = pref.getFloat("max_w", 360f); maxH.value = pref.getFloat("max_h", 220f); maxX.value = pref.getFloat("max_x", 0f); maxY.value = pref.getFloat("max_y", 48f)
            mediaMidW.value = pref.getFloat("media_mid_w", 320f); mediaMidH.value = pref.getFloat("media_mid_h", 80f); mediaMidX.value = pref.getFloat("media_mid_x", 0f); mediaMidY.value = pref.getFloat("media_mid_y", 48f)
            mediaMaxW.value = pref.getFloat("media_max_w", 360f); mediaMaxH.value = pref.getFloat("media_max_h", 200f); mediaMaxX.value = pref.getFloat("media_max_x", 0f); mediaMaxY.value = pref.getFloat("media_max_y", 48f)
            cubeW.value = pref.getFloat("cube_w", 85f); cubeH.value = pref.getFloat("cube_h", 85f); cubeX.value = pref.getFloat("cube_x", 0f); cubeY.value = pref.getFloat("cube_y", 48f)
            ringThickness.value = pref.getFloat("ring_thickness", 6f)
            expandUpwards.value = pref.getBoolean("expand_upwards", false)
            useSystemFont.value = pref.getBoolean("use_system_font", true)
        } catch (e: Exception) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                val prefix = intent.getStringExtra("prefix")
                if (prefix != null) {
                    val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f); val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                    when (prefix) { 
                        "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }
                        "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }
                        "mid" -> { midW.value = w; midH.value = h; midX.value = x; midY.value = y }
                        "max" -> { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y }
                        "media_mid" -> { mediaMidW.value = w; mediaMidH.value = h; mediaMidX.value = x; mediaMidY.value = y }
                        "media_max" -> { mediaMaxW.value = w; mediaMaxH.value = h; mediaMaxX.value = x; mediaMaxY.value = y }
                        "cube" -> { cubeW.value = w; cubeH.value = h; cubeX.value = x; cubeY.value = y } 
                    }
                    ringThickness.value = intent.getFloatExtra("ring_thickness", ringThickness.value)
                    expandUpwards.value = intent.getBooleanExtra("expand_upwards", expandUpwards.value)
                } 
                val pref = ctx.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
                useSystemFont.value = pref.getBoolean("use_system_font", true)
                activeTheme.value = IslandTheme(
                    buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp,
                    buttonSpacing = intent.getFloatExtra("theme_button_spacing", 16f).dp,
                    buttonCornerRadius = intent.getFloatExtra("theme_button_radius", 50f).dp,
                    actionAnimType = intent.getStringExtra("theme_anim_type") ?: "BOUNCE"
                )
                intent.getStringExtra("gesture_payload")?.let { onGestureSettingsUpdated?.invoke(it) } ?: loadPreferences()
            }
        }
    }

    init {
        loadPreferences()
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

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) { lifecycleOwner.attach() }
                override fun onViewDetachedFromWindow(v: View) { lifecycleOwner.detach() }
            })

            setContent {
                val islandFont = if (useSystemFont.value) FontFamily.Default else FontFamily.Monospace 
                MaterialTheme(typography = Typography(bodyMedium = androidx.compose.material3.Typography().bodyMedium.copy(fontFamily = islandFont))) {
                    CompositionLocalProvider(LocalContext provides moduleContext, LocalIslandTheme provides activeTheme.value, LocalIslandFont provides islandFont) {
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
        flowJob = CoroutineScope(AndroidUiDispatcher.CurrentThread).launch { insetsUpdateFlow.debounce(50).collect { this@DynamicIslandView.requestLayout() } }
        val filter = IntentFilter("com.example.dynamicisland.RELOAD_PREFS")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) context.registerReceiver(receiver, filter, "com.redwood.permission.SECURE_IPC", null, Context.RECEIVER_EXPORTED) else @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, filter, "com.redwood.permission.SECURE_IPC", null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { if (insetsListenerProxy != null) { val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener"); viewTreeObserver.javaClass.getMethod("removeOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, insetsListenerProxy) } } catch (e: Exception) {}
        flowJob?.cancel(); flowJob = null; try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val haptic = LocalHapticFeedback.current
        var isSquished by remember { mutableStateOf(false) }
        val touchScale by animateFloatAsState(targetValue = if (isSquished) 0.94f else 1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f), label = "squish")
        
        val model = activeModel.value
        val isMedia = model is LiveActivityModel.Music
        
        val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> if (isMedia) mediaMidW.value else midW.value; IslandState.TYPE_3_MAX -> if (isMedia) mediaMaxW.value else maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetWidth = rawTargetWidth.coerceAtLeast(displayCutoutWidth.floatValue + 4f)
        val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> if (isMedia) mediaMidH.value else midH.value; IslandState.TYPE_3_MAX -> if (isMedia) mediaMaxH.value else maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> if (isMedia) mediaMidX.value else midX.value; IslandState.TYPE_3_MAX -> if (isMedia) mediaMaxX.value else maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> if (isMedia) mediaMidY.value else midY.value; IslandState.TYPE_3_MAX -> if (isMedia) mediaMaxY.value else maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.72f, stiffness = 320f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, spring(dampingRatio = 0.72f, stiffness = 320f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring(dampingRatio = 0.72f, stiffness = 320f), label = "y")
        val rad by animateDpAsState(if (state == IslandState.TYPE_3_MAX) 42.dp else (targetHeight / 2).dp, physicsSpec, label = "rad")

        val targetBgColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.Black
        val bgColor by animateColorAsState(targetBgColor, tween(400), label = "bgColor")

        LaunchedEffect(state, model) {
            val wp = windowParams ?: return@LaunchedEffect; val wm = windowManager ?: return@LaunchedEffect
            wp.flags = if (model?.isSensitive == true) wp.flags or WindowManager.LayoutParams.FLAG_SECURE else wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
            if (state == IslandState.HIDDEN) { wp.width = 0; wp.height = 0; wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE } 
            else { wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = WindowManager.LayoutParams.MATCH_PARENT; wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() }
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        Row(
            modifier = Modifier.fillMaxWidth().offset(x = offsetX.dp, y = offsetY.coerceAtLeast(0f).dp).height(maxH.value.dp.coerceAtLeast(mediaMaxH.value.dp)), 
            horizontalArrangement = Arrangement.Center, 
            verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .onGloballyPositioned { coordinates ->
                        val loc = IntArray(2); this@DynamicIslandView.getLocationOnScreen(loc); val b = coordinates.boundsInRoot()
                        val gL = loc[0] + b.left.toInt(); val gT = loc[1] + b.top.toInt(); val gR = loc[0] + b.right.toInt(); val gB = loc[1] + b.bottom.toInt()
                        if (abs(mainPillRect.left - gL) > 5 || abs(mainPillRect.bottom - gB) > 5 || mainPillRect.isEmpty) { mainPillRect.set(gL, gT, gR, gB); insetsUpdateFlow.tryEmit(Unit) }
                    }
                    .width(width).height(height)
                    .graphicsLayer { scaleX = touchScale; scaleY = touchScale; transformOrigin = TransformOrigin(0.5f, 0f) }
                    .clip(RoundedCornerShape(rad))
                    .background(bgColor)
                    // 🚀 UNIFIED MASTER GESTURE DETECTOR
                    .pointerInput(state) {
                        detectTapGestures(
                            onPress = { isSquished = true; tryAwaitRelease(); isSquished = false },
                            onTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.SINGLE_TAP) },
                            onDoubleTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onGestureEvent?.invoke(IslandGesture.DOUBLE_TAP) },
                            onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onGestureEvent?.invoke(IslandGesture.LONG_PRESS) }
                        )
                    }
                    .pointerInput(state) {
                        var dX = 0f; var dY = 0f
                        detectDragGestures(
                            onDragEnd = {
                                if (abs(dX) > abs(dY)) { if (dX > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_RIGHT) else if (dX < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_LEFT) } 
                                else { if (dY > 40f) onGestureEvent?.invoke(IslandGesture.SWIPE_DOWN) else if (dY < -40f) onGestureEvent?.invoke(IslandGesture.SWIPE_UP) }
                                dX = 0f; dY = 0f
                            }
                        ) { change, dragAmount -> change.consume(); dX += dragAmount.x; dY += dragAmount.y }
                    }
            ) {
                // 🚀 ADAPTIVE UI CONTAINER (Zero Hardcoded Paddings)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state != IslandState.HIDDEN && state != IslandState.TYPE_0_RING) {
                        AnimatedContent(
                            targetState = state,
                            transitionSpec = { (fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200))) togetherWith fadeOut(tween(100)) },
                            label = "UI Transition"
                        ) { s ->
                            when (s) {
                                IslandState.TYPE_3_MAX -> { if (model is LiveActivityModel.Music) MusicMax(model) else if (model is LiveActivityModel.Dashboard) DashboardMax(model) }
                                IslandState.TYPE_2_MID -> {
                                    when (model) {
                                        is LiveActivityModel.Music -> MusicMid(model)
                                        is LiveActivityModel.Otp -> OtpMid(model)
                                        is LiveActivityModel.SystemAlert -> SystemAlertMid(model)
                                        is LiveActivityModel.Charging -> ChargingMid(model)
                                        is LiveActivityModel.AppTimerWarning -> AppTimerWarningMid(model)
                                        is LiveActivityModel.OngoingTask -> OngoingTaskMid(model)
                                        else -> {}
                                    }
                                }
                                IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> {
                                    when (model) {
                                        is LiveActivityModel.Music -> MusicMini(model)
                                        is LiveActivityModel.RealityPill -> RealityPillMini(model)
                                        is LiveActivityModel.HardwareMonitor -> HardwareGaugeMini(model)
                                        else -> {}
                                    }
                                }
                                IslandState.TYPE_CUBE -> if (model is LiveActivityModel.Charging) ChargingCube(model)
                                else -> {} 
                            }
                        }
                    }
                    
                    // 🚀 REWRITTEN RING STATE: Breathing + Sweep Animation
                    if (state == IslandState.TYPE_0_RING) {
                        val musicModel = model as? LiveActivityModel.Music
                        val isMedia = musicModel != null && musicModel.isPlaying
                        val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.intValue <= 20

                        if (shouldShowRing) {
                            val infiniteTransition = rememberInfiniteTransition(label = "ring_anim")
                            val pulseScale by infiniteTransition.animateFloat(initialValue = 0.95f, targetValue = 1.05f, animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
                            
                            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }) {
                                val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                                val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }
                                val baseColor = if (isMedia) { musicModel?.dominantColor?.let { Color(it) } ?: Color.White } else if (globalIsCharging.value) Color.Green else if (globalBatteryLevel.intValue <= 20) Color.Red else Color.White
                                
                                val strokeW = ringThickness.value.dp.toPx() 
                                val inset = strokeW / 2
                                val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                                val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                                val progressPercent = progress.coerceIn(0f, 1f)

                                val sweepGradient = Brush.sweepGradient(0.0f to baseColor.copy(alpha = 0.2f), 0.8f to baseColor, 1.0f to baseColor.copy(alpha = 0.2f))

                                drawArc(color = baseColor.copy(alpha=0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))
                                drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = 360f * progressPercent, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Round), alpha = 0.95f)
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = state == IslandState.TYPE_SPLIT, enter = scaleIn(spring(dampingRatio=0.72f, stiffness=320f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                val sModel = splitModel.value
                val splitBg = if (sModel is LiveActivityModel.Charging) { if (sModel.isPluggedIn) Color.Green.copy(alpha=0.2f) else if (sModel.level <= 20) Color.Red.copy(alpha=0.2f) else Color(0xFF121212).copy(alpha=0.75f) } else Color(0xFF121212).copy(alpha=0.75f)
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(height)
                            .onGloballyPositioned { coordinates ->
                                val loc = IntArray(2); this@DynamicIslandView.getLocationOnScreen(loc); val b = coordinates.boundsInRoot()
                                val gL = loc[0] + b.left.toInt(); val gT = loc[1] + b.top.toInt(); val gR = loc[0] + b.right.toInt(); val gB = loc[1] + b.bottom.toInt()
                                if (abs(splitCubeRect.left - gL) > 5 || splitCubeRect.isEmpty) { splitCubeRect.set(gL, gT, gR, gB); insetsUpdateFlow.tryEmit(Unit) }
                            }
                            .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape)
                            .clickable { onSplitPillClick?.invoke() },
                        contentAlignment = Alignment.Center) {
                        if (sModel is LiveActivityModel.Charging) { val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White; Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = LocalIslandFont.current) }
                    }
                }
            }
        }
    }
}
