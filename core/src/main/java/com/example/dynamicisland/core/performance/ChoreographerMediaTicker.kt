package com.example.dynamicisland.core.performance

import android.media.session.PlaybackState
import android.os.SystemClock
import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BATCH 5: Choreographer-Synced Media Ticker
 *
 * Replaces the 1-second coroutine delay loop with a Choreographer.FrameCallback
 * that fires in sync with the display's actual refresh rate (60/90/120/144Hz).
 *
 * The old approach:
 *   while(isActive) { delay(1000); updatePosition() }
 *   Problem: positions updates at 1Hz — seeker jumps every second.
 *
 * This approach:
 *   Choreographer fires every frame → interpolate position mathematically.
 *   Result: seeker advances smoothly at the display refresh rate with
 *   zero extra CPU cost because we read a formula, not a network call.
 *
 * Formula:
 *   currentPosition = lastKnownPosition + (elapsedRealtime - lastUpdateTime) * playbackSpeed
 *
 * This mirrors ExoPlayer and the Android media framework's own interpolation.
 */
class ChoreographerMediaTicker {

    companion object {
        // Only push Compose state if position changed by more than this many ms.
        // Prevents unnecessary recompositions at sub-millisecond granularity.
        private const val MIN_UPDATE_THRESHOLD_MS = 16L  // ~1 frame at 60fps
    }

    private val _position      = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val isRunning      = AtomicBoolean(false)
    private var lastEmittedPos = -1L

    @Volatile private var lastKnownPositionMs: Long   = 0L
    @Volatile private var lastKnownUpdateTimeMs: Long = 0L   // elapsedRealtime units
    @Volatile private var playbackSpeed: Float         = 1f
    @Volatile private var isPlaying: Boolean           = false
    @Volatile private var durationMs: Long             = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isRunning.get()) return

            if (isPlaying) {
                val nowMs        = SystemClock.elapsedRealtime()
                val elapsedMs    = nowMs - lastKnownUpdateTimeMs
                val interpolated = (lastKnownPositionMs + (elapsedMs * playbackSpeed).toLong())
                    .coerceIn(0L, durationMs.coerceAtLeast(1L))

                if (Math.abs(interpolated - lastEmittedPos) >= MIN_UPDATE_THRESHOLD_MS) {
                    _position.value = interpolated
                    lastEmittedPos  = interpolated
                }
            }

            // Re-register for the next vsync
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    /**
     * Feed every PlaybackState update from your MediaController.Callback here.
     * The ticker interpolates positions between server-side updates automatically.
     */
    fun onPlaybackStateChanged(state: PlaybackState?, duration: Long) {
        if (state == null) { stop(); return }

        // Convert PlaybackState's uptimeMillis timestamp to elapsedRealtime
        val realtimeOffset    = SystemClock.elapsedRealtime() - SystemClock.uptimeMillis()
        lastKnownPositionMs   = state.position
        lastKnownUpdateTimeMs = state.lastPositionUpdateTime + realtimeOffset
        playbackSpeed         = state.playbackSpeed
        isPlaying             = state.state == PlaybackState.STATE_PLAYING
        durationMs            = duration

        if (isPlaying) {
            start()
        } else {
            _position.value = lastKnownPositionMs
            lastEmittedPos  = lastKnownPositionMs
        }
    }

    /** Called when the user drags the seeker scrubber */
    fun seekTo(positionMs: Long) {
        lastKnownPositionMs   = positionMs
        lastKnownUpdateTimeMs = SystemClock.elapsedRealtime()
        _position.value       = positionMs
        lastEmittedPos        = positionMs
    }

    /** Call when track changes to reset state */
    fun reset() {
        lastKnownPositionMs   = 0L
        lastKnownUpdateTimeMs = SystemClock.elapsedRealtime()
        _position.value       = 0L
        lastEmittedPos        = -1L
    }

    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    fun stop() {
        isRunning.set(false)
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    fun isActive() = isRunning.get() && isPlaying

    /**
     * Non-flow accessor for use inside Canvas draw lambdas and
     * InteractiveWavyMediaBar's posProvider parameter.
     * Replaces the old { currentMediaPos.longValue } lambda.
     */
    fun currentPosition(): Long {
        if (!isPlaying) return lastKnownPositionMs
        val elapsed = SystemClock.elapsedRealtime() - lastKnownUpdateTimeMs
        return (lastKnownPositionMs + (elapsed * playbackSpeed).toLong())
            .coerceIn(0L, durationMs.coerceAtLeast(1L))
    }
}