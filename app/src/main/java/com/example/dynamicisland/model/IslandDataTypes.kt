package com.example.dynamicisland.model

import android.graphics.Bitmap

data class CustomMediaAction(
    val actionName: String, 
    val iconBitmap: Bitmap?, 
    val iconResId: Int?, 
    val isEnabled: Boolean
)

// 🎛️ Holds the state of an individual Quick Settings Tile from SystemUI
data class QSTileState(
    val tileSpec: String,          // The internal ID (e.g., "wifi", "custom(com.app/...)")
    val label: String,             // The display name ("Wi-Fi", "AdGuard")
    val isActive: Boolean,         // Is it currently ON?
    val isUnavailable: Boolean,    // Is it disabled/greyed out?
    val iconBitmap: Bitmap? = null // Extracted native icon
)
