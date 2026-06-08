package com.example.dynamicisland.core.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.dynamicisland.core.settings.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import kotlinx.coroutines.*

/**
 * Feature #135 / #158: Automated Privacy Clipboard Cleaner Engine.
 * Automatically clears sensitive items like OTP codes, credit cards, and credentials after a timeout.
 */
@Singleton
class ClipboardCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var cleaningJob: Job? = null

    // Pattern matching arrays for explicit token checking
    private val sensitivePatterns = listOf(
        Regex("password|passwort|clave|senha|secret|token", RegexOption.IGNORE_CASE),
        Regex("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b"), // 16-Digit Cards
        Regex("\\b\\d{4,8}\\b"),                                       // Explicit 4-8 Digit OTP/PIN Codes
        Regex("\\b[A-Z0-9]{6,10}\\b")                                  // Alpha-numeric Transaction Hashes
    )

    /**
     * Inspects the active clipboard stream and schedules a secure deletion if matches occur.
     */
    fun scheduleIfSensitive() {
        val currentSettings = settingsManager.getSettingsState()
        
        // Safety switch verification from Pillar 4 settings
        if (!currentSettings.clipboardCleaner) return

        val primaryClip = clipboard.primaryClip ?: return
        if (primaryClip.itemCount == 0) return
        
        val text = primaryClip.getItemAt(0).text?.toString() ?: return

        if (isSensitive(text)) {
            cleaningJob?.cancel()
            cleaningJob = scope.launch {
                val delayDuration = 60000L
                delay(delayDuration)

                // Verify the block hasn't been modified by user interventions during sleep cycle
                val freshClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (freshClip == text) {
                    // Execute secure erasure via empty assignment
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }
    }

    /**
     * Aborts pending destruction coroutines.
     */
    fun cancelPendingClear() {
        cleaningJob?.cancel()
    }

    private fun isSensitive(text: String): Boolean {
        return sensitivePatterns.any { it.containsMatchIn(text) }
    }
}