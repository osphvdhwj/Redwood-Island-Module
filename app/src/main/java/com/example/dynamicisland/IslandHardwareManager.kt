package com.example.dynamicisland

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest

class IslandHardwareManager(
    private val context: Context,
    private val audioManager: AudioManager,
    private val scope: CoroutineScope
) {
    // 🎛️ Background flow for smooth volume dragging
    val volumeFlow = MutableStateFlow(-1)
    
    // Tracks the current auto-brightness state
    var isAutoBrightnessEnabled = false
        private set

    init {
        scope.launch(Dispatchers.IO) {
            volumeFlow
                .filter { it >= 0 }
                .collectLatest { percent ->
                    delay(50) // Debounce rapid drag events
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val targetVolume = ((percent.toFloat() / 100f) * maxVolume).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                }
        }
    }

    fun setSystemVolume(percent: Int, view: DynamicIslandView?) {
        try {
            // Instantly update the UI slider so it feels 120fps smooth
            view?.updateHardwareVolume(percent)
            // Pass the data to our background Coroutine Flow
            volumeFlow.value = percent
        } catch (e: Exception) {}
    }

    fun updateVolumeState(view: DynamicIslandView?) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percent = if (maxVolume > 0) (current * 100) / maxVolume else 0
        view?.updateHardwareVolume(percent)
    }

    fun setSystemBrightness(percent: Int, view: DynamicIslandView?) {
        try {
            val resolver = context.contentResolver
            
            // Fetch true hardware ceiling (e.g. 4095 on Poco), fallback to 255
            val maxBrightness = try { 
                Settings.System.getInt(resolver, "screen_brightness_maximum") 
            } catch (e: Exception) { 
                255 
            }
            
            // Map 0-100% UI slider perfectly to 0-HardwareMax
            val targetBrightness = ((percent.toFloat() / 100f) * maxBrightness).toInt()
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
            
            // Push immediately to local state
            view?.updateHardwareBrightness(percent)
        } catch (e: Exception) {}
    }

    fun updateBrightnessState(view: DynamicIslandView?) {
        try {
            val resolver = context.contentResolver
            val brightness = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            val maxBrightness = try { Settings.System.getInt(resolver, "screen_brightness_maximum") } catch (e: Exception) { 255 }
            
            isAutoBrightnessEnabled = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1
            
            val percent = ((brightness.toFloat() / maxBrightness.toFloat()) * 100f).toInt()
            view?.updateHardwareBrightness(percent.coerceIn(0, 100))
            view?.updateAutoBrightnessState(isAutoBrightnessEnabled)
        } catch (e: Exception) {}
    }

    fun toggleAutoBrightness(view: DynamicIslandView?) {
        try {
            val resolver = context.contentResolver
            val newMode = if (isAutoBrightnessEnabled) 0 else 1
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newMode)
            updateBrightnessState(view)
        } catch (e: Exception) {}
    }

    fun toggleRingerMode(view: DynamicIslandView?) {
        try {
            val currentRingerMode = audioManager.ringerMode
            val nextMode = when (currentRingerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = nextMode
            view?.updateRingerState(nextMode)
        } catch (e: Exception) {}
    }

    fun toggleMicMute() {
        try { audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute } catch (e: Exception) {}
    }

    fun toggleSpeakerphone() {
        try { audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn } catch (e: Exception) {}
    }
}
