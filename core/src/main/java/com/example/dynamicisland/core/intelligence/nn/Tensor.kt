package com.example.dynamicisland.core.intelligence.nn

import kotlin.math.exp
import kotlin.math.max
import kotlin.random.Random

/**
 * 🧠 ELITE NATIVE TENSOR ENGINE
 */
class Tensor(val rows: Int, val cols: Int) {
    val data = FloatArray(rows * cols)

    operator fun get(r: Int, c: Int): Float = data[r * cols + c]
    operator fun set(r: Int, c: Int, value: Float) { data[r * cols + c] = value }

    fun randomize() {
        for (i in data.indices) {
            data[i] = (Random.nextFloat() * 2f - 1f) * 0.1f
        }
    }

    fun argmax(): Int {
        var maxVal = Float.NEGATIVE_INFINITY
        var maxIdx = -1
        for (i in data.indices) {
            if (data[i] > maxVal) {
                maxVal = data[i]
                maxIdx = i
            }
        }
        return maxIdx
    }

    fun dot(other: Tensor): Tensor {
        if (this.cols != other.rows) throw IllegalArgumentException("Shape mismatch: ${this.cols} != ${other.rows}")
        val result = Tensor(this.rows, other.cols)
        for (i in 0 until this.rows) {
            for (j in 0 until other.cols) {
                var sum = 0f
                for (k in 0 until this.cols) {
                    sum += this[i, k] * other[k, j]
                }
                result[i, j] = sum
            }
        }
        return result
    }

    fun add(other: Tensor): Tensor {
        val result = Tensor(rows, cols)
        for (i in data.indices) {
            result.data[i] = this.data[i] + other.data[i]
        }
        return result
    }

    fun relu(): Tensor {
        val result = Tensor(rows, cols)
        for (i in data.indices) {
            result.data[i] = max(0f, this.data[i])
        }
        return result
    }

    fun softmax(): Tensor {
        val result = Tensor(rows, cols)
        var maxVal = Float.NEGATIVE_INFINITY
        for (d in data) if (d > maxVal) maxVal = d
        
        var sum = 0f
        for (i in data.indices) {
            result.data[i] = exp(data[i] - maxVal)
            sum += result.data[i]
        }
        for (i in data.indices) {
            result.data[i] /= sum
        }
        return result
    }
}
