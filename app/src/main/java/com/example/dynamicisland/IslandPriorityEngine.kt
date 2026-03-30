package com.example.dynamicisland

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
        
        // 🎬 Auto-Hide if watching a video in Landscape
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape && currentMedia?.isVideo == true) {
            _islandState.value = IslandState.HIDDEN
            return true
        }

        if (isPanelExpanded) {
            _islandState.value = IslandState.HIDDEN
            return userForceCollapsed
        }

        if (currentCall != null) {
            _activeModel.value = currentCall
            _islandState.value = if (userForceCollapsed) IslandState.TYPE_0_RING else IslandState.TYPE_2_MID
            _splitModel.value = null
            return userForceCollapsed
        }

        if (transientModel != null) {
            _activeModel.value = transientModel
            if (transientModel.isCritical && !userForceCollapsed) _islandState.value = IslandState.TYPE_2_MID
            else _islandState.value = IslandState.TYPE_1_MINI
            
            if (currentMedia != null && isMediaEnabled) _splitModel.value = currentMedia
            else _splitModel.value = null
            return userForceCollapsed
        }

        if (currentActiveModel is LiveActivityModel.Dashboard && currentVisualState == IslandState.TYPE_3_MAX) {
            return userForceCollapsed
        }

        if (currentMedia != null && isMediaEnabled) {
            _activeModel.value = currentMedia
            _splitModel.value = null
            if (currentVisualState == IslandState.TYPE_3_MAX && !userForceCollapsed) {
                _islandState.value = IslandState.TYPE_3_MAX
            } else if (currentMedia.isPlaying && !userForceCollapsed) {
                _islandState.value = IslandState.TYPE_1_MINI
            } else {
                _islandState.value = IslandState.TYPE_0_RING
            }
            return userForceCollapsed
        }

        if (currentHardware?.isGamingModeOn == true) {
            _activeModel.value = currentHardware
            _islandState.value = IslandState.TYPE_1_MINI
            _splitModel.value = null
            return false
        }

        _activeModel.value = null
        _splitModel.value = null
        _islandState.value = IslandState.TYPE_0_RING
        return true
    }
}
