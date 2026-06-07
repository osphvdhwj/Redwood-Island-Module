package com.example.dynamicisland.core.intelligence

import android.content.Context
import com.example.dynamicisland.core.intelligence.nn.SequentialNet
import com.example.dynamicisland.core.intelligence.nn.Tensor
import com.example.dynamicisland.core.util.RedwoodLogger
import com.example.dynamicisland.shared.model.IslandIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 🧠 ELITE NEURAL PREDICTION ENGINE
 * 
 * Uses a true Multi-Layer Perceptron (MLP) built from scratch in Kotlin.
 * Contains over 12,000 parameters to map non-linear relationships between 
 * time, day, battery, and previous app usage to predict the next user action.
 */
class IslandPredictionEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val INPUT_DIM = 4
    private val HIDDEN_1 = 64
    private val HIDDEN_2 = 128
    private val MAX_APPS = 100 // Top 100 most used apps
    
    private val mlp = SequentialNet(intArrayOf(INPUT_DIM, HIDDEN_1, HIDDEN_2, MAX_APPS))
    
    private val appIndexMap = mutableMapOf<String, Int>()
    private val indexAppMap = mutableMapOf<Int, String>()
    private var currentAppIndex = 0
    
    private val _prediction = MutableStateFlow<PredictionResult?>(null)
    val prediction = _prediction.asStateFlow()

    init {
        seedAppIndices()
    }

    /**
     * Called by the system when a new app is launched.
     * This acts as the "Online Training" trigger.
     */
    fun onAppForegrounded(packageName: String, batteryLevel: Int = 50) {
        scope.launch {
            if (!appIndexMap.containsKey(packageName)) {
                if (appIndexMap.size < MAX_APPS) {
                    val idx = appIndexMap.size
                    appIndexMap[packageName] = idx
                    indexAppMap[idx] = packageName
                } else return@launch // App not tracked
            }
            
            val targetIdx = appIndexMap[packageName]!!
            
            // 1. Train the network on what just happened
            val inputTensor = buildInputVector(batteryLevel)
            mlp.train(inputTensor, targetIdx, 0.01f)
            
            // 2. Predict what will happen next
            currentAppIndex = targetIdx
            
            val nextInput = buildInputVector(batteryLevel)
            val outputDist = mlp.forward(nextInput)
            
            // Find argmax
            var maxProb = -1f
            var maxIdx = -1
            for (i in 0 until MAX_APPS) {
                val prob = outputDist[0, i]
                if (prob > maxProb) {
                    maxProb = prob
                    maxIdx = i
                }
            }
            
            if (maxIdx != -1 && maxProb > 0.6f && indexAppMap.containsKey(maxIdx)) {
                val predPkg = indexAppMap[maxIdx]!!
                RedwoodLogger.d("MLP Prediction: $predPkg (Conf: ${maxProb * 100}%)")
                _prediction.value = PredictionResult(predPkg, maxProb)
            } else {
                _prediction.value = null
            }
        }
    }

    private fun buildInputVector(battery: Int): Tensor {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY) / 24f // Normalized 0-1
        val day = cal.get(Calendar.DAY_OF_WEEK) / 7f // Normalized 0-1
        val prevApp = currentAppIndex.toFloat() / MAX_APPS.toFloat()
        val bat = battery / 100f
        
        val t = Tensor(1, INPUT_DIM)
        t[0, 0] = hour
        t[0, 1] = day
        t[0, 2] = prevApp
        t[0, 3] = bat
        return t
    }

    private fun seedAppIndices() {
        val commonApps = listOf(
            "com.whatsapp", "com.google.android.youtube", "com.google.android.apps.maps",
            "com.spotify.music", "com.instagram.android", "com.twitter.android",
            "com.android.chrome", "com.google.android.apps.messaging", "com.android.dialer"
        )
        commonApps.forEachIndexed { i, pkg ->
            appIndexMap[pkg] = i
            indexAppMap[i] = pkg
        }
    }

    data class PredictionResult(val predictedPackage: String, val confidenceScore: Float)

    companion object {
        @Volatile
        private var instance: IslandPredictionEngine? = null

        fun get(context: Context): IslandPredictionEngine {
            return instance ?: synchronized(this) {
                instance ?: IslandPredictionEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
