package com.example.dynamicisland.core.util

import android.graphics.Bitmap
import android.util.LruCache

object IslandImageCache {
    
    // 🛡️ STRICT RAM LIMIT: Exactly 15 Megabytes
    private val maxMemory = 15 * 1024 * 1024 
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
    
    data class CacheEntry(val bitmap: Bitmap, val timestamp: Long)

    private val memoryCache = object : LruCache<String, CacheEntry>(maxMemory) {
        override fun sizeOf(key: String, value: CacheEntry): Int {
            return value.bitmap.byteCount
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: CacheEntry, newValue: CacheEntry?) {
       
        }
    }

    // Track the history of media IDs to enforce the "Max 4 Songs" rule
    private val songHistory = mutableListOf<String>()

    fun put(key: String, bitmap: Bitmap) {
        memoryCache.put(key, CacheEntry(bitmap, System.currentTimeMillis()))
        
        // 🎵 SMART MEDIA LIMIT: If we store a new song, ensure we only keep the last 4
        if (key.startsWith("media_")) {
            if (!songHistory.contains(key)) songHistory.add(key)
            
            if (songHistory.size > 4) {
                val oldestKey = songHistory.removeAt(0)
                memoryCache.remove(oldestKey)
                memoryCache.remove("${oldestKey}_blurred") // Also delete the blurred background
            }
        }
    }

    fun get(key: String): Bitmap? {
        val entry = memoryCache.get(key) ?: return null
        
        // ⏱️ TIME-TO-LIVE (TTL): If the image is older than 2 hours, delete it.
        val twoHoursMs = 2 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - entry.timestamp > twoHoursMs) {
            memoryCache.remove(key)
            return null
        }
        return entry.bitmap
    }
    
    fun clearAll() {
        memoryCache.evictAll()
        songHistory.clear()
    }
}
