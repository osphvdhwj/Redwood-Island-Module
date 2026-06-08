package com.example.dynamicisland.core.intelligence.nn

/**
 * 🚀 MULTI-LAYER PERCEPTRON (MLP)
 */
class SequentialNet(val layerSizes: IntArray) {
    val weights = Array(layerSizes.size - 1) { i -> Tensor(layerSizes[i], layerSizes[i + 1]).apply { randomize() } }
    val biases = Array(layerSizes.size - 1) { i -> Tensor(1, layerSizes[i + 1]).apply { randomize() } }

import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
    fun forward(input: Tensor): Tensor {
        var current = input
        for (i in weights.indices) {
            current = current.dot(weights[i]).add(biases[i])
            if (i < weights.size - 1) {
                current = current.relu()
            }
        }
        return current.softmax()
    }

    fun train(input: Tensor, targetIndex: Int, lr: Float) {
        val activations = mutableListOf<Tensor>(input)
        val zValues = mutableListOf<Tensor>() 

        var current = input
        for (i in weights.indices) {
            val z = current.dot(weights[i]).add(biases[i])
            zValues.add(z)
            
            current = if (i < weights.size - 1) z.relu() else z.softmax()
            activations.add(current)
        }

        val output = activations.last()
        val dZ = Tensor(1, output.cols)
        for (i in 0 until output.cols) {
            dZ[0, i] = if (i == targetIndex) output[0, i] - 1f else output[0, i]
        }

        var currentError = dZ

        for (i in weights.indices.reversed()) {
            val prevActivation = activations[i]
            
            val dW = Tensor(weights[i].rows, weights[i].cols)
            for (r in 0 until prevActivation.cols) {
                for (c in 0 until currentError.cols) {
                    dW[r, c] = prevActivation[0, r] * currentError[0, c]
                }
            }
            
            val dB = currentError

            var nextError: Tensor? = null
            if (i > 0) {
                nextError = Tensor(1, weights[i].rows)
                for (r in 0 until currentError.cols) {
                    for (c in 0 until weights[i].rows) {
                        nextError[0, c] += currentError[0, r] * weights[i][c, r]
                    }
                }
                
                val zPrev = zValues[i - 1]
                for (j in 0 until nextError.cols) {
                    if (zPrev[0, j] <= 0f) {
                        nextError[0, j] = 0f
                    }
                }
            }

            for (j in weights[i].data.indices) {
                weights[i].data[j] -= lr * dW.data[j]
            }
            for (j in biases[i].data.indices) {
                biases[i].data[j] -= lr * dB.data[j]
            }

            if (nextError != null) {
                currentError = nextError
            }
        }
    }
}
