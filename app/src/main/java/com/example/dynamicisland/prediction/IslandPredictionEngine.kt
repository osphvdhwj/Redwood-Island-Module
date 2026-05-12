package com.dynamicisland.prediction

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.dynamicisland.model.LiveActivityModel
import com.dynamicisland.settings.SettingsState
import androidx.compose.ui.graphics.Color

/**
 * IslandPredictionEngine — predicts what the island should show next
 * and powers ambient / pre‑warming features.
 */
class IslandPredictionEngine(private val context: Context) {

    // Ambient audio level (0f..1f) for ring reactivity
    private val _ambientAudioLevel = MutableStateFlow(0f)
    val ambientAudioLevel: StateFlow<Float> = _ambientAudioLevel

    // Predicted app (package name + brand colour)
    private val _predictedApp = MutableStateFlow<PredictedApp?>(null)
    val predictedApp: StateFlow<PredictedApp?> = _predictedApp

    // Confidence of the current prediction (0f..1f)
    var confidence: Float = 0f
        private set

    // Whether prediction‑based tint is currently allowed by settings
    var predictionTintEnabled: Boolean = true

    /**
     * Called by IslandController whenever the island state changes.
     * Updates internal predictions based on the active model and settings.
     */
    fun onStateChange(state: IslandState, model: LiveActivityModel?, settings: SettingsState) {
        predictionTintEnabled = settings.predictionTint

        // When music is active, we "predict" the music app as next
        if (model is LiveActivityModel.Music) {
            // Spotify — you can extend this to any package you want
            _predictedApp.value = PredictedApp(
                packageName = "com.spotify.music",
                brandColor = Color(0xFF1DB954)
            )
            confidence = 0.9f
        } else if (model is LiveActivityModel.Navigation) {
            _predictedApp.value = PredictedApp(
                packageName = "com.google.android.apps.maps",
                brandColor = Color(0xFF4285F4)
            )
            confidence = 0.85f
        } else {
            // No strong prediction — reset
            _predictedApp.value = null
            confidence = 0f
        }
    }

    /**
     * Updates the ambient audio level (called by AudioBeatDetector or similar).
     */
    fun updateAmbientLevel(level: Float) {
        _ambientAudioLevel.value = level.coerceIn(0f, 1f)
    }

    /**
     * Attempts an advanced prediction based on time, location, etc.
     * Currently a lightweight stub.
     */
    fun refreshPredictions() {
        // In a full implementation, query usage stats or ML model
        // For now, we just decay confidence gradually
        if (confidence > 0.1f) {
            confidence -= 0.01f
        } else {
            _predictedApp.value = null
            confidence = 0f
        }
    }

    /**
     * Data class representing a predicted app and its brand colour.
     */
    data class PredictedApp(
        val packageName: String,
        val brandColor: Color
    )
}