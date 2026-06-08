package com.example.dynamicisland.core.experimental

import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.ipc.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import kotlinx.coroutines.flow.StateFlow
import com.example.dynamicisland.shared.settings.*

/**
import com.example.dynamicisland.shared.model.*
 * Guided mindfulness breathing pacer.
 *
 * Cycles through INHALE → HOLD → EXHALE → REST.
 * Provides real‑time phase and progress (0‑1) as StateFlows.
 *
 * Usage:
 *   val pacer = MindfulnessBreathPacer()
 *   pacer.inhaleSeconds = 4
 *   pacer.holdSeconds = 2
 *   pacer.exhaleSeconds = 6
 *   pacer.cycles = 5
 *   pacer.start()
 */
class MindfulnessBreathPacer {

    enum class Phase { INHALE, HOLD, EXHALE, REST }

    private val _currentPhase = MutableStateFlow(Phase.REST)
    val phase: StateFlow<Phase> = _currentPhase

    private val _progress = MutableStateFlow(0f)
    val phaseProgress: StateFlow<Float> = _progress

    var inhaleSeconds = 4
    var holdSeconds = 2
    var exhaleSeconds = 6
    var cycles = 5

    private var job: Job? = null

    /**
     * Starts the breathing session. If a session is already running, it will be cancelled before a new one.
     */
    fun start() {
        stop()
        job = CoroutineScope(Dispatchers.Default).launch {
            repeat(cycles) {
                animatePhase(Phase.INHALE, inhaleSeconds * 1000L)
                animatePhase(Phase.HOLD, holdSeconds * 1000L)
                animatePhase(Phase.EXHALE, exhaleSeconds * 1000L)
                // Brief pause between cycles
                _currentPhase.value = Phase.REST
                _progress.value = 0f
                delay(200)
            }
            // End session
            _currentPhase.value = Phase.REST
            _progress.value = 0f
        }
    }

    /**
     * Stops the current breathing session immediately.
     */
    fun stop() {
        job?.cancel()
        job = null
        _currentPhase.value = Phase.REST
        _progress.value = 0f
    }

    private suspend fun animatePhase(phase: Phase, durationMs: Long) {
        _currentPhase.value = phase
        val steps = 100
        val stepDelay = durationMs / steps
        repeat(steps) { i ->
            _progress.value = i.toFloat() / steps
            delay(stepDelay)
        }
        _progress.value = 1f
    }
}
