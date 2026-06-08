package com.example.dynamicisland.core.settings

import androidx.compose.runtime.*
import javax.inject.Inject

/**
 * 🛠️ SETTINGS VIEWMODEL
 * 
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
 * Provides a reactive bridge between the persistent settings manager
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * and the Compose-based configuration UI.
 */
class SettingsViewModel @Inject constructor(private val settingsManager: SettingsManager) {
    
    var state by mutableStateOf(SettingsState())
        private set

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        state = settingsManager.getSettingsState()
    }

    fun <T> updateSetting(key: SettingsManager.SettingKey, value: T) {
        when (value) {
            is Boolean -> settingsManager.putBoolean(key, value)
            is Int -> settingsManager.putInt(key, value)
            is Float -> settingsManager.putFloat(key, value)
            is String -> settingsManager.putString(key, value)
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                settingsManager.putStringSet(key, value as Set<String>)
            }
        }
        loadAllSettings()
        settingsManager.broadcastUpdate()
    }

    fun resetAll() {
        settingsManager.resetAll()
        loadAllSettings()
        settingsManager.broadcastUpdate()
    }

    fun clearAiMemory(): Boolean = settingsManager.clearAiMemory()

    fun exportAiData(): String? = settingsManager.exportAiData()
}
