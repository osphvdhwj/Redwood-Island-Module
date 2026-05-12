package com.example.dynamicisland.manager

data class IslandAction(
    val tileSpec: String? = null,
    val label: String = "",
    val iconBitmap: android.graphics.Bitmap? = null,
    val isUnavailable: Boolean = false
)