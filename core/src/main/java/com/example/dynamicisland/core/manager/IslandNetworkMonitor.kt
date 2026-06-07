package com.example.dynamicisland.core.manager

import android.net.TrafficStats
import com.example.dynamicisland.shared.model.LiveActivityModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*

@Singleton
class IslandNetworkMonitor @Inject constructor() {
    private var downloadSpeedJob: Job? = null
    private var lastRxBytes = 0L

    fun startMonitoring(
        scope: CoroutineScope,
        onUpdate: (String) -> Unit
    ) {
        if (downloadSpeedJob?.isActive == true) return
        
        lastRxBytes = TrafficStats.getTotalRxBytes()
        downloadSpeedJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(1000)
                val currentRx = TrafficStats.getTotalRxBytes()
                val bytesPerSec = currentRx - lastRxBytes
                lastRxBytes = currentRx

                val speedStr = if (bytesPerSec > 1048576) {
                    String.format("%.1f MB/s", bytesPerSec / 1048576f)
                } else {
                    String.format("%d KB/s", bytesPerSec / 1024)
                }
                onUpdate(speedStr)
            }
        }
    }

    fun stopMonitoring() {
        downloadSpeedJob?.cancel()
        downloadSpeedJob = null
    }
}
