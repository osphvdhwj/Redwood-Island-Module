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
 */
class IslandPredictionEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val INPUT_DIM = 4
    private val HIDDEN_1 = 64
    private val HIDDEN_2 = 128
    private val MAX_APPS = 100 
    
    private val mlp = SequentialNet(intArrayOf(INPUT_DIM, HIDDEN_1, HIDDEN_2, MAX_APPS))
    
    private val appIndexMap = mutableMapOf<String, Int>()
    private val indexAppMap = mutableMapOf<Int, String>()
    private var currentAppIndex = 0
    
    private val _prediction = MutableStateFlow<PredictionResult?>(null)
    val prediction = _prediction.asStateFlow()

    init {
        seedAppIndices()
    }

    fun onAppForegrounded(packageName: String, batteryLevel: Int = 50) {
        scope.launch {
            if (!appIndexMap.containsKey(packageName)) {
                if (appIndexMap.size < MAX_APPS) {
                    val idx = appIndexMap.size
                    appIndexMap[packageName] = idx
                    indexAppMap[idx] = packageName
                } else return@launch 
            }
            
            val targetIdx = appIndexMap[packageName]!!
            
            val inputTensor = buildInputVector(batteryLevel)
            mlp.train(inputTensor, targetIdx, 0.01f)
            
            currentAppIndex = targetIdx
            
            val nextInput = buildInputVector(batteryLevel)
            val outputDist = mlp.forward(nextInput)
            val maxIdx = outputDist.argmax()
            val maxProb = outputDist[0, maxIdx]
            
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
        val hour = cal.get(Calendar.HOUR_OF_DAY) / 24f 
        val day = cal.get(Calendar.DAY_OF_WEEK) / 7f 
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
