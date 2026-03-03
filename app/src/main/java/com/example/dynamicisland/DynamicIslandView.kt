package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.palette.graphics.Palette
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.Locale

@SuppressLint("ModifierFactoryUnreferencedReceiver")
fun Modifier.bounceClick(
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "bounce"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    }

    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

// THE DEFERRED WAVY SLIDER (POINT 8)
@Composable
fun WavySlider(
    progress: Float,
    isPlaying: Boolean,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.3f),
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
) {
    val isDragging by interactionSource.collectIsDraggedAsState()
    var visualProgress by remember { mutableStateOf(progress) }

    // Snapback logic
    LaunchedEffect(progress, isDragging) {
        if (!isDragging) visualProgress = progress
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phaseShift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 24f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)), label = "phaseShift"
    )

    Box(modifier = modifier.height(24.dp), contentAlignment = Alignment.CenterStart) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val midY = size.height / 2
            val activeWidth = width * visualProgress
            val waveLengthPx = 24.dp.toPx()
            val amplitudePx = 3.dp.toPx()

            drawLine(color = inactiveColor, start = androidx.compose.ui.geometry.Offset(activeWidth, midY), end = androidx.compose.ui.geometry.Offset(width, midY), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)

            if (isPlaying) {
                androidx.compose.ui.graphics.drawscope.clipRect(left = 0f, right = activeWidth) {
                    val path = androidx.compose.ui.graphics.Path()
                    var x = -phaseShift.dp.toPx()
                    path.moveTo(x, midY)
                    while (x < activeWidth + waveLengthPx) {
                        val halfWave = waveLengthPx / 2
                        path.quadraticBezierTo(x + halfWave / 2, midY - amplitudePx, x + halfWave, midY)
                        path.quadraticBezierTo(x + halfWave * 1.5f, midY + amplitudePx, x + waveLengthPx, midY)
                        x += waveLengthPx
                    }
                    drawPath(path = path, color = activeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            } else {
                drawLine(color = activeColor, start = androidx.compose.ui.geometry.Offset(0f, midY), end = androidx.compose.ui.geometry.Offset(activeWidth, midY), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            drawRoundRect(color = activeColor, topLeft = androidx.compose.ui.geometry.Offset(activeWidth - 4.dp.toPx(), midY - 10.dp.toPx()), size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 20.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()))
        }

        androidx.compose.material3.Slider(
            value = visualProgress,
            onValueChange = { visualProgress = it },
            onValueChangeFinished = { onSeekFinished(visualProgress) },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxSize().alpha(0.01f)
        )
    }
}

// WAVY CIRCULAR RING (POINTS 5 & 6)
@Composable
fun WavyCircularRing(progress: Float, isPlaying: Boolean, modifier: Modifier = Modifier, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "ringWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "phaseShift"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val radius = size.width / 2f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val path = androidx.compose.ui.graphics.Path()
        val amplitude = if (isPlaying) 4.dp.toPx() else 0f
        val waves = 8
        val maxAngle = (progress * 360).toInt().coerceIn(0, 360)

        if (maxAngle > 0) {
            for (i in -90..(-90 + maxAngle)) {
                val angleRad = Math.toRadians(i.toDouble())
                val r = radius + amplitude * Math.sin(waves * angleRad + phase)
                val x = (center.x + r * Math.cos(angleRad)).toFloat()
                val y = (center.y + r * Math.sin(angleRad)).toFloat()
                if (i == -90) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

// DASHBOARD UI COMPONENT
@Composable
fun DashboardMax(textColor: Color) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var volume by remember { mutableStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)) }

    // Fetch Pinned Apps from SharedPreferences
    val prefs = context.getSharedPreferences("DynamicIslandPrefs", Context.MODE_PRIVATE)
    val pinnedAppsStr = prefs.getString("pinned_apps", "com.whatsapp,com.instagram.android") ?: ""
    val pinnedPackages = pinnedAppsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QsButton(R.drawable.ic_wifi_vector, "Connect", haptic, textColor) { 
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
            QsButton(R.drawable.ic_play_vector, "Torch", haptic, textColor) { /* Torch Backend via CameraManager */ }
            QsButton(R.drawable.ic_alarm_vector, "Sound", haptic, textColor) { /* Sound Mode */ }
            QsButton(R.drawable.ic_sync_vector, "Add", haptic, textColor) {
                val intent = Intent(context, ConfigActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(textColor.copy(alpha=0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Icon(painterResource(R.drawable.ic_play_vector), contentDescription = "Volume", tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            androidx.compose.material3.Slider(
                value = volume, onValueChange = { newVol ->
                    volume = newVol
                    val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (newVol * maxVol).toInt(), 0)
                },
                modifier = Modifier.weight(1f).height(24.dp), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha=0.3f))
            )
        }

        Column {
            Text("Pinned Apps", color = textColor.copy(alpha=0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                pinnedPackages.take(4).forEach { pkg ->
                    var appIcon by remember { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(pkg) {
                        try {
                            val drawable = context.packageManager.getApplicationIcon(pkg)
                            val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                            appIcon = bmp
                        } catch (e: Exception) { }
                    }
                    if (appIcon != null) {
                        Image(bitmap = appIcon!!.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).bounceClick(haptic) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        })
                    } else {
                        Box(modifier = Modifier.size(40.dp).background(textColor.copy(alpha=0.1f), RoundedCornerShape(12.dp)))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun QsButton(iconRes: Int, label: String, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback, textColor: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(haptic) { onClick() }) {
        Box(modifier = Modifier.size(56.dp).background(textColor.copy(alpha=0.15f), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), contentDescription = label, tint = textColor, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    fun start() {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun pause() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }
    fun resume() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
    fun destroy() { lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY) }
}

@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    enum class IslandState { HIDDEN, TYPE_1_MINI, TYPE_2_MID, TYPE_3_MAX, TYPE_SPLIT }

    var camOffsetX = mutableStateOf(0)
    var camOffsetY = mutableStateOf(48)
    var ringOffsetY = mutableStateOf(48)
    var camWidth = mutableStateOf(24)
    var camHeight = mutableStateOf(24)
    var pillScaleX = mutableStateOf(1f)
    var pillScaleY = mutableStateOf(1f)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val isScreenOn = mutableStateOf(true)
    val isLandscape = mutableStateOf(context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

    private val musicState = mutableStateOf<MusicData?>(null)
    private val liveActivityState = mutableStateOf<LiveActivityData?>(null)
    private val secondaryActivityState = mutableStateOf<LiveActivityData?>(null)

    data class MusicData(val title: String, val artist: String, val art: Bitmap?, val isPlaying: Boolean, val progress: Float, val duration: Long, val currentPosition: Long, val packageName: String = "", val appIcon: Bitmap? = null, val dominantColor: Color = Color.Cyan, val repeatMode: Int = 0, val shuffleMode: Int = 0)
    data class LiveActivityData(val title: String, val data: String, val progress: Float?, val color: Int, val type: ActivityType)

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onOutputSwitcherClick: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onShuffleClick: (() -> Unit)? = null
    var onLikeClick: (() -> Unit)? = null
    var onLoopClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()
    private lateinit var recomposer: androidx.compose.runtime.Recomposer

    private val configReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                "com.example.dynamicisland.UPDATE_CONFIG" -> {
                    camOffsetX.value = intent.getIntExtra("offsetX", 0)
                    camOffsetY.value = intent.getIntExtra("offsetY", 48)
                    ringOffsetY.value = intent.getIntExtra("ringOffsetY", 48)
                    camWidth.value = intent.getIntExtra("camWidth", 24)
                    camHeight.value = intent.getIntExtra("camHeight", 24)
                    pillScaleX.value = intent.getFloatExtra("pillScaleX", 1f)
                    pillScaleY.value = intent.getFloatExtra("pillScaleY", 1f)
                }
                "com.example.dynamicisland.SHOW_RING_PREVIEW" -> {
                    IslandController.forceHide()
                    updateMusicInfo("Preview", "Ring", null, "", null, Color.Cyan)
                    updateMusicProgress(5000L, 10000L)
                }
                "com.example.dynamicisland.TEST_RING" -> {
                    IslandController.postActivity(LiveActivityModel(id = "test_ring", type = ActivityType.DOWNLOAD, title = "Test Ring", dataText = "50%", progress = 0.5f, accentColor = android.graphics.Color.CYAN, isTransient = true))
                }
                "com.example.dynamicisland.TOGGLE_PREVIEW" -> {
                    IslandController.postActivity(LiveActivityModel(id = "preview", type = ActivityType.GENERAL, title = "Preview Mode", dataText = "Adjusting...", accentColor = android.graphics.Color.WHITE, isTransient = true))
                }
            }
        }
    }

    init {
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.dynamicisland.UPDATE_CONFIG")
            addAction("com.example.dynamicisland.TEST_RING")
            addAction("com.example.dynamicisland.TOGGLE_PREVIEW")
            addAction("com.example.dynamicisland.SHOW_RING_PREVIEW")
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
                    if (isScreenOn.value && !isLandscape.value) IslandUI(islandState.value) else IslandUI(IslandState.HIDDEN)
                }
            }
        }

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.compositionContext = recomposer
        runRecomposeScope.launch { recomposer.runRecomposeAndApplyChanges() }

        addView(composeView)
        lifecycleOwner.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        windowManager = null
        IslandController.forceHide()
        recomposer.cancel()
        lifecycleOwner.destroy()
        BatteryPlugin.stop(context)
        try { context.unregisterReceiver(configReceiver) } catch (e: Exception) { }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val targetWidth = when (state) {
            IslandState.HIDDEN -> camWidth.value.dp
            IslandState.TYPE_1_MINI -> 180.dp * pillScaleX.value
            IslandState.TYPE_2_MID -> 320.dp * pillScaleX.value
            IslandState.TYPE_3_MAX -> 360.dp * pillScaleX.value
            IslandState.TYPE_SPLIT -> 340.dp * pillScaleX.value
        }
        val targetHeight = when (state) {
            IslandState.HIDDEN -> camHeight.value.dp
            IslandState.TYPE_1_MINI -> 36.dp * pillScaleY.value
            IslandState.TYPE_2_MID -> 80.dp * pillScaleY.value
            IslandState.TYPE_3_MAX -> 200.dp * pillScaleY.value
            IslandState.TYPE_SPLIT -> 36.dp * pillScaleY.value
        }

        val physicsSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 400f)
        val width by animateDpAsState(targetValue = targetWidth, animationSpec = physicsSpec, label = "width")
        val height by animateDpAsState(targetValue = targetHeight, animationSpec = physicsSpec, label = "height")
        val cornerRadius by animateDpAsState(targetValue = when (state) { IslandState.TYPE_2_MID, IslandState.TYPE_3_MAX -> 28.dp else -> 50.dp }, animationSpec = physicsSpec, label = "radius")

        val gestureDensity = androidx.compose.ui.platform.LocalDensity.current.density
        val swipeThresholdY = 50f * gestureDensity
        val swipeThresholdX = 100f * gestureDensity

        LaunchedEffect(width, height) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density
            val padW = if (state == IslandState.HIDDEN) 16 else 120
            val padH = if (state == IslandState.HIDDEN) 16 else 150

            wp.width = (width.value * density).toInt() + (padW * density).toInt()
            wp.height = (height.value * density).toInt() + (padH * density).toInt()
            wp.x = camOffsetX.value
            wp.y = camOffsetY.value
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        var musicBackgroundColor by remember { mutableStateOf(Color.Black.copy(alpha = 0.75f)) }
        LaunchedEffect(musicState.value?.art) {
            val art = musicState.value?.art
            if (art != null && !art.isRecycled) {
                Palette.from(art).generate { palette ->
                    val dominant = palette?.getDominantColor(android.graphics.Color.BLACK) ?: android.graphics.Color.BLACK
                    musicBackgroundColor = Color(dominant).copy(alpha = 0.85f)
                }
            } else { musicBackgroundColor = Color.Black.copy(alpha = 0.75f) }
        }

        val backgroundColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_SPLIT) Color.Transparent else if (musicState.value?.isPlaying == true) musicBackgroundColor else Color.Black.copy(alpha = 0.75f)
        val borderColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_SPLIT) Color.Transparent else Color.White.copy(alpha = 0.15f)
        val contentColor = Color.White

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val activity = liveActivityState.value
            
            // WAVY HIDDEN RING
            if (state == IslandState.HIDDEN && musicState.value != null && musicState.value!!.progress > 0f) {
                Box(modifier = Modifier.offset(y = (ringOffsetY.value - camOffsetY.value).dp)) {
                    WavyCircularRing(
                        progress = musicState.value!!.progress,
                        isPlaying = musicState.value!!.isPlaying,
                        modifier = Modifier.size(camWidth.value.dp + 16.dp, camHeight.value.dp + 16.dp),
                        color = musicState.value!!.dominantColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor)
                    .border(if (state == IslandState.HIDDEN) 0.dp else 1.dp, borderColor, RoundedCornerShape(cornerRadius))
                    .pointerInput(Unit) {
                        var totalDx = 0f; var totalDy = 0f
                        detectDragGestures(
                            onDragStart = { totalDx = 0f; totalDy = 0f },
                            onDragEnd = {
                                if (totalDy > swipeThresholdY) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onSwipeDown?.invoke() }
                                else if (kotlin.math.abs(totalDx) > swipeThresholdX) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (totalDx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                                }
                            },
                            onDrag = { change, dragAmount -> change.consume(); totalDx += dragAmount.x; totalDy += dragAmount.y }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onSingleTap?.invoke() }, 
                            onDoubleTap = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onDoubleTap?.invoke() },
                            onLongPress = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onLongPress?.invoke() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state != IslandState.HIDDEN) {
                    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                        AnimatedContent(targetState = Pair(state, if (activity != null) "live" else "music"), transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "content_morph") { (currentState, layoutType) ->
                            if (layoutType == "live" && activity != null) {
                                if (activity.type == ActivityType.DASHBOARD && currentState == IslandState.TYPE_3_MAX) {
                                    DashboardMax(contentColor)
                                } else {
                                    when (currentState) {
                                        IslandState.TYPE_1_MINI -> UniversalMini(contentColor, activity)
                                        IslandState.TYPE_2_MID -> UniversalMid(contentColor, activity)
                                        IslandState.TYPE_3_MAX -> UniversalMax(contentColor, activity)
                                        IslandState.TYPE_SPLIT -> SplitPill(contentColor, activity, secondaryActivityState.value)
                                        else -> {}
                                    }
                                }
                            } else {
                                when (currentState) {
                                    IslandState.TYPE_1_MINI -> MusicMini(contentColor)
                                    IslandState.TYPE_2_MID -> MusicMid(contentColor)
                                    IslandState.TYPE_3_MAX -> MusicMax(contentColor)
                                    IslandState.TYPE_SPLIT -> SplitPill(contentColor, activity ?: LiveActivityData("Media", "Playing", null, android.graphics.Color.MAGENTA, ActivityType.MEDIA), secondaryActivityState.value)
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM GRABBER `___`
            if (state != IslandState.HIDDEN && state != IslandState.TYPE_SPLIT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); onCloseClick?.invoke() },
                                onDrag = { change, _ -> change.consume() }
                            )
                        }
                        .bounceClick(haptic) { onCloseClick?.invoke() }
                )
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun SplitPill(textColor: Color, primary: LiveActivityData, secondary: LiveActivityData?) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).height(36.dp).background(Color.Black.copy(alpha=0.75f), RoundedCornerShape(50)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                val primaryIconAlpha = if (primary.type == ActivityType.CHARGING) alphaPulse else 1f
                Icon(painterResource(getIconForType(primary.type)), contentDescription = null, tint = Color(primary.color), modifier = Modifier.size(16.dp).alpha(primaryIconAlpha))
                Spacer(Modifier.width(8.dp))
                Text(primary.title, color = textColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(camWidth.value.dp + 16.dp))
            if (secondary != null) {
                Box(modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha=0.75f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    val secondaryIconAlpha = if (secondary.type == ActivityType.CHARGING) alphaPulse else 1f
                    Icon(painterResource(getIconForType(secondary.type)), contentDescription = null, tint = Color(secondary.color), modifier = Modifier.size(16.dp).alpha(secondaryIconAlpha))
                }
            } else Spacer(modifier = Modifier.size(36.dp))
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMini(textColor: Color, activity: LiveActivityData) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f
            Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(16.dp).alpha(iconAlpha))
            Spacer(Modifier.width(8.dp))
            Text(text = "${activity.title} • ${activity.data}", color = textColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityData) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaPulse by infiniteTransition.animateFloat(initialValue = 0.4f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alphaPulse")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (activity.progress != null) {
                    androidx.compose.material3.CircularProgressIndicator(progress = { activity.progress }, color = Color(activity.color), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                }
                val iconAlpha = if (activity.type == ActivityType.CHARGING) alphaPulse else 1f
                Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(24.dp).alpha(iconAlpha))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = activity.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = activity.data, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMax(textColor: Color, activity: LiveActivityData) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = activity.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Text(text = activity.data, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }

    private fun getIconForType(type: ActivityType): Int {
        return when(type) {
            ActivityType.CALL -> R.drawable.ic_call_vector
            ActivityType.NAVIGATION -> R.drawable.ic_map_vector
            ActivityType.TIMER -> R.drawable.ic_timer_vector
            ActivityType.MESSAGE -> R.drawable.ic_mail_vector
            ActivityType.ALARM -> R.drawable.ic_alarm_vector
            ActivityType.CHARGING -> R.drawable.ic_battery_charging_vector
            ActivityType.BATTERY_LOW -> R.drawable.ic_battery_alert_vector
            ActivityType.BATTERY_FULL -> R.drawable.ic_battery_full_vector
            ActivityType.BLUETOOTH -> R.drawable.ic_bluetooth_vector
            ActivityType.WIFI -> R.drawable.ic_wifi_vector
            else -> R.drawable.ic_sync_vector
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(textColor: Color) {
        val music = musicState.value ?: return
        Box(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(end = 16.dp)) {
                if (music.art != null && !music.art.isRecycled) {
                    Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.offset(x = (-6).dp).size(36.dp).clip(RoundedCornerShape(8.dp)))
                } else Spacer(Modifier.width(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = "${music.title} • ${music.artist}", color = textColor, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f).basicMarquee())
            }
            androidx.compose.material3.LinearProgressIndicator(progress = { music.progress }, modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter), color = textColor, trackColor = Color.Transparent)
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMid(textColor: Color) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val music = musicState.value ?: return
        Box(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(end = 20.dp)) {
                if (music.art != null && !music.art.isRecycled) {
                    Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.offset(x = (-10).dp).size(48.dp).clip(RoundedCornerShape(12.dp)))
                } else Spacer(Modifier.width(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                     Text(text = music.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                     Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                }
                val playIcon = if (music.isPlaying) R.drawable.ic_pause_vector else R.drawable.ic_play_vector
                Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(32.dp).bounceClick(haptic) { onPlayPauseClick?.invoke() })
            }
            androidx.compose.material3.LinearProgressIndicator(progress = { music.progress }, modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.BottomCenter), color = textColor, trackColor = Color.Transparent)
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMax(textColor: Color) {
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val music = musicState.value ?: return

        Box(modifier Box(modifier = Modifier.fillMaxSize()) {
            if (music.art != null && !music.art.isRecycled) {
                Image(bitmap = music.art.asImageBitmap(), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.5f)
            }

            Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (music.appIcon != null && !music.appIcon.isRecycled) {
                        Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Icon", modifier = Modifier.size(24.dp))
                    } else {
                        Icon(painterResource(R.drawable.ic_play_vector), contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                    }

                    Row(modifier = Modifier.background(textColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).bounceClick(haptic) { onOutputSwitcherClick?.invoke() }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_phone_vector), contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("This phone", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(text = music.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                        Spacer(Modifier.height(4.dp))
                        Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                    }

                    val playIcon = if (music.isPlaying) R.drawable.ic_pause_vector else R.drawable.ic_play_vector
                    Box(modifier = Modifier.size(72.dp).background(textColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp)).bounceClick(haptic) { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(36.dp))
                    }
                }

                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isDragged by interactionSource.collectIsDraggedAsState()

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isDragged) Text(formatTime(music.currentPosition), color = textColor, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    else Icon(painterResource(R.drawable.ic_prev_vector), contentDescription = "Prev", tint = textColor, modifier = Modifier.size(28.dp).bounceClick(haptic) { onPrevClick?.invoke() })

                    Spacer(Modifier.width(12.dp))
                    val safeProgress = if (music.progress.isNaN() || music.progress.isInfinite()) 0f else music.progress.coerceIn(0f, 1f)
                    
                    WavySlider(progress = safeProgress, isPlaying = music.isPlaying, onSeekFinished = { newProgress -> onSeekTo?.invoke((newProgress * music.duration).toLong()) }, modifier = Modifier.weight(1f), activeColor = textColor, inactiveColor = textColor.copy(alpha = 0.3f), interactionSource = interactionSource)

                    Spacer(Modifier.width(12.dp))
                    if (isDragged) Text(formatTime(music.duration), color = textColor, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    else Icon(painterResource(R.drawable.ic_next_vector), contentDescription = "Next", tint = textColor, modifier = Modifier.size(28.dp).bounceClick(haptic) { onNextClick?.invoke() })

                    Spacer(Modifier.width(20.dp))
                    Icon(painterResource(R.drawable.ic_heart_vector), contentDescription = "Like", tint = textColor, modifier = Modifier.size(24.dp).bounceClick(haptic) { onLikeClick?.invoke() })
                    Spacer(Modifier.width(16.dp))
                    val shuffleTint = if (music.shuffleMode == 1) Color.Cyan else textColor.copy(alpha = 0.5f)
                    Icon(painterResource(R.drawable.ic_shuffle_vector), contentDescription = "Shuffle", tint = shuffleTint, modifier = Modifier.size(24.dp).bounceClick(haptic) { onShuffleClick?.invoke() })
                    Spacer(Modifier.width(16.dp))
                    val loopTint = when (music.repeatMode) { 2 -> Color.Cyan 1 -> Color.Yellow else -> textColor.copy(alpha = 0.5f) }
                    Icon(painterResource(R.drawable.ic_sync_vector), contentDescription = "Loop", tint = loopTint, modifier = Modifier.size(24.dp).bounceClick(haptic) { onLoopClick?.invoke() })
                }
            }
        }
    }

    private fun formatTime(ms: Long): String = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)))

    fun updateScreenState(isOn: Boolean) {
        if (isScreenOn.value != isOn) {
            isScreenOn.value = isOn
            if (isOn) lifecycleOwner.resume() else lifecycleOwner.pause()
        }
    }

    fun setState(newState: IslandState) {
        if (!isAttachedToWindow && newState != IslandState.HIDDEN) return
        islandState.value = newState
        val wp = windowParams ?: return
        val wm = windowManager ?: return
        wp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        try { wm.updateViewLayout(this, wp) } catch (e: Exception) { }
    }

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int, type: ActivityType) { liveActivityState.value = LiveActivityData(title, data, progress, color, type) }

    fun updateActivities(primary: LiveActivityModel, secondary: LiveActivityModel) {
        liveActivityState.value = LiveActivityData(primary.title, primary.dataText, primary.progress, primary.accentColor, primary.type)
        secondaryActivityState.value = LiveActivityData(secondary.title, secondary.dataText, secondary.progress, secondary.accentColor, secondary.type)
    }

    fun clearLiveActivityUI() { liveActivityState.value = null }
    fun clearSecondaryActivityUI() { secondaryActivityState.value = null }
    fun clearMusicState() { musicState.value = null }

    fun updateMediaModes(repeatMode: Int, shuffleMode: Int) {
        musicState.value = musicState.value?.copy(repeatMode = repeatMode, shuffleMode = shuffleMode)
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?, packageName: String = "", appIcon: Bitmap? = null, dominantColor: Color = Color.Cyan) {
        val current = musicState.value
        musicState.value = current?.copy(title = title ?: "", artist = artist ?: "", art = art, packageName = packageName, appIcon = appIcon, dominantColor = dominantColor) ?: MusicData(title ?: "", artist ?: "", art, false, 0f, 0L, 0L, packageName, appIcon, dominantColor)
    }
    fun updatePlayPauseState(isPlaying: Boolean) { musicState.value = musicState.value?.copy(isPlaying = isPlaying) }
    fun updateMusicProgress(positionMs: Long, durationMs: Long) {
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
        musicState.value = musicState.value?.copy(progress = progress, duration = durationMs, currentPosition = positionMs)
    }
}
