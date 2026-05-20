package com.example.dynamicisland.util

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pillar 5: Performance Optimization
 * Ensures the Compose UI uses Hardware-Accelerated `RenderEffect.createBlurEffect`
 * on supported devices (Android 12+) instead of falling back to legacy software blurs.
 */
fun Modifier.hardwareBlur(radius: Dp): Modifier {
    if (radius <= 0.dp) return this

    return this.graphicsLayer {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadius = radius.toPx()
            if (blurRadius > 0) {
                renderEffect = RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.DECAL
                ).asComposeRenderEffect()
            }
        } else {
            // Provide no-op or fallback for older devices to prevent software blur lag
            alpha = 0.95f
        }
    }
}
