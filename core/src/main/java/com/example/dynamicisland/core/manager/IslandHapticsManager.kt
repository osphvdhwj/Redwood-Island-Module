package com.example.dynamicisland.core.manager

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.dynamicisland.core.settings.SettingsManager
import javax.inject.Inject
import javax.inject.Singleton
import com.example.dynamicisland.shared.model.IslandState

/**
 * 📳 ISLAND HAPTICS MANAGER
 * 
 * Provides tactile feedback for Island transitions and gestures.
 */
@Singleton
class IslandHapticsManager @Inject constructor(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun triggerTransitionHaptic(newState: IslandState, oldState: IslandState?, pkg: String?) {
        if (!settingsManager.getSettingsState().hapticFeedback) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (newState) {
                IslandState.TYPE_3_MAX -> VibrationEffect.createOneShot(20, 255)
                IslandState.TYPE_2_MID -> VibrationEffect.createOneShot(15, 200)
                else -> VibrationEffect.createOneShot(10, 150)
            }
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(15)
        }
    }
}
