package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
// The missing Foundation and Drawing modifiers
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.draw.alpha

// Material You Dynamic Colors (CrDroid 11.8 integration)
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.ui.platform.LocalContext

// Custom Canvas & Gradients for the Glowing Ring
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

// Haptic Feedback Engine
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// Hardware Gauge Icon
import androidx.compose.material.icons.filled.Info

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.changedToDown
import androidx.core.graphics.drawable.toBitmap


import androidx.lifecycle.*
import androidx.savedstate.*

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



    var camOffsetX = mutableStateOf(0)
    var camOffsetY = mutableStateOf(48)
    var camWidth = mutableStateOf(24)
    var camHeight = mutableStateOf(24)

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

    private val configReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.UPDATE_CONFIG" -> {
                    camOffsetX.value = intent.getIntExtra("offsetX", 0)
                    camOffsetY.value = intent.getIntExtra("offsetY", 48)
                    camWidth.value = intent.getIntExtra("camWidth", 24)
                    camHeight.value = intent.getIntExtra("camHeight", 24)
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
        val filter = IntentFilter().apply {
            addAction("com.example.dynamicisland.UPDATE_CONFIG")
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
            IslandState.HIDDEN -> camWidth.value.dp
            IslandState.TYPE_1_MINI -> 180.dp
            IslandState.TYPE_2_MID -> 320.dp
            IslandState.TYPE_3_MAX -> 360.dp
            else -> 180.dp
        }
        val targetHeight = when (state) {
            IslandState.HIDDEN -> camHeight.value.dp
            IslandState.TYPE_1_MINI -> 36.dp
            IslandState.TYPE_2_MID -> 80.dp
            IslandState.TYPE_3_MAX -> 220.dp
            else -> 36.dp
        }
        val cornerRadius = when (state) {
            IslandState.HIDDEN -> (camHeight.value / 2).dp
            IslandState.TYPE_1_MINI -> 18.dp
            IslandState.TYPE_2_MID -> 32.dp
            IslandState.TYPE_3_MAX -> 42.dp
            else -> 18.dp
        }

        val physicsSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 400f)
        val width by animateDpAsState(targetWidth, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight, physicsSpec, label = "height")
        val rad by animateDpAsState(cornerRadius, physicsSpec, label = "rad")

        // THE INVISIBLE TAB FIX: Dynamic Padding
        LaunchedEffect(width, height, state) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) {
                wp.width = 0
                wp.height = 0
            } else {
                // Shrink the invisible boundary for MINI/MID so you can touch the top screen
                val extraW = if (state == IslandState.TYPE_3_MAX) 120 else 20
                val extraH = if (state == IslandState.TYPE_3_MAX) 150 else 20

                wp.width = (width.value * density).toInt() + (extraW * density).toInt()
                wp.height = (height.value * density).toInt() + (extraH * density).toInt()
            }
            wp.x = camOffsetX.value
            wp.y = camOffsetY.value
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(rad))
                    .background(Color.Black)
                    .clickable {
                        if (state != IslandState.TYPE_3_MAX) onSingleTap?.invoke()
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                if (state != IslandState.HIDDEN) {
                    val model = activeModel.value

                                        // Safely animate the padding to prevent transient negative values on Android 15
                    val bottomPadding by animateDpAsState(
                        targetValue = if (state == IslandState.TYPE_3_MAX) 24.dp else 0.dp,
                        label = "bottomPadding"
                    )

                    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                        AnimatedContent(targetState = state, label = "morph") { s ->
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

                    // THE GRABBER LOGIC (___)
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
                            Box(modifier = Modifier.width(40.dp).height(5.dp).background(Color.DarkGray, CircleShape))
                        }
                    }
                }
            }
        }
    }

    // --- DASHBOARD UI ---
    @Composable
    fun DashboardMax(model: LiveActivityModel.Dashboard) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                // 100% safe mathematically drawn icons (No R.drawable)
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

    // --- MUSIC UI ---
    @Composable
    fun MusicMax(music: LiveActivityModel.Music) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)))
                else Box(Modifier.size(60.dp).background(Color.DarkGray, RoundedCornerShape(12.dp)))

                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = music.artist, color = Color.LightGray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Native Slider Engine (Safe from SystemUI crashes)
            val interactionSource = remember { MutableInteractionSource() }
            val isDragged by interactionSource.collectIsDraggedAsState()
            var localPosition by remember(isDragged) { mutableStateOf(music.positionMs.toFloat()) }

            val realProgress = if (music.durationMs > 0) (music.positionMs.toFloat() / music.durationMs.toFloat()) else 0f
            val safeProgress = if (realProgress.isNaN() || realProgress.isInfinite()) 0f else realProgress.coerceIn(0f, 1f)

            Slider(
                value = if (isDragged) (localPosition / music.durationMs.toFloat()).coerceIn(0f, 1f) else safeProgress,
                onValueChange = { localPosition = it * music.durationMs.toFloat() },
                onValueChangeFinished = { onSeekTo?.invoke(localPosition.toLong()) },
                interactionSource = interactionSource,
                colors = SliderDefaults.colors(activeTrackColor = Color.White, inactiveTrackColor = Color.DarkGray, thumbColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })

                // Safe Play/Pause handling
                val playIcon = if (music.isPlaying) Icons.Default.Close else Icons.Default.PlayArrow
                Box(modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.15f), CircleShape).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
            }
        }
    }

    // Mini and Mid
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun GeneralMini(general: LiveActivityModel.General) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val iconRes = getIconForType(general.type)
            Icon(androidx.compose.ui.res.painterResource(iconRes), contentDescription = null, tint = Color(general.accentColor), modifier = Modifier.size(16.dp))
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
                 Text(text = music.artist, color = Color.LightGray, fontSize = 14.sp, maxLines = 1)
            }
        }
    }

    @Composable
    fun HardwareGaugeMini(hw: LiveActivityModel.HardwareMonitor) {
        // Dynamically shift color based on thermal throttling thresholds
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

            // Visual Line Gauge
            androidx.compose.material3.LinearProgressIndicator(
                progress = { (hw.cpuTempCelsius / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = tempColor,
                trackColor = Color.DarkGray
            )

            Spacer(Modifier.width(8.dp))
            Text(text = "${hw.cpuFreqMhz} MHz", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityModel) {
        // Create a pulsing alpha animation for the charging state
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
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
                    CircularProgressIndicator(
                        progress = { progress },
                        color = Color(colorInt),
                        trackColor = textColor.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Apply the pulse animation ONLY if it is actively charging
                val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f

                Icon(
                    androidx.compose.ui.res.painterResource(getIconForType(activity.type)),
                    contentDescription = null,
                    tint = Color(colorInt), // This automatically applies the Red/Yellow/Green battery color
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

    // --- Public API ---
    fun setState(newState: IslandState) {
        if (!isAttachedToWindow && newState != IslandState.HIDDEN) return
        islandState.value = newState
    }
    fun setModel(model: LiveActivityModel?) { activeModel.value = model }

    private fun getIconForType(type: ActivityType): Int {
        return when(type) {
            ActivityType.CALL -> R.drawable.ic_call_vector
            ActivityType.NAVIGATION -> R.drawable.ic_map_vector
            ActivityType.TIMER -> R.drawable.ic_timer_vector
            ActivityType.MESSAGE -> R.drawable.ic_mail_vector
            ActivityType.ALARM -> R.drawable.ic_alarm_vector
            ActivityType.CHARGING -> R.drawable.ic_battery_charging_vector // Active charging
            ActivityType.BATTERY_LOW -> R.drawable.ic_battery_alert_vector // Disconnected/Low
            ActivityType.BLUETOOTH -> R.drawable.ic_bluetooth_vector
            ActivityType.WIFI -> R.drawable.ic_wifi_vector
            else -> R.drawable.ic_sync_vector
        }
    }

}
