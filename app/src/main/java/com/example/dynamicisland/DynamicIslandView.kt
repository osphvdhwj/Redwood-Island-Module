package com.example.dynamicisland

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
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
import kotlin.math.sin

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

    fun pause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    fun resume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
}

// --- Data Models for Notifications ---
data class NotificationActionModel(
    val title: String,
    val actionIntent: PendingIntent,
    val remoteInputs: Array<RemoteInput>? // Standard Android RemoteInput
)

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
    val isScreenOn = mutableStateOf(true)
    val isLandscape = mutableStateOf(false)

    // Data States
    private val notificationState = mutableStateOf<NotificationData?>(null)
    private val musicState = mutableStateOf<MusicData?>(null)
    private val liveActivityState = mutableStateOf<LiveActivityData?>(null)
    private val chargingState = mutableStateOf<ChargingData?>(null)

    data class NotificationData(
        val title: String,
        val text: String,
        val icon: Icon?,
        val category: String? = null,
        val actions: List<NotificationActionModel> = emptyList()
    )

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

    // Gesture Callbacks for IslandController
    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onLongPress: (() -> Unit)? = null

    // Music Controls
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onShuffleClick: (() -> Unit)? = null
    var onLoopClick: (() -> Unit)? = null
    var onCloseClick: (() -> Unit)? = null

    // Notification Callbacks
    var onActionClick: ((NotificationActionModel) -> Unit)? = null
    var onReplySend: ((NotificationActionModel, String) -> Unit)? = null

    // Swipe Gestures
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    init {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        // FIX 1: Add the missing ViewModelStoreOwner to prevent crashes
        val viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
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

        // FIX 2: Manually start the Compose Engine (Recomposer)
        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.compositionContext = recomposer
        runRecomposeScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        addView(composeView)
        lifecycleOwner.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let {
            val landscape = it.orientation == Configuration.ORIENTATION_LANDSCAPE
            if (isLandscape.value != landscape) {
                isLandscape.value = landscape
            }
        }
    }

    fun updateScreenState(isOn: Boolean) {
        if (isScreenOn.value != isOn) {
            isScreenOn.value = isOn
            if (isOn) {
                lifecycleOwner.resume()
            } else {
                lifecycleOwner.pause()
            }
        }
    }

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    // --- Compose UI ---
    @Composable
    fun DynamicIslandTheme(context: Context, content: @Composable () -> Unit) {
        // Use Dynamic Colors (Material You)
        val colorScheme = dynamicDarkColorScheme(context)
        MaterialTheme(colorScheme = colorScheme, content = content)
    }

    @Composable
    fun IslandUI(state: IslandState) {
        // Updated Dimensions: Wider pills, not taller
        val targetWidth = when (state) {
            IslandState.HIDDEN -> 24.dp
            IslandState.TYPE_1_MINI -> 110.dp
            IslandState.TYPE_2_MID -> 300.dp // Wider (was 240)
            IslandState.TYPE_3_MAX -> 340.dp // Wider (was 260)
        }

        val targetHeight = when (state) {
            IslandState.HIDDEN -> 24.dp
            IslandState.TYPE_1_MINI -> 24.dp // Slightly shorter (was 36/40)
            IslandState.TYPE_2_MID -> 80.dp  // Shorter (was 100)
            IslandState.TYPE_3_MAX -> 180.dp // Shorter (was 260)
        }

        // Adjust height for Notification Reply (needs more space)
        val notif = notificationState.value
        val hasReply = notif?.actions?.any { !it.remoteInputs.isNullOrEmpty() } == true
        val finalHeight = if (state == IslandState.TYPE_3_MAX && hasReply) 220.dp else targetHeight

        val width by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "width"
        )
        val height by animateDpAsState(
            targetValue = finalHeight,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "height"
        )

        // FIX 3: Sync Compose Animation with WindowManager Bounds
        LaunchedEffect(width, height) {
            val wp = windowParams
            val wm = windowManager
            if (wp != null && wm != null) {
                // Convert Dp to Px
                val pxWidth = (width.value * context.resources.displayMetrics.density).toInt()
                val pxHeight = (height.value * context.resources.displayMetrics.density).toInt()

                // Add a small buffer to prevent clipping during bouncy springs
                wp.width = pxWidth + 50
                wp.height = pxHeight + 100 // Add space for bottom padding

                try {
                    wm.updateViewLayout(this@DynamicIslandView, wp)
                } catch (e: Exception) {}
            }
        }

        // Dynamic Color logic
        val music = musicState.value
        val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
        val targetColor = if (music != null && state != IslandState.HIDDEN) {
             Color(music.dominantColor).copy(alpha = 1f)
        } else {
             surfaceColor
        }

        val backgroundColor by animateColorAsState(
            targetValue = if (state == IslandState.HIDDEN) Color.Transparent else targetColor,
            animationSpec = spring(stiffness = Spring.StiffnessLow), label = "color"
        )

        val cornerRadius = 42.dp

        // FIX: Removed .fillMaxWidth() so it doesn't block the CrDroid status bar gestures!
        Box(
            modifier = Modifier.padding(top = 40.dp), // Keep top padding
            contentAlignment = Alignment.TopCenter
        ) {
            // The Pill Container
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor)
                    // If HIDDEN, draw a stroked circle ring if desired
                    .then(if (state == IslandState.HIDDEN) Modifier.border(1.dp, Color.Gray.copy(alpha=0.5f), CircleShape) else Modifier)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -20) onSwipeLeft?.invoke()
                            else if (dragAmount > 20) onSwipeRight?.invoke()
                        }
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
                        IslandState.TYPE_3_MAX -> ExpandedContent()
                        else -> {}
                    }
                }
            }
        }
    }

    @Composable
    fun MiniContent() {
        val music = musicState.value
        val notif = notificationState.value
        val charging = chargingState.value

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (charging != null && charging.isCharging) {
                Text(text = "${charging.level}%", style = MaterialTheme.typography.labelSmall, color = Color(charging.color))
                Spacer(Modifier.width(4.dp))
                 Box(Modifier.size(16.dp).background(Color(charging.color), RoundedCornerShape(4.dp)))
            } else if (music != null && music.isPlaying) {
                 if (music.art != null) {
                     Image(bitmap = music.art.asImageBitmap(), contentDescription = "Art", modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)))
                 } else {
                     Box(Modifier.size(16.dp).background(Color.Green, RoundedCornerShape(4.dp)))
                 }
            } else if (notif != null) {
                 // Use extracted Icon
                 if (notif.icon != null) {
                     // Need to load drawable, but for now placeholder or generic
                     Box(Modifier.size(16.dp).background(Color.Blue, RoundedCornerShape(4.dp)))
                 } else {
                     Box(Modifier.size(16.dp).background(Color.Blue, RoundedCornerShape(4.dp)))
                 }
            }
        }
    }

    @Composable
    fun ExpandedContent() {
        val music = musicState.value
        val notif = notificationState.value
        val charging = chargingState.value

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (charging != null && charging.isCharging) {
                Text("Charging", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                    WaveLoadingView(progress = charging.level / 100f, color = Color(charging.color), modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(8.dp))
                Text(text = "${charging.level}%", style = MaterialTheme.typography.headlineMedium, color = Color(charging.color))

            } else if (music != null) {
                // BIGGEST PILL REDESIGN
                Box(modifier = Modifier.fillMaxSize()) {
                    // 1. Background Image Layer
                    if (music.art != null && islandState.value == IslandState.TYPE_3_MAX) {
                        Image(
                            bitmap = music.art.asImageBitmap(),
                            contentDescription = "Background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.25f) // Darken so text is readable
                        )
                    }

                    // 2. Foreground Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Row: Info & Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = music.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = music.artist, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onCloseClick?.invoke() }, modifier = Modifier.size(24.dp)) {
                                androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel), contentDescription = "Close", tint = Color.White)
                            }
                        }

                        if (islandState.value == IslandState.TYPE_3_MAX) {
                            // Middle Row: Slider & Timers
                            Column(modifier = Modifier.fillMaxWidth()) {
                                var sliderPos by remember(music.progress) { mutableStateOf(music.progress * music.duration) }

                                Slider(
                                    value = sliderPos,
                                    onValueChange = { sliderPos = it },
                                    onValueChangeFinished = { onSeekTo?.invoke(sliderPos.toLong()) },
                                    valueRange = 0f..(music.duration.toFloat().coerceAtLeast(1f)),
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.Gray
                                    )
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = formatTime(sliderPos.toLong()), color = Color.LightGray, fontSize = 12.sp)
                                    Text(text = formatTime(music.duration), color = Color.LightGray, fontSize = 12.sp)
                                }
                            }
                        }

                        // Bottom Row: Media Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (islandState.value == IslandState.TYPE_3_MAX) {
                                IconButton(onClick = { onShuffleClick?.invoke() }) {
                                    androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = "Shuffle", tint = Color.White) // Placeholder icon
                                }
                            }

                            IconButton(onClick = { onPrevClick?.invoke() }) {
                                androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_media_previous), contentDescription = "Prev", tint = Color.White)
                            }

                            val playIcon = if (music.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                            IconButton(onClick = { onPlayPauseClick?.invoke() }, modifier = Modifier.size(56.dp)) {
                                androidx.compose.material3.Icon(painter = painterResource(playIcon), contentDescription = "Play/Pause", tint = Color.White, modifier = Modifier.size(40.dp))
                            }

                            IconButton(onClick = { onNextClick?.invoke() }) {
                                androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_media_next), contentDescription = "Next", tint = Color.White)
                            }

                            if (islandState.value == IslandState.TYPE_3_MAX) {
                                IconButton(onClick = { onLoopClick?.invoke() }) {
                                    androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_menu_rotate), contentDescription = "Loop", tint = Color.White) // Placeholder icon
                                }
                            }
                        }
                    }
                }

            } else if (notif != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.size(32.dp).background(Color.Gray, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(text = notif.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(text = notif.text, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    if (notif.category == android.app.Notification.CATEGORY_MESSAGE) {
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.sym_action_chat), contentDescription = "Chat", tint = Color.Cyan, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (notif.actions.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        notif.actions.forEach { action ->
                            if (!action.remoteInputs.isNullOrEmpty()) {
                                var replyText by remember { mutableStateOf("") }
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicTextField(
                                        value = replyText,
                                        onValueChange = { replyText = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            if (replyText.isNotEmpty()) {
                                                onReplySend?.invoke(action, replyText)
                                                replyText = ""
                                            }
                                        }),
                                        decorationBox = { innerTextField ->
                                            if (replyText.isEmpty()) Text("Reply...", color = Color.Gray, fontSize = 14.sp)
                                            innerTextField()
                                        }
                                    )
                                    IconButton(onClick = {
                                        if (replyText.isNotEmpty()) {
                                            onReplySend?.invoke(action, replyText)
                                            replyText = ""
                                        }
                                    }) {
                                        androidx.compose.material3.Icon(painter = painterResource(android.R.drawable.ic_menu_send), contentDescription = "Send", tint = Color.Cyan)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onActionClick?.invoke(action) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(action.title, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

            } else {
                Text("Dashboard", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    @Composable
    fun WaveLoadingView(progress: Float, color: Color, modifier: Modifier = Modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "wave")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "phase"
        )

        Canvas(modifier = modifier) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val waterLevel = canvasHeight * (1f - progress)
            val waveAmplitude = 12f // Height of the ripples

            val path = Path().apply {
                moveTo(0f, canvasHeight)
                lineTo(0f, waterLevel)

                // Draw the sine wave
                val step = 5f
                var x = 0f
                while (x <= canvasWidth + step) {
                    val angularFreq = (2f * Math.PI.toFloat() * 1.5f) / canvasWidth
                    val y = waterLevel + waveAmplitude * sin((x * angularFreq) + phase)
                    lineTo(x, y)
                    x += step
                }

                lineTo(canvasWidth, canvasHeight)
                close()
            }
            drawPath(path = path, color = color)
        }
    }

    // --- Controller API ---
    fun setState(newState: IslandState) {
        islandState.value = newState
        val wp = windowParams ?: return
        val wm = windowManager ?: return

        when (newState) {
            IslandState.HIDDEN, IslandState.TYPE_1_MINI -> {
                // Pass touches through, NO keyboard focus
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            IslandState.TYPE_2_MID, IslandState.TYPE_3_MAX -> {
                // Intercept touches outside, ALLOW keyboard focus for replies
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL.inv()
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            }
        }

        try {
            wm.updateViewLayout(this, wp)
        } catch (e: Exception) {
            // Ignore layout update errors during fast transitions
        }
    }

    fun showMini() = setState(IslandState.TYPE_1_MINI)
    fun expand() = setState(IslandState.TYPE_2_MID)
    fun showDashboard() = setState(IslandState.TYPE_3_MAX)
    fun hide() = setState(IslandState.HIDDEN)
    fun collapse() = showMini()

    val isExpanded: Boolean get() = islandState.value == IslandState.TYPE_2_MID || islandState.value == IslandState.TYPE_3_MAX

    // Data Updates
    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?, category: String?, actions: List<NotificationActionModel>) {
        notificationState.value = NotificationData(
            title ?: "",
            text ?: "",
            icon,
            category,
            actions
        )
        musicState.value = null
        chargingState.value = null
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        var dominantColor = android.graphics.Color.DKGRAY
        if (art != null) {
            Palette.from(art).generate { palette ->
                dominantColor = palette?.getVibrantColor(
                    palette.getDominantColor(android.graphics.Color.DKGRAY)
                ) ?: android.graphics.Color.DKGRAY
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

        notificationState.value = null
        chargingState.value = null
    }

    fun updateChargingInfo(level: Int, isCharging: Boolean, color: Int) {
        if (isCharging) {
            chargingState.value = ChargingData(level, isCharging, color)
        } else {
            chargingState.value = null
        }
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

    fun setContextGlow(bitmap: Bitmap?) { }

    fun updateMiniPillContent(title: String, icon: Icon?, color: Int) { }
}
