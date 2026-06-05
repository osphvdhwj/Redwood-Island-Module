package com.example.dynamicisland.core.manager

import android.content.Context
import android.view.WindowManager
import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.shared.model.IslandState
import com.example.dynamicisland.shared.settings.SettingsState

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
        currentWeather: LiveActivityModel.WeatherMood? = null,
        isMediaEnabled: Boolean,
        userForceCollapsed: Boolean,
        currentActiveModel: LiveActivityModel?,
        currentVisualState: IslandState,
        settings: SettingsState = SettingsState()
    ): PriorityResult {
        
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isGaming = currentHardware?.isGamingModeOn == true

        // 1. Notification Shade takes absolute priority (Hide Island)
        if (isPanelExpanded) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, userForceCollapsed)
        }

        // 2. 📞 CALLS
        if (currentCall != null) {
            val baseState = if (userForceCollapsed) IslandState.TYPE_0_RING else IslandState.TYPE_2_MID
            return PriorityResult(
                constrainState(baseState, currentCall, settings),
                currentCall,
                null,
                userForceCollapsed
            )
        }

        // 3. 🚨 CRITICAL ALERTS
        if (transientModel != null && transientModel.isCritical) {
            return PriorityResult(
                constrainState(IslandState.TYPE_2_MID, transientModel, settings),
                transientModel,
                null,
                userForceCollapsed
            )
        }

        // 4. 🎬🎮 IMMERSION MODE
        if (currentMedia?.isVideo == true || (isGaming && isLandscape)) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, true)
        }

        // 5. 🔔 STANDARD TRANSIENTS
        if (transientModel != null && !transientModel.isCritical) {
            val split = if (currentMedia != null && isMediaEnabled && currentMedia.isVideo == false) currentMedia else null
            return PriorityResult(
                constrainState(IslandState.TYPE_1_MINI, transientModel, settings),
                transientModel,
                split,
                userForceCollapsed
            )
        }

        // 6. 🎛️ DASHBOARD
        if (currentActiveModel is LiveActivityModel.Dashboard && currentVisualState == IslandState.TYPE_3_MAX) {
            return PriorityResult(currentVisualState, currentActiveModel, null, userForceCollapsed)
        }

        // 7. 🎵 MUSIC
        if (currentMedia != null && isMediaEnabled) {
            val baseState = if (currentVisualState == IslandState.TYPE_3_MAX && !userForceCollapsed) {
                IslandState.TYPE_3_MAX
            } else if (currentMedia.isPlaying && !userForceCollapsed) {
                IslandState.TYPE_2_MID 
            } else {
                IslandState.TYPE_0_RING
            }
            return PriorityResult(
                constrainState(baseState, currentMedia, settings),
                currentMedia,
                null,
                userForceCollapsed
            )
        }
        
        // 7.5 EXTERNAL ACTIVITIES
        if (activeExternalActivity != null) {
            return PriorityResult(
                constrainState(IslandState.TYPE_2_MID, activeExternalActivity, settings),
                activeExternalActivity,
                null,
                userForceCollapsed
            )
        }

        // 8. 📱 GAMING (Portrait) 
        if (isGaming) {
            return PriorityResult(IslandState.HIDDEN, currentActiveModel, null, true)
        }

        // 9. 🌤️ WEATHER (Ambient Mood)
        if (currentWeather != null) {
            return PriorityResult(IslandState.TYPE_0_RING, currentWeather, null, true)
        }

        // 10. DEFAULT IDLE (Nav Island / Camera Cutout Ring)
        return if (settings.navIslandMode) {
             // 🌓 NAV ISLAND PILLAR: Persistent App Launcher when idle
             PriorityResult(IslandState.TYPE_1_MINI, LiveActivityModel.Dashboard(pinnedApps = settings.pinnedApps.toList()), null, false)
        } else {
             PriorityResult(IslandState.TYPE_0_RING, null, null, true)
        }
    }

    private fun constrainState(
        target: IslandState,
        model: LiveActivityModel,
        settings: SettingsState
    ): IslandState {
        var current = target
        while (current != IslandState.HIDDEN) {
            val allowed = when (model) {
                is LiveActivityModel.Music -> when (current) {
                    IslandState.TYPE_2_MID -> settings.allowMusicMid
                    IslandState.TYPE_3_MAX -> settings.allowMusicMax
                    else -> true
                }
                is LiveActivityModel.Charging -> when (current) {
                    IslandState.TYPE_1_MINI -> settings.allowChargingMini
                    IslandState.TYPE_2_MID -> settings.allowChargingMid
                    else -> true
                }
                is LiveActivityModel.NotificationStack -> when (current) {
                    IslandState.TYPE_1_MINI -> settings.allowNotifMini
                    IslandState.TYPE_2_MID -> settings.allowNotifMid
                    IslandState.TYPE_3_MAX -> settings.allowNotifMax
                    else -> true
                }
                is LiveActivityModel.Call -> when (current) {
                    IslandState.TYPE_2_MID -> settings.allowCallMid
                    IslandState.TYPE_3_MAX -> settings.allowCallMax
                    else -> true
                }
                is LiveActivityModel.OngoingTask -> when (current) {
                    IslandState.TYPE_1_MINI -> settings.allowTaskMini
                    IslandState.TYPE_2_MID -> settings.allowTaskMid
                    else -> true
                }
                else -> true
            }

            if (allowed) return current
            
            // Downgrade
            current = when (current) {
                IslandState.TYPE_3_MAX -> IslandState.TYPE_2_MID
                IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI
                IslandState.TYPE_1_MINI -> IslandState.TYPE_0_RING
                IslandState.TYPE_0_RING -> IslandState.HIDDEN
                else -> IslandState.HIDDEN
            }
        }
        return current
    }
}
