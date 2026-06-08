package com.example.dynamicisland.core.manager

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🛠️ ISLAND STORAGE MANAGER
 * 
 * Manages filesystem-level archives and historical data for the Island.
 */
@Singleton
class IslandStorageManager @Inject constructor(
    private val context: Context
) {
    val stashHistory = mutableListOf<String>()

    fun archive(data: String) {
        stashHistory.add(data)
    }
}
