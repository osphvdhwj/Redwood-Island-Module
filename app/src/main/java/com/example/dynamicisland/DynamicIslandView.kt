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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.Locale
import kotlin.math.abs

// ... [Keep OverlayLifecycleOwner exact same as before] ...
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
}

@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    enum class IslandState {
        HIDDEN,      // Transparent/invisible
        TYPE_1_MINI, // Compact pill
        TYPE_2_MID,  // Expanded notification
        TYPE_3_MAX   // Full control center
    }

    var camOffsetX = mutableStateOf(0)
    var camOffsetY = mutableStateOf(48)
    var camWidth = mutableStateOf(24)
    var camHeight = mutableStateOf(24)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val isScreenOn = mutableStateOf(true)
    val isLandscape = mutableStateOf(context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)

    private val musicState = mutableStateOf<MusicData?>(null)
    private val liveActivityState = mutableStateOf<LiveActivityData?>(null)

    data class MusicData(val title: String, val artist: String, val art: Bitmap?, val isPlaying: Boolean, val progress: Float, val duration: Long, val currentPosition: Long)
    data class LiveActivityData(val title: String, val data: String, val progress: Float?, val color: Int, val type: ActivityType)

    // Callbacks
    var onSingleTap: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    init {
        // Listen for live config updates from your app
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == "com.example.dynamicisland.UPDATE_CONFIG") {
                    camOffsetX.value = intent.getIntExtra("offsetX", 0)
                    camOffsetY.value = intent.getIntExtra("offsetY", 48)
                    camWidth.value = intent.getIntExtra("camWidth", 24)
                    camHeight.value = intent.getIntExtra("camHeight", 24)

                    // Force a layout update
                    val wp = windowParams ?: return
                    wp.x = camOffsetX.value
                    wp.y = camOffsetY.value
                    try { windowManager?.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
                } else if (intent.action == "com.example.dynamicisland.TEST_RING") {
                    // Trigger a fake download to test the ring
                    IslandController.postActivity(LiveActivityModel(
                        id = "test_ring", type = ActivityType.DOWNLOAD,
                        title = "Test Ring", dataText = "50%", progress = 0.5f,
                        accentColor = android.graphics.Color.CYAN, isTransient = true
                    ))
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.dynamicisland.UPDATE_CONFIG")
            addAction("com.example.dynamicisland.TEST_RING")
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

        val composeView = ComposeView(context).apply {
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    // Orientation Check: Hide completely if in Landscape
                    if (isScreenOn.value && !isLandscape.value) {
                        IslandUI(islandState.value)
                    } else {
                        IslandUI(IslandState.HIDDEN)
                    }
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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let {
            isLandscape.value = it.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Added standard cleanup per user feedback in earlier steps
        windowManager = null
        IslandController.forceHide()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        // Physical dimensions matching Apple's standard specs
        val targetWidth = when (state) {
            IslandState.HIDDEN -> camWidth.value.dp // Shrink exactly to camera width
            IslandState.TYPE_1_MINI -> 180.dp
            IslandState.TYPE_2_MID -> 320.dp
            IslandState.TYPE_3_MAX -> 360.dp
        }
        val targetHeight = when (state) {
            IslandState.HIDDEN -> camHeight.value.dp // Shrink exactly to camera height
            IslandState.TYPE_1_MINI -> 36.dp
            IslandState.TYPE_2_MID -> 80.dp // Sleeker mid state
            IslandState.TYPE_3_MAX -> 200.dp
        }

        val physicsSpec = spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)

        val width by animateDpAsState(targetValue = targetWidth, animationSpec = physicsSpec, label = "width")
        val height by animateDpAsState(targetValue = targetHeight, animationSpec = physicsSpec, label = "height")

        LaunchedEffect(width, height) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect

            // FIX 4: MASSIVE Padding increase.
            // The MAX state requires heavy vertical padding so touches aren't cut off by the Window bounds.
            val density = context.resources.displayMetrics.density
            wp.width = (width.value * density).toInt() + (120 * density).toInt()
            wp.height = (height.value * density).toInt() + (150 * density).toInt()

            wp.x = camOffsetX.value
            wp.y = camOffsetY.value

            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        // OLED Black Design for perfect camera blending
        val backgroundColor = if (state == IslandState.HIDDEN) Color.Transparent else Color.Black
        val borderColor = if (state == IslandState.HIDDEN) Color.Transparent else Color.White.copy(alpha = 0.15f)
        val contentColor = Color.White // White text on OLED Black

        // The Master Container
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            // 1. THE GLOWING PROGRESS RING
            val activity = liveActivityState.value
            if (state == IslandState.HIDDEN && activity != null && activity.progress != null) {
                // UI CRASH FIX: Absolute safety check against Compose NaN crashes
                val safeProgress = if (activity.progress.isNaN() || activity.progress.isInfinite()) 0f else activity.progress.coerceIn(0f, 1f)

                androidx.compose.material3.CircularProgressIndicator(
                    progress = { safeProgress },
                    modifier = Modifier.size(camWidth.value.dp + 6.dp, camHeight.value.dp + 6.dp),
                    color = Color(activity.color),
                    trackColor = Color(activity.color).copy(alpha = 0.2f),
                    strokeWidth = 3.dp
                )
            }

            // The main black pill
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(percent = 50)) // Perfect pill shape
                    .background(backgroundColor)
                    .border(if (state == IslandState.HIDDEN) 0.dp else 1.dp, borderColor, RoundedCornerShape(percent = 50))
                    .pointerInput(Unit) {
                        var dy = 0f
                        detectDragGestures(
                            onDragEnd = { if (dy < -30) onSwipeUp?.invoke() },
                            onDrag = { change, dragAmount -> change.consume(); dy += dragAmount.y }
                        )
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { onSingleTap?.invoke() }) },
                contentAlignment = Alignment.Center
            ) {
                if (state != IslandState.HIDDEN) {
                    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                        val activity = liveActivityState.value
                        val isLive = activity != null

                        // Smooth morphing between states and apps
                        AnimatedContent(
                            targetState = Pair(state, if (isLive) "live" else "music"),
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            label = "content_morph"
                        ) { (currentState, layoutType) ->
                            if (layoutType == "live" && activity != null) {
                                when (currentState) {
                                    IslandState.TYPE_1_MINI -> UniversalMini(contentColor, activity)
                                    IslandState.TYPE_2_MID -> UniversalMid(contentColor, activity)
                                    IslandState.TYPE_3_MAX -> UniversalMax(contentColor, activity)
                                    else -> {}
                                }
                            } else {
                                when (currentState) {
                                    IslandState.TYPE_1_MINI -> MusicMini(contentColor)
                                    IslandState.TYPE_2_MID -> MusicMid(contentColor)
                                    IslandState.TYPE_3_MAX -> MusicMax(contentColor)
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Universal App Composables ---
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMini(textColor: Color, activity: LiveActivityData) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            val iconRes = getIconForType(activity.type)
            Icon(painterResource(iconRes), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "${activity.title} • ${activity.data}", color = textColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityData) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            // Left Icon/Progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (activity.progress != null) {
                    androidx.compose.material3.CircularProgressIndicator(progress = { activity.progress }, color = Color(activity.color), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                }
                Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = activity.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 Text(text = activity.data, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Right Action Button (e.g. End Call)
            if (activity.type == ActivityType.CALL || activity.type == ActivityType.ALARM) {
                Box(modifier = Modifier.size(40.dp).background(Color.Red.copy(alpha=0.2f), RoundedCornerShape(20.dp)).clickable { onCloseClick?.invoke() }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription="Close", tint=Color.Red)
                }
            }
        }
    }

    @Composable
    fun UniversalMax(textColor: Color, activity: LiveActivityData) {
        // For MAX state on general apps, we center a larger version of MID
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
            ActivityType.CALL -> android.R.drawable.ic_menu_call
            ActivityType.NAVIGATION -> android.R.drawable.ic_menu_mapmode
            ActivityType.TIMER -> android.R.drawable.ic_menu_recent_history
            ActivityType.MESSAGE -> android.R.drawable.ic_dialog_email
            ActivityType.ALARM -> android.R.drawable.ic_lock_idle_alarm
            ActivityType.CHARGING -> android.R.drawable.ic_lock_idle_charging
            else -> android.R.drawable.stat_sys_download
        }
    }

    // --- Music Composables ---
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(textColor: Color) {
        val music = musicState.value ?: return
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(text = "${music.title} • ${music.artist}", color = textColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @Composable
    fun MusicMid(textColor: Color) {
        val music = musicState.value ?: return
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (music.art != null) {
                Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(32.dp).clickable { onPlayPauseClick?.invoke() })
        }
    }

    @Composable
    fun MusicMax(textColor: Color) {
        val music = musicState.value ?: return
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (music.art != null) {
                    Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                } else {
                    Box(Modifier.size(64.dp).background(Color.DarkGray, RoundedCornerShape(12.dp)))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                WavySlider(
                    value = music.progress,
                    onValueChange = { newProgress -> onSeekTo?.invoke((newProgress * music.duration).toLong()) },
                    waveLength = 20.dp, waveHeight = 4.dp,
                    waveVelocity = if (music.isPlaying) 15.dp to WaveDirection.HEAD else 0.dp to WaveDirection.HEAD,
                    waveThickness = 4.dp, trackThickness = 4.dp, modifier = Modifier.fillMaxWidth().height(24.dp)
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(music.currentPosition), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(formatTime(music.duration), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    Icon(painterResource(android.R.drawable.ic_media_previous), contentDescription = "Prev", tint = textColor, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                    val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    Box(modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp)).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(32.dp))
                    }
                    Icon(painterResource(android.R.drawable.ic_media_next), contentDescription = "Next", tint = textColor, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
                }
            }
        }
    }

    private fun formatTime(ms: Long): String = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)))

    // --- Data Updates ---
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

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int, type: ActivityType) {
        liveActivityState.value = LiveActivityData(title, data, progress, color, type)
    }

    fun clearLiveActivityUI() { liveActivityState.value = null }
    fun clearMusicState() { musicState.value = null }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        val current = musicState.value
        musicState.value = current?.copy(title = title ?: "", artist = artist ?: "", art = art) ?: MusicData(title ?: "", artist ?: "", art, false, 0f, 0L, 0L)
    }
    fun updatePlayPauseState(isPlaying: Boolean) { musicState.value = musicState.value?.copy(isPlaying = isPlaying) }
    fun updateMusicProgress(positionMs: Long, durationMs: Long) {
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
        musicState.value = musicState.value?.copy(progress = progress, duration = durationMs, currentPosition = positionMs)
    }
}
