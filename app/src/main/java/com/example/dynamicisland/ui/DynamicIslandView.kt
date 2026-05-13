// File: app/src/main/java/com/example/dynamicisland/ui/DynamicIslandView.kt
package com.example.dynamicisland.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer   // ← added missing import
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.dynamicisland.gesture.IslandGesture    // ← added missing import
import com.example.dynamicisland.ipc.IslandState
import com.example.dynamicisland.manager.*
import com.example.dynamicisland.model.*
import com.example.dynamicisland.settings.SettingsState    // ← added (ensure SettingsState exists)
import com.example.dynamicisland.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlin.math.abs

// Helper functions (must be inside the file, top-level)
fun Modifier.glassBackground(blurRadius: androidx.compose.ui.unit.Dp): Modifier = this
    .blur(blurRadius)
    .background(Color.White.copy(alpha = 0.1f))

fun getPillShape(shape: String, cornerRadius: Float): androidx.compose.foundation.shape.RoundedCornerShape {
    return when (shape) {
        "capsule" -> androidx.compose.foundation.shape.RoundedCornerShape(50)
        "squircle" -> androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius / 2)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius.dp)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    var controller: IslandController? = null

    var onAppPinnedClick: ((String) -> Unit)? = null
    var onQsTileClick: ((String) -> Unit)? = null

    var pendingNotifColor = mutableIntStateOf(android.graphics.Color.WHITE)
    var hasUnseenNotif = mutableStateOf(false)

    // ── Pill geometry and padding ────────────────────────────────────────────
    var ringW = mutableStateOf(45f); var ringH = mutableStateOf(45f); var ringX = mutableStateOf(0f); var ringY = mutableStateOf(48f)
    var miniW = mutableStateOf(180f); var miniH = mutableStateOf(36f); var miniX = mutableStateOf(0f); var miniY = mutableStateOf(48f)
    var midW = mutableStateOf(320f); var midH = mutableStateOf(80f); var midX = mutableStateOf(0f); var midY = mutableStateOf(48f)
    var maxW = mutableStateOf(360f); var maxH = mutableStateOf(220f); var maxX = mutableStateOf(0f); var maxY = mutableStateOf(48f)
    var cubeW = mutableStateOf(85f); var cubeH = mutableStateOf(85f); var cubeX = mutableStateOf(0f); var cubeY = mutableStateOf(48f)

    var padT = mutableStateOf(0f); var padB = mutableStateOf(0f); var padL = mutableStateOf(0f); var padR = mutableStateOf(0f)
    var ringThickness = mutableStateOf(6f)
    var expandUpwards = mutableStateOf(false)
    var isCubeRotationEnabled = mutableStateOf(true)
    var activeTheme = mutableStateOf(IslandTheme())

    // Hardware / global states
    var globalBatteryLevel = mutableIntStateOf(100)
    var hardwareVolume = mutableIntStateOf(0)
    var hardwareBrightness = mutableIntStateOf(0)
    var hardwareAutoBrightness = mutableStateOf(false)
    var hardwareRingerMode = mutableIntStateOf(android.media.AudioManager.RINGER_MODE_NORMAL)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)
    var displayCutoutWidth = mutableFloatStateOf(0f)
    var pinnedApps = mutableStateListOf("", "", "", "", "", "", "", "")
    var qsTiles = mutableStateListOf("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings")

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null)

    // System callbacks
    var onVolumeDrag: ((Int) -> Unit)? = null
    var onBrightnessDrag: ((Int) -> Unit)? = null
    var onAutoBrightnessToggle: (() -> Unit)? = null
    var onRingerToggle: (() -> Unit)? = null
    var onGestureEvent: ((IslandGesture) -> Unit)? = null
    var onGestureSettingsUpdated: ((String?) -> Unit)? = null
    var onSplitPillClick: (() -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onMicToggle: (() -> Unit)? = null
    var onSpeakerToggle: (() -> Unit)? = null
    var onEndCallClick: (() -> Unit)? = null
    var onOpenCallUI: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onAudioOutputClick: (() -> Unit)? = null
    var onCustomMediaAction: ((String) -> Unit)? = null

    // ── Gaming HUD stats ─────────────────────────────────────────────────────
    var gamingFps = mutableFloatStateOf(0f)
    var gamingFrameMs = mutableFloatStateOf(0f)
    var gamingJankPct = mutableFloatStateOf(0f)
    fun updateGamingStats(fps: Float, frameMs: Float, jankPct: Float) {
        gamingFps.floatValue     = fps
        gamingFrameMs.floatValue = frameMs
        gamingJankPct.floatValue = jankPct
    }

    // ── Elastic stretch animation state ──────────────────────────────────────
    val elasticScale = Animatable(1f)
    private var pullOffset = 0f

    // ── Sub‑systems ──────────────────────────────────────────────────────────
    private var flowJob: Job? = null
    val mainPillRect = android.graphics.Rect()
    val splitCubeRect = android.graphics.Rect()
    val insetsUpdateFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    private var insetsListenerProxy: Any? = null

    val composeView = ComposeView(context)
    private val prefsReceiver = IslandPreferencesManager.getReceiver(this)

    private val appChangeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == "com.example.dynamicisland.APP_CHANGED") composeView.visibility = View.VISIBLE
        }
    }

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> composeView.visibility = View.GONE
                Intent.ACTION_SCREEN_ON -> composeView.visibility = View.VISIBLE
            }
        }
    }

    init {
        IslandPreferencesManager.load(this)
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        composeView.setViewTreeLifecycleOwner(this@DynamicIslandView)
        composeView.setViewTreeViewModelStoreOwner(this@DynamicIslandView)
        // Fix for setViewTreeSavedStateRegistryOwner (requires API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            composeView.setViewTreeSavedStateRegistryOwner(this@DynamicIslandView)
        }

        composeView.setContent {
            // Safely retrieve settings from the controller or fallback
            val settings = controller?.settingsState ?: SettingsState()
            val shape = getPillShape(settings.pillShape, settings.pillCornerRadius)

            // Background: liquid glass, dynamic gradient, or solid dark
            val backgroundModifier = if (settings.designLanguage == DesignLanguage.APPLE_LIQUID_GLASS) {
                Modifier.glassBackground(blurRadius = settings.blurIntensity.dp)
            } else if (settings.dynamicGradient && activeModel.value is LiveActivityModel.Music) {
                // Use controller's gradient if available, else default
                val gradientColors = controller?.currentGradientColors ?: listOf(Color.DarkGray, Color.Black)
                Modifier.background(Brush.verticalGradient(gradientColors))
            } else {
                Modifier.background(Color.Black.copy(alpha = 0.85f))
            }

            // Shadow and glow
            val shadowModifier = if (settings.shadowCasting) {
                Modifier.shadow(16.dp, shape, ambientColor = controller?.currentBrandColor?.copy(alpha = 0.4f) ?: Color.White.copy(alpha = 0.4f))
            } else Modifier

            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(
                    LocalIslandTheme provides activeTheme.value,
                    androidx.compose.ui.platform.LocalContext provides moduleContext
                ) {
                    // We need a coroutine scope to call suspend functions inside drag handlers
                    val scope = rememberCoroutineScope()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .graphicsLayer {
                                scaleX = elasticScale.value
                                scaleY = elasticScale.value
                            }
                            .clip(shape)
                            .then(backgroundModifier)
                            .then(shadowModifier)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        // Launch inside a coroutine (this is a suspend function)
                                        scope.launch { elasticScale.animateTo(1f, spring()) }
                                        pullOffset = 0f
                                    },
                                    onDragCancel = { scope.launch { elasticScale.snapTo(1f) } }
                                ) { _, dragAmount ->
                                    pullOffset = (pullOffset + dragAmount).coerceIn(-50f, 100f)
                                    // snapTo is not a suspend function, but call it correctly
                                    elasticScale.snapTo(1f + pullOffset * 0.002f)
                                }
                            }
                    ) {
                        // Call the main UI composable
                        controller?.let { IslandMainUI(it) }
                    }
                }
            }
        }

        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val recomposer = Recomposer(coroutineContext)
        composeView.setParentCompositionContext(recomposer)
        CoroutineScope(coroutineContext).launch { recomposer.runRecomposeAndApplyChanges() }
        addView(composeView)

        try {
            val pingIntent = Intent("com.example.dynamicisland.REQUEST_PREFS")
            pingIntent.setPackage("com.example.dynamicisland")
            pingIntent.addFlags(0x01000000)
            context.sendBroadcast(pingIntent)
        } catch (e: Exception) {}
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            if (insetsListenerProxy == null) {
                insetsListenerProxy = java.lang.reflect.Proxy.newProxyInstance(context.classLoader, arrayOf(listenerClass)) { _, method, args ->
                    if (method.name == "onComputeInternalInsets") {
                        val info = args[0]
                        val touchableInsetsRegion = info.javaClass.getField("TOUCHABLE_INSETS_REGION").getInt(null)
                        info.javaClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType).invoke(info, touchableInsetsRegion)
                        val region = info.javaClass.getField("touchableRegion").get(info) as android.graphics.Region
                        region.setEmpty()
                        if (islandState.value != IslandState.HIDDEN) {
                            if (!mainPillRect.isEmpty) region.op(mainPillRect, android.graphics.Region.Op.UNION)
                            if (islandState.value == IslandState.TYPE_SPLIT && !splitCubeRect.isEmpty) region.op(splitCubeRect, android.graphics.Region.Op.UNION)
                        }
                    }
                    null
                }
            }
            viewTreeObserver.javaClass.getMethod("addOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, insetsListenerProxy)
        } catch (e: Exception) {}

        val displayCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics?.windowInsets?.displayCutout
        } else null
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density

        flowJob = CoroutineScope(AndroidUiDispatcher.CurrentThread).launch {
            insetsUpdateFlow.conflate().collect {
                windowParams?.let { wp ->
                    try { windowManager?.updateViewLayout(this@DynamicIslandView, wp) } catch (e: Exception) {}
                }
            }
        }

        val securePermission = "com.redwood.permission.SECURE_IPC"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(prefsReceiver, IntentFilter("com.example.dynamicisland.RELOAD_PREFS"), securePermission, null, Context.RECEIVER_EXPORTED)
            context.registerReceiver(appChangeReceiver, IntentFilter("com.example.dynamicisland.APP_CHANGED"), Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(prefsReceiver, IntentFilter("com.example.dynamicisland.RELOAD_PREFS"), securePermission, null)
            context.registerReceiver(appChangeReceiver, IntentFilter("com.example.dynamicisland.APP_CHANGED"))
        }

        context.registerReceiver(screenReceiver, IntentFilter().apply { addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF) })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        try {
            if (insetsListenerProxy != null) {
                val aliveObserver = if (viewTreeObserver.isAlive) viewTreeObserver else this.viewTreeObserver
                val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
                aliveObserver.javaClass.getMethod("removeOnComputeInternalInsetsListener", listenerClass).invoke(aliveObserver, insetsListenerProxy)
            }
        } catch (e: Exception) {}

        flowJob?.cancel()
        flowJob = null

        try { context.unregisterReceiver(prefsReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(appChangeReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(screenReceiver) } catch (e: Exception) {}

        BatteryPlugin.stop(context)
        context.sendBroadcast(Intent("com.example.dynamicisland.RESTORE_CLOCK").setPackage("com.android.systemui"))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration?) {
        super.onConfigurationChanged(newConfig)
        val displayCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            windowManager?.currentWindowMetrics?.windowInsets?.displayCutout
        } else null
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density
    }

    // ── Existing update helpers ──────────────────────────────────────────────
    fun updateAutoBrightnessState(isAuto: Boolean) { hardwareAutoBrightness.value = isAuto }
    fun updateRingerState(mode: Int) { hardwareRingerMode.intValue = mode }
    fun updateHardwareVolume(vol: Int) { hardwareVolume.intValue = vol }
    fun updateHardwareBrightness(bright: Int) { hardwareBrightness.intValue = bright }
    fun updateTicker(pos: Long) { currentMediaPos.longValue = pos }
    fun updateBattery(level: Int, isCharging: Boolean) {
        globalBatteryLevel.intValue = level
        globalIsCharging.value = isCharging
    }
}