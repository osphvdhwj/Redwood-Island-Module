package com.example.dynamicisland.core.ui.design

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*

/**
 * Premium Haptic Manager for UI interactions.
 * Provides consistent, high-quality tactile feedback.
 */
class HapticManager(private val view: android.view.View) {
    
    fun light() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun medium() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun heavy() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun success() {
        // Double tick for success
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        view.postDelayed({
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }, 50)
    }

    fun error() {
        // Triple sharp tick for error
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        view.postDelayed({
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }, 60)
        view.postDelayed({
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }, 120)
    }
    
    fun toggleOn() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    
    fun toggleOff() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberHapticManager(): HapticManager {
    val view = LocalView.current
    return remember(view) { HapticManager(view) }
}
