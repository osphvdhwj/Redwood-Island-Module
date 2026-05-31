package com.example.dynamicisland.settings

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.settings.SettingsManager.SettingKey

class SettingsViewModel(private val settingsManager: SettingsManager) {
    var state by mutableStateOf(SettingsState())
        private set

    init {
        loadAllSettings()
    }

    private fun loadAllSettings() {
        state = settingsManager.getSettingsState()
    }

    fun <T> updateSetting(key: SettingKey, value: T) {
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
