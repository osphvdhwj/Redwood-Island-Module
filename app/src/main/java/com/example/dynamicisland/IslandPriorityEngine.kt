package com.example.dynamicisland

import android.content.Context
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow

object IslandPriorityEngine {

    // 🎛️ Deterministic Priority Weights
    fun LiveActivityModel.getPriorityWeight(): Int {
        return when (this) {
            is LiveActivityModel.Call -> if (state == "RINGING") 100 else 90
            is LiveActivityModel.SystemAlert -> if (alertType == "THERMAL") 85 else 60
            is LiveActivityModel.Charging -> if (isCritical) 80 else 40 
            is LiveActivityModel.AppTimerWarning -> 70
            is LiveActivityModel.OngoingTask -> 50
            is LiveActivityModel.Music -> 20
            is LiveActivityModel.RealityPill -> 10
            is LiveActivityModel.General -> 5
            is LiveActivityModel.HardwareMonitor -> 1
            else -> 0
        }
    }

    // 🎛️ Weighted State Machine Router with User State Protection
    fun evaluatePriority(
        context: Context,
        windowManager: WindowManager?,
        topAppPackage: String,
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
        val rotation = try { windowManager?.defaultDisplay?.rotation ?: 0 } catch(e: Throwable) { 0 }
        val isLandscapeNow = rotation == android.view.Surface.ROTATION_90 || rotation == android.view.Surface.ROTATION_270
        
        val prefs = context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)
        val blacklistedGames = prefs.getString("gaming_blacklist", "com.dts.freefiremax,com.tencent.ig") ?: ""
        val isBlacklistedAppActive = topAppPackage.isNotEmpty() && blacklistedGames.contains(topAppPackage)
        val shouldHideLandscape = isLandscapeNow && prefs.getBoolean("hide_landscape", false)

        // 1. Gather all currently active states
        val activeCandidates = listOfNotNull(
            currentCall,
            transientModel,
            if (isMediaEnabled) currentMedia else null
        ).sortedByDescending { it.getPriorityWeight() }

        val dominantModel = activeCandidates.firstOrNull()

        // 2. Global Hide Overrides (Games/Video)
        if ((shouldHideLandscape || currentHardware?.isGamingModeOn == true || isBlacklistedAppActive)) {
            // ONLY pierce the game overlay if the weight is critical (80+)
            if (dominantModel == null || dominantModel.getPriorityWeight() < 80) {
                _islandState.value = IslandState.HIDDEN
                return userForceCollapsed
            }
        }

        // Protect User's Manual Dashboard
        if (currentActiveModel is LiveActivityModel.Dashboard && currentVisualState != IslandState.TYPE_0_RING && currentVisualState != IslandState.HIDDEN) {
            if (dominantModel == null || dominantModel.getPriorityWeight() < 80) {
                return userForceCollapsed 
            }
        }

        var newUserForceCollapsed = userForceCollapsed

        // 3. Apply the Dominant State
        if (dominantModel != null) {
            _activeModel.value = dominantModel
            
            // 4. Handle Split-Screen (Cube) Logic
            val secondaryModel = activeCandidates.drop(1).firstOrNull { it is LiveActivityModel.Music || it is LiveActivityModel.Charging }
            
            if (secondaryModel != null && dominantModel.getPriorityWeight() >= 80) {
                _splitModel.value = secondaryModel
                _islandState.value = IslandState.TYPE_SPLIT
            } else {
                _splitModel.value = null
                
                // Protect User's Expanded Pill (Mid/Max)
                val isSameBaseModel = currentActiveModel?.javaClass == dominantModel.javaClass
                val isExpanded = currentVisualState == IslandState.TYPE_2_MID || currentVisualState == IslandState.TYPE_3_MAX
                
                if (isSameBaseModel && isExpanded && !userForceCollapsed) {
                    // Do nothing! Respect the user's manual expansion.
                } else {
                    // Route to correct default visual state
                    _islandState.value = when {
                        userForceCollapsed -> IslandState.TYPE_0_RING
                        dominantModel is LiveActivityModel.SystemAlert || dominantModel is LiveActivityModel.Charging || dominantModel is LiveActivityModel.AppTimerWarning -> IslandState.TYPE_2_MID
                        dominantModel is LiveActivityModel.Call && dominantModel.state == "RINGING" -> IslandState.TYPE_1_MINI
                        dominantModel is LiveActivityModel.Call && dominantModel.state == "ONGOING" -> IslandState.TYPE_1_MINI 
                        dominantModel is LiveActivityModel.Music -> IslandState.TYPE_1_MINI
                        else -> IslandState.TYPE_1_MINI
                    }
                }
            }
        } else {
            // Nothing active
            if (currentActiveModel !is LiveActivityModel.Dashboard) {
                _activeModel.value = null
                _splitModel.value = null
                _islandState.value = IslandState.TYPE_0_RING
            }
        }
        return newUserForceCollapsed
    }
}
