package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
// Note: accompanist might not be available, I will use a simple Bitmap converter or AndroidView if needed.
// Actually, let's stick to standard Compose. I'll load drawable to bitmap if needed or use AndroidView for Icons.

// --- Lifecycle Wrapper for Xposed Compose Injection ---
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
}

@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    enum class IslandState {
        HIDDEN,      // Invisible Camera Punch-Hole Target
        TYPE_1_MINI, // 0.5 - 0.8 cm High
        TYPE_2_MID,  // 1.4 - 2.0 cm High
        TYPE_3_MAX   // 4.0 - 4.5 cm High
    }

    // Observable states for Compose
    val islandState = mutableStateOf(IslandState.HIDDEN)

    // Data States
    private val notificationState = mutableStateOf<NotificationData?>(null)
    private val musicState = mutableStateOf<MusicData?>(null)
    private val liveActivityState = mutableStateOf<LiveActivityData?>(null)

    data class NotificationData(val title: String, val text: String, val icon: Icon?)
    data class MusicData(val title: String, val artist: String, val art: Bitmap?, val isPlaying: Boolean, val progress: Float, val duration: Long)
    data class LiveActivityData(val title: String, val data: String, val progress: Float?, val color: Int)

    // Gesture Callbacks for IslandController
    var onSingleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.dynamicisland.UPDATE_CONFIG") {
                val newY = intent.getIntExtra("offset_y", 0)
                updateWindowPosition(newY)
            }
        }
    }

    init {
        val lifecycleOwner = OverlayLifecycleOwner()
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        val composeView = ComposeView(context).apply {
            setContent {
                DynamicIslandTheme(context) {
                    IslandUI(islandState.value)
                }
            }
        }
        addView(composeView)
        lifecycleOwner.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter("com.example.dynamicisland.UPDATE_CONFIG")
        // Use RECEIVER_EXPORTED (0x2) for Android 14+ to allow broadcasts from other processes
        context.registerReceiver(configReceiver, filter, 2)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            context.unregisterReceiver(configReceiver)
        } catch (e: Exception) {}
    }

    private fun updateWindowPosition(y: Int) {
        val wm = windowManager ?: return
        val params = windowParams ?: return
        params.y = y
        try {
            wm.updateViewLayout(this, params)
        } catch (e: Exception) {}
    }

    // --- Compose UI ---
    @Composable
    fun DynamicIslandTheme(context: Context, content: @Composable () -> Unit) {
        // Dynamic Material You Colors
        val colorScheme = dynamicDarkColorScheme(context)
        MaterialTheme(colorScheme = colorScheme, content = content)
    }

    @Composable
    fun IslandUI(state: IslandState) {
        // Size mappings (1 cm is roughly 63 dp)
        val targetWidth = when (state) {
            IslandState.HIDDEN -> 24.dp
            IslandState.TYPE_1_MINI -> 120.dp
            IslandState.TYPE_2_MID -> 240.dp
            IslandState.TYPE_3_MAX -> 300.dp
        }

        val targetHeight = when (state) {
            IslandState.HIDDEN -> 24.dp
            IslandState.TYPE_1_MINI -> 36.dp
            IslandState.TYPE_2_MID -> 100.dp
            IslandState.TYPE_3_MAX -> 200.dp
        }

        // Animated Dimensions
        val width by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width"
        )
        val height by animateDpAsState(
            targetValue = targetHeight,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "height"
        )

        // Color Transitions
        val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
        val backgroundColor by animateColorAsState(
            targetValue = if (state == IslandState.HIDDEN) Color.Transparent else surfaceColor,
            animationSpec = spring(stiffness = Spring.StiffnessLow), label = "color"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp), // Adjust for status bar/cutout alignment
            contentAlignment = Alignment.TopCenter
        ) {
            // The Pill Container
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(42.dp))
                    .background(backgroundColor)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onSingleTap?.invoke() },
                            onLongPress = { onLongPress?.invoke() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Content Rendering
                if (state != IslandState.HIDDEN) {
                    when (state) {
                        IslandState.TYPE_1_MINI -> MiniContent()
                        IslandState.TYPE_2_MID, IslandState.TYPE_3_MAX -> ExpandedContent()
                        else -> {}
                    }
                }
            }
        }
    }

    @Composable
    fun MiniContent() {
        // Priority: Live Activity > Music > Notification
        val music = musicState.value
        val notif = notificationState.value

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (music != null && music.isPlaying) {
                 // Mini Music Visualizer (Fake)
                 Box(Modifier.size(16.dp).background(Color.Green, RoundedCornerShape(4.dp)))
            } else if (notif != null) {
                 // Mini Icon
                 // Use a placeholder or load drawable
                 Box(Modifier.size(16.dp).background(Color.Blue, RoundedCornerShape(4.dp)))
            }
        }
    }

    @Composable
    fun ExpandedContent() {
        val music = musicState.value
        val notif = notificationState.value

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (music != null) {
                Text(text = music.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = music.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Controls would go here
            } else if (notif != null) {
                Text(text = notif.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(text = notif.text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Text("Dashboard", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // --- Controller API ---
    fun setState(newState: IslandState) {
        islandState.value = newState

        val wp = windowParams ?: return
        if (newState == IslandState.HIDDEN) {
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
        }
        windowManager?.updateViewLayout(this, wp)
    }

    fun showMini() = setState(IslandState.TYPE_1_MINI)
    fun expand() = setState(IslandState.TYPE_2_MID)
    fun showDashboard() = setState(IslandState.TYPE_3_MAX)
    fun hide() = setState(IslandState.HIDDEN)

    // Legacy alias
    fun collapse() = showMini()

    val isExpanded: Boolean get() = islandState.value == IslandState.TYPE_2_MID || islandState.value == IslandState.TYPE_3_MAX

    // Data Updates
    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?) {
        notificationState.value = NotificationData(title ?: "", text ?: "", icon)
        musicState.value = null // Clear music priority
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        val current = musicState.value
        musicState.value = current?.copy(title = title ?: "", artist = artist ?: "", art = art)
            ?: MusicData(title ?: "", artist ?: "", art, false, 0f, 0L)
        notificationState.value = null
    }

    fun updatePlayPauseState(isPlaying: Boolean) {
        val current = musicState.value ?: MusicData("", "", null, false, 0f, 0L)
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

    fun setContextGlow(bitmap: Bitmap?) {
        // Implement glow logic if needed, or ignore for now
    }

    fun updateMiniPillContent(title: String, icon: Icon?, color: Int) {
         // Update mini state
    }
}
