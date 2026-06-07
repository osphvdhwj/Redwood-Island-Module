package com.example.dynamicisland.core.manager

import android.content.ClipboardManager
import android.content.Context
import com.example.dynamicisland.core.ui.DynamicIslandView
import com.example.dynamicisland.shared.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class IslandClipboardManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onNewClipCaught: (String) -> Unit
) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var lastClipText = ""

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (clipboard.hasPrimaryClip()) {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text != lastClipText) {
                    lastClipText = text
                    
                    // Trigger the callback to open the Island!
                    scope.launch { onNewClipCaught(text) }
                }
            }
        }
    }

    fun startListening() {
        try { clipboard.addPrimaryClipChangedListener(clipListener) } catch (e: Throwable) {}
    }

    fun stopListening() {
        try { clipboard.removePrimaryClipChangedListener(clipListener) } catch (e: Throwable) {}
    }
}
