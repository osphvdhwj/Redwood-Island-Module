package com.example.dynamicisland.performance

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * Metaball Shader for Dynamic Island.
 * Creates a fluid "tearing" effect when the island splits into two.
 */
private const val METABALL_AGSL = """
    uniform float2 iResolution;
    uniform float4 iPill1; // x, y, w, h
    uniform float4 iPill2; // x, y, radius, blobiness
    uniform half4 iColor;

    float sdRoundedRect(float2 p, float2 b, float4 r) {
        float2 q = abs(p) - b + float2(r.x, r.y);
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord;
        
        // Potential field from Pill 1 (Main Pill)
        float2 p1_center = iPill1.xy + iPill1.zw / 2.0;
        float d1 = sdRoundedRect(uv - p1_center, iPill1.zw / 2.0, float4(iPill1.w / 2.0));
        
        // Potential field from Pill 2 (Split Circle)
        float d2 = length(uv - iPill2.xy) - iPill2.z;
        
        // Metaball fusion logic
        float b = iPill2.w; // Blobiness factor
        float res = exp(-b * d1) + exp(-b * d2);
        float field = -log(res) / b;
        
        float alpha = 1.0 - smoothstep(-1.0, 1.0, field);
        return half4(iColor.rgb, iColor.a * alpha);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.metaballFluid(
    pill1: android.graphics.Rect,
    pill2Center: android.graphics.Offset,
    pill2Radius: Float,
    blobiness: Float = 0.4f,
    color: Color = Color.Black
): Modifier = this.drawWithCache {
    val shader = RuntimeShader(METABALL_AGSL)
    onDrawWithContent {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iPill1", pill1.left.toFloat(), pill1.top.toFloat(), pill1.width().toFloat(), pill1.height().toFloat())
        shader.setFloatUniform("iPill2", pill2Center.x, pill2Center.y, pill2Radius, blobiness)
        shader.setFloatUniform("iColor", color.red, color.green, color.blue, color.alpha)
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.shader = shader
            }
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}
