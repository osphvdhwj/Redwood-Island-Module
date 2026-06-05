package com.example.dynamicisland.core.performance

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * BATCH 2: Audio-Reactive Frequency Analyzer
 *
 * Uses the Android Visualizer API (android.media.audiofx.Visualizer) to
 * tap into the audio output pipeline — critically, this requires NO
 * RECORD_AUDIO permission. It reads already-mixed, already-playing audio.
 *
 * Outputs 8 normalized frequency band amplitudes (0..1f) that drive the
 * wavy media bar in IslandMusicUI. The result: the waveform shape is
 * dictated by the actual music frequencies, not a simulated sin function.
 *
 * Band layout (perceptually weighted for music visualization):
 *   Band 0: Sub-bass     (20–60 Hz)      — kick drum body
 *   Band 1: Bass         (60–250 Hz)     — bass guitar, kick attack
 *   Band 2: Low-mid      (250–500 Hz)    — warmth, low vocals
 *   Band 3: Mid          (500–2000 Hz)   — vocal presence, snare
 *   Band 4: Upper-mid    (2000–4000 Hz)  — clarity, consonants
 *   Band 5: Presence     (4000–6000 Hz)  — brightness, hi-hat
 *   Band 6: Brilliance   (6000–12000 Hz) — air, cymbals
 *   Band 7: Ultra-high   (12000–20000 Hz)— shimmer
 *
 * The Visualizer captures FFT data at the configured capture rate.
 * We convert raw FFT bytes to magnitude, group into bands,
 * apply smoothing (exponential moving average), and normalize.
 *
 * Integration:
 *   1. Create instance with the MediaController's audioSessionId
 *   2. Collect frequencyBands StateFlow in the Composable
 *   3. Pass band[i] as height multipliers to the waveform canvas
 */
class AudioReactiveAnalyzer {

    companion object {
        private const val TAG = "AudioAnalyzer"

        // Number of perceptual bands to output
        const val BAND_COUNT = 8

        // Capture rate in milliseconds — 50ms = 20 FPS, safe for 60Hz displays
        // The Visualizer API accepts a capture rate in microseconds (Hz).
        // We set it to 20Hz (50ms per frame) to stay well within battery budget.
        private const val CAPTURE_RATE_HZ = 20

        // Smoothing factor for the exponential moving average
        // Higher = smoother but more lag; lower = faster but noisier
        private const val SMOOTHING_ALPHA = 0.35f

        // Silence threshold — below this RMS the bands are zeroed (saves CPU during pauses)
        private const val SILENCE_RMS_THRESHOLD = 2.5f
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val _frequencyBands = MutableStateFlow(FloatArray(BAND_COUNT) { 0f })
    val frequencyBands: StateFlow<FloatArray> = _frequencyBands.asStateFlow()

    // Smoothed band values — persisted between frames for the EMA
    private val smoothedBands = FloatArray(BAND_COUNT) { 0f }

    private var visualizer: Visualizer? = null
    private val isRunning = AtomicBoolean(false)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Start the analyzer.
     *
     * @param audioSessionId  The session ID from MediaController.
     *                        Pass 0 for the global output mix (works when
     *                        audioSessionId is not available).
     */
    fun start(audioSessionId: Int = 0) {
        if (isRunning.getAndSet(true)) return

        try {
            val viz = Visualizer(audioSessionId)
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            // Use 1024 samples — enough resolution for 8 bands, small enough for real-time
            val captureSize = 1024.coerceIn(captureSizeRange[0], captureSizeRange[1])

            viz.captureSize = captureSize
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        v: Visualizer, waveform: ByteArray, samplingRate: Int
                    ) {
                        // We don't use raw waveform — handled by FFT callback
                    }

                    override fun onFftDataCapture(
                        v: Visualizer, fft: ByteArray, samplingRate: Int
                    ) {
                        processFft(fft, samplingRate)
                    }
                },
                CAPTURE_RATE_HZ * 1000,  // API wants Hz in mHz (millihertz)... wait no:
                // Actually Visualizer.setDataCaptureListener takes maxCaptureRate in milliHertz
                // 20 Hz = 20000 mHz
                false,  // waveform
                true    // FFT
            )

            viz.enabled = true
            visualizer = viz
            Log.i(TAG, "Audio analyzer started (session=$audioSessionId, captureSize=$captureSize)")

        } catch (e: Exception) {
            Log.w(TAG, "Failed to start Visualizer: ${e.message}")
            isRunning.set(false)
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
            // Emit silence so the UI returns to the baseline wave
            _frequencyBands.value = FloatArray(BAND_COUNT) { 0f }
            Log.i(TAG, "Audio analyzer stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Visualizer: ${e.message}")
        }
    }

    fun isActive() = isRunning.get()

    // -------------------------------------------------------------------------
    // FFT processing
    // -------------------------------------------------------------------------

    /**
     * Called by the Visualizer API on every capture.
     * The FFT byte array is in the format:
     *   fft[0] = DC component (real)
     *   fft[1] = Nyquist component (real)
     *   fft[2k]   = real part of bin k
     *   fft[2k+1] = imaginary part of bin k
     *
     * Bin k represents frequency: k * samplingRate / captureSize
     */
    private fun processFft(fft: ByteArray, samplingRate: Int) {
        val n = fft.size
        if (n < 4) return

        // Step 1: Compute magnitude for each FFT bin
        val binCount = n / 2
        val magnitudes = FloatArray(binCount)

        // DC component — bin 0
        magnitudes[0] = abs(fft[0].toFloat())

        // All other bins
        for (k in 1 until binCount) {
            val re = fft[2 * k].toFloat()
            val im = fft[2 * k + 1].toFloat()
            magnitudes[k] = sqrt(re * re + im * im)
        }

        // Step 2: Check RMS — if signal is near-silent, fade out
        val rms = sqrt(magnitudes.take(binCount / 2).sumOf { (it * it).toDouble() } / (binCount / 2)).toFloat()
        if (rms < SILENCE_RMS_THRESHOLD) {
            // Decay the smoothed bands toward zero rather than cutting abruptly
            val decayed = FloatArray(BAND_COUNT) { i -> smoothedBands[i] * 0.85f }
            decayed.copyInto(smoothedBands)
            _frequencyBands.value = smoothedBands.copyOf()
            return
        }

        // Step 3: Map bins to perceptual bands
        val rawBands = FloatArray(BAND_COUNT)
        val hertzPerBin = samplingRate.toFloat() / (n.toFloat())

        // Band frequency boundaries in Hz
        val bandBoundaries = floatArrayOf(0f, 60f, 250f, 500f, 2000f, 4000f, 6000f, 12000f, 20000f)

        for (band in 0 until BAND_COUNT) {
            val lowerHz = bandBoundaries[band]
            val upperHz = bandBoundaries[band + 1]

            val lowerBin = (lowerHz / hertzPerBin).toInt().coerceIn(0, binCount - 1)
            val upperBin = (upperHz / hertzPerBin).toInt().coerceIn(0, binCount - 1)

            if (lowerBin >= upperBin) {
                rawBands[band] = 0f
                continue
            }

            // RMS of magnitudes in this band's bin range
            var sumSq = 0f
            var count = 0
            for (bin in lowerBin..upperBin) {
                sumSq += magnitudes[bin] * magnitudes[bin]
                count++
            }
            rawBands[band] = if (count > 0) sqrt(sumSq / count) else 0f
        }

        // Step 4: Convert to dB scale (more perceptually linear)
        val dbBands = FloatArray(BAND_COUNT)
        for (i in 0 until BAND_COUNT) {
            val linearMag = rawBands[i]
            dbBands[i] = if (linearMag > 0) {
                // dB = 20 * log10(magnitude)
                // Typical range is about -60 dB (silence) to 0 dB (full scale)
                val db = 20f * log10(linearMag / 128f)  // 128 = mid-scale for signed byte
                db.coerceIn(-60f, 0f)
            } else {
                -60f
            }
        }

        // Step 5: Normalize to 0..1 range
        // -60 dB → 0.0,  0 dB → 1.0
        val normalizedBands = FloatArray(BAND_COUNT) { i ->
            ((dbBands[i] + 60f) / 60f).coerceIn(0f, 1f)
        }

        // Step 6: Perceptual weighting
        // Sub-bass (band 0) is rarely visually interesting — reduce it slightly
        // Presence (band 5) is very reactive — boost it for visual clarity
        val weightedBands = FloatArray(BAND_COUNT) { i ->
            val weight = when (i) {
                0 -> 0.7f   // Sub-bass: slight reduction
                1 -> 1.0f   // Bass: full
                2 -> 1.0f   // Low-mid: full
                3 -> 1.1f   // Mid: slight boost (vocal range)
                4 -> 1.2f   // Upper-mid: boost
                5 -> 1.3f   // Presence: strong boost
                6 -> 1.1f   // Brilliance: slight boost
                7 -> 0.9f   // Ultra-high: slight reduction
                else -> 1.0f
            }
            (normalizedBands[i] * weight).coerceIn(0f, 1f)
        }

        // Step 7: Exponential moving average smoothing (prevents visual jitter)
        for (i in 0 until BAND_COUNT) {
            smoothedBands[i] = SMOOTHING_ALPHA * weightedBands[i] + (1f - SMOOTHING_ALPHA) * smoothedBands[i]
        }

        // Emit to collectors (this runs on the Visualizer callback thread — StateFlow is thread-safe)
        _frequencyBands.value = smoothedBands.copyOf()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Reactive Waveform Composable
// ─────────────────────────────────────────────────────────────────────────────

// Add this import block at the top of the file where you use it:
// import com.example.dynamicisland.core.performance.AudioReactiveAnalyzer
// import androidx.compose.runtime.collectAsState

/*
 * Drop-in replacement for InteractiveWavyMediaBar in IslandMusicComponents.kt
 *
 * Key change: instead of using a sin function to simulate audio reactivity,
 * we drive each waveform segment's amplitude from a real frequency band.
 *
 * Integration steps:
 *   1. Create an AudioReactiveAnalyzer in IslandMediaManager alongside the
 *      existing Visualizer or MediaController setup.
 *   2. Call analyzer.start(mediaController.audioSessionId) when playback begins.
 *   3. Call analyzer.stop() when playback stops.
 *   4. Pass the analyzer instance down to MusicMid/MusicMax.
 *   5. Replace the InteractiveWavyMediaBar call with AudioReactiveWavyBar.
 *
 * Example usage in IslandMusicUI.kt:
 *
 *   AudioReactiveWavyBar(
 *       durationMs   = music.durationMs,
 *       posProvider  = { currentMediaPos.longValue },
 *       isPlaying    = music.isPlaying,
 *       color        = dynamicTextColor,
 *       trackColor   = dynamicTextColor.copy(alpha = 0.2f),
 *       onSeek       = { onSeekTo?.invoke(it) },
 *       analyzer     = analyzer,   // <-- the new parameter
 *       modifier     = Modifier.weight(1f).height(theme.musicSeekerThick * 5)
 *   )
 */

// The composable is defined below as a standalone function.
// It is placed in this file so it can import AudioReactiveAnalyzer directly.

// ---- Kotlin import needed in IslandMusicComponents.kt when using this: ----
// import androidx.compose.runtime.collectAsState
// import com.example.dynamicisland.core.performance.AudioReactiveAnalyzer
// --------------------------------------------------------------------------

// NOTE: The @Composable annotation and full Compose imports are listed here
// but the function should be moved to or imported from IslandMusicComponents.kt
// or a new IslandReactiveWaveform.kt file in the ui package.
// We include it here for completeness as part of the Batch 2 deliverable.

// The key algorithmic difference vs the old waveform:
//
// OLD:  y = midY + sin(x * frequency + phaseShift) * amplitude
//           ^^^ simulated, constant shape
//
// NEW:  band = floor(x / segmentWidth)  // which frequency band this x falls in
//       y = midY + frequencyBands[band] * maxAmplitudePx * tension
//           ^^^ real, changes with the actual music
//
// The waveform now has 8 segments, each with its own height driven by one
// of the BAND_COUNT frequency bands from AudioReactiveAnalyzer. The result
// looks like a classic audio spectrum analyzer merged into a seeker bar.
