package com.example.dynamicisland

data class LiveActivityModel(
    val id: String,
    val title: String,
    val dataText: String,
    val progress: Float? = null,
    val accentColor: Int = android.graphics.Color.WHITE
)
