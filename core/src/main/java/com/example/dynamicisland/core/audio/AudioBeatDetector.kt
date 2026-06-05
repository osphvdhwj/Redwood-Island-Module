package com.example.dynamicisland.core.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

class AudioBeatDetector {
    private val bpmFlow = MutableStateFlow(0f)
    val bpm: StateFlow<Float> = bpmFlow

    private val energyFlow = MutableStateFlow(0f)
    val energy: StateFlow<Float> = energyFlow

    private val waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveform: StateFlow<List<Float>> = waveformData

    private var job: Job? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val time = System.currentTimeMillis() * 0.001
                bpmFlow.value = (80 + sin(time * 0.5) * 40).toFloat()
                energyFlow.value = (0.3f + abs(sin(time * 2.0)).toFloat() * 0.7f).coerceIn(0f, 1f)
                waveformData.value = List(64) { i ->
                    (sin(time * 10 + i * 0.2).toFloat() * energyFlow.value * 0.5f + 0.5f)
                }
                delay(50)
            }
        }
    }

    fun stop() {
        isRunning = false
        job?.cancel()
    }

    fun processPcmShort(pcm: ShortArray) {
        var sum = 0f
        for (sample in pcm) sum += sample * sample
        energyFlow.value = (sqrt(sum / pcm.size) / 32768f).coerceIn(0f, 1f)
    }
}