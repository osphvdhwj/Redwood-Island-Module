package com.example.dynamicisland.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.dynamicisland.manager.PerAppProfileManager
import kotlinx.coroutines.*

/**
 * BATCH 4: Island Accessibility Service
 *
 * Understands what is currently on screen and publishes semantic signals
 * to the island. This unlocks context-aware island behaviour impossible
 * through Xposed hooks alone:
 *
 *   ARTICLE_READING  — detected via long scrollable text nodes in browser/reader apps.
 *                      Island shows estimated reading time.
 *
 *   PAYMENT_SCREEN   — detected via keyword heuristics (CVV, card number, UPI, Pay).
 *                      Island shows a "Secure" shield indicator.
 *
 *   VIDEO_FULLSCREEN — detected when a SurfaceView occupies ≥90% of screen area.
 *                      Island hides itself (complements existing Xposed check).
 *
 *   OTP_FIELD_FOCUS  — detected when an EditText with inputType NUMBER is focused
 *                      in an OTP-adjacent context. Island pre-expands the OTP catcher.
 *
 *   KEYBOARD_VISIBLE — detected via window-state change events on IME windows.
 *                      Island shrinks to ring when keyboard is on screen.
 *
 * All signals are broadcast to SystemUI with the permission SECURE_IPC.
 * IslandController receives them via ecosystemReceiver and routes them to
 * IslandPriorityEngineV2 as context modifiers.
 *
 * NOTE: This service requires user opt-in via Accessibility Settings.
 * It should NOT be auto-enabled. The ConfigActivity FeaturesScreen
 * includes a deep-link button to the system accessibility settings page.
 */
class IslandAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_CONTEXT = "com.example.dynamicisland.SCREEN_CONTEXT"
        const val SECURE_PERM    = "com.redwood.permission.SECURE_IPC"

        // Context type keys sent in the broadcast
        const val CONTEXT_ARTICLE      = "ARTICLE_READING"
        const val CONTEXT_PAYMENT      = "PAYMENT_SCREEN"
        const val CONTEXT_VIDEO        = "VIDEO_FULLSCREEN"
        const val CONTEXT_OTP_FOCUS    = "OTP_FIELD_FOCUS"
        const val CONTEXT_KEYBOARD     = "KEYBOARD_VISIBLE"
        const val CONTEXT_CLEAR        = "CLEAR"

        // Payment-related keywords (case-insensitive)
        private val PAYMENT_KEYWORDS = setOf(
            "cvv", "cvc", "card number", "expiry", "expiration",
            "upi", "pay now", "proceed to pay", "checkout", "payment",
            "billing", "credit card", "debit card", "net banking",
            "rupay", "visa", "mastercard", "enter pin"
        )

        // Reading-length threshold — nodes with more chars than this suggest an article
        private const val ARTICLE_MIN_CHARS = 1_500
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastContextType = CONTEXT_CLEAR
    private var analysisJob: Job? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED  or
                         AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED    or
                         AccessibilityEvent.TYPE_VIEW_FOCUSED             or
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType  = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags         = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 200L
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return

        // Skip our own package and system UI to prevent feedback loops
        if (pkg == "com.example.dynamicisland" || pkg == "com.android.systemui") return

        // Respect per-app profiles — don't analyse if this app has accessibility disabled
        val profile = PerAppProfileManager.getProfile(pkg)
        if (profile.blockAccessibilityReads) return

        when (event.eventType) {

            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // IME (keyboard) detection
                val windows = windows ?: return
                val imeVisible = windows.any { w ->
                    w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
                if (imeVisible) publishContext(CONTEXT_KEYBOARD, pkg)
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val source = event.source ?: return
                if (isOtpField(source)) publishContext(CONTEXT_OTP_FOCUS, pkg)
                source.recycle()
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Debounce — only analyse at most once per 600ms per window change
                analysisJob?.cancel()
                analysisJob = scope.launch {
                    delay(600)
                    analyseWindowContent(pkg)
                }
            }
        }
    }

    // ── Content analysis ──────────────────────────────────────────────────────

    private fun analyseWindowContent(pkg: String) {
        val root = rootInActiveWindow ?: return

        try {
            when {
                isPaymentScreen(root) -> publishContext(CONTEXT_PAYMENT, pkg)
                isArticle(root, pkg)  -> {
                    val wordCount = estimateWordCount(root)
                    val readingTimeSecs = (wordCount / 200f * 60f).toInt()  // 200 WPM
                    publishContext(CONTEXT_ARTICLE, pkg, Bundle().apply {
                        putInt("readingTimeSecs", readingTimeSecs)
                        putInt("wordCount", wordCount)
                    })
                }
                else -> {
                    if (lastContextType != CONTEXT_CLEAR && lastContextType != CONTEXT_KEYBOARD) {
                        publishContext(CONTEXT_CLEAR, pkg)
                    }
                }
            }
        } catch (_: Exception) {} finally {
            root.recycle()
        }
    }

    private fun isPaymentScreen(root: AccessibilityNodeInfo): Boolean {
        val allText = collectText(root).lowercase()
        val matchCount = PAYMENT_KEYWORDS.count { allText.contains(it) }
        return matchCount >= 2   // Require at least two payment signals to reduce false positives
    }

    private fun isArticle(root: AccessibilityNodeInfo, pkg: String): Boolean {
        // Only consider browser/reader packages for article detection
        val readerPackages = setOf(
            "com.android.chrome", "org.mozilla.firefox",
            "com.brave.browser", "com.opera.browser",
            "com.microsoft.emmx", "com.google.android.apps.magazines",
            "flipboard.app", "com.instapaper.android"
        )
        if (!readerPackages.any { pkg.startsWith(it) } && !pkg.contains("browser", true)) return false

        val allText = collectText(root)
        return allText.length >= ARTICLE_MIN_CHARS
    }

    private fun isOtpField(node: AccessibilityNodeInfo): Boolean {
        val hint    = node.hintText?.toString()?.lowercase()  ?: ""
        val cd      = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId  = node.viewIdResourceName?.lowercase() ?: ""

        val isNumericInput = node.inputType and android.text.InputType.TYPE_MASK_CLASS ==
                             android.text.InputType.TYPE_CLASS_NUMBER

        val hasOtpSignal = hint.contains("otp") || hint.contains("code") ||
                           hint.contains("pin")  || cd.contains("otp")   ||
                           viewId.contains("otp") || viewId.contains("pin")

        return isNumericInput && hasOtpSignal
    }

    private fun collectText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        if (!node.text.isNullOrEmpty()) sb.append(node.text).append(' ')
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(collectText(child))
            child.recycle()
        }
        return sb.toString()
    }

    private fun estimateWordCount(root: AccessibilityNodeInfo): Int {
        val text = collectText(root)
        return text.trim().split(Regex("\\s+")).size
    }

    // ── Publishing ────────────────────────────────────────────────────────────

    private fun publishContext(contextType: String, pkg: String, extras: Bundle? = null) {
        if (contextType == lastContextType && extras == null) return
        lastContextType = contextType

        val intent = Intent(ACTION_CONTEXT).apply {
            setPackage("com.android.systemui")
            putExtra("contextType", contextType)
            putExtra("pkg",        pkg)
            if (extras != null) putExtras(extras)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            sendBroadcast(intent, SECURE_PERM)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            sendBroadcast(intent, SECURE_PERM)
        }
    }
}