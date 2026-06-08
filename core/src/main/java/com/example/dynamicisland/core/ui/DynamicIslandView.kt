package com.example.dynamicisland.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.RedwoodTheme
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.core.gesture.IslandGesture
import com.example.dynamicisland.core.gesture.MLGestureClassifier
import com.example.dynamicisland.core.manager.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.core.util.*
import com.example.dynamicisland.core.util.LocalRedwoodStrings
import com.example.dynamicisland.core.util.TranslationProvider
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.settings.AestheticStyle
import com.example.dynamicisland.shared.settings.DesignLanguage
import com.example.dynamicisland.shared.settings.IconPack
import com.example.dynamicisland.shared.settings.SettingsState
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

fun Modifier.glassBackground(blurRadius: androidx.compose.ui.unit.Dp, isLowLatency: Boolean): Modifier = this.background(Color.Black)

fun getPillShape(shape: String, cornerRadius: Float): androidx.compose.foundation.shape.RoundedCornerShape {
    val safeRadius = cornerRadius.coerceAtLeast(0f)
    return when (shape) {
        "capsule" -> androidx.compose.foundation.shape.RoundedCornerShape(50)
        "squircle" -> androidx.compose.foundation.shape.RoundedCornerShape(safeRadius / 2)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(safeRadius.dp)
    }
}


@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context),
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        controller?.gestureClassifier?.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

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
    
    // --- UI State Fields ---
    val pinnedApps = mutableStateListOf("", "", "", "", "", "", "", "")
    val qsTiles = mutableStateListOf("", "", "", "", "", "", "")
    
    val displayCutoutWidth = mutableFloatStateOf(94f)
    val mainPillRect = mutableStateOf(android.graphics.Rect())
    val splitCubeRect = mutableStateOf(android.graphics.Rect())
    val insetsUpdateFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val activePrivacyOp = mutableStateOf<String?>(null)
    val edgeLightActive = mutableStateOf(false)
    val gamingFrameMs = mutableFloatStateOf(16.6f)
    val gamingJankPct = mutableFloatStateOf(0f)
    val clipboardStashCount = mutableIntStateOf(0)
    
    // --- LiveBridge Mode States ---
    val bridgeOffsetX = mutableFloatStateOf(0f)
    val bridgeOffsetY = mutableFloatStateOf(0f)
    val isBridgeDragging = mutableStateOf(false)
    val currentVelocity = mutableStateOf(Offset.Zero)
    val gpuLoad = mutableFloatStateOf(0f)
    
    // Call UI Callbacks
    var onOpenCallUI: (() -> Unit)? = null
    var onMicToggle: (() -> Unit)? = null
    var onSpeakerToggle: (() -> Unit)? = null
    var onEndCallClick: (() -> Unit)? = null
    
    // Media UI Callbacks
    var onAudioOutputClick: (() -> Unit)? = null
    var onCustomMediaAction: ((String) -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onReplySend: ((String) -> Unit)? = null
    
    // Split UI Callbacks
    var onSplitPillClick: (() -> Unit)? = null

    val pendingNotifColor = mutableIntStateOf(android.graphics.Color.WHITE)
    val hasUnseenNotif = mutableStateOf(false)
    val calibrationMode = mutableStateOf(false)
    val calibrationTarget = mutableStateOf("ring")

    val ringW = mutableStateOf(48f); val ringH = mutableStateOf(48f); val ringX = mutableStateOf(0f); val ringY = mutableStateOf(48f); val ringR = mutableStateOf(24f)
    val miniW = mutableStateOf(180f); val miniH = mutableStateOf(36f); val miniX = mutableStateOf(0f); val miniY = mutableStateOf(48f); val miniR = mutableStateOf(18f)
    val midW = mutableStateOf(320f); val midH = mutableStateOf(80f); val midX = mutableStateOf(0f); val midY = mutableStateOf(48f); val midR = mutableStateOf(24f)
    val maxW = mutableStateOf(360f); val maxH = mutableStateOf(220f); val maxX = mutableStateOf(0f); val maxY = mutableStateOf(48f); val maxR = mutableStateOf(42f)
    val cubeW = mutableStateOf(85f); val cubeH = mutableStateOf(85f); val cubeX = mutableStateOf(0f); val cubeY = mutableStateOf(48f); val cubeR = mutableStateOf(20f)

    val padT = mutableStateOf(0f); val padB = mutableStateOf(0f); val padL = mutableStateOf(0f); val padR = mutableStateOf(0f)
    val ringThickness = mutableStateOf(6f)
    val expandUpwards = mutableStateOf(false)
    val isCubeRotationEnabled = mutableStateOf(true)
    val activeTheme = mutableStateOf(IslandTheme())

    val globalBatteryLevel = mutableIntStateOf(100)
    val globalIsCharging = mutableStateOf(false)
    val isBatteryPulsing = mutableStateOf(false)
    val gamingFps = mutableFloatStateOf(0f)
    val currentMediaPos = mutableLongStateOf(0L)

    val islandState = mutableStateOf(IslandState.TYPE_0_RING)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null)
    val currentHardware = mutableStateOf<LiveActivityModel.HardwareMonitor?>(null)

    var onVolumeDrag: ((Int) -> Unit)? = null
    var onStreamVolumeDrag: ((Int, Int) -> Unit)? = null
    var onBrightnessDrag: ((Int) -> Unit)? = null
    var onAutoBrightnessToggle: (() -> Unit)? = null
    var onRingerToggle: (() -> Unit)? = null
    var onGestureEvent: ((IslandGesture) -> Unit)? = null
    var onGestureSettingsUpdated: ((String?) -> Unit)? = null
    var onPlayPauseClick: (() -> Unit)? = null
    var onNextClick: (() -> Unit)? = null
    var onPrevClick: (() -> Unit)? = null

    val elasticScale = Animatable(1f)
    val metaballTearProgress = Animatable(0f)

    // 🛡️ OLED Anti-Burn-In State
    val pixelShiftX = mutableFloatStateOf(0f)
    val pixelShiftY = mutableFloatStateOf(0f)

    private var burnInJob: Job? = null

    init {
        id = generateViewId()
        setViewTreeLifecycleOwner(this)
        setViewTreeViewModelStoreOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val composeView = ComposeView(context).apply {
            setContent {
                val settings = controller?.settingsState ?: SettingsState()
                
                AppMD3Theme(settings = settings) {
                    CompositionLocalProvider(
                        LocalIconPack provides settings.iconPack,
                        LocalRedwoodStrings provides TranslationProvider.getStrings(settings.appLanguage)
                    ) {
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(modifier = Modifier
                                .graphicsLayer { 
                                    translationX = pixelShiftX.value
                                    translationY = pixelShiftY.value
                                }
                            ) {
                                RingUI(isBatteryPulsing.value)
                                IslandUI(islandState.value)
                            }
                        }
                    }
                }
                
                // 🔥 Start Burn-In Protection
                LaunchedEffect(settings.antiBurnInEnabled) {
                    burnInJob?.cancel()
                    if (settings.antiBurnInEnabled) {
                        burnInJob = launch {
                            while(true) {
                                delay(60000) // 1 minute
                                val intensity = settings.antiBurnInIntensity.coerceIn(0.5f, 3f)
                                pixelShiftX.value = Random.nextFloat() * intensity * (if(Random.nextBoolean()) 1 else -1)
                                pixelShiftY.value = Random.nextFloat() * intensity * (if(Random.nextBoolean()) 1 else -1)
                            }
                        }
                    } else {
                        pixelShiftX.value = 0f
                        pixelShiftY.value = 0f
                    }
                }
            }
        }
        addView(composeView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        burnInJob?.cancel()
    }

    fun updateAutoBrightnessState(isAuto: Boolean) { /* impl */ }
    fun updateRingerState(mode: Int) { /* impl */ }
    fun updateHardwareVolume(vol: Int) { /* impl */ }
    fun updateHardwareBrightness(bright: Int) { /* impl */ }
    fun updateTicker(pos: Long) { currentMediaPos.longValue = pos }
    fun updateBattery(level: Int, isCharging: Boolean) {
        globalBatteryLevel.intValue = level
        globalIsCharging.value = isCharging
    }

    fun triggerBatteryPulse() {
        CoroutineScope(AndroidUiDispatcher.Main).launch {
            isBatteryPulsing.value = true
            delay(1000)
            isBatteryPulsing.value = false
        }
    }
}
