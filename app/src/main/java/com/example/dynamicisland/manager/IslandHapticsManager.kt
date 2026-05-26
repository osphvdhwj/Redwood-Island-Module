package com.example.dynamicisland.manager

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.dynamicisland.ipc.IslandState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IslandHapticsManager @Inject constructor(
    private val context: Context,
    private val settingsManager: com.example.dynamicisland.settings.SettingsManager
) {
    private var lastHapticState: IslandState = IslandState.HIDDEN

    fun triggerTransitionHaptic(newState: IslandState, currentCallState: String?, topAppPackage: String) {
        if (!settingsManager.getSettingsState().hapticFeedback) return
        if (newState == lastHapticState) return
        val prev = lastHapticState
        lastHapticState = newState

        val strength = when {
            newState == IslandState.TYPE_3_MAX -> 3
            newState == IslandState.TYPE_2_MID && currentCallState == "RINGING" -> 2
            newState == IslandState.TYPE_0_RING && prev != IslandState.HIDDEN -> 1
            newState.ordinal > prev.ordinal -> 2
            else -> 0
        }
        if (strength > 0) performCustomHaptic(strength, topAppPackage)
    }

    fun performCustomHaptic(strength: Int, topAppPackage: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            if (vibrator == null || !vibrator.hasVibrator()) return

            val timing = longArrayOf(0, (30 * strength).toLong(), 50, (40 * strength).toLong())
            val amplitudes = intArrayOf(0, (100 * strength).coerceAtMost(255), 0, (150 * strength).coerceAtMost(255))
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createWaveform(timing, amplitudes, -1))
            } else {
                vibrator.vibrate(timing, -1)
            }
        } catch (e: Exception) {
            // Gracefully ignore haptic failures to prevent system-wide crashes
        }
    }
}
