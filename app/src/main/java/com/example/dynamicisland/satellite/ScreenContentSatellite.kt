package com.example.dynamicisland.satellite

import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

/**
 * 👁️ SCREEN CONTENT SATELLITE (Feature B)
 *
 * A ghost satellite that uses accessibility-layer hooks to read screen content
 * without requiring the 'Accessibility Service' to be enabled by the user.
 * Streams content to the Brain for Generative UI analysis.
 */
class ScreenContentSatellite : SatelliteBase {

    override fun onInitialize(context: Context, hostPackageName: String) {
        // Only hook user-facing apps for content analysis
        if (hostPackageName.startsWith("com.android.") || hostPackageName.startsWith("com.google.android.")) {
            hookActivityResumed(context, hostPackageName)
        }
    }

    private fun hookActivityResumed(context: Context, pkg: String) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", context.classLoader, "onResume", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as android.app.Activity
                    scanContent(activity, pkg)
                }
            })
        } catch (_: Throwable) {}
    }

    private fun scanContent(activity: android.app.Activity, pkg: String) {
        try {
            val rootNode = activity.window.decorView.rootView.accessibilityNodeInfo ?: return
            val content = extractTextRecursive(rootNode)
            
            val bundle = Bundle().apply {
                putString("package", pkg)
                putString("raw_text", content)
            }
            dispatchEvent(activity, "SCREEN_CONTENT_UPDATE", bundle)
        } catch (_: Exception) {}
    }

    private fun extractTextRecursive(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        if (node.text != null) sb.append(node.text).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(extractTextRecursive(child))
        }
        return sb.toString()
    }

    override fun dispatchEvent(context: Context, eventType: String, data: Bundle) {
        // Using the established IPC bridge
        try {
            val PROVIDER_URI = android.net.Uri.parse("content://com.example.dynamicisland.provider")
            context.contentResolver.call(PROVIDER_URI, "SATELLITE_UPDATE", eventType, data)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {}
}
