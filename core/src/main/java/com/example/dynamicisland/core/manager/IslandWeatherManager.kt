package com.example.dynamicisland.core.manager

import android.content.Context
import android.graphics.Color
import com.example.dynamicisland.shared.model.LiveActivityModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IslandWeatherManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onWeatherUpdate: (LiveActivityModel.WeatherMood) -> Unit
) {
    private val mockConditions = listOf(
        Pair("Sunny", Color.parseColor("#FFD700")),
        Pair("Rainy", Color.parseColor("#4A90E2")),
        Pair("Cloudy", Color.parseColor("#B0BEC5")),
        Pair("Thunderstorm", Color.parseColor("#800080"))
    )

    fun startPolling() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                // In a production environment, this would query a ContentProvider, 
                // broadcast receiver, or an API like OpenWeather.
                // For now, we simulate changing weather conditions every 30 minutes.
                
                val currentCondition = mockConditions.random()
                
                val weatherModel = LiveActivityModel.WeatherMood(
                    condition = currentCondition.first,
                    color = currentCondition.second
                )
                
                onWeatherUpdate(weatherModel)
                
                delay(30 * 60 * 1000L) // Wait 30 minutes
            }
        }
    }
}
