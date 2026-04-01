package com.example.dynamicisland

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
@SuppressLint("ViewConstructor")
class DynamicIslandView(context: Context, val moduleContext: Context) : FrameLayout(context), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

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

    // State Variables
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
    
    // Global & Hardware States
    var globalBatteryLevel = mutableIntStateOf(100)
    var hardwareVolume = mutableIntStateOf(0)
    var hardwareBrightness = mutableIntStateOf(0)
    var hardwareAutoBrightness = mutableStateOf(false)
    var hardwareRingerMode = mutableIntStateOf(android.media.AudioManager.RINGER_MODE_NORMAL)
    var globalIsCharging = mutableStateOf(false)
    var currentMediaPos = mutableLongStateOf(0L)
    var displayCutoutWidth = mutableFloatStateOf(0f)
    var pinnedApps = mutableStateListOf<String>("", "", "", "", "", "", "", "")
    var qsTiles = mutableStateListOf<String>("WiFi", "Bluetooth", "Torch", "Location", "Airplane", "DND", "Settings") 

    val islandState = mutableStateOf(IslandState.HIDDEN)
    val activeModel = mutableStateOf<LiveActivityModel?>(null)
    val splitModel = mutableStateOf<LiveActivityModel?>(null) 

    // System Callbacks
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

    // Update Functions
    fun updateAutoBrightnessState(isAuto: Boolean) { hardwareAutoBrightness.value = isAuto }
    fun updateRingerState(mode: Int) { hardwareRingerMode.intValue = mode }
    fun updateHardwareVolume(vol: Int) { hardwareVolume.intValue = vol }
    fun updateHardwareBrightness(bright: Int) { hardwareBrightness.intValue = bright }
    fun updateTicker(pos: Long) { currentMediaPos.longValue = pos }
    fun updateBattery(level: Int, isCharging: Boolean) { globalBatteryLevel.intValue = level; globalIsCharging.value = isCharging }

    // Private Subsystems
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

        // Xposed WindowManager Hack to only register touches over the pills
        try {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
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
            viewTreeObserver.javaClass.getMethod("addOnComputeInternalInsetsListener", listenerClass).invoke(viewTreeObserver, insetsListenerProxy)
        } catch (e: Exception) {}

        composeView.setViewTreeLifecycleOwner(this@DynamicIslandView)
        composeView.setViewTreeViewModelStoreOwner(this@DynamicIslandView)
        composeView.setViewTreeSavedStateRegistryOwner(this@DynamicIslandView)

        composeView.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalContext provides moduleContext, 
                    LocalIslandTheme provides activeTheme.value
                ) {
                    IslandUI(islandState.value)
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
        val displayCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { windowManager?.currentWindowMetrics?.windowInsets?.displayCutout } else null
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density

        flowJob = CoroutineScope(AndroidUiDispatcher.CurrentThread).launch {
            insetsUpdateFlow.debounce(50).collect { this@DynamicIslandView.requestLayout() }
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
        
        // 🧹 Trigger the kill switch safely via the direct reference
        controller?.destroy()
        
        lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
        
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
        context.sendBroadcast(android.content.Intent("com.example.dynamicisland.RESTORE_CLOCK").setPackage("com.android.systemui"))
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration?) {
        super.onConfigurationChanged(newConfig)
        
        // 🚀 FIXED: Ensure Cutouts & Scale remeasure properly when you toggle DPI in Developer Settings
        val displayCutout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { 
            windowManager?.currentWindowMetrics?.windowInsets?.displayCutout 
        } else null
        
        displayCutoutWidth.floatValue = (displayCutout?.boundingRects?.firstOrNull()?.width() ?: 0) / context.resources.displayMetrics.density
    }
}
}
