package com.example.dynamicisland.core.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.example.dynamicisland.core.domain.state.IslandController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 🏝️ DYNAMIC ISLAND OVERLAY SERVICE
 * 
 * The standalone service that hosts the Dynamic Island UI.
 * Runs in the Core App process and uses root/system permissions to render over all apps.
 */
@AndroidEntryPoint
class DynamicIslandOverlayService : Service() {

    @Inject
    lateinit var controller: IslandController

    private lateinit var windowManager: WindowManager
    private var islandView: DynamicIslandView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    private fun showOverlay() {
        if (islandView != null) return

        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }

        islandView = DynamicIslandView(this, this)
        controller.createIslandView(islandView!!)
        controller.start(this)

        windowManager.addView(islandView, params)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        islandView?.let { windowManager.removeView(it) }
        islandView = null
        controller.destroy()
        super.onDestroy()
    }
}
