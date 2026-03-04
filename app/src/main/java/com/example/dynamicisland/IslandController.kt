package com.example.dynamicisland
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner


import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.view.WindowManager

class IslandController(private val context: Context) {

    private lateinit var windowManager: WindowManager
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var composeView: ComposeView? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Master States
    private val _islandState = MutableStateFlow(IslandState.TYPE_0_RING)
    val islandState = _islandState.asStateFlow()

    private val _activeModel = MutableStateFlow<LiveActivityModel?>(null)
    val activeModel = _activeModel.asStateFlow()

    // Data Backing Fields
    private var currentMedia: LiveActivityModel.Music? = null
    private var currentHardware: LiveActivityModel.HardwareMonitor? = null
    private var transientModel: LiveActivityModel? = null
    private var transientJob: Job? = null

    // Media Tracking
    private val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private var activeMediaController: MediaController? = null
    private var mediaTickerJob: Job? = null

    init {
        // TEMP SAFETY QUARANTINE: Disabled to isolate SystemUI bootloop
        // setupHardwareMonitor()
        // setupMediaListener()
    }

    /**
     * Binds the Compose UI to this controller.
     */
    fun createIslandView(wm: android.view.WindowManager, params: android.view.WindowManager.LayoutParams): ComposeView {
        this.windowManager = wm
        this.layoutParams = params

        val view = ComposeView(context).apply {
            setContent {
                DynamicIslandView(controller = this@IslandController)
            }
        }

        // Initialize and attach the custom LifecycleOwner
        val lifecycleOwner = RedwoodLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        view.setViewTreeLifecycleOwner(lifecycleOwner)
        view.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        this.composeView = view
        return view
    }

    // THE FIX 4 INTEGRATION (With Hidden State Protection)
    fun updateWindowBounds(targetWidthDp: Float, targetHeightDp: Float) {
        if (composeView == null || !composeView!!.isAttachedToWindow) return

        val density = context.resources.displayMetrics.density

        if (targetWidthDp == 0f || targetHeightDp == 0f) {
            // HIDDEN STATE: Shrink the physical window to 0 so it consumes NO touches.
            layoutParams.width = 0
            layoutParams.height = 0
        } else {
            // ACTIVE STATES: Apply your PR #34 math to prevent clipping.
            layoutParams.width = (targetWidthDp * density).toInt() + (120 * density).toInt()
            layoutParams.height = (targetHeightDp * density).toInt() + (150 * density).toInt()
        }

        try {
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            // Silently catch layout update limits to prevent SystemUI panics
        }
    }



    // --- STATE ROUTING & PRIORITIES ---

    private fun evaluatePriority() {
        // 1. Transients (Charging, Calls) override everything temporarily
        if (transientModel != null) {
            _activeModel.value = transientModel
            _islandState.value = IslandState.TYPE_2_MID
            return
        }

        // 2. Media has next highest priority
        if (currentMedia != null && currentMedia?.isPlaying == true) {
            _activeModel.value = currentMedia
            // We default to Mid. Max is only triggered by user interaction.
            if (_islandState.value != IslandState.TYPE_3_MAX) {
                _islandState.value = IslandState.TYPE_2_MID
            }
            return
        }

        // 3. Hardware / Gaming Monitor
        if (currentHardware?.isGamingModeOn == true) {
            _activeModel.value = currentHardware
            _islandState.value = IslandState.TYPE_1_MINI
            return
        }

        // Fallback to Ring if nothing is active
        _activeModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
    }

    // --- TRANSIENT (BATTERY) FIX ---

    fun postTransientNotification(model: LiveActivityModel, durationMs: Long = 5000L) {
        // Critical Fix: Cancel any existing timer so we don't get stuck in a loop
        transientJob?.cancel()

        transientModel = model
        evaluatePriority()

        transientJob = scope.launch {
            delay(durationMs)
            transientModel = null
            evaluatePriority() // Recalculate without the transient
        }
    }

    // --- MEDIA ENGINE ---

    private fun setupMediaListener() {
        try {
            // Requires Notification Listener permission or System/Root privileges via Xposed
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateActiveMediaController(controllers?.firstOrNull())
            }, ComponentName(context, "com.example.dynamicisland.DummyListener"))

            updateActiveMediaController(mediaSessionManager.getActiveSessions(ComponentName(context, "com.example.dynamicisland.DummyListener")).firstOrNull())
        } catch (e: Exception) {
            Log.e("DynamicIsland", "Media Listener failed (Permissions missing?)", e)
        }
    }

    private fun updateActiveMediaController(controller: MediaController?) {
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controller

        if (controller == null) {
            currentMedia = null
            stopMediaTicker()
            evaluatePriority()
            return
        }

        controller.registerCallback(mediaCallback)
        extractMediaData(controller)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            extractMediaData(activeMediaController)
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            extractMediaData(activeMediaController)
        }
    }

    private fun extractMediaData(controller: MediaController?) {
        if (controller == null) return
        val metadata = controller.metadata
        val pbState = controller.playbackState ?: return

        val isPlaying = pbState.state == PlaybackState.STATE_PLAYING
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

        // Extract exact Custom Actions provided by the app (e.g., Spotify's Heart)
        val extractedCustomActions = pbState.customActions.map { action ->
            CustomMediaAction(
                actionName = action.name.toString(),
                icon = null, // We'll handle cross-package icon rendering in the UI layer
                pendingIntent = null, // Can invoke via activeMediaController.transportControls.sendCustomAction()
                isEnabled = true
            )
        }

        currentMedia = LiveActivityModel.Music(
            id = "media_main",
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
            isPlaying = isPlaying,
            durationMs = duration,
            positionMs = pbState.position,
            appPackageName = controller.packageName,
            customActions = extractedCustomActions
        )

        if (isPlaying) startMediaTicker() else stopMediaTicker()
        evaluatePriority()
    }

    // Runs only when media is playing to feed the WavySlider
    private fun startMediaTicker() {
        mediaTickerJob?.cancel()
        mediaTickerJob = scope.launch {
            while (isActive) {
                activeMediaController?.playbackState?.position?.let { pos ->
                    currentMedia = currentMedia?.copy(positionMs = pos)
                    // We directly update the flow to bypass the priority evaluator for micro-updates
                    if (_activeModel.value is LiveActivityModel.Music) {
                        _activeModel.value = currentMedia
                    }
                }
                delay(1000) // Update slider every second
            }
        }
    }

    private fun stopMediaTicker() {
        mediaTickerJob?.cancel()
    }

    // --- USER INTENTS (BUTTONS & GESTURES) ---

    fun onIslandTapped() {
        // Point 1 Fix: Open App directly if in Ring/Mini/Mid
        val current = _activeModel.value
        if (_islandState.value != IslandState.TYPE_3_MAX && current is LiveActivityModel.Music) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(current.appPackageName)
                launchIntent?.let { context.startActivity(it) }
            } catch (e: Exception) {
                Log.e("DynamicIsland", "Failed to launch app", e)
            }
        } else {
            // Expand to Max Dashboard
            _islandState.value = IslandState.TYPE_3_MAX
        }
    }

    fun onDoubleTap() {
        // Point 13 Fix: Instantly collapse MAX to MINI
        if (_islandState.value == IslandState.TYPE_3_MAX) {
            _islandState.value = IslandState.TYPE_1_MINI
        }
    }

    fun onTripleTap() {
        // Point 13 Fix: Instantly collapse to RING
        _islandState.value = IslandState.TYPE_0_RING
    }

    fun onSwipeUp() {
        // Cleanly dismiss current transient or collapse state
        if (transientModel != null) {
            transientJob?.cancel()
            transientModel = null
            evaluatePriority()
        } else {
            _islandState.value = IslandState.TYPE_0_RING
        }
    }

    fun launchOutputSwitcher() {
        try {
            // Point 10 Fix: FLAG_ACTIVITY_NEW_TASK is legally required from SystemUI
            val intent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Trigger volume panel
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
        }
    }

    fun sendMediaCommand(command: String, customAction: CustomMediaAction? = null) {
        val controls = activeMediaController?.transportControls ?: return
        when (command) {
            "PLAY" -> controls.play()
            "PAUSE" -> controls.pause()
            "NEXT" -> controls.skipToNext()
            "PREV" -> controls.skipToPrevious()
            "CUSTOM" -> customAction?.let { controls.sendCustomAction(it.actionName, null) }
        }
    }

    // --- HARDWARE SUBSCRIPTION ---

    private fun setupHardwareMonitor() {
        scope.launch {
            HardwareMonitors.startMonitoring().collect { hw ->
                currentHardware = hw
                // Only re-evaluate if it crosses the gaming threshold so it doesn't spam the UI
                if (hw.isGamingModeOn || _activeModel.value is LiveActivityModel.HardwareMonitor) {
                    evaluatePriority()
                }
            }
        }
    }

    fun cleanup() {
        scope.cancel()
    }
}
