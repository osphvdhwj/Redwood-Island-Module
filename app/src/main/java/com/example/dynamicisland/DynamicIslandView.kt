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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
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
import kotlin.math.abs

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
class DynamicIslandView(
    context: Context,
    val moduleContext: Context 
) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)

    var isCubeRotationEnabled = mutableStateOf(true)

    // Battery State via Broadcast
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) // 🚀 NEW: The tiny right pill

    var onSingleTap: (() -> Unit)? = null
    var onDoubleTap: (() -> Unit)? = null
    var onPillLongPress: (() -> Unit)? = null
    var onGrabberTap: (() -> Unit)? = null
    var onGrabberLongPress: (() -> Unit)? = null
    var onSwipeUp: (() -> Unit)? = null
    var onSwipeDown: (() -> Unit)? = null
    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null
    
    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    private fun loadPreferences() {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable()
            pref.reload()
            ringW.value = pref.getFloat("ring_w", 45f); ringH.value = pref.getFloat("ring_h", 45f); ringX.value = pref.getFloat("ring_x", 0f); ringY.value = pref.getFloat("ring_y", 48f)
            miniW.value = pref.getFloat("mini_w", 180f); miniH.value = pref.getFloat("mini_h", 36f); miniX.value = pref.getFloat("mini_x", 0f); miniY.value = pref.getFloat("mini_y", 48f)
            midW.value = pref.getFloat("mid_w", 320f); midH.value = pref.getFloat("mid_h", 80f); midX.value = pref.getFloat("mid_x", 0f); midY.value = pref.getFloat("mid_y", 48f)
            maxW.value = pref.getFloat("max_w", 360f); maxH.value = pref.getFloat("max_h", 220f); maxX.value = pref.getFloat("max_x", 0f); maxY.value = pref.getFloat("max_y", 48f)
            isCubeRotationEnabled.value = pref.getBoolean("rotate_cube", true)
        } catch (e: Exception) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                val prefix = intent.getStringExtra("prefix")
                if (prefix != null) {
                    val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f)
                    val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                    when (prefix) { "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }; "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }; "mid" -> { midW.value = w; midH.value = h; midX.value = x; midY.value = y }; "max" -> { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y } }
                } else loadPreferences()
            } else if (intent.action == "com.example.dynamicisland.BATTERY_UPDATE") {
                globalBatteryLevel.value = intent.getIntExtra("level", 100)
                globalIsCharging.value = intent.getBooleanExtra("isCharging", false)
            }
        }
    }

    init {
        loadPreferences()
        val filter = IntentFilter().apply { addAction("com.example.dynamicisland.RELOAD_PREFS"); addAction("com.example.dynamicisland.BATTERY_UPDATE") }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED) } else { @Suppress("UnspecifiedRegisterReceiverFlag") context.registerReceiver(receiver, filter) }

        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

        val composeView = ComposeView(context).apply {
            setContent { MaterialTheme(colorScheme = darkColorScheme()) { CompositionLocalProvider(LocalContext provides moduleContext) { IslandUI(islandState.value) } } }
        }
        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }
        addView(composeView)
        lifecycleOwner.start()
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        // 🚀 CUBE AND SPLIT TARGET DIMENSIONS
        val targetWidth = when (state) { IslandState.TYPE_1_MINI -> miniW.value; IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> 85f; else -> ringW.value }
        val targetHeight = when (state) { IslandState.TYPE_1_MINI -> miniH.value; IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> maxH.value; IslandState.TYPE_CUBE -> 85f; else -> ringH.value }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.65f, stiffness = 400f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        val offsetX by animateFloatAsState(targetX, spring<Float>(dampingRatio=0.65f, stiffness=400f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.65f, stiffness=400f), label = "y")

        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        var dragOffsetX by remember { mutableStateOf(0f) }
        var dragOffsetY by remember { mutableStateOf(0f) }

        val model = activeModel.value

        val targetBgColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else {
            if (model is LiveActivityModel.Music && model.dominantColor != null) Color(model.dominantColor).copy(alpha = 0.65f) else Color(0xFF121212).copy(alpha = 0.75f) 
        }
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.15f), animationSpec = tween(600), label = "borderColor")

        LaunchedEffect(width, height, offsetX, offsetY, state) {
            if (!isAttachedToWindow) return@LaunchedEffect
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect
            val density = context.resources.displayMetrics.density

            if (state == IslandState.HIDDEN) {
                wp.width = 0; wp.height = 0
            } else {
                val extraW = if (state == IslandState.TYPE_SPLIT) 100 else 30 // Extra width for the right tiny cube
                val extraH = if (state == IslandState.TYPE_3_MAX) 60 else 30
                wp.width = (width.value * density).toInt() + (extraW * density).toInt()
                wp.height = (height.value * density).toInt() + (extraH * density).toInt()
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND; wp.blurBehindRadius = 45 
                }
            }
            wp.x = offsetX.toInt(); wp.y = offsetY.toInt()
            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
        }

        // 🚀 THE NEW SPLIT-LAYOUT ROOT
        Row(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(dragOffsetX) > abs(dragOffsetY)) { if (dragOffsetX > 40) onSwipeRight?.invoke() else if (dragOffsetX < -40) onSwipeLeft?.invoke() }
                        else { if (dragOffsetY > 40) onSwipeDown?.invoke() else if (dragOffsetY < -40) onSwipeUp?.invoke() }
                        dragOffsetX = 0f; dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount -> change.consume(); dragOffsetX += dragAmount.x; dragOffsetY += dragAmount.y }
                )
            },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            // 🌟 THE MAIN PILL
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(rad))
                    .background(bgColor) 
                    .border(1.dp, borderColor, RoundedCornerShape(rad))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onSingleTap?.invoke() }, onDoubleTap = { onDoubleTap?.invoke() }, onLongPress = { onPillLongPress?.invoke() }) },
                contentAlignment = Alignment.TopCenter
            ) {
                if ((state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX) && model is LiveActivityModel.Music && model.albumArt != null) {
                    Image(bitmap = model.albumArt.asImageBitmap(), contentDescription = "Cinematic BG", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.35f).blur(24.dp))
                }

                if (state != IslandState.HIDDEN) {
                    val bottomPadding by animateDpAsState(targetValue = when(state) { IslandState.TYPE_3_MAX -> 24.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> 12.dp; else -> 0.dp }, label = "bottomPadding")

                    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding.coerceAtLeast(0.dp))) {
                        when (state) {
                            IslandState.TYPE_3_MAX -> { if (model is LiveActivityModel.Music) MusicMax(model) }
                            IslandState.TYPE_2_MID -> { if (model is LiveActivityModel.Music) MusicMid(model) }
                            IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> { if (model is LiveActivityModel.Music) MusicMini(model) }
                            IslandState.TYPE_CUBE -> { if (model is LiveActivityModel.Charging) ChargingCube(model) }
                            else -> {} 
                        }
                    }

                    if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_2_MID || state == IslandState.TYPE_3_MAX || state == IslandState.TYPE_SPLIT) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(when(state) { IslandState.TYPE_3_MAX -> 32.dp; IslandState.TYPE_2_MID -> 20.dp; else -> 16.dp }).pointerInput(Unit) { detectTapGestures(onTap = { onGrabberTap?.invoke() }, onLongPress = { onGrabberLongPress?.invoke() }) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.width(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 24.dp else 40.dp).height(if (state == IslandState.TYPE_1_MINI || state == IslandState.TYPE_SPLIT) 3.dp else 5.dp).background(Color.White.copy(alpha=0.4f), CircleShape))
                        }
                    }
                }
                
                // 🌟 REAL-TIME PROGRESS ARC IN GHOST BOX (R)
                if (state == IslandState.TYPE_0_RING) {
                    val isMedia = model is LiveActivityModel.Music && model.isPlaying
                    val shouldShowRing = isMedia || globalIsCharging.value || globalBatteryLevel.value <= 20

                    if (shouldShowRing) {
                        val progress = if (isMedia) ((model as LiveActivityModel.Music).positionMs.toFloat() / model.durationMs.toFloat()) else globalBatteryLevel.value / 100f
                        val progressColor = if (isMedia) Color.White else if (globalIsCharging.value) Color.Green else if (globalBatteryLevel.value <= 20) Color.Red else Color.White
                        
                        Canvas(modifier = Modifier.size(ringW.value.dp, ringH.value.dp).align(Alignment.Center)) {
                            val strokeW = 3.dp.toPx()
                            drawArc(color = progressColor.copy(alpha=0.2f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeW))
                            drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, style = Stroke(strokeW, cap = StrokeCap.Round))
                        }
                    }
                }
            }

            // 🌟 THE SPLIT TINY CUBE
            AnimatedVisibility(visible = state == IslandState.TYPE_SPLIT, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                val sModel = splitModel.value
                val splitBg = if (sModel is LiveActivityModel.Charging) {
                    if (sModel.isPluggedIn) Color.Green.copy(alpha=0.2f) else if (sModel.level <= 20) Color.Red.copy(alpha=0.2f) else Color(0xFF121212).copy(alpha=0.75f)
                } else Color(0xFF121212).copy(alpha=0.75f)

                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(height).clip(CircleShape).background(splitBg).border(1.dp, borderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sModel is LiveActivityModel.Charging) {
                            val iconColor = if (sModel.isPluggedIn) Color.Green else if (sModel.level <= 20) Color.Red else Color.White
                            Text(text = "${sModel.level}%", color = iconColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 🚀 NEW SQUARE CUBE FOR CHARGING
    @Composable
    fun ChargingCube(model: LiveActivityModel.Charging) {
        val color = if (model.isPluggedIn) Color.Green else if (model.level <= 20) Color.Red else Color.White
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = if (model.isPluggedIn) Icons.Default.Add else Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${model.level}%", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val s = ms / 1000
        return String.format("%d:%02d", s / 60, s % 60)
    }
    
// ==========================================
    // MEDIA PILL COMPONENTS
    // ==========================================

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMini(music: LiveActivityModel.Music) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                val infiniteTransition = rememberInfiniteTransition()
                val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rot")
                val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f
                
                // 🚀 FIXED: ContentScale.Crop creates the perfect 📀 Disk
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Spinning Art", modifier = Modifier.size(24.dp).clip(CircleShape).rotate(currentRotation)) 
                else Box(Modifier.size(24.dp).background(Color.White.copy(0.2f), CircleShape))

                Spacer(Modifier.width(8.dp))
                Text(text = "${music.title} • ${music.artist}", color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f).basicMarquee())
                
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                Icon(imageVector = playIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) // 🚀 Bigger Button
            }
            
            // 🚀 NEW: S Pill Bottom Progress Bar
            val safeDuration = if (music.durationMs > 0) music.durationMs.toFloat() else 1f
            val progress = (music.positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress }, color = Color.White.copy(alpha=0.8f), trackColor = Color.Transparent,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).height(2.dp).padding(bottom = 1.dp).clip(CircleShape)
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MusicMid(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color(music.titleTextColor)
        val secondaryTextColor = dynamicTextColor.copy(alpha = 0.7f)
        val progress = if (music.durationMs > 0) (music.positionMs.toFloat() / music.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rot")
        val currentRotation = if (isCubeRotationEnabled.value && music.isPlaying) rotation else 0f

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                CircularProgressIndicator(progress = { progress }, color = dynamicTextColor, trackColor = dynamicTextColor.copy(alpha = 0.2f), strokeWidth = 2.dp, modifier = Modifier.fillMaxSize())
                // 🚀 FIXED: Rotating M Pill 📀 Disk
                if (music.albumArt != null) Image(bitmap = music.albumArt.asImageBitmap(), contentScale = ContentScale.Crop, contentDescription = "Art", modifier = Modifier.size(44.dp).clip(CircleShape).rotate(currentRotation))
                else Box(Modifier.size(44.dp).background(Color.White.copy(alpha=0.2f), CircleShape))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                 Text(text = music.title, color = dynamicTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.basicMarquee())
                 Text(text = music.artist, color = secondaryTextColor, fontSize = 14.sp, maxLines = 1, modifier = Modifier.basicMarquee())
            }
            
            val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
            Icon(imageVector = playIcon, contentDescription = "Status", tint = dynamicTextColor, modifier = Modifier.size(32.dp).padding(end = 4.dp).clickable { onPlayPauseClick?.invoke() }) // 🚀 Bigger Button
        }
    }

    @Composable
    fun MusicMax(music: LiveActivityModel.Music) {
        val dynamicTextColor = Color(music.titleTextColor)

        Column(modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 🚀 FIXED: Replaced spinning art with the actual App Icon (Spotify/YouTube)!
                if (music.appIcon != null) Image(bitmap = music.appIcon.asImageBitmap(), contentDescription = "App Logo", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).clickable { onPillLongPress?.invoke() }) 
                else Box(Modifier.size(60.dp).background(Color.White.copy(alpha=0.2f), RoundedCornerShape(14.dp)))
                
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = music.title, color = dynamicTextColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = music.artist, color = dynamicTextColor.copy(alpha=0.8f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val haptic = LocalHapticFeedback.current
            val interactionSource = remember { MutableInteractionSource() }
            val isDragged by interactionSource.collectIsDraggedAsState()
            var localPosition by remember(isDragged) { mutableStateOf(music.positionMs.toFloat()) }
            val safeDuration = if (music.durationMs > 0) music.durationMs.toFloat() else 1f
            val safePosition = if (isDragged) localPosition else music.positionMs.toFloat()
            val progress = (safePosition / safeDuration).coerceIn(0f, 1f)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = formatTime(safePosition.toLong()), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp)
                Text(text = formatTime(music.durationMs), color = dynamicTextColor.copy(alpha=0.7f), fontSize = 12.sp)
            }
            Slider(value = progress, onValueChange = { localPosition = it * safeDuration; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, onValueChangeFinished = { onSeekTo?.invoke(localPosition.toLong()); haptic.performHapticFeedback(HapticFeedbackType.LongPress) }, interactionSource = interactionSource, colors = SliderDefaults.colors(activeTrackColor = dynamicTextColor, inactiveTrackColor = dynamicTextColor.copy(alpha=0.3f), thumbColor = dynamicTextColor), modifier = Modifier.fillMaxWidth().height(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = dynamicTextColor, modifier = Modifier.size(36.dp).clickable { onPrevClick?.invoke() })
                val playIcon = if (music.isPlaying) ImageVector.vectorResource(id = R.drawable.ic_pause_vector) else ImageVector.vectorResource(id = R.drawable.ic_play_vector)
                Box(modifier = Modifier.size(56.dp).background(dynamicTextColor.copy(alpha = 0.2f), CircleShape).clickable { onPlayPauseClick?.invoke() }, contentAlignment = Alignment.Center) {
                    Icon(imageVector = playIcon, contentDescription = "Play/Pause", tint = dynamicTextColor, modifier = Modifier.size(32.dp))
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = dynamicTextColor, modifier = Modifier.size(36.dp).clickable { onNextClick?.invoke() })
            }
        }
    }

    // ==========================================
    // 🚀 NEW DASHBOARD SCAFFOLDING (Idle State)
    // ==========================================

    @Composable
    fun DashboardMid(model: LiveActivityModel.Dashboard) {
        // M Pill: Pinned Apps (Scaffolded for next phase)
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Phone, null, tint=Color.White, modifier=Modifier.size(24.dp)) }
            Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Email, null, tint=Color.White, modifier=Modifier.size(24.dp)) }
            Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Build, null, tint=Color.White, modifier=Modifier.size(24.dp)) }
            Box(Modifier.size(44.dp).background(Color.White.copy(0.2f), CircleShape), contentAlignment=Alignment.Center) { Icon(Icons.Default.Settings, null, tint=Color.White, modifier=Modifier.size(24.dp)) }
        }
    }

    @Composable
    fun DashboardMax(model: LiveActivityModel.Dashboard) {
        // B Pill: Full QS Tiles (Scaffolded for next phase)
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Quick Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Box(modifier = Modifier.size(60.dp).background(if (model.isWifiOn) Color.Blue else Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "WiFi", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Box(modifier = Modifier.size(60.dp).background(if (model.isTorchOn) Color.Yellow else Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Torch", tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Box(modifier = Modifier.size(60.dp).background(Color.White.copy(0.2f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "More", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Slider(value = model.currentVolume.toFloat(), onValueChange = {}, valueRange = 0f..model.maxVolume.toFloat(), colors = SliderDefaults.colors(activeTrackColor = Color.White))
        }
    }
    fun setState(newState: IslandState) { islandState.value = newState }
    fun setModel(model: LiveActivityModel?) { activeModel.value = model }
    fun setSplitModel(model: LiveActivityModel?) { splitModel.value = model }
}
