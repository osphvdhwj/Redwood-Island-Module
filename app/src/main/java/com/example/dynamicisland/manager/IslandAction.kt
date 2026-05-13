package com.example.dynamicisland.manager

import android.graphics.Bitmap
data class IslandAction(
    val tileSpec: String? = null,
    val label: String = "",
    val iconRes: Int = 0,
    val isUnavailable: Boolean = false
)