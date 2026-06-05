package com.example.dynamicisland.core.manager

import com.example.dynamicisland.core.model.*
import com.example.dynamicisland.core.ui.DynamicIslandView
import com.example.dynamicisland.core.hook.*

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.pow
import kotlin.math.roundToInt

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
        } catch (e: Throwable) {}
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
            val maxHardware = try { Settings.System.getInt(resolver, "screen_brightness_maximum") } catch (e: Throwable) { 255 }
            
            // 🚀 Perceptual Gamma Curve (Slider to Hardware)
            val normalizedSlider = percent.toFloat() / 100f
            val targetBrightness = (normalizedSlider.pow(2.2f) * maxHardware).roundToInt().coerceIn(0, maxHardware)
            
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
            view?.updateHardwareBrightness(percent)
        } catch (e: Throwable) {}
    }

    fun updateBrightnessState(view: DynamicIslandView?) {
        try {
            val resolver = context.contentResolver
            val hardwareBrt = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
            val maxHardware = try { Settings.System.getInt(resolver, "screen_brightness_maximum") } catch (e: Throwable) { 255 }
            
            isAutoBrightnessEnabled = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == 1
            
            // 🚀 Inverse Gamma Curve (Hardware to Slider)
            val normalizedHardware = hardwareBrt.toFloat() / maxHardware.toFloat()
            val percent = (normalizedHardware.pow(1f / 2.2f) * 100f).roundToInt().coerceIn(0, 100)
            
            view?.updateHardwareBrightness(percent)
            view?.updateAutoBrightnessState(isAutoBrightnessEnabled)
        } catch (e: Throwable) {}
    }

    fun toggleAutoBrightness(view: DynamicIslandView?) {
        try {
            val resolver = context.contentResolver
            val newMode = if (isAutoBrightnessEnabled) 0 else 1
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newMode)
            updateBrightnessState(view)
        } catch (e: Throwable) {}
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
        } catch (e: Throwable) {}
    }

    fun setStreamVolume(streamType: Int, percent: Int) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val targetVolume = ((percent.toFloat() / 100f) * maxVolume).toInt()
            audioManager.setStreamVolume(streamType, targetVolume, 0)
        } catch (e: Throwable) {}
    }

    fun toggleMicMute() {
        try { audioManager.isMicrophoneMute = !audioManager.isMicrophoneMute } catch (e: Throwable) {}
    }

    fun toggleSpeakerphone() {
        try { audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn } catch (e: Throwable) {}
    }
}
