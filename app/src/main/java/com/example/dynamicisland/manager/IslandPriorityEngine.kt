package com.example.dynamicisland.manager

import android.content.Context
import android.view.WindowManager
import com.example.dynamicisland.model.*
import com.example.dynamicisland.ipc.IslandState

data class PriorityResult(
    val islandState: IslandState,
    val activeModel: LiveActivityModel?,
    val splitModel: LiveActivityModel?,
    val userForceCollapsed: Boolean
)

object IslandPriorityEngine {
    fun evaluatePriority(
        context: Context,
        windowManager: WindowManager?,
        topAppPackage: String,
        isPanelExpanded: Boolean,
        currentCall: LiveActivityModel.Call?,
        transientModel: LiveActivityModel?,
        activeExternalActivity: LiveActivityModel.ExternalActivity? = null,
        currentMedia: LiveActivityModel.Music?,
        currentHardware: LiveActivityModel.HardwareMonitor?,
        isMediaEnabled: Boolean,
        userForceCollapsed: Boolean,
        currentActiveModel: LiveActivityModel?,
        currentVisualState: IslandState
    ): PriorityResult {
        
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isGaming = currentHardware?.isGamingModeOn == true

        // 1. Notification Shade takes absolute priority (Hide Island)
        if (isPanelExpanded) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, userForceCollapsed)
        }

        // 2. 📞 CALLS: Always break through, even in games or videos!
        if (currentCall != null) {
            return PriorityResult(
                if (userForceCollapsed) IslandState.TYPE_0_RING else IslandState.TYPE_2_MID,
                currentCall,
                null,
                userForceCollapsed
            )
        }

        // 3. 🚨 CRITICAL ALERTS: (Low Battery, OTP) Break through games and videos!
        if (transientModel != null && transientModel.isCritical) {
            return PriorityResult(IslandState.TYPE_2_MID, transientModel, null, userForceCollapsed)
        }

        // 4. 🎬🎮 IMMERSION MODE: Hide idle states for Video and Landscape Games
        if (currentMedia?.isVideo == true || (isGaming && isLandscape)) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, true)
        }

        // 5. 🔔 STANDARD TRANSIENTS: (Downloads, Bluetooth, etc.)
        if (transientModel != null && !transientModel.isCritical) {
            val split = if (currentMedia != null && isMediaEnabled && currentMedia.isVideo == false) currentMedia else null
            return PriorityResult(IslandState.TYPE_1_MINI, transientModel, split, userForceCollapsed)
        }

        // 6. 🎛️ DASHBOARD
        if (currentActiveModel is LiveActivityModel.Dashboard && currentVisualState == IslandState.TYPE_3_MAX) {
            return PriorityResult(currentVisualState, currentActiveModel, null, userForceCollapsed)
        }

        // 7. 🎵 MUSIC
        if (currentMedia != null && isMediaEnabled) {
            val state = if (currentVisualState == IslandState.TYPE_3_MAX && !userForceCollapsed) {
                IslandState.TYPE_3_MAX
            } else if (currentMedia.isPlaying && !userForceCollapsed) {
                IslandState.TYPE_2_MID 
            } else {
                IslandState.TYPE_0_RING
            }
            return PriorityResult(state, currentMedia, null, userForceCollapsed)
        }
        
        // 7.5 EXTERNAL ACTIVITIES (via SDK)
        if (activeExternalActivity != null) {
            return PriorityResult(IslandState.TYPE_2_MID, activeExternalActivity, null, userForceCollapsed)
        }

        // 8. 📱 GAMING (Portrait) 
        if (isGaming) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, true)
        }

        // 9. DEFAULT IDLE (Camera Cutout Ring)
        return PriorityResult(IslandState.TYPE_0_RING, null, null, true)
    }
}
