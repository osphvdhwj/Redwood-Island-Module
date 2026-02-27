package com.example.dynamicisland

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.palette.graphics.Palette
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

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
        HIDDEN,      // The default Camera Punch-Hole ⭕
        TYPE_1_MINI, // Playing Music Pill (Marquee)
        TYPE_2_MID,  // Timer / Battery Pill
        TYPE_3_MAX   // Expanded iOS Screenshot Music Player
    }

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val isScreenOn = mutableStateOf(true)
    val isLandscape = mutableStateOf(false)

    // Data States (Removed General Notifications)
    private val musicState = mutableStateOf<MusicData?>(null)
    private val liveActivityState = mutableStateOf<LiveActivityData?>(null)
    private val chargingState = mutableStateOf<ChargingData?>(null)

    data class MusicData(
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val isPlaying: Boolean,
        val progress: Float,
        val duration: Long,
        val dominantColor: Int = android.graphics.Color.DKGRAY
    )
    data class LiveActivityData(val title: String, val data: String, val progress: Float?, val color: Int)
    data class ChargingData(val level: Int, val isCharging: Boolean, val color: Int)

    // Gesture Callbacks
    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    // Media Actions
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onLoopClick: (() -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    init {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        val viewModelStoreOwner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        val composeView = ComposeView(context).apply {
            setContent {
                DynamicIslandTheme(context) {
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
            val landscape = it.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape.value != landscape) isLandscape.value = landscape
        }
    }

    fun updateScreenState(isOn: Boolean) {
        if (isScreenOn.value != isOn) {
            isScreenOn.value = isOn
            if (isOn) lifecycleOwner.resume() else lifecycleOwner.pause()
        }
    }

    fun clearMusicState() { musicState.value = null }

    @Composable
    fun DynamicIslandTheme(context: Context, content: @Composable () -> Unit) {
        val colorScheme = try { dynamicDarkColorScheme(context) } catch (e: Throwable) { darkColorScheme() }
        MaterialTheme(colorScheme = colorScheme, content = content)
    }

    @Composable
    fun IslandUI(state: IslandState) {
        val targetWidth = when (state) {
            IslandState.HIDDEN -> 26.dp // Default Camera Hole Size
            IslandState.TYPE_1_MINI -> 150.dp // Wider for scrolling text
            IslandState.TYPE_2_MID -> 300.dp
            IslandState.TYPE_3_MAX -> 360.dp // Expanded player
        }

        val targetHeight = when (state) {
            IslandState.HIDDEN -> 26.dp
            IslandState.TYPE_1_MINI -> 36.dp
            IslandState.TYPE_2_MID -> 80.dp
            IslandState.TYPE_3_MAX -> 200.dp // Expanded player height
        }

        val width by animateDpAsState(targetValue = targetWidth, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width")
        val height by animateDpAsState(targetValue = targetHeight, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "height")

        LaunchedEffect(width, height) {
            val wp = windowParams
            val wm = windowManager
            if (wp != null && wm != null) {
                val pxWidth = (width.value * context.resources.displayMetrics.density).toInt()
                val pxHeight = (height.value * context.resources.displayMetrics.density).toInt()
                wp.width = pxWidth + 50
                wp.height = pxHeight + 100
                try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
            }
        }

        // The background color is solid black for ⭕ Default, Mini, and Mid pills
        val backgroundColor = Color.Black

        Box(modifier = Modifier.padding(top = 40.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(42.dp))
                    .background(backgroundColor)
                    .pointerInput(Unit) {
                        var dragAccumulationX = 0f
                        detectDragGestures(
                            onDragStart = { dragAccumulationX = 0f },
                            onDragEnd = {
                                if (abs(dragAccumulationX) > 40) {
                                    if (dragAccumulationX < -40) onSwipeLeft?.invoke()
                                    else if (dragAccumulationX > 40) onSwipeRight?.invoke()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulationX += dragAmount.x
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onSingleTap?.invoke() },
                            onDoubleTap = { onDoubleTap?.invoke() },
                            onLongPress = { onLongPress?.invoke() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state != IslandState.HIDDEN) {
                    when (state) {
                        IslandState.TYPE_1_MINI -> MiniContent()
                        IslandState.TYPE_2_MID -> ExpandedContent()
                        IslandState.TYPE_3_MAX -> MusicPlayerScreenshotUI()
                        else -> {}
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MiniContent() {
        val music = musicState.value
        val charging = chargingState.value

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
        ) {
            if (charging != null && charging.isCharging) {
                Text(text = "${charging.level}%", style = MaterialTheme.typography.labelSmall, color = Color(charging.color))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(16.dp).background(Color(charging.color), RoundedCornerShape(4.dp)))
            } else if (music != null) {
                if (music.art != null) {
                    Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(24.dp).clip(CircleShape))
                } else {
                    Box(Modifier.size(24.dp).background(Color.DarkGray, CircleShape))
                }
                Spacer(Modifier.width(8.dp))
                // Marquee scrolling text for song title
                Text(
                    text = music.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
    }

    @Composable
    fun MusicPlayerScreenshotUI() {
        val music = musicState.value ?: return

        Box(modifier = Modifier.fillMaxSize()) {
            // Background Album Art (Darkened)
            if (music.art != null) {
                Image(
                    bitmap = music.art.asImageBitmap(),
                    contentDescription = "Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().drawWithContent {
                        drawContent()
                        drawRect(Color.Black.copy(alpha = 0.5f)) // Dimming effect
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2C)))
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: App Icon & "This phone"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    // Small Music App Icon placeholder
                    Box(modifier = Modifier.size(28.dp).background(Color(0xFFF2E6E6), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(android.R.drawable.ic_media_play), contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    }

                    // "This phone" Chip
                    Row(
                        modifier = Modifier.background(Color(0xFFF2E6E6), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(android.R.drawable.ic_menu_call), contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("This phone", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Middle Row: Titles and Play/Pause Button
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = music.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = music.artist, color = Color(0xFFD0D0D0), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    // Giant Play/Pause Button
                    val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFF2E6E6), RoundedCornerShape(20.dp))
                            .clickable { onPlayPauseClick?.invoke() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = Color.Black, modifier = Modifier.size(36.dp))
                    }
                }

                // Bottom Row: Controls & Scrubber
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(painterResource(android.R.drawable.ic_media_previous), contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onPrevClick?.invoke() })
                    Spacer(modifier = Modifier.width(16.dp))

                    // Scrubber Bar
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(music.progress).height(4.dp).background(Color.White, RoundedCornerShape(2.dp)))
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(painterResource(android.R.drawable.ic_media_next), contentDescription = "Next", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onNextClick?.invoke() })
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Close", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onCloseClick?.invoke() })
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(painterResource(android.R.drawable.ic_menu_rotate), contentDescription = "Loop", tint = Color.White, modifier = Modifier.size(28.dp).clickable { onLoopClick?.invoke() })
                }
            }
        }
    }

    @Composable
    fun ExpandedContent() {
        val charging = chargingState.value
        val liveAct = liveActivityState.value

        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (charging != null && charging.isCharging) {
                Text("Charging", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(text = "${charging.level}%", style = MaterialTheme.typography.headlineMedium, color = Color(charging.color))
            } else if (liveAct != null) {
                Text(liveAct.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(liveAct.data, style = MaterialTheme.typography.headlineMedium, color = Color(liveAct.color))
            }
        }
    }

    // --- Controller API ---
    fun setState(newState: IslandState) {
        islandState.value = newState
        val wp = windowParams ?: return
        val wm = windowManager ?: return

        // THE FIX: ALWAYS allow touches to pass through to the screen behind!
        wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        if (newState == IslandState.HIDDEN || newState == IslandState.TYPE_1_MINI) {
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            // Only need focus if we ever add typing back, but usually media players don't need focus.
            wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        }

        try { wm.updateViewLayout(this, wp) } catch (e: Exception) { }
    }

    fun showMini() = setState(IslandState.TYPE_1_MINI)
    fun expand() = setState(IslandState.TYPE_2_MID)
    fun showDashboard() = setState(IslandState.TYPE_3_MAX)
    fun hide() = setState(IslandState.HIDDEN)
    fun collapse() = showMini()

    val isExpanded: Boolean get() = islandState.value == IslandState.TYPE_2_MID || islandState.value == IslandState.TYPE_3_MAX

    // Data Updates
    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        var dominantColor = android.graphics.Color.DKGRAY
        if (art != null) {
            Palette.from(art).generate { palette ->
                dominantColor = palette?.getVibrantColor(palette.getDominantColor(android.graphics.Color.DKGRAY)) ?: android.graphics.Color.DKGRAY
                updateMusicStateInternal(title, artist, art, dominantColor)
            }
        } else {
            updateMusicStateInternal(title, artist, art, dominantColor)
        }
    }

    private fun updateMusicStateInternal(title: String?, artist: String?, art: Bitmap?, color: Int) {
        val current = musicState.value
        musicState.value = current?.copy(title = title ?: "", artist = artist ?: "", art = art, dominantColor = color)
            ?: MusicData(title ?: "", artist ?: "", art, false, 0f, 0L, color)
        chargingState.value = null
    }

    fun updateChargingInfo(level: Int, isCharging: Boolean, color: Int) {
        if (isCharging) chargingState.value = ChargingData(level, isCharging, color) else chargingState.value = null
    }

    fun updatePlayPauseState(isPlaying: Boolean) {
        val current = musicState.value ?: return
        musicState.value = current.copy(isPlaying = isPlaying)
    }

    fun updateMusicProgress(positionMs: Long, durationMs: Long) {
        val current = musicState.value ?: return
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
        musicState.value = current.copy(progress = progress, duration = durationMs)
    }

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int) {
        liveActivityState.value = LiveActivityData(title, data, progress, color)
    }

    fun setContextGlow(bitmap: Bitmap?) { }
    fun updateMiniPillContent(title: String, icon: android.graphics.drawable.Icon?, color: Int) { }
    // General Notification API removed safely
}
