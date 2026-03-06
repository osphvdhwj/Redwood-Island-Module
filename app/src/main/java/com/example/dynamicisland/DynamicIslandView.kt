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

    // 16 Independent State Variables from Config Studio
    val ringW = mutableStateOf(45f)
    val ringH = mutableStateOf(45f)
    val ringX = mutableStateOf(0f)
    val ringY = mutableStateOf(48f)

    val miniW = mutableStateOf(180f)
    val miniH = mutableStateOf(36f)
    val miniX = mutableStateOf(0f)
    val miniY = mutableStateOf(48f)

    val midW = mutableStateOf(320f)
    val midH = mutableStateOf(80f)
    val midX = mutableStateOf(0f)
    val midY = mutableStateOf(48f)

    val maxW = mutableStateOf(360f)
    val maxH = mutableStateOf(220f)
    val maxX = mutableStateOf(0f)
    val maxY = mutableStateOf(48f)

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
            val modCtx = context.createPackageContext("com.example.dynamicisland", Context.CONTEXT_IGNORE_SECURITY)
            val prefs = modCtx.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

            ringW.value = prefs.getFloat("ring_w", 45f)
            ringH.value = prefs.getFloat("ring_h", 45f)
            ringX.value = prefs.getFloat("ring_x", 0f)
            ringY.value = prefs.getFloat("ring_y", 48f)

            miniW.value = prefs.getFloat("mini_w", 180f)
            miniH.value = prefs.getFloat("mini_h", 36f)
            miniX.value = prefs.getFloat("mini_x", 0f)
            miniY.value = prefs.getFloat("mini_y", 48f)

            midW.value = prefs.getFloat("mid_w", 320f)
            midH.value = prefs.getFloat("mid_h", 80f)
            midX.value = prefs.getFloat("mid_x", 0f)
            midY.value = prefs.getFloat("mid_y", 48f)

            maxW.value = prefs.getFloat("max_w", 360f)
            maxH.value = prefs.getFloat("max_h", 220f)
            maxX.value = prefs.getFloat("max_x", 0f)
            maxY.value = prefs.getFloat("max_y", 48f)
        } catch (e: Exception) {}
    }

    private val configReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.RELOAD_PREFS" -> loadPreferences()
                "com.example.dynamicisland.LIVE_PREVIEW" -> {
                    val typeStr = intent.getStringExtra("preview_state") ?: "TYPE_2_MID"
                    activeModel.value = LiveActivityModel.General("preview", ActivityType.MESSAGE, "Preview", "Adjusting", null, android.graphics.Color.CYAN, true)
                    islandState.value = try { IslandState.valueOf(typeStr) } catch(e: Exception) { IslandState.TYPE_2_MID }
                }
            }
        }
    }

    init {
        loadPreferences()
        val filter = IntentFilter().apply {
            addAction("com.example.dynamicisland.RELOAD_PREFS")
            addAction("com.example.dynamicisland.LIVE_PREVIEW")
        }
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            context.registerReceiver(configReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
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

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val targetW = when (state) {
            IslandState.TYPE_1_MINI -> miniW.value
            IslandState.TYPE_2_MID -> midW.value
            IslandState.TYPE_3_MAX -> maxW.value
            else -> ringW.value
        }
        val targetH = when (state) {
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

        val width by animateDpAsState(targetW.dp, physicsSpec, label = "w")
        val height by animateDpAsState(targetH.dp, physicsSpec, label = "h")
        val offsetX by animateFloatAsState(targetX, floatSpec, label = "x")
        val offsetY by animateFloatAsState(targetY, floatSpec, label = "y")
        val rad by animateDpAsState(if (state == IslandState.TYPE_3_MAX) 42.dp else (targetH / 2).dp, physicsSpec, label = "r")

        LaunchedEffect(width, height, offsetX, offsetY, state) {
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) {
                wp.width = 0; wp.height = 0
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
                    .background(Color.Black)
                    .clickable { if (state != IslandState.TYPE_3_MAX) onSingleTap?.invoke() },
                contentAlignment = Alignment.TopCenter
            ) {
                if (state != IslandState.HIDDEN) {
                    val model = activeModel.value
                    AnimatedContent(targetState = state, label = "content") { s ->
                        when (s) {
                            IslandState.TYPE_3_MAX -> {
                                if (model is LiveActivityModel.Dashboard) DashboardMax(model)
                                else if (model is LiveActivityModel.Music) MusicMax(model)
                            }
                            IslandState.TYPE_2_MID -> {
                                if (model is LiveActivityModel.Music) MusicMid(model)
                                else if (model is LiveActivityModel.Charging) ChargingMid(model)
                                else if (model is LiveActivityModel.General) GeneralMid(model)
                            }
                            else -> {
                                if (model is LiveActivityModel.Music) MusicMini(model)
                                else if (model is LiveActivityModel.HardwareMonitor) HardwareGaugeMini(model)
                                else if (model is LiveActivityModel.General) GeneralMini(model)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MusicMax(music: LiveActivityModel.Music) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)))
                else Box(Modifier.size(60.dp).background(Color.DarkGray, RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(music.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(music.artist, color = Color.LightGray, fontSize = 14.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp))
            val haptic = LocalHapticFeedback.current
            Slider(
                value = if (music.durationMs > 0) music.positionMs.toFloat() / music.durationMs else 0f,
                onValueChange = { onSeekTo?.invoke((it * music.durationMs).toLong()); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                colors = SliderDefaults.colors(activeTrackColor = Color.White)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                Icon(if (music.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(36.dp).clickable { onPlayPauseClick?.invoke() })
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
            }
        }
    }

    @Composable fun DashboardMax(model: LiveActivityModel.Dashboard) { /* Dashboard Logic */ }
    @Composable fun MusicMid(m: LiveActivityModel.Music) { /* Mid Logic */ }
    @Composable fun ChargingMid(c: LiveActivityModel.Charging) { UniversalMid(Color.White, c) }
    @Composable fun GeneralMid(g: LiveActivityModel.General) { UniversalMid(Color.White, g) }
    @Composable fun MusicMini(m: LiveActivityModel.Music) { /* Mini Logic */ }
    @Composable fun GeneralMini(g: LiveActivityModel.General) { /* Mini Logic */ }
    @Composable fun HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) { /* Gauge Logic */ }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityModel) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Icon(getIconForType(activity.type), null, tint = Color.Green, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Activity", color = textColor, fontWeight = FontWeight.Bold, modifier = Modifier.basicMarquee())
                Text("Status", color = textColor.copy(0.7f), fontSize = 14.sp)
            }
        }
    }

    private fun getIconForType(type: ActivityType): ImageVector = when(type) {
        ActivityType.CHARGING -> Icons.Default.BatteryChargingFull
        else -> Icons.Default.Info
    }

    fun setState(s: IslandState) { islandState.value = s }
    fun setModel(m: LiveActivityModel?) { activeModel.value = m }
}
