package com.example.dynamicisland.core.ui.design

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.design.IslandColors
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.core.ui.design.premiumClickable
import com.example.dynamicisland.core.ui.design.geminiAura
import com.example.dynamicisland.shared.model.*
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
    fun heavy() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    fun success() {
        // Double tick for success
        view.postDelayed({
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }, 50)
    fun error() {
        // Triple sharp tick for error
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }, 60)
        }, 120)
    fun toggleOn() {
    fun toggleOff() {
}
@Composable
fun rememberHapticManager(): HapticManager {
    val view = LocalView.current
    return remember(view) { HapticManager(view) }
