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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.compositionContext
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

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun start() {
        savedStateRegistryController.performRestore(android.os.Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
}

@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    // 16 Independent State Variables
    var ringW = mutableStateOf(45f)
    var ringH = mutableStateOf(45f)
    var ringX = mutableStateOf(0f)
    var ringY = mutableStateOf(48f)

    var miniW = mutableStateOf(180f)
    var miniH = mutableStateOf(36f)
    var miniX = mutableStateOf(0f)
    var miniY = mutableStateOf(48f)

    var midW = mutableStateOf(320f)
    var midH = mutableStateOf(80f)
    var midX = mutableStateOf(0f)
    var midY = mutableStateOf(48f)

    var maxW = mutableStateOf(360f)
    var maxH = mutableStateOf(220f)
    var maxX = mutableStateOf(0f)
    var maxY = mutableStateOf(48f)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)

    // Interaction Callbacks
    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    private fun loadPreferences() {
        try {
            // Read safely from SystemUI using Xposed
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable()
            pref.reload()

            ringW.value = pref.getFloat("ring_w", 45f)
            ringH.value = pref.getFloat("ring_h", 45f)
            ringX.value = pref.getFloat("ring_x", 0f)
            ringY.value = pref.getFloat("ring_y", 48f)

            miniW.value = pref.getFloat("mini_w", 180f)
            miniH.value = pref.getFloat("mini_h", 36f)
            miniX.value = pref.getFloat("mini_x", 0f)
            miniY.value = pref.getFloat("mini_y", 48f)

            midW.value = pref.getFloat("mid_w", 320f)
            midH.value = pref.getFloat("mid_h", 80f)
            midX.value = pref.getFloat("mid_x", 0f)
            midY.value = pref.getFloat("mid_y", 48f)

            maxW.value = pref.getFloat("max_w", 360f)
            maxH.value = pref.getFloat("max_h", 220f)
            maxX.value = pref.getFloat("max_x", 0f)
            maxY.value = pref.getFloat("max_y", 48f)
        } catch (e: Exception) {
            // Failsafe to defaults
        }
    }

    private val configReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> {
                    // Extract the live numbers straight from the RAM Payload!
                    val prefix = intent.getStringExtra("prefix")
                    if (prefix != null) {
                        val w = intent.getFloatExtra("w", 0f)
                        val h = intent.getFloatExtra("h", 0f)
                        val x = intent.getFloatExtra("x", 0f)
                        val y = intent.getFloatExtra("y", 0f)

                        // Instantly update the UI states without touching disk
                        when (prefix) {
                            "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }
                            "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }
                            "mid" ->  { midW.value = w; midH.value = h; midX.value = x; midY.value = y }
                            "max" ->  { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y }
                        }
                    } else {
                        // Fallback to disk read on first boot
                        loadPreferences()
                    }
                }
                "com.example.dynamicisland.LIVE_PREVIEW" -> {
                    val type = intent.getStringExtra("preview_state") ?: "TYPE_2_MID"
                    activeModel.value = LiveActivityModel.General("preview", ActivityType.MESSAGE, "Preview", "Adjusting", null, android.graphics.Color.CYAN, true)
                    when (type) {
                        "TYPE_1_MINI" -> setState(IslandState.TYPE_1_MINI)
                        "TYPE_2_MID" -> setState(IslandState.TYPE_2_MID)
                        "TYPE_3_MAX" -> {
                            activeModel.value = LiveActivityModel.Dashboard()
                            setState(IslandState.TYPE_3_MAX)
                        }
                        else -> setState(IslandState.HIDDEN)
                    }
                }
                "com.example.dynamicisland.TEST_RING" -> {
                    activeModel.value = LiveActivityModel.Charging(id = "preview_charging", level = 50, isPluggedIn = true)
                    setState(IslandState.TYPE_2_MID)
                }
            }
        }
    }

    init {
        loadPreferences()

        val filter = IntentFilter().apply {
            addAction("com.example.dynamicisland.RELOAD_PREFS")
            addAction("com.example.dynamicisland.TEST_RING")
            addAction("com.example.dynamicisland.LIVE_PREVIEW")
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(configReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(configReceiver, filter)
        }

        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

        val composeView = ComposeView(context).apply {
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    IslandUI(islandState.value)
                }
            }
        }

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.compositionContext = recomposer
        runRecomposeScope.launch { recomposer.runRecomposeAndApplyChanges() }

        addView(composeView)
        lifecycleOwner.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(configReceiver)
        } catch (e: Exception) {}
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val targetWidth = when (state) {
            IslandState.TYPE_1_MINI -> miniW.value
            IslandState.TYPE_2_MID -> midW.value
            IslandState.TYPE_3_MAX -> maxW.value
            else -> ringW.value
        }
        val targetHeight = when (state) {
            IslandState.TYPE_1_MINI -> miniH.value
            IslandState.TYPE_2_MID -> midH.value
            IslandState.TYPE_3_MAX -> maxH.value
            else -> ringH.value
        }
        val targetX = when (state) {
            IslandState.TYPE_1_MINI -> miniX.value
            IslandState.TYPE_2_MID -> midX.value
            IslandState.TYPE_3_MAX -> maxX.value
            else -> ringX.value
        }
        val targetY = when (state) {
            IslandState.TYPE_1_MINI -> miniY.value
            IslandState.TYPE_2_MID -> midY.value
            IslandState.TYPE_3_MAX -> maxY.value
            else -> ringY.value
        }

        val physicsSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 400f)
        val floatSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)
        
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, floatSpec, label = "x")
        val offsetY by animateFloatAsState(targetY, floatSpec, label = "y")

        val radTarget = if (state == IslandState.TYPE_3_MAX) 42.dp else (targetHeight / 2).dp
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        // --- UI PHASE: GLASSMORPHISM & DYNAMIC COLOR ---
        val targetBgColor = if (state == IslandState.HIDDEN) {
            Color.Transparent // 100% Invisible when completely idle
        } else {
            val model = activeModel.value
            // If music is playing and we extracted an album color, crossfade into it!
            if (model is LiveActivityModel.Music && model.dominantColor != null) {
                Color(model.dominantColor).copy(alpha = 0.65f)
            } else {
                // Default: Premium Frosted Dark Glass
                Color(0xFF121212).copy(alpha = 0.85f)
            }
        }
        
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(500), label = "bgColor")
        
        // A subtle white rim-light to make it look physical
        val borderColor by animateColorAsState(
            targetValue = if (state == IslandState.HIDDEN) Color.Transparent else Color.White.copy(alpha = 0.15f),
            animationSpec = tween(500), label = "borderColor"
        )

        LaunchedEffect(width, height, offsetX, offsetY, state) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) {
                wp.width = 0
                wp.height = 0
            } else {
                val extraW = if (state == IslandState.TYPE_3_MAX) 120 else 20
                val extraH = if (state == IslandState.TYPE_3_MAX) 150 else 20
                wp.width = (width.value * density).toInt() + (extraW * density).toInt()
                wp.height = (height.value * density).toInt() + (extraH * density).toInt()
            }
            wp.x = offsetX.toInt()
            wp.y = offsetY.toInt()
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(rad))
                    .background(bgColor) // Dynamically colored background
                    .border(1.dp, borderColor, RoundedCornerShape(rad)) // Glass rim-light
                    .clickable {
                        if (state != IslandState.TYPE_3_MAX) onSingleTap?.invoke()
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                if (state != IslandState.HIDDEN) {
                    val model = activeModel.value

                    val bottomPadding by animateDpAsState(
                        targetValue = if (state == IslandState.TYPE_3_MAX) 24.dp else 0.dp,
                        label = "bottomPadding"
                    )

                    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                        AnimatedContent(
                            targetState = state,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f)) togetherWith 
                                (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f))
                            },
                            label = "content_morph"
                        ) { s ->
                            when (s) {
                                IslandState.TYPE_3_MAX -> {
                                    if (model is LiveActivityModel.Dashboard) DashboardMax(model)
                                    else if (model is LiveActivityModel.Music) MusicMax(model)
                                }
                                IslandState.TYPE_2_MID -> {
                                    if (model is LiveActivityModel.Music) MusicMid(model)
                                    else if (model is LiveActivityModel.General) GeneralMid(model)
                                    else if (model is LiveActivityModel.Charging) ChargingMid(model)
                                }
                                else -> {
                                    if (model is LiveActivityModel.Music) MusicMini(model)
                                    else if (model is LiveActivityModel.General) GeneralMini(model)
                                    else if (model is LiveActivityModel.HardwareMonitor) HardwareGaugeMini(model)
                                }
                            }
                        }
                    }

                    if (state == IslandState.TYPE_3_MAX) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(32.dp)
                                .clickable { onCloseClick?.invoke() }
                                .pointerInput(Unit) {
                                    detectDragGestures { _, dragAmount ->
                                        if (dragAmount.y < -10) onCloseClick?.invoke()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(40.dp).height(5.dp).background(Color.White.copy(alpha=0.3f), CircleShape))
                        }
                    }
                }
                
                val activity = activeModel.value
                if (state == IslandState.HIDDEN && activity != null) {
                    val progress = when(activity) {
                        is LiveActivityModel.General -> activity.progress
                        is LiveActivityModel.Charging -> activity.level / 100f
                        else -> null
                    }
                    if (progress != null) {
                        val safeProgress = if (progress.isNaN() || progress.isInfinite()) 0f else progress.coerceIn(0f, 1f)
                        val ringBrush = Brush.sweepGradient(colors = listOf(Color.Cyan, Color.Blue, Color.Magenta, Color.Cyan))

                        Canvas(modifier = Modifier.size(ringW.value.dp + 6.dp, ringH.value.dp + 6.dp)) {
                            drawArc(
                                brush = ringBrush,
                                startAngle = -90f,
                                sweepAngle = safeProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DashboardMax(model: LiveActivityModel.Dashboard) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(50.dp).background(if (model.isWifiOn) Color.Blue else Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "WiFi", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Box(modifier = Modifier.size(50.dp).background(if (model.isTorchOn) Color.Yellow else Color.DarkGray, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Torch", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Slider(value = model.currentVolume.toFloat(), onValueChange = {}, valueRange = 0f..model.maxVolume.toFloat(), colors = SliderDefaults.colors(activeTrackColor = Color.White))
        }
    }

    @Composable
    fun MusicMax(music: LiveActivityModel.Music) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)))
                else Box(Modifier.size(60.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(12.dp)))

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = music.artist, color = Color.White.copy(alpha=0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            val haptic = LocalHapticFeedback.current
            val interactionSource = remember { MutableInteractionSource() }
            val isDragged by interactionSource.collectIsDraggedAsState()
            var localPosition by remember(isDragged) { mutableStateOf(music.positionMs.toFloat()) }

            val realProgress = if (music.durationMs > 0) (music.positionMs.toFloat() / music.durationMs.toFloat()) else 0f
            val safeProgress = if (realProgress.isNaN() || realProgress.isInfinite()) 0f else realProgress.coerceIn(0f, 1f)

            Slider(
                value = if (isDragged) (localPosition / music.durationMs.toFloat()).coerceIn(0f, 1f) else safeProgress,
                onValueChange = { 
                    localPosition = it * music.durationMs.toFloat() 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onValueChangeFinished = { 
                    onSeekTo?.invoke(localPosition.toLong()) 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                interactionSource = interactionSource,
                colors = SliderDefaults.colors(activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha=0.3f), thumbColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                val playIcon = if (music.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow
                Box(modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.15f), CircleShape).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun GeneralMini(general: LiveActivityModel.General) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Icon(imageVector = getIconForType(general.type), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "${general.title} • ${general.dataText}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(music: LiveActivityModel.Music) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @Composable
    fun MusicMid(music: LiveActivityModel.Music) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                 Text(text = music.artist, color = Color.White.copy(alpha=0.7f), fontSize = 14.sp, maxLines = 1)
            }
        }
    }

    @Composable
    fun HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) {
        val tempColor = when {
            hw.cpuTempCelsius > 45f -> Color.Red
            hw.cpuTempCelsius > 38f -> Color.Yellow
            else -> Color.Green
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Hardware", tint = tempColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = tempColor,
                trackColor = Color.White.copy(alpha=0.2f)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityModel) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "alphaPulse"
        )

        val progress = when(activity) {
            is LiveActivityModel.General -> activity.progress
            is LiveActivityModel.Charging -> activity.level / 100f
            else -> null
        }
        val colorInt = when(activity) {
            is LiveActivityModel.General -> activity.accentColor
            is LiveActivityModel.Charging -> android.graphics.Color.GREEN
            else -> android.graphics.Color.WHITE
        }
        val title = when(activity) {
            is LiveActivityModel.General -> activity.title
            is LiveActivityModel.Charging -> if (activity.isPluggedIn) "Charging" else "Disconnected"
            else -> ""
        }
        val dataText = when(activity) {
            is LiveActivityModel.General -> activity.dataText
            is LiveActivityModel.Charging -> "${activity.level}%"
            else -> ""
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (progress != null) {
                    CircularProgressIndicator(progress = { progress }, color = Color(colorInt), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                }
                val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f
                Icon(
                    imageVector = getIconForType(activity.type), 
                    contentDescription = null, 
                    tint = Color(colorInt), 
                    modifier = Modifier.size(24.dp).alpha(iconAlpha)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = dataText, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
        }
    }

    @Composable
    fun ChargingMid(charging: LiveActivityModel.Charging) {
        UniversalMid(Color.White, charging)
    }

    @Composable
    fun GeneralMid(general: LiveActivityModel.General) {
        UniversalMid(Color.White, general)
    }

    fun setState(newState: IslandState) {
        islandState.value = newState
    }
    
    fun setModel(model: LiveActivityModel?) { 
        activeModel.value = model 
    }

    private fun getIconForType(type: ActivityType): ImageVector {
        return when(type) {
            ActivityType.CALL -> Icons.Default.Phone
            ActivityType.NAVIGATION -> Icons.Default.LocationOn
            ActivityType.TIMER -> Icons.Default.Notifications
            ActivityType.MESSAGE -> Icons.Default.Email
            ActivityType.ALARM -> Icons.Default.Notifications
            ActivityType.CHARGING -> Icons.Default.Add
            ActivityType.BATTERY_LOW -> Icons.Default.Warning
            ActivityType.BLUETOOTH -> Icons.Default.Share
            ActivityType.WIFI -> Icons.Default.Search
            ActivityType.HARDWARE -> Icons.Default.Info
            else -> Icons.Default.Info
        }
    }
}
