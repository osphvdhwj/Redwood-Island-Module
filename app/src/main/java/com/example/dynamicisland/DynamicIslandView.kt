package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image 
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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
    init { savedStateRegistryController.performRestore(Bundle()) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    
    fun attach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun detach() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context) {

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)
    var cubeW = mutableStateOf(85f); var cubeH = mutableStateOf(85f); var cubeX = mutableStateOf(0f); var cubeY = mutableStateOf(48f)

    var padT = mutableStateOf(0f); var padB = mutableStateOf(0f); var padL = mutableStateOf(0f); var padR = mutableStateOf(0f)
    var ringThickness = mutableStateOf(6f)
    var expandUpwards = mutableStateOf(false)

    var isCubeRotationEnabled = mutableStateOf(true)
    var customOffsetY = mutableFloatStateOf(0f)
    var customBaseWidth = mutableFloatStateOf(100f)
    var activeTheme = mutableStateOf(IslandTheme())
    var globalBatteryLevel = mutableIntStateOf(100)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)
    
    var displayCutoutWidth = mutableFloatStateOf(0f)
    var pinnedApps = mutableStateListOf<String>("", "", "", "", "", "", "", "")
    var qsTiles = mutableStateListOf<String>("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) 

    fun updateTicker(pos: Long) { currentMediaPos.longValue = pos }
    fun updateBattery(level: Int, isCharging: Boolean) { globalBatteryLevel.intValue = level; globalIsCharging.value = isCharging }

    var onGestureEvent: ((IslandGesture) -> Unit)? = null
    var onGestureSettingsUpdated: ((String?) -> Unit)? = null
    var onSplitPillClick: (() -> Unit)? = null

    var onPlayPauseClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onAudioOutputClick: (() -> Unit)? = null

    private val lifecycleOwner = OverlayLifecycleOwner()

    private fun loadPreferences() {
        try {
            val pref = XSharedPreferences("com.example.dynamicisland", "island_prefs")
            pref.makeWorldReadable(); pref.reload()
            ringW.value = pref.getFloat("ring_w", 45f); ringH.value = pref.getFloat("ring_h", 45f); ringX.value = pref.getFloat("ring_x", 0f); ringY.value = pref.getFloat("ring_y", 48f)
            miniW.value = pref.getFloat("mini_w", 180f); miniH.value = pref.getFloat("mini_h", 36f); miniX.value = pref.getFloat("mini_x", 0f); miniY.value = pref.getFloat("mini_y", 48f)
            midW.value = pref.getFloat("mid_w", 320f); midH.value = pref.getFloat("mid_h", 80f); midX.value = pref.getFloat("mid_x", 0f); midY.value = pref.getFloat("mid_y", 48f)
            maxW.value = pref.getFloat("max_w", 360f); maxH.value = pref.getFloat("max_h", 220f); maxX.value = pref.getFloat("max_x", 0f); maxY.value = pref.getFloat("max_y", 48f)
            cubeW.value = pref.getFloat("cube_w", 85f); cubeH.value = pref.getFloat("cube_h", 85f); cubeX.value = pref.getFloat("cube_x", 0f); cubeY.value = pref.getFloat("cube_y", 48f)
            padT.value = pref.getFloat("pad_t", 0f); padB.value = pref.getFloat("pad_b", 0f); padL.value = pref.getFloat("pad_l", 0f); padR.value = pref.getFloat("pad_r", 0f)
            ringThickness.value = pref.getFloat("ring_thickness", 6f)
            expandUpwards.value = pref.getBoolean("expand_upwards", false)
            isCubeRotationEnabled.value = pref.getBoolean("rotate_cube", true)
        } catch (e: Throwable) {}
    }

    private val receiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.RELOAD_PREFS") {
                val prefix = intent.getStringExtra("prefix")
                if (prefix != null) {
                    val w = intent.getFloatExtra("w", 0f); val h = intent.getFloatExtra("h", 0f); val x = intent.getFloatExtra("x", 0f); val y = intent.getFloatExtra("y", 0f)
                    when (prefix) { "ring" -> { ringW.value = w; ringH.value = h; ringX.value = x; ringY.value = y }; "mini" -> { miniW.value = w; miniH.value = h; miniX.value = x; miniY.value = y }; "mid" -> { midW.value = w; midH.value = h; midX.value = x; midY.value = y }; "max" -> { maxW.value = w; maxH.value = h; maxX.value = x; maxY.value = y }; "cube" -> { cubeW.value = w; cubeH.value = h; cubeX.value = x; cubeY.value = y } }
                    padT.value = intent.getFloatExtra("pad_t", padT.value); padB.value = intent.getFloatExtra("pad_b", padB.value); padL.value = intent.getFloatExtra("pad_l", padL.value); padR.value = intent.getFloatExtra("pad_r", padR.value)
                    ringThickness.value = intent.getFloatExtra("ring_thickness", ringThickness.value)
                    expandUpwards.value = intent.getBooleanExtra("expand_upwards", expandUpwards.value)
                    
                    for (i in 0..7) { val pkg = intent.getStringExtra("pinned_app_$i"); if (pkg != null) pinnedApps[i] = pkg }
                    for (i in 0..6) { val qs = intent.getStringExtra("qs_tile_$i"); if (qs != null) qsTiles[i] = qs }
                } 

                customOffsetY.floatValue = intent.getFloatExtra("tweak_offset_y", customOffsetY.floatValue)
                customBaseWidth.floatValue = intent.getFloatExtra("tweak_base_width", customBaseWidth.floatValue)

                val capString = intent.getStringExtra("theme_media_cap") ?: "Round"
                val parsedCap = if (capString == "Square") StrokeCap.Square else StrokeCap.Round

                activeTheme.value = IslandTheme(
                    mediaBarCap = parsedCap,
                    mediaBarThickness = intent.getFloatExtra("theme_media_thick", 4f).dp,
                    titleOffsetX = intent.getFloatExtra("theme_title_x", 0f).dp,
                    titleOffsetY = intent.getFloatExtra("theme_title_y", 0f).dp,
                    titleSize = intent.getFloatExtra("theme_title_size", 16f).sp,
                    timeTextSize = intent.getFloatExtra("theme_time_size", 12f).sp,
                    timeTextOffsetX = intent.getFloatExtra("theme_time_x", 0f).dp,
                    batteryRingThickness = intent.getFloatExtra("theme_bat_ring", 12f).dp,
                    cornerRadius = intent.getFloatExtra("theme_corner_radius", 50f).dp,
                    albumArtSize = intent.getFloatExtra("theme_album_art_size", 44f).dp,
                    
                    buttonSize = intent.getFloatExtra("theme_button_size", 48f).dp,
                    buttonSpacing = intent.getFloatExtra("theme_button_spacing", 16f).dp,
                    buttonCornerRadius = intent.getFloatExtra("theme_button_radius", 50f).dp,
                    actionAnimType = intent.getStringExtra("theme_anim_type") ?: "BOUNCE"
                )

                val payload = intent.getStringExtra("gesture_payload")
                if (payload != null) onGestureSettingsUpdated?.invoke(payload) else loadPreferences()
            }
        }
    }

    init {
        loadPreferences()
        
        val composeView = ComposeView(context).apply {
            // 🚀 FIX: Tie the spoofed lifecycle strictly to the hardware window attachment state
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() })

            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    lifecycleOwner.attach()
                }
                override fun onViewDetachedFromWindow(v: View) {
                    lifecycleOwner.detach()
                }
            })
            
            setContent {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    CompositionLocalProvider(LocalContext provides moduleContext, LocalIslandTheme provides activeTheme.value) {
                        IslandUI(islandState.value)
                    }
                }
            }
        }
        
        val coroutineContext = AndroidUiDispatcher.CurrentThread; val recomposer = androidx.compose.runtime.Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }
        addView(composeView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val displayCutout = try { if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { windowManager?.currentWindowMetrics?.windowInsets?.displayCutout } else null } catch(e:Throwable){null}
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density

        val filter = IntentFilter("com.example.dynamicisland.RELOAD_PREFS")
        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, securePermission, null, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter, securePermission, null)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try { context.unregisterReceiver(receiver) } catch (e: Throwable) {}
        BatteryPlugin.stop(context)
        try { context.sendBroadcast(android.content.Intent("com.example.dynamicisland.RESTORE_CLOCK").setPackage("com.android.systemui")) } catch(e:Throwable){}
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun IslandUI(state: IslandState) {
        val haptic = LocalHapticFeedback.current
        
        var isSquished by remember { mutableStateOf(false) }
        val squishX by animateFloatAsState(targetValue = if (isSquished) 1.03f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "sx")
        val squishY by animateFloatAsState(targetValue = if (isSquished) 0.94f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "sy")

        val rawTargetWidth = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniW.value; IslandState.TYPE_2_MID -> midW.value; IslandState.TYPE_3_MAX -> maxW.value; IslandState.TYPE_CUBE -> cubeW.value; else -> ringW.value }
        val targetWidth = rawTargetWidth.coerceAtLeast(100f)
        val targetHeight = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniH.value; IslandState.TYPE_2_MID -> midH.value; IslandState.TYPE_3_MAX -> maxH.value; IslandState.TYPE_CUBE -> cubeH.value; else -> ringH.value }
        val targetX = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniX.value; IslandState.TYPE_2_MID -> midX.value; IslandState.TYPE_3_MAX -> maxX.value; IslandState.TYPE_CUBE -> cubeX.value; else -> ringX.value }
        val targetY = when (state) { IslandState.TYPE_1_MINI, IslandState.TYPE_SPLIT -> miniY.value; IslandState.TYPE_2_MID -> midY.value; IslandState.TYPE_3_MAX -> maxY.value; IslandState.TYPE_CUBE -> cubeY.value; else -> ringY.value }

        val physicsSpec = spring<Dp>(dampingRatio = 0.72f, stiffness = 200f)
        val width by animateDpAsState(targetWidth.dp, physicsSpec, label = "width")
        val height by animateDpAsState(targetHeight.dp, physicsSpec, label = "height")
        
        val offsetX by animateFloatAsState(targetX, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "x")
        val offsetY by animateFloatAsState(targetY, spring<Float>(dampingRatio=0.82f, stiffness=350f), label = "y")
        val radTarget = when (state) { IslandState.TYPE_3_MAX -> 42.dp; IslandState.TYPE_2_MID -> 16.dp; IslandState.TYPE_CUBE -> 24.dp; else -> (targetHeight / 2).dp }
        val rad by animateDpAsState(radTarget, physicsSpec, label = "rad")

        val model = activeModel.value

        val targetBgColor = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.Black
        val bgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(600), label = "bgColor")
        val borderColor by animateColorAsState(targetValue = if (state == IslandState.HIDDEN || state == IslandState.TYPE_0_RING) Color.Transparent else Color.White.copy(alpha = 0.08f), animationSpec = tween(600), label = "borderColor")

        // 🚀 FIX: Prevent WindowToken race condition crashes and defer height sizing to onSizeChanged
        LaunchedEffect(state, model, expandUpwards.value) {
            val wp = windowParams ?: return@LaunchedEffect
            val wm = windowManager ?: return@LaunchedEffect

            if (model?.isSensitive == true) { wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_SECURE } else { wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_SECURE.inv() }

            if (state == IslandState.HIDDEN) {
                wp.width = 0 
                wp.height = 0 
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                wp.width = WindowManager.LayoutParams.MATCH_PARENT
                wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            }
            
            wp.gravity = if (expandUpwards.value) (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL) else (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            
            try { 
                wm.updateViewLayout(this@DynamicIslandView, wp) 
            } catch (e: Exception) {
                kotlinx.coroutines.yield()
                try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch (ignore: Exception) {}
            }
        }

        val boxAlignment = if (expandUpwards.value) Alignment.BottomCenter else Alignment.TopCenter
        
        // 🚀 FIX: The Zero-Pixel Death Trap 
        // Intercepts the actual UI size to force the WindowManager to expand, preventing OS suspension
        var currentWindowHeight by remember { mutableIntStateOf(0) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = offsetY.coerceAtLeast(0f).dp)
                .padding(bottom = 20.dp)
                .onSizeChanged { size ->
                    if (currentWindowHeight != size.height && size.height > 0) {
                        currentWindowHeight = size.height
                        if (state != IslandState.HIDDEN) {
                            val wp = windowParams ?: return@onSizeChanged
                            val wm = windowManager ?: return@onSizeChanged
                            wp.height = size.height
                            try { wm.updateViewLayout(this@DynamicIslandView, wp) } catch(e: Throwable){}
                        }
                    }
                },
            contentAlignment = boxAlignment
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().offset(x = offsetX.dp),
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = if (expandUpwards.value) Alignment.Bottom else Alignment.Top
            ) {
                
                Box { 
                    if (model is LiveActivityModel.SystemAlert || model is LiveActivityModel.RealityPill) {
                        val infiniteTransition = rememberInfiniteTransition(label="glow")
                        val glowAlpha by infiniteTransition.animateFloat(initialValue = 0.1f, targetValue = 0.4f, animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMod
