package com.example.dynamicisland.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.dynamicisland.core.manager.PerAppProfileManager
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
import kotlinx.coroutines.*

/**
 * 📦 ISLAND ACCESSIBILITY SERVICE
 * 
 * Extracts semantic meaning from the active window and streams it to the Brain.
 */
class IslandAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_BRAIN_EVENT = "com.example.dynamicisland.BRAIN_EVENT"
        const val SECURE_PERM        = "com.redwood.permission.SECURE_IPC"

        const val CONTEXT_ARTICLE      = "ARTICLE_READING"
        const val CONTEXT_PAYMENT      = "PAYMENT_SCREEN"
        const val CONTEXT_VIDEO        = "VIDEO_FULLSCREEN"
        const val CONTEXT_OTP_FOCUS    = "OTP_FIELD_FOCUS"
        const val CONTEXT_KEYBOARD     = "KEYBOARD_VISIBLE"
        const val CONTEXT_CLEAR        = "CLEAR"

        private val PAYMENT_KEYWORDS = setOf(
            "cvv", "cvc", "card number", "expiry", "expiration",
            "upi", "pay now", "proceed to pay", "checkout", "payment",
            "billing", "credit card", "debit card", "net banking",
            "rupay", "visa", "mastercard", "enter pin"
        )

        private const val ARTICLE_MIN_CHARS = 1_500
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastContextType = CONTEXT_CLEAR
    private var analysisJob: Job? = null

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

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.example.dynamicisland.core" || pkg == "com.android.systemui") return

        val profile = PerAppProfileManager.getProfile(pkg)
        if (profile.blockAccessibilityReads) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
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
                analysisJob?.cancel()
                analysisJob = scope.launch {
                    delay(600)
                    analyseWindowContent(pkg)
                }
            }
        }
    }

    private fun analyseWindowContent(pkg: String) {
        val root = rootInActiveWindow ?: return
        try {
            when {
                isPaymentScreen(root) -> publishContext(CONTEXT_PAYMENT, pkg)
                isArticle(root, pkg)  -> {
                    val wordCount = estimateWordCount(root)
                    val readingTimeSecs = (wordCount / 200f * 60f).toInt()
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
        return PAYMENT_KEYWORDS.count { allText.contains(it) } >= 2
    }

    private fun isArticle(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val readerPackages = setOf("com.android.chrome", "org.mozilla.firefox", "com.brave.browser")
        if (!readerPackages.any { pkg.startsWith(it) } && !pkg.contains("browser", true)) return false
        return collectText(root).length >= ARTICLE_MIN_CHARS
    }

    private fun isOtpField(node: AccessibilityNodeInfo): Boolean {
        val hint = node.hintText?.toString()?.lowercase() ?: ""
        val isNumeric = node.inputType and android.text.InputType.TYPE_MASK_CLASS == android.text.InputType.TYPE_CLASS_NUMBER
        return isNumeric && (hint.contains("otp") || hint.contains("code"))
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

    private fun estimateWordCount(root: AccessibilityNodeInfo): Int = collectText(root).trim().split(Regex("\\s+")).size

    private fun publishContext(contextType: String, pkg: String, extras: Bundle? = null) {
        if (contextType == lastContextType && extras == null) return
        lastContextType = contextType

        val intent = Intent(ACTION_BRAIN_EVENT).apply {
            setPackage("com.example.dynamicisland.core")
            putExtra("action", "SCREEN_CONTEXT")
            putExtra("contextType", contextType)
            putExtra("pkg", pkg)
            if (extras != null) putExtras(extras)
        }
        sendBroadcast(intent, SECURE_PERM)
    }
}
