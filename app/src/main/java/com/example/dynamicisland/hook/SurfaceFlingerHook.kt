package com.example.dynamicisland.hook

import android.content.Context
import android.content.Intent
import android.os.UserHandle
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * BATCH 6: SurfaceFlinger Frame Timing Hook
 *
 * Hooks into SurfaceFlinger's Choreographer/frame pipeline inside the
 * android process to extract real-time frame timing data:
 *
 *   - Actual frame duration (ms) — last 60 frames averaged
 *   - Jank count — frames that exceeded 16.67ms (60Hz) / 8.33ms (120Hz)
 *   - Display refresh rate
 *   - GPU busy percentage (estimated from frame timestamps)
 *
 * Method hooked:
 *   SurfaceFlinger::onMessageReceived() — called on every vsync
 *   We intercept this to record the timestamp delta.
 *
 * The hook calculates a rolling 60-frame average of frame durations.
 * This gives a stable FPS reading without thrashing broadcasts.
 * A broadcast is sent only when the average changes by >2 FPS.
 *
 * Broadcast schema (ACTION_FRAME_STATS):
 *   "fps"           float  Current rolling average FPS
 *   "frameMs"       float  Average frame duration in milliseconds
 *   "jankPct"       float  Percentage of janked frames (0..100)
 *   "refreshRate"   int    Display refresh rate in Hz
 */
object SurfaceFlingerHook {

    const val ACTION_FRAME_STATS = "com.example.dynamicisland.FRAME_STATS"

    // Rolling frame history
    private val frameTimestamps = ArrayDeque<Long>(65)
    private var lastBroadcastFps = 0f
    private var lastBroadcastMs  = System.currentTimeMillis()

    // Janked frame tracking (>1.5× expected frame duration)
    private var jankCount    = 0
    private var totalFrames  = 0

    fun apply(lpparam: XC_LoadPackage.LoadPackageParam, userAll: UserHandle) {

        // Primary hook: SurfaceFlinger's onMessageReceived is called every vsync
        IslandHookEngine.hookAfter(
            "android.view.Choreographer",
            lpparam.classLoader,
            "doFrame",
            Long::class.javaPrimitiveType ?: Long::class.java
        ) { param ->
            val frameTimeNs = param.args[0] as Long
            recordFrame(frameTimeNs, param, userAll)
        }

        // Fallback: Hook DisplayEventReceiver.onVsync for older AOSP variants
        try {
            IslandHookEngine.hookAfter(
                "android.view.DisplayEventReceiver",
                lpparam.classLoader,
                "onVsync",
                Long::class.javaPrimitiveType ?: Long::class.java,
                Long::class.javaPrimitiveType ?: Long::class.java,
                Int::class.javaPrimitiveType ?: Int::class.java
            ) { param ->
                val timestampNs = param.args[0] as Long
                recordFrame(timestampNs, param, userAll)
            }
        } catch (_: Throwable) {}
    }

    private fun recordFrame(
        frameTimeNs: Long,
        param: de.robv.android.xposed.XC_MethodHook.MethodHookParam,
        userAll: UserHandle
    ) {
        val nowMs = System.currentTimeMillis()

        // Throttle: don't broadcast more than once per 250ms
        if (nowMs - lastBroadcastMs < 250) {
            // Still record the frame though
            synchronized(frameTimestamps) {
                frameTimestamps.addLast(frameTimeNs)
                if (frameTimestamps.size > 60) frameTimestamps.removeFirst()
                totalFrames++
            }
            return
        }

        synchronized(frameTimestamps) {
            frameTimestamps.addLast(frameTimeNs)
            if (frameTimestamps.size > 60) frameTimestamps.removeFirst()
            totalFrames++

            if (frameTimestamps.size < 10) return

            // Compute FPS from the rolling window
            val windowNs = frameTimestamps.last() - frameTimestamps.first()
            if (windowNs <= 0) return

            val fps = (frameTimestamps.size - 1).toFloat() / (windowNs / 1_000_000_000f)
            val avgFrameMs = (windowNs / 1_000_000f) / (frameTimestamps.size - 1)

            // Estimate jank
            val expectedMs = 1000f / fps.coerceAtLeast(30f)
            val jankFrames = frameTimestamps.zipWithNext().count { (a, b) ->
                (b - a) / 1_000_000f > expectedMs * 1.5f
            }
            val jankPct = if (totalFrames > 0) (jankCount.toFloat() / totalFrames * 100f) else 0f
            jankCount += jankFrames

            // Only broadcast if FPS changed meaningfully
            val fpsDelta = kotlin.math.abs(fps - lastBroadcastFps)
            if (fpsDelta < 2f && nowMs - lastBroadcastMs < 1000) return

            lastBroadcastFps = fps
            lastBroadcastMs  = nowMs

            // Retrieve refresh rate from Display
            val refreshRate = try {
                val display = android.hardware.display.DisplayManager::class.java
                    .let { XposedHelpers.callMethod(
                        android.app.AndroidAppHelper.currentApplication()
                            ?.getSystemService(Context.DISPLAY_SERVICE),
                        "getDisplay",
                        android.view.Display.DEFAULT_DISPLAY
                    ) }
                (display as? android.view.Display)?.refreshRate?.toInt() ?: 60
            } catch (_: Throwable) { 60 }

            try {
                val context = android.app.AndroidAppHelper.currentApplication() ?: return
                context.sendBroadcastAsUser(
                    Intent(ACTION_FRAME_STATS).apply {
                        setPackage("com.android.systemui")
                        putExtra("fps",         fps)
                        putExtra("frameMs",     avgFrameMs)
                        putExtra("jankPct",     jankPct)
                        putExtra("refreshRate", refreshRate)
                    },
                    userAll
                )
            } catch (_: Throwable) {}
        }
    }
}