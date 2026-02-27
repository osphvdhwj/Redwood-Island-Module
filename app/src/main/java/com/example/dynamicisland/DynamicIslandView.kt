package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.concurrent.TimeUnit
import java.util.Locale

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
        HIDDEN,      // Transparent Ring
        TYPE_1_MINI, // Small Pill
        TYPE_2_MID,  // Medium Pill
        TYPE_3_MAX   // Full Player
    }

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val isScreenOn = mutableStateOf(true)
    val isLandscape = mutableStateOf(false)

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
        val currentPosition: Long,
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
    var onSwipeUp: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    init {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        val viewModelStoreOwner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        val composeView = ComposeView(context).apply {
            setContent {
                DynamicIslandTheme {
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
    fun DynamicIslandTheme(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme(), content = content)
    }

    @Composable
    fun IslandUI(state: IslandState) {
        // Dimensions matching visual requirements
        val targetWidth = when (state) {
            IslandState.HIDDEN -> 10.dp
            IslandState.TYPE_1_MINI -> 180.dp
            IslandState.TYPE_2_MID -> 320.dp
            IslandState.TYPE_3_MAX -> 360.dp
        }

        val targetHeight = when (state) {
            IslandState.HIDDEN -> 10.dp
            IslandState.TYPE_1_MINI -> 36.dp
            IslandState.TYPE_2_MID -> 100.dp
            IslandState.TYPE_3_MAX -> 220.dp
        }

        // Push it down to be UNDER the camera (approx 48dp)
        val topPadding = 48.dp

        val physicsSpec = spring<Dp>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        val width by animateDpAsState(targetValue = targetWidth, animationSpec = physicsSpec, label = "width")
        val height by animateDpAsState(targetValue = targetHeight, animationSpec = physicsSpec, label = "height")

        LaunchedEffect(width, height) {
            if (!isAttachedToWindow) return@LaunchedEffect // CRASH FIX

            val wp = windowParams
            val wm = windowManager
            if (wp != null && wm != null) {
                val pxWidth = (width.value * context.resources.displayMetrics.density).toInt()
                val pxHeight = (height.value * context.resources.displayMetrics.density).toInt()

                wp.width = pxWidth + 30
                wp.height = pxHeight + (topPadding.value * context.resources.displayMetrics.density).toInt() + 30

                try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
            }
        }

        // Glassmorphism Styles (Water/Glass)
        // Water is clearer than "White Glass". Less opaque.
        val shape = RoundedCornerShape(32.dp)

        // Active: Water/Glass (More transparent white)
        // Hidden: Transparent
        val backgroundColor = if (state == IslandState.HIDDEN)
            Color.Transparent
        else
            Color.White.copy(alpha = 0.65f) // Water (clearer than 0.85f)

        val borderColor = if (state == IslandState.HIDDEN)
            Color.Transparent
        else
            Color.White.copy(alpha = 0.3f)

        val borderWidth = if (state == IslandState.HIDDEN) 0.dp else 1.dp

        // Text Color for Water Background (Dark text is best for contrast)
        val contentColor = Color.Black

        Box(modifier = Modifier.padding(top = topPadding), contentAlignment = Alignment.TopCenter) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape)
                    .pointerInput(Unit) {
                        var dragAccumulationX = 0f
                        var dragAccumulationY = 0f
                        detectDragGestures(
                            onDragStart = { dragAccumulationX = 0f; dragAccumulationY = 0f },
                            onDragEnd = {
                                if (abs(dragAccumulationX) > abs(dragAccumulationY)) {
                                    if (dragAccumulationX < -40) onSwipeLeft?.invoke()
                                    else if (dragAccumulationX > 40) onSwipeRight?.invoke()
                                } else {
                                    if (dragAccumulationY < -40) onSwipeUp?.invoke()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulationX += dragAmount.x
                                dragAccumulationY += dragAmount.y
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
                    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                        when (state) {
                            IslandState.TYPE_1_MINI -> MusicMiniContent(contentColor)
                            IslandState.TYPE_2_MID -> MusicMidContent(contentColor)
                            IslandState.TYPE_3_MAX -> MusicMaxContent(contentColor)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun MusicMiniContent(textColor: Color) {
        val music = musicState.value
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
        ) {
            if (music != null) {
                 Text(
                    text = "${music.title} • ${music.artist}",
                    color = textColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
    }

    @Composable
    fun MusicMidContent(textColor: Color) {
        val music = musicState.value ?: return
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
             if (music.art != null) {
                Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Box(Modifier.size(48.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                 Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.width(12.dp))

            val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            Icon(
                painterResource(playIcon),
                contentDescription = "Play/Pause",
                tint = textColor,
                modifier = Modifier.size(32.dp).clickable { onPlayPauseClick?.invoke() }
            )
        }
    }

    @Composable
    fun MusicMaxContent(textColor: Color) {
        val music = musicState.value ?: return

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Source Info
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(android.R.drawable.ic_media_play), contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                }
                Row(
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("This phone", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Middle Row: Titles & Main Action
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = music.artist, color = textColor.copy(alpha = 0.7f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.width(16.dp))

                val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                        .clickable { onPlayPauseClick?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(playIcon), contentDescription = "Play/Pause", tint = textColor, modifier = Modifier.size(32.dp))
                }
            }

            // Bottom Row: Wavy Slider & Controls
            Column(modifier = Modifier.fillMaxWidth()) {
                // Wavy Slider
                WavySlider(
                    value = music.progress,
                    onValueChange = { newProgress ->
                         // Calculate time from progress fraction
                         val newTime = (newProgress * music.duration).toLong()
                         onSeekTo?.invoke(newTime)
                    },
                    waveLength = 20.dp,
                    waveHeight = 4.dp,
                    // FIX: waveVelocity expects a Pair<Dp, WaveDirection>
                    waveVelocity = if (music.isPlaying) 15.dp to WaveDirection.HEAD else 0.dp to WaveDirection.HEAD,
                    waveThickness = 4.dp,
                    trackThickness = 4.dp,
                    modifier = Modifier.fillMaxWidth().height(24.dp)
                )

                // Time Indicators
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(music.currentPosition), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(formatTime(music.duration), color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(painterResource(android.R.drawable.ic_media_previous), contentDescription = "Prev", tint = textColor, modifier = Modifier.size(32.dp).clickable { onPrevClick?.invoke() })
                    Icon(painterResource(android.R.drawable.ic_media_next), contentDescription = "Next", tint = textColor, modifier = Modifier.size(32.dp).clickable { onNextClick?.invoke() })
                    Icon(painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Close", tint = textColor, modifier = Modifier.size(28.dp).clickable { onCloseClick?.invoke() })
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(ms),
            TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms))
        )
    }

    // --- Controller API ---
    fun setState(newState: IslandState) {
        if (!isAttachedToWindow && newState != IslandState.HIDDEN) return // Prevent zombie updates

        islandState.value = newState
        val wp = windowParams ?: return
        val wm = windowManager ?: return

        wp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                   WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                   WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                   WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                   WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

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
            ?: MusicData(title ?: "", artist ?: "", art, false, 0f, 0L, 0L, color)
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
        musicState.value = current.copy(progress = progress, duration = durationMs, currentPosition = positionMs)
    }

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int) {
        liveActivityState.value = LiveActivityData(title, data, progress, color)
    }
}
