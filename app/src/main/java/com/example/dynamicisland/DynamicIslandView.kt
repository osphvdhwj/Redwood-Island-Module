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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun start() { savedStateRegistryController.performRestore(android.os.Bundle()); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
}

@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)
    var currentMediaPos = mutableLongStateOf(0L) //🆕
    
    var cubeW = mutableStateOf(85f); var cubeH = mutableStateOf(85f); var cubeX = mutableStateOf(0f); var cubeY = mutableStateOf(48f)

    var padT = mutableStateOf(0f); var padB = mutableStateOf(0f); var padL = mutableStateOf(0f); var padR = mutableStateOf(0f)
    var ringThickness = mutableStateOf(6f)

    var isCubeRotationEnabled = mutableStateOf(true)
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) 

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: ((IslandState) -> Unit)? = null
    var onPillLongPress: (() -> Unit)? = null
    var onGrabberDragDown: (() -> Unit)? = null
    var onGrabberDragUp: (() -> Unit)? = null
    var onGrabberLongPress: (() -> Unit)? = null
    var onSwipeLeft: ((IslandState) -> Unit)? = null
    var onSwipeRight: ((IslandState) -> Unit)? = null
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
            isCubeRotationEnabled.value = pref.getBoolean("rotate_cube", true)
        } catch (e: Exception) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> {
                    val prefix = intent.getStringExtra("prefix")
                    if (prefix != null) {
                        val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f); val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                        when (prefix) { 
                            "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }
                            "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }
                            "mid" -> { midW.value = w; midH.value = h; midX.value = x; midY.value = y }
                            "max" -> { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y }
                            "cube" -> { cubeW.value = w; cubeH.value = h; cubeX.value = x; cubeY.value = y } 
                        }
                        padT.value = intent.getFloatExtra("pad_t", padT.value)
                        padB.value = intent.getFloatExtra("pad_b", padB.value)
                        padL.value = intent.getFloatExtra("pad_l", padL.value)
                        padR.value = intent.getFloatExtra("pad_r", padR.value)
                        ringThickness.value = intent.getFloatExtra("ring_thickness", ringThickness.value)
                    } else {
                        loadPreferences()
                    }
                }
                "com.example.dynamicisland.BATTERY_UPDATE" -> {
                    globalBatteryLevel.value = intent.getIntExtra("level", 100)
                    globalIsCharging.value = intent.getBooleanExtra("isCharging", false)
                }
                "com.example.dynamicisland.TICKER_UPDATE" -> {
                    currentMediaPos.longValue = intent.getLongExtra("pos", 0L)
                }
            }
        }
    }

    init {
        loadPreferences()
        
        // 🚀 CRITICAL: Added TICKER_UPDATE to the IntentFilter so the view actually hears the broadcast!
        val filter = IntentFilter().apply { 
            addAction("com.example.dynamicisland.RELOAD_PREFS")
            addAction("com.example.dynamicisland.BATTERY_UPDATE")
            addAction("com.example.dynamicisland.TICKER_UPDATE") 
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else { 
            @Suppress("UnspecifiedRegisterReceiverFlag") 
            context.registerReceiver(receiver, filter) 
        }
        
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

        val composeView = ComposeView(context).apply { 
            setContent { 
                MaterialTheme(colorScheme = darkColorScheme()) { 
                    CompositionLocalProvider(LocalContext provides moduleContext) { IslandUI(islandState.value) } 
                } 
            } 
        }
        
        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }
        addView(composeView)
        lifecycleOwner.start()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val targetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        // 🚀 FLUID APPLE PHYSICS (Slower, heavier spring, less bouncy)
        val physicsSpec = spring<Dp>(dampingRatio = 0.82f, stiffness = 350f)
        
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "y")
        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        val model = activeModel.value

        // 0.01f makes it invisible to eyes, but 100% physically touchable for WindowManager!
        val targetBgColor = if (state == IslandState.HIDDEN) Color.Transparent 
        else if (state == IslandState.TYPE_0_RING) Color.Black.copy(alpha = 0.01f) 
        else {
            if (model is LiveActivityModel.Music && model.dominantColor != null && state != IslandState.TYPE_3_MAX) Color(model.dominantColor).copy(alpha = 0.65f) 
            else if (state == IslandState.TYPE_3_MAX) Color(0xFF121212).copy(alpha = 0.4f) 
            else Color(0xFF121212).copy(alpha = 0.75f) 
        }
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.15f), animationSpec = tween(600), label = "borderColor")

        LaunchedEffect(width, height, offsetX, offsetY, state) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) { wp.width = 0; wp.height = 0 } else {
                // 🚀 FIXED: Removed arbitrary padding that blocked touches outside the pill!
                val splitExtra = if (state == IslandState.TYPE_SPLIT) (height.value + 16f) else 0f
                wp.width = ((width.value + splitExtra) * density).toInt()
                wp.height = (height.value * density).toInt()
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { 
                    wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND; wp.blurBehindRadius = 45 
                }
            }
            wp.x = offsetX.toInt(); wp.y = offsetY.toInt()
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        var dragOffsetX by remember { mutableStateOf(0f) }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(width).height(height).clip(RoundedCornerShape(rad))
                    .background(bgColor).border(1.dp, borderColor, RoundedCornerShape(rad))
                    .pointerInput(Unit) { 
                        detectTapGestures(
                            onTap = { if (state == IslandState.TYPE_0_RING || state == IslandState.HIDDEN) onSingleTap?.invoke() },
                            onDoubleTap = { onDoubleTap?.invoke(state) },
                            onLongPress = { if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) onPillLongPress?.invoke() } 
                        ) 
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { if (abs(dragOffsetX) > 40) { if (dragOffsetX > 40) onSwipeRight?.invoke(state) else if (dragOffsetX < -40) onSwipeLeft?.invoke(state) }; dragOffsetX = 0f },
                            onDrag = { change, dragAmount -> change.consume(); dragOffsetX += dragAmount.x } 
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // INNER PADDING BOX
                Box(modifier = Modifier.fillMaxSize().padding(start = padL.value.dp, top = padT.value.dp, end = padR.value.dp, bottom = padB.value.dp)) {
                    
                    if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                        Image(bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(if (state == IslandState.TYPE_3_MAX) 0.85f else 0.35f).blur(if (state == IslandState.TYPE_3_MAX) 40.dp else 24.dp))
                    }

                    if (state != IslandState.HIDDEN) {
                        val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")
                        Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                            AnimatedContent(targetState = state, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "UI Transition") { s ->
                                when (s) {
                                    IslandState.TYPE_3_MAX -> { if (model is LiveActivityModel.Dashboard) DashboardMax(model) else if (model is LiveActivityModel.Music) MusicMax(model) }
                                    IslandState.TYPE_2_MID -> { if (model is LiveActivityModel.Dashboard) DashboardMid(model) else if (model is LiveActivityModel.Music) MusicMid(model) else if (model is LiveActivityModel.General) GeneralMid(model) else if (model is LiveActivityModel.Charging) ChargingMid(model) }
                                    IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { if (model is LiveActivityModel.Music) MusicMini(model) else if (model is LiveActivityModel.General) GeneralMini(model) else if (model is LiveActivityModel.HardwareMonitor) HardwareGaugeMini(model) }
                                    IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                                    else -> {} 
                                }
                            }
                        }

                        if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX || state == IslandState.TYPE_SPLIT) {
                            var grabberDragY by remember { mutableStateOf(0f) }
                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(when(state) { IslandState.TYPE_3_MAX -> 32.dp; IslandState.TYPE_2_MID -> 20.dp; else -> 16.dp })
                                    .pointerInput(Unit) { detectDragGestures(onDragEnd = { if (grabberDragY > 40) onGrabberDragDown?.invoke() else if (grabberDragY < -40) onGrabberDragUp?.invoke(); grabberDragY = 0f }, onDrag = { change, dragAmount -> change.consume(); grabberDragY += dragAmount.y }) }
                                    .pointerInput(Unit) { detectTapGestures(onLongPress = { onGrabberLongPress?.invoke() }) }, 
                                contentAlignment = Alignment.Center
                            ) { 
                                Box(modifier = Modifier
                                    .width(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 24.dp else 40.dp)
                                    .height(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 3.dp else 5.dp)
                                    .shadow(2.dp, CircleShape) // 🚀 ADDED GRABBER DROP SHADOW
                                    .background(Color.White.copy(alpha=0.8f), CircleShape)
                                ) 
                            }
                        }
                    }
                    
                    if (state == IslandState.TYPE_0_RING) {
                        val isMedia = model is LiveActivityModel.Music && model.isPlaying
                        val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.value <= 20
                        if (shouldShowRing) {
                            val progress = if (isMedia) ((model as LiveActivityModel.Music).positionMs.toFloat() / model.durationMs.toFloat()) else globalBatteryLevel.value / 100f
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

            // 🚀 SMOOTH SCALE IN CHARGING CUBE
            AnimatedVisibility(visible = state == IslandState.TYPE_SPLIT, enter = scaleIn(spring(dampingRatio=0.8f, stiffness=300f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                val sModel = splitModel.value
                val splitBg = if (sModel is LiveActivityModel.Charging) { if (sModel.isPluggedIn) Color.Green.copy(alpha=0.2f) else if (sModel.level <= 20) Color.Red.copy(alpha=0.2f) else Color(0xFF121212).copy(alpha=0.75f) } else Color(0xFF121212).copy(alpha=0.75f)
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(height).clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape), contentAlignment = Alignment.Center) {
                        if (sModel is LiveActivityModel.Charging) { val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White; Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    private fun formatTime(ms: Long): String { if (ms <= 0) return "0:00"; val s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60) }

    @Composable
    fun ChargingCube(model: LiveActivityModel.Charging) {
        val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${model.level}%", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(music: LiveActivityModel.Music) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                val infiniteTransition = rememberInfiniteTransition(); val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
                val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape).rotate(currentRotation)) else Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f).basicMarquee())
                Spacer(Modifier.width(8.dp))
                Text(text = "${formatTime(music.positionMs)} / ${formatTime(music.durationMs)}", color = Color.White.copy(alpha=0.7f), fontSize = 12.sp)
            }
            val safeDuration = if (music.durationMs > 0) currentMediaPos.longValue.toFloat() else 1f
            LinearProgressIndicator(progress = { (music.positionMs.toFloat() / safeDuration).coerceIn(0f, 1f) }, color = Color.White.copy(alpha=0.8f), trackColor = Color.Transparent, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).height(2.dp).padding(bottom = 1.dp).clip(CircleShape))
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMid(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color(music.titleTextColor)
        val progress = if (music.durationMs > 0) (currentMediaPos.longValue.toFloat() / music.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        val infiniteTransition = rememberInfiniteTransition(); val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart))
        val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(progress = { progress }, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha = 0.2f), strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
                if (music.albumArt != null) {
                    Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(44.dp).clip(CircleShape).rotate(currentRotation).pointerInput(Unit) { detectTapGestures(onLongPress = { onPillLongPress?.invoke() }) }) 
                } else Box(Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = dynamicTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = music.artist, color = dynamicTextColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
            Spacer(Modifier.width(8.dp))
            Text(text = "${formatTime(music.positionMs)} / ${formatTime(music.durationMs)}", color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMax(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color.White 
        var audioIcon by remember { mutableStateOf(Icons.Default.Smartphone) }; var audioLabel by remember { mutableStateOf("Phone") }
        LaunchedEffect(music) {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val devices = am.getDevices(android.media.AudioManager.GET_DEVICES_OUTPUTS)
            val hasBt = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER || it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            val hasHeadphone = devices.any { it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET }
            if (hasBt) { audioIcon = Icons.Default.Bluetooth; audioLabel = "Bluetooth" } else if (hasHeadphone) { audioIcon = Icons.Default.Headset; audioLabel = "Headphones" } else { audioIcon = Icons.Default.Smartphone; audioLabel = "Phone" }
        }

        Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (music.appIcon != null) { Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Logo", modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).pointerInput(Unit) { detectTapGestures(onLongPress = { onPillLongPress?.invoke() }) }) } else Box(Modifier.size(36.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(10.dp)))
                Row(modifier = Modifier.background(Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp).clickable { onAudioOutputClick?.invoke() }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(audioIcon, contentDescription = "Output", tint = dynamicTextColor, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(6.dp)); Text(audioLabel, color = dynamicTextColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = music.title, color = dynamicTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee()) 
            Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = 16.sp, maxLines = 1, modifier = Modifier.fillMaxWidth().basicMarquee())
            Spacer(modifier = Modifier.height(16.dp))

            val haptic = LocalHapticFeedback.current; val interactionSource = remember { MutableInteractionSource() }; val isDragged by interactionSource.collectIsDraggedAsState()
            var localPosition by remember(isDragged) { mutableStateOf(currentMediaPos.longValue.toFloat()) }
            val safeDuration = if (music.durationMs > 0) music.durationMs.toFloat() else 1f
            val safePosition = if (isDragged) localPosition else music.positionMs.toFloat()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(text = formatTime(safePosition.toLong()), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp); Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp) }
            Slider(value = (safePosition / safeDuration).coerceIn(0f, 1f), onValueChange = { localPosition = it * safeDuration; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, onValueChangeFinished = { onSeekTo?.invoke(localPosition.toLong()); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }, interactionSource = interactionSource, colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.3f), thumbColor = dynamicTextColor), modifier = Modifier.fillMaxWidth().height(24.dp))
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = dynamicTextColor, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                Box(modifier = Modifier.size(56.dp).background(dynamicTextColor.copy(alpha = 0.2f), CircleShape).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) { Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = dynamicTextColor, modifier = Modifier.size(32.dp)) }
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = dynamicTextColor, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
            }
        }
    }

    @Composable
    fun DashboardMid(model: LiveActivityModel.Dashboard) { Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) { Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Phone, null, tint=Color.White, modifier=Modifier.size(24.dp)) }; Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Email, null, tint=Color.White, modifier=Modifier.size(24.dp)) }; Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Build, null, tint=Color.White, modifier=Modifier.size(24.dp)) }; Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Settings, null, tint=Color.White, modifier=Modifier.size(24.dp)) } } }
    @Composable
    fun DashboardMax(model: LiveActivityModel.Dashboard) { Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Quick Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start)); Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Box(modifier = Modifier.size(60.dp).background(if (model.isWifiOn) Color.Blue else Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(28.dp)) }; Box(modifier = Modifier.size(60.dp).background(if (model.isTorchOn) Color.Yellow else Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, null, tint = Color.Black, modifier = Modifier.size(28.dp)) }; Box(modifier = Modifier.size(60.dp).background(Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(28.dp)) } }; Spacer(modifier = Modifier.height(24.dp)); Slider(value = model.currentVolume.toFloat(), onValueChange = {}, valueRange = 0f..model.maxVolume.toFloat(), colors = SliderDefaults.colors(activeTrackColor = Color.White)) } }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun GeneralMini(general: LiveActivityModel.General) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = getIconForType(general.type), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee()) } }
    @Composable
    fun HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) { val tempColor = when { hw.cpuTempCelsius > 45f -> Color.Red; hw.cpuTempCelsius > 38f -> Color.Yellow; else -> Color.Green }; Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) { Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); androidx.compose.material3.LinearProgressIndicator(progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) }, modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)), color = tempColor, trackColor = Color.White.copy(alpha=0.2f)); Spacer(Modifier.width(8.dp)); Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) } }
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityModel) { val infiniteTransition = rememberInfiniteTransition(label = "pulse"); val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse"); val progress = when(activity) { is LiveActivityModel.General -> activity.progress; is LiveActivityModel.Charging -> activity.level / 100f; else -> null }; val colorInt = when(activity) { is LiveActivityModel.General -> activity.accentColor; is LiveActivityModel.Charging -> android.graphics.Color.GREEN; else -> android.graphics.Color.WHITE }; val title = when(activity) { is LiveActivityModel.General -> activity.title; is LiveActivityModel.Charging -> if (activity.isPluggedIn) "Charging" else "Disconnected"; else -> "" }; val dataText = when(activity) { is LiveActivityModel.General -> activity.dataText; is LiveActivityModel.Charging -> "${activity.level}%"; else -> "" }; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) { if (progress != null) CircularProgressIndicator(progress = { progress }, color = Color(colorInt), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize()); val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f; Icon(imageVector = getIconForType(activity.type), contentDescription = null, tint = Color(colorInt), modifier = Modifier.size(24.dp).alpha(iconAlpha)) }; Spacer(Modifier.width(16.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee()); Text(text = dataText, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee()) } } }
    @Composable
    fun ChargingMid(charging: LiveActivityModel.Charging) { UniversalMid(Color.White, charging) }
    @Composable
    fun GeneralMid(general: LiveActivityModel.General) { UniversalMid(Color.White, general) }
    fun setState(newState: IslandState) { islandState.value = newState }
    fun setModel(model: LiveActivityModel?) { activeModel.value = model }
    fun setSplitModel(model: LiveActivityModel?) { splitModel.value = model }
    private fun getIconForType(type: ActivityType): ImageVector { return when(type) { ActivityType.CALL -> Icons.Default.Phone; ActivityType.NAVIGATION -> Icons.Default.LocationOn; ActivityType.TIMER -> Icons.Default.Notifications; ActivityType.MESSAGE -> Icons.Default.Email; ActivityType.ALARM -> Icons.Default.Notifications; ActivityType.CHARGING -> Icons.Default.Add; ActivityType.BATTERY_LOW -> Icons.Default.Warning; ActivityType.BLUETOOTH -> Icons.Default.Share; ActivityType.WIFI -> Icons.Default.Wifi; ActivityType.HARDWARE -> Icons.Default.Info; else -> Icons.Default.Info } }
}
