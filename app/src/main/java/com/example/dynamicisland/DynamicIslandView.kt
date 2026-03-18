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
import androidx.compose.ui.text.font.FontFamily
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
    init { savedStateRegistryController.performRestore(Bundle()) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun attach() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    fun detach() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) }
}

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
    var useSystemFont = mutableStateOf(true) 
    var isCubeRotationEnabled = mutableStateOf(true) 
    
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
    
    // Missing variables restored here:
    var onAudioOutputClick: (() -> Unit)? = null
    var onDragHandleExpand: (() -> Unit)? = null
    var onDragHandleCollapse: (() -> Unit)? = null

    private var flowJob: Job? = null
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val mainPillRect = android.graphics.Rect()
    private val splitCubeRect = android.graphics.Rect()
    private var insetsListenerProxy: Any? = null
    private val insetsUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)

    private fun loadPreferences() {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable()
            pref.reload()
            
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
            isCubeRotationEnabled.value = pref.getBoolean("enable_cube_rotation", true) 
            
            for (i in 0..7) { val pkg = pref.getString("pinned_app_$i", ""); if (pkg != null) pinnedApps[i] = pkg }
            for (i in 0..6) { val qs = pref.getString("qs_tile_$i", ""); if (qs != null) qsTiles[i] = qs }
        } catch (e: Exception) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                loadPreferences()
                intent.getStringExtra("gesture_payload")?.let { onGestureSettingsUpdated?.invoke(it) }
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
                    CompositionLocalProvider(LocalContext provides moduleContext, LocalIslandFont provides islandFont) {
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
        val floatPhysicsSpec = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)
        
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, floatPhysicsSpec, label = "x")
        val offsetY by animateFloatAsState(targetY, floatPhysicsSpec, label = "y")
        val rad by animateDpAsState(if (state == IslandState.TYPE_3_MAX) 42.dp else (targetHeight / 2).dp, physicsSpec, label = "rad")

        val targetBgColor = if (state == IslandState.HIDDEN) Color.Transparent 
        else if (state == IslandState.TYPE_0_RING) Color.Black.copy(alpha = 0.05f) 
        else {
            if (isMedia && (model as LiveActivityModel.Music).dominantColor != null && state != IslandState.TYPE_3_MAX) Color(model.dominantColor!!).copy(alpha = 0.65f) 
            else if (state == IslandState.TYPE_3_MAX) Color(0xFF0F0F0F).copy(alpha = 0.55f)
            else Color(0xFF121212).copy(alpha = 0.85f) 
        }
        val bgColor by animateColorAsState(targetBgColor, tween(400), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN) Color.Transparent else if (state == IslandState.TYPE_0_RING) Color.White.copy(alpha=0.4f) else Color.White.copy(alpha = 0.15f), animationSpec = tween(400), label = "borderColor")

        LaunchedEffect(state, model) {
            val wp = windowParams ?: return@LaunchedEffect; val wm = windowManager ?: return@LaunchedEffect
            wp.flags = if (model?.isSensitive == true) wp.flags or WindowManager.LayoutParams.FLAG_SECURE else wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
            if (state == IslandState.HIDDEN) { wp.width = 0; wp.height = 0; wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE } 
            else { wp.width = WindowManager.LayoutParams.MATCH_PARENT; wp.height = WindowManager.LayoutParams.MATCH_PARENT; wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv() }
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        Box(modifier = Modifier.fillMaxSize()) {
            
            Box(
                modifier = Modifier
                    .align(if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter)
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .width(width).height(height)
                    .onGloballyPositioned { coordinates ->
                        val loc = IntArray(2); this@DynamicIslandView.getLocationOnScreen(loc); val b = coordinates.boundsInRoot()
                        val gL = loc[0] + b.left.toInt(); val gT = loc[1] + b.top.toInt(); val gR = loc[0] + b.right.toInt(); val gB = loc[1] + b.bottom.toInt()
                        if (abs(mainPillRect.left - gL) > 5 || abs(mainPillRect.bottom - gB) > 5 || mainPillRect.isEmpty) { mainPillRect.set(gL, gT, gR, gB); insetsUpdateFlow.tryEmit(Unit) }
                    }
                    .graphicsLayer { scaleX = touchScale; scaleY = touchScale; transformOrigin = TransformOrigin(0.5f, 0.5f) }
                    .clip(RoundedCornerShape(rad))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(rad))
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
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                        Image(
                            bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, 
                            modifier = Modifier.fillMaxSize()
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                            }
                            .alpha(if (state == IslandState.TYPE_3_MAX) 0.65f else 0.45f).blur(40.dp) 
                        )
                    }

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
                    
                    if (state == IslandState.TYPE_0_RING) {
                        val musicModel = model as? LiveActivityModel.Music
                        val isMedia = musicModel != null && musicModel.isPlaying
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val safeDur = if (musicModel != null && musicModel.durationMs > 0) musicModel.durationMs.toFloat() else 1f
                            val progress = if (isMedia) { (currentMediaPos.longValue.toFloat() / safeDur) } else { globalBatteryLevel.intValue / 100f }
                            val baseColor = if (isMedia) { musicModel?.dominantColor?.let { Color(it) } ?: Color.White } else if (globalIsCharging.value) Color.Green else if (globalBatteryLevel.intValue <= 20) Color.Red else Color.White
                            
                            val strokeW = ringThickness.value.dp.toPx() 
                            val inset = strokeW / 2
                            val arcSize = androidx.compose.ui.geometry.Size(size.width - strokeW, size.height - strokeW)
                            val arcTopLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                            
                            val safeProgress = progress.coerceIn(0.02f, 1f) 
                            val progressAngle = 360f * safeProgress
                            val sweepGradient = Brush.sweepGradient(0.0f to baseColor.copy(alpha = 0.2f), 0.8f to baseColor, 1.0f to baseColor.copy(alpha = 0.2f))

                            drawArc(color = baseColor.copy(alpha=0.15f), startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW))
                            drawArc(brush = sweepGradient, startAngle = -90f, sweepAngle = progressAngle, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Round), alpha = 0.95f)
                            
                            val markerAngle = -90f + progressAngle
                            drawArc(color = Color.White, startAngle = markerAngle - 2f, sweepAngle = 4f, useCenter = false, topLeft = arcTopLeft, size = arcSize, style = Stroke(strokeW, cap = StrokeCap.Round))
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state == IslandState.TYPE_SPLIT, 
                enter = scaleIn(spring(dampingRatio=0.72f, stiffness=320f)) + fadeIn(), exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter)
                    .offset(x = (offsetX + (targetWidth/2) + 24f).dp, y = offsetY.dp)
            ) {
                val sModel = splitModel.value
                val splitBg = if (sModel is LiveActivityModel.Charging) { if (sModel.isPluggedIn) Color.Green.copy(alpha=0.2f) else if (sModel.level <= 20) Color.Red.copy(alpha=0.2f) else Color(0xFF121212).copy(alpha=0.75f) } else Color(0xFF121212).copy(alpha=0.75f)
                Box(modifier = Modifier.size(height)
                        .onGloballyPositioned { coordinates ->
                            val loc = IntArray(2); this@DynamicIslandView.getLocationOnScreen(loc); val b = coordinates.boundsInRoot()
                            val gL = loc[0] + b.left.toInt(); val gT = loc[1] + b.top.toInt(); val gR = loc[0] + b.right.toInt(); val gB = loc[1] + b.bottom.toInt()
                            if (abs(splitCubeRect.left - gL) > 5 || splitCubeRect.isEmpty) { splitCubeRect.set(gL, gT, gR, gB); insetsUpdateFlow.tryEmit(Unit) }
                        }
                        .clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSplitPillClick?.invoke() },
                    contentAlignment = Alignment.Center) {
                    if (sModel is LiveActivityModel.Charging) { val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White; Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = LocalIslandFont.current) }
                }
            }
        }
    }
}
