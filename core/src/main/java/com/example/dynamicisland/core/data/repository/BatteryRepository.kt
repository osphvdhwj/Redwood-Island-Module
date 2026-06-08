package com.example.dynamicisland.core.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.example.dynamicisland.core.domain.dispatchers.DispatcherProvider
import com.example.dynamicisland.core.domain.lifecycle.BackendComponent
import com.example.dynamicisland.core.domain.state.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.ipc.*
import com.example.dynamicisland.shared.model.*
import com.example.dynamicisland.shared.settings.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryState(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val color: Int = Color.GREEN,
    val wattage: Float = 0f
)

@Singleton
class BatteryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) : BackendComponent {

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.hardware())
    private var isRegistered = false
    private var pollingJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                updateFromIntent(intent)
            }
        }
    }

    override fun onStart() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(receiver, filter)
            intent?.let { updateFromIntent(it) }
            isRegistered = true
        }
    }

    override fun onStop() {
        if (isRegistered) {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            isRegistered = false
        }
        stopPolling()
    }

    private fun updateFromIntent(intent: Intent) {
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (scale > 0) ((level * 100f) / scale).toInt() else 0

        val currentState = _batteryState.value
        val newState = currentState.copy(
            level = percent,
            isCharging = isCharging,
            color = getBatteryColor(percent),
            wattage = calculateWattage()
        )
        
        _batteryState.value = newState
        managePolling(isCharging)
    }

    private fun managePolling(isCharging: Boolean) {
        if (isCharging && pollingJob?.isActive != true) {
            startPolling()
        } else if (!isCharging) {
            stopPolling()
        }
    }

    private fun startPolling() {
        pollingJob = scope.launch {
            while (isActive) {
                delay(3000)
                _batteryState.value = _batteryState.value.copy(wattage = calculateWattage())
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun calculateWattage(): Float {
        return try {
            val currentFile = File("/sys/class/power_supply/battery/current_now")
            val voltageFile = File("/sys/class/power_supply/battery/voltage_now")
            
            if (currentFile.exists() && voltageFile.exists()) {
                val currentMicroAmps = Math.abs(currentFile.readText().trim().toFloat())
                val voltageMicroVolts = voltageFile.readText().trim().toFloat()
                (currentMicroAmps / 1_000_000f) * (voltageMicroVolts / 1_000_000f)
            } else 0f
        } catch (e: Exception) { 0f }
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
