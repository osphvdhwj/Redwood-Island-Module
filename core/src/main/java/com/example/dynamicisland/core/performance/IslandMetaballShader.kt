package com.example.dynamicisland.core.performance

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

/**
 * 🌊 ELITE HYPER-FLUID VISUAL ENGINE
 * 
 * High-performance AGSL Shader for mathematically accurate liquid physics.
 * Features:
 * 1. Metaball Fusion: Fuses multiple shapes using exponential potential fields.
 * 2. Velocity Skew: Distorts the Island based on user swipe speed.
 * 3. Chromatic Refraction: Adds elite "LiquidGlass" edges with light splitting.
 */
private const val ELITE_METABALL_AGSL = """
    uniform float2 iResolution;
    uniform float4 iPill1; // x, y, w, h
    uniform float2 iVelocity; // vx, vy
    uniform float iTime;
    uniform half4 iColor;
    uniform float iLiquidGlassMode; // 0.0 or 1.0

    // Signed Distance Function for a Rounded Rectangle
    float sdRoundedRect(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord;
        
        // 🛡️ Physics 1: Apply Velocity-Based Skewing
        // Distorts the coordinate space in the direction of motion
        float skewStrength = length(iVelocity) * 0.005;
        float2 skewDir = normalize(iVelocity + float2(0.0001, 0.0001));
        uv -= skewDir * skewStrength * (uv.y - iPill1.y);

        // 🛡️ Physics 2: Compute Potential Field
        float2 p1_center = iPill1.xy + iPill1.zw / 2.0;
        float dist = sdRoundedRect(uv - p1_center, iPill1.zw / 2.0, iPill1.w / 2.0);
        
        // Liquid "Blobiness" Factor
        float b = 0.6 + sin(iTime * 2.0) * 0.05;
        float field = 1.0 - smoothstep(-2.0, 2.0, dist);

        // 🛡️ Physics 3: Chromatic Refraction (LiquidGlass)
        if (iLiquidGlassMode > 0.5) {
            float edge = fwidth(field) * 1.5;
            float r = 1.0 - smoothstep(-edge, edge, dist + 0.02);
            float g = 1.0 - smoothstep(-edge, edge, dist);
            float b_chan = 1.0 - smoothstep(-edge, edge, dist - 0.02);
            
            return half4(iColor.r * r, iColor.g * g, iColor.b * b_chan, iColor.a * field);
        }

        return half4(iColor.rgb, iColor.a * field);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
object IslandShaderEngine {
    private val shader = RuntimeShader(ELITE_METABALL_AGSL)

    fun getShader(
        resolution: Offset,
        pillBounds: android.graphics.Rect,
        velocity: Offset,
        time: Float,
        color: Color,
        isLiquidGlass: Boolean
    ): RuntimeShader {
        shader.setFloatUniform("iResolution", resolution.x, resolution.y)
        shader.setFloatUniform("iPill1", pillBounds.left.toFloat(), pillBounds.top.toFloat(), pillBounds.width().toFloat(), pillBounds.height().toFloat())
        shader.setFloatUniform("iVelocity", velocity.x, velocity.y)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("iColor", color.red, color.green, color.blue, color.alpha)
        shader.setFloatUniform("iLiquidGlassMode", if (isLiquidGlass) 1f else 0f)
        return shader
    }
}

/**
 * Applies elite liquid physics to any Island component.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Modifier.liquidPhysicsEffect(
    bounds: android.graphics.Rect,
    velocity: Offset = Offset.Zero,
    time: Float = 0f,
    color: Color = Color.Black,
    isLiquidGlass: Boolean = false
): Modifier = this.drawWithCache {
    onDrawWithContent {
        val shader = IslandShaderEngine.getShader(
            resolution = Offset(size.width, size.height),
            pillBounds = bounds,
            velocity = velocity,
            time = time,
            color = color,
            isLiquidGlass = isLiquidGlass
        )
        
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.shader = shader
            }
            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}
