package com.example.dynamicisland.core.manager

import android.content.Context
import android.location.LocationManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📍 ISLAND LOCATION MANAGER
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*
import com.example.dynamicisland.shared.model.*
 * 
 * Tracks geofences and proximity for location-based Island triggers.
 */
@Singleton
class IslandLocationManager @Inject constructor(
    private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}
