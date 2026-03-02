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
import kotlin.math.abs

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

    enum class IslandState { HIDDEN, TYPE_1_MINI, TYPE_2_MID, TYPE_3_MAX, TYPE_SPLIT }

    var camOffsetX = mutableStateOf(0)
    var camOffsetY = mutableStateOf(48)
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

    data class MusicData(val title: String, val artist: String, val art: Bitmap?, val isPlaying: Boolean, val progress: Float, val duration: Long, val currentPosition: Long)
    data class LiveActivityData(val title: String, val data: String, val progress: Float?, val color: Int, val type: ActivityType)

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onLikeClick: (() -> Unit)? = null
    var onLoopClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()
    private lateinit var recomposer: androidx.compose.runtime.Recomposer

    init {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == "com.example.dynamicisland.UPDATE_CONFIG") {
                    camOffsetX.value = intent.getIntExtra("offsetX", 0)
                    camOffsetY.value = intent.getIntExtra("offsetY", 48)
                    camWidth.value = intent.getIntExtra("camWidth", 24)
                    camHeight.value = intent.getIntExtra("camHeight", 24)
                    pillScaleX.value = intent.getFloatExtra("pillScaleX", 1f)
                    pillScaleY.value = intent.getFloatExtra("pillScaleY", 1f)
                } else if (intent.action == "com.example.dynamicisland.TEST_RING") {
                    IslandController.postActivity(LiveActivityModel(
                        id = "test_ring", type = ActivityType.DOWNLOAD, title = "Test Ring",
                        dataText = "50%", progress = 0.5f, accentColor = android.graphics.Color.CYAN, isTransient = true
                    ))
                } else if (intent.action == "com.example.dynamicisland.TOGGLE_PREVIEW") {
                    IslandController.postActivity(LiveActivityModel(
                        id = "preview", type = ActivityType.GENERAL, title = "Preview Mode",
                        dataText = "Adjusting...", accentColor = android.graphics.Color.WHITE, isTransient = true
                    ))
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("com.example.dynamicisland.UPDATE_CONFIG")
            addAction("com.example.dynamicisland.TEST_RING")
            addAction("com.example.dynamicisland.TOGGLE_PREVIEW")
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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let { isLandscape.value = it.orientation == Configuration.ORIENTATION_LANDSCAPE }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        windowManager = null
        IslandController.forceHide()
        recomposer.cancel()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
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

        // ICE CUBE MORPHING
        val cornerRadius by animateDpAsState(
            targetValue = when (state) {
                IslandState.TYPE_2_MID, IslandState.TYPE_3_MAX -> 28.dp // Cube-like corners
                else -> 50.dp // Perfect pill for MINI and SPLIT
            },
            animationSpec = physicsSpec, label = "radius"
        )

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
            } else {
                musicBackgroundColor = Color.Black.copy(alpha = 0.75f)
            }
        }

        val backgroundColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_SPLIT) Color.Transparent else if (musicState.value?.isPlaying == true) musicBackgroundColor else Color.Black.copy(alpha = 0.75f)
        val borderColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_SPLIT) Color.Transparent else Color.White.copy(alpha = 0.15f)
        val contentColor = Color.White

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val activity = liveActivityState.value
            if (state == IslandState.HIDDEN && activity?.progress != null) {
                val safeProgress = if (activity.progress.isNaN() || activity.progress.isInfinite()) 0f else activity.progress.coerceIn(0f, 1f)
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { safeProgress }, modifier = Modifier.size(camWidth.value.dp + 6.dp, camHeight.value.dp + 6.dp),
                    color = Color(activity.color), trackColor = Color(activity.color).copy(alpha = 0.2f), strokeWidth = 3.dp
                )
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
                                if (totalDy < -50) onSwipeUp?.invoke()
                                else if (totalDy > 50) onSwipeDown?.invoke()
                                else if (kotlin.math.abs(totalDx) > 100) { if (totalDx > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke() }
                            },
                            onDrag = { change, dragAmount -> change.consume(); totalDx += dragAmount.x; totalDy += dragAmount.y }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onSingleTap?.invoke() }, onDoubleTap = { onDoubleTap?.invoke() })
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state != IslandState.HIDDEN) {
                    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                        AnimatedContent(targetState = Pair(state, if (activity != null) "live" else "music"), transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "content_morph") { (currentState, layoutType) ->
                            if (layoutType == "live" && activity != null) {
                                when (currentState) {
                                    IslandState.TYPE_1_MINI -> UniversalMini(contentColor, activity)
                                    IslandState.TYPE_2_MID -> UniversalMid(contentColor, activity)
                                    IslandState.TYPE_3_MAX -> UniversalMax(contentColor, activity)
                                    IslandState.TYPE_SPLIT -> SplitPill(contentColor, activity, secondaryActivityState.value)
                                    else -> {}
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
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    private fun SplitPill(textColor: Color, primary: LiveActivityData, secondary: LiveActivityData?) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f).height(36.dp).background(Color.Black.copy(alpha=0.75f), RoundedCornerShape(50)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (primary.type == ActivityType.MEDIA && musicState.value?.art != null) {
                    val art = musicState.value?.art
                    if (art != null && !art.isRecycled) {
                        Image(bitmap = art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)))
                    }
                } else {
                    Icon(painterResource(getIconForType(primary.type)), contentDescription = null, tint = Color(primary.color), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(primary.title, color = textColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(camWidth.value.dp + 16.dp))
            if (secondary != null) {
                Box(modifier = Modifier.size(36.dp).background(Color.Black.copy(alpha=0.75f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(getIconForType(secondary.type)), contentDescription = null, tint = Color(secondary.color), modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMini(textColor: Color, activity: LiveActivityData) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = "${activity.title} • ${activity.data}", color = textColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun UniversalMid(textColor: Color, activity: LiveActivityData) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                if (activity.progress != null) {
                    androidx.compose.material3.CircularProgressIndicator(progress = { activity.progress }, color = Color(activity.color), trackColor = textColor.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                }
                Icon(painterResource(getIconForType(activity.type)), contentDescription = null, tint = Color(activity.color), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = activity.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = activity.data, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
            if (activity.type == ActivityType.CALL || activity.type == ActivityType.ALARM) {
                Box(modifier = Modifier.size(40.dp).background(Color.Red.copy(alpha=0.2f), RoundedCornerShape(20.dp)).clickable { onCloseClick?.invoke() }, contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_close_vector), contentDescription="Close", tint=Color.Red)
                }
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

    // MANDATORY FIX: Local Static Vectors to avoid System UI Crash
    private fun getIconForType(type: ActivityType): Int {
        return when(type) {
            ActivityType.CALL -> R.drawable.ic_call_vector
            ActivityType.NAVIGATION -> R.drawable.ic_map_vector
            ActivityType.TIMER -> R.drawable.ic_timer_vector
            ActivityType.MESSAGE -> R.drawable.ic_mail_vector
            ActivityType.ALARM -> R.drawable.ic_alarm_vector
            ActivityType.CHARGING -> R.drawable.ic_charging_vector
            else -> R.drawable.ic_sync_vector
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(textColor: Color) {
        val music = musicState.value ?: return
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(text = "${music.title} • ${music.artist}", color = textColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMid(textColor: Color) {
        val music = musicState.value ?: return
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            if (music.art != null && !music.art.isRecycled) {
                Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
            val playIcon = if (music.isPlaying) R.drawable.ic_pause_vector else R.drawable.ic_play_vector
            Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(32.dp).clickable { onPlayPauseClick?.invoke() })
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMax(textColor: Color) {
        val music = musicState.value ?: return

        // FULL BLEED BACKGROUND
        Box(modifier = Modifier.fillMaxSize()) {
            if (music.art != null && !music.art.isRecycled) {
                Image(bitmap = music.art.asImageBitmap(), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.3f)
            }

            Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (music.art != null && !music.art.isRecycled) {
                        Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = music.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                        Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 1, modifier = Modifier.basicMarquee())
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    // OUTPUT SWITCHER
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        // Note: If you have an ic_route_vector use that, else use a known safe vector to avoid AVD crash
                        Icon(painterResource(R.drawable.ic_sync_vector), contentDescription = null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("This Phone", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

                    val safeProgress = if (music.progress.isNaN() || music.progress.isInfinite()) 0f else music.progress.coerceIn(0f, 1f)
                    androidx.compose.material3.Slider(
                        value = safeProgress, onValueChange = { newProgress -> onSeekTo?.invoke((newProgress * music.duration).toLong()) },
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = textColor, activeTrackColor = textColor, inactiveTrackColor = textColor.copy(alpha = 0.3f))
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(music.currentPosition), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                        Text(formatTime(music.duration), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // ADVANCED MEDIA CONTROLS
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(painterResource(R.drawable.ic_sync_vector), contentDescription = "Loop", tint = textColor.copy(alpha=0.6f), modifier = Modifier.size(24.dp).clickable { onLoopClick?.invoke() })
                        Icon(painterResource(R.drawable.ic_prev_vector), contentDescription = "Prev", tint = textColor, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                        val playIcon = if (music.isPlaying) R.drawable.ic_pause_vector else R.drawable.ic_play_vector
                        Box(modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp)).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                            Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(36.dp))
                        }
                        Icon(painterResource(R.drawable.ic_next_vector), contentDescription = "Next", tint = textColor, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
                        Icon(painterResource(R.drawable.ic_alarm_vector), contentDescription = "Like", tint = textColor.copy(alpha=0.6f), modifier = Modifier.size(24.dp).clickable { onLikeClick?.invoke() })
                    }
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
