package com.example.dynamicisland.core.manager

import android.content.Context
import android.location.LocationManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📍 ISLAND LOCATION MANAGER
 * 
 * Tracks geofences and proximity for location-based Island triggers.
 */
@Singleton
class IslandLocationManager @Inject constructor(
    private val context: Context
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
}
