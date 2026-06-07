package com.example.dynamicisland.core.intelligence.nn

/**
 * 🚀 MULTI-LAYER PERCEPTRON (MLP)
 * 
 * Supports deep forward passes and dynamic backpropagation (SGD).
 * Designed to hold 10,000+ parameters for complex behavioral mapping.
 */
class SequentialNet(val layerSizes: IntArray) {
    val weights = Array(layerSizes.size - 1) { i -> Tensor(layerSizes[i], layerSizes[i + 1]).apply { randomize() } }
    val biases = Array(layerSizes.size - 1) { i -> Tensor(1, layerSizes[i + 1]).apply { randomize() } }

    /**
     * Forward pass generating an output probability distribution.
     * @param input Tensor of shape [1, input_size]
     */
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

    /**
     * Trains the network on a single sample using Stochastic Gradient Descent.
     * @param input Tensor of shape [1, input_size]
     * @param targetIndex The index of the correct output class.
     * @param lr Learning Rate.
     */
    fun train(input: Tensor, targetIndex: Int, lr: Float) {
        // Forward pass with activations cached for backprop
        val activations = mutableListOf<Tensor>(input)
        val zValues = mutableListOf<Tensor>() // Pre-activation sums

        var current = input
        for (i in weights.indices) {
            val z = current.dot(weights[i]).add(biases[i])
            zValues.add(z)
            
            current = if (i < weights.size - 1) z.relu() else z.softmax()
            activations.add(current)
        }

        // Backpropagation: Calculate Output Error (Cross Entropy + Softmax derivative)
        val output = activations.last()
        val dZ = Tensor(1, output.cols)
        for (i in 0 until output.cols) {
            dZ[0, i] = if (i == targetIndex) output[0, i] - 1f else output[0, i]
        }

        var currentError = dZ

        // Propagate backwards
        for (i in weights.indices.reversed()) {
            val prevActivation = activations[i]
            
            // Calculate gradients
            val dW = Tensor(weights[i].rows, weights[i].cols)
            for (r in 0 until prevActivation.cols) {
                for (c in 0 until currentError.cols) {
                    dW[r, c] = prevActivation[0, r] * currentError[0, c]
                }
            }
            
            val dB = currentError // For batch size 1

            // Calculate error for next layer down (if not input layer)
            var nextError: Tensor? = null
            if (i > 0) {
                nextError = Tensor(1, weights[i].rows)
                for (r in 0 until currentError.cols) {
                    for (c in 0 until weights[i].rows) {
                        nextError[0, c] += currentError[0, r] * weights[i][c, r]
                    }
                }
                
                // Derivative of ReLU
                val zPrev = zValues[i - 1]
                for (j in 0 until nextError.cols) {
                    if (zPrev[0, j] <= 0f) {
                        nextError[0, j] = 0f
                    }
                }
            }

            // Apply gradients (SGD)
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
