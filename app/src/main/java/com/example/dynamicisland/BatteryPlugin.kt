package com.example.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

object BatteryPlugin {

    var onBatteryChanged: ((level: Int, isCharging: Boolean, color: Int) -> Unit)? = null
    private var isRegistered = false

    private var lastChargingState: Boolean? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (isCharging != lastChargingState) {
                    lastChargingState = isCharging
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val percent = if (scale > 0) (level * 100) / scale.toFloat() else 0f
                    onBatteryChanged?.invoke(percent.toInt(), isCharging, getBatteryColor(percent.toInt()))
                }
            }
        }
    }

    fun start(context: Context) {
        if (!isRegistered) {
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            isRegistered = true
        }
    }

    fun stop(context: Context) {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
            isRegistered = false
        }
    }

    @ColorInt
    private fun getBatteryColor(percent: Int): Int {
        return when {
            percent <= 20 -> Color.RED
            percent <= 50 -> interpolateColor(Color.RED, Color.YELLOW, (percent - 20) / 30f)
            percent <= 100 -> interpolateColor(Color.YELLOW, Color.GREEN, (percent - 50) / 50f)
            else -> Color.GREEN
        }
    }

    @ColorInt
    private fun interpolateColor(@ColorInt startColor: Int, @ColorInt endColor: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        return ColorUtils.blendARGB(startColor, endColor, f)
    }
}
