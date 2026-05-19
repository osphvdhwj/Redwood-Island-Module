package com.example.dynamicisland.privacy

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.*

/**
 * Automatically clears sensitive clipboard content after a timeout.
 * Detects common patterns like OTP codes, credit card numbers, and passwords.
 */
class ClipboardCleaner(context: Context, private val timeoutMs: Long = 30_000L) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var cleaningJob: Job? = null

    /**
     * Schedule a cleanup if the current clipboard text looks sensitive.
     */
    fun scheduleIfSensitive() {
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        if (isSensitive(text)) {
            cleaningJob?.cancel()
            cleaningJob = CoroutineScope(Dispatchers.Main).launch {
                delay(timeoutMs)
                // Only clear if still the same text (avoids removing an innocent clip)
                val current = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                if (current == text) {
                    clipboard.clearPrimaryClip()
                }
            }
        }
    }

    /**
     * Cancel any pending cleanup (e.g., on user manual paste).
     */
    fun cancel() {
        cleaningJob?.cancel()
    }

    private fun isSensitive(text: String): Boolean {
        return listOf(
            Regex("password|passwort|clave|senha", RegexOption.IGNORE_CASE),
            Regex("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}\\b"),  // credit card 16 digits
            Regex("\\b\\d{6,10}\\b"),                               // OTP or PIN
            Regex("\\b\\d{3}[-.]?\\d{2}[-.]?\\d{4}\\b")             // SSN-like
        ).any { it.containsMatchIn(text) }
    }
}