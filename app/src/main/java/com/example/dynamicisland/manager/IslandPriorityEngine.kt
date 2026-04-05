package com.example.dynamicisland.manager

import android.content.Context
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow

object IslandPriorityEngine {
    fun evaluatePriority(
        context: Context,
        windowManager: WindowManager?,
        topAppPackage: String,
        isPanelExpanded: Boolean,
        currentCall: LiveActivityModel.Call?,
        transientModel: LiveActivityModel?,
        currentMedia: LiveActivityModel.Music?,
        currentHardware: LiveActivityModel.HardwareMonitor?,
        isMediaEnabled: Boolean,
        userForceCollapsed: Boolean,
        currentActiveModel: LiveActivityModel?,
        currentVisualState: IslandState,
        _activeModel: MutableStateFlow<LiveActivityModel?>,
        _splitModel: MutableStateFlow<LiveActivityModel?>,
        _islandState: MutableStateFlow<IslandState>
    ): Boolean {
        
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isGaming = currentHardware?.isGamingModeOn == true

        // 1. Notification Shade takes absolute priority (Hide Island)
        if (isPanelExpanded) {
            _islandState.value = IslandState.HIDDEN
            return userForceCollapsed
        }

        // 2. 📞 CALLS: Always break through, even in games or videos!
        if (currentCall != null) {
            _activeModel.value = currentCall
            _islandState.value = if (userForceCollapsed) IslandState.TYPE_0_RING else IslandState.TYPE_2_MID
            _splitModel.value = null
            return userForceCollapsed
        }

        // 3. 🚨 CRITICAL ALERTS: (Low Battery, OTP) Break through games and videos!
        if (transientModel != null && transientModel.isCritical) {
            _activeModel.value = transientModel
            _islandState.value = IslandState.TYPE_2_MID
            _splitModel.value = null
            return userForceCollapsed
        }

        // 4. 🎬🎮 IMMERSION MODE: Hide idle states for Video and Landscape Games
        if (currentMedia?.isVideo == true || (isGaming && isLandscape)) {
            _islandState.value = IslandState.HIDDEN
            return true
        }

        // 5. 🔔 STANDARD TRANSIENTS: (Downloads, Bluetooth, etc.)
        if (transientModel != null && !transientModel.isCritical) {
            _activeModel.value = transientModel
            _islandState.value = IslandState.TYPE_1_MINI
            // Only show split music pill if it's actually music, not a video in the background
            if (currentMedia != null && isMediaEnabled && currentMedia.isVideo == false) _splitModel.value = currentMedia
            else _splitModel.value = null
            return userForceCollapsed
        }

        // 6. 🎛️ DASHBOARD
        if (currentActiveModel is LiveActivityModel.Dashboard && currentVisualState == IslandState.TYPE_3_MAX) {
            return userForceCollapsed
        }

        // 7. 🎵 MUSIC
        if (currentMedia != null && isMediaEnabled) {
            _activeModel.value = currentMedia
            _splitModel.value = null
            if (currentVisualState == IslandState.TYPE_3_MAX && !userForceCollapsed) {
                _islandState.value = IslandState.TYPE_3_MAX
            } else if (currentMedia.isPlaying && !userForceCollapsed) {
                // 🚀 FIXED: Now uses the MID state for active music!
                _islandState.value = IslandState.TYPE_2_MID 
            } else {
                _islandState.value = IslandState.TYPE_0_RING
            }
            return userForceCollapsed
        }
        
        // 8. 📱 GAMING (Portrait) 
        // If they are gaming in portrait mode, hide the idle ring so it's not distracting
        if (isGaming) {
            _islandState.value = IslandState.HIDDEN
            return true
        }

        // 9. DEFAULT IDLE (Camera Cutout Ring)
        _activeModel.value = null
        _splitModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
        return true
    }
}
