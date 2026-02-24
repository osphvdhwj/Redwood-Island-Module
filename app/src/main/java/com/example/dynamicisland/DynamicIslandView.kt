package com.example.dynamicisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.util.AttributeSet
import android.view.DisplayCutout
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.palette.graphics.Palette
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.FloatValueHolder

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundDrawable = GradientDrawable()

    private lateinit var widthSpring: SpringAnimation
    private lateinit var heightSpring: SpringAnimation

    private val notificationContainer: LinearLayout
    private val iconView: ImageView
    private val textContainer: LinearLayout
    private val titleView: TextView
    private val messageView: TextView

    private val musicContainer: LinearLayout
    private val albumArtView: ImageView
    private val musicInfoContainer: LinearLayout
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val musicWaveform: View
    private val playPauseButton: ImageView

    var collapsedWidth = 100
    var collapsedHeight = 50
    var expandedWidth = 600
    var expandedHeight = 200
    var cornerRadius = 50f

    var isExpanded = false
        private set

    // WindowManager support
    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    init {
        // True Black for OLED
        backgroundDrawable.setColor(Color.BLACK)
        backgroundDrawable.cornerRadius = cornerRadius
        background = backgroundDrawable

        // Elevation WAR (Less critical for Overlay, but good practice)
        this.elevation = 9999f
        this.translationZ = 9999f

        // --- Notification Layout ---
        notificationContainer = LinearLayout(context)
        notificationContainer.orientation = LinearLayout.HORIZONTAL
        notificationContainer.gravity = Gravity.CENTER_VERTICAL
        notificationContainer.alpha = 0f
        notificationContainer.setPadding(35, 15, 35, 15)

        iconView = ImageView(context)
        val iconParams = LinearLayout.LayoutParams(60, 60)
        iconParams.rightMargin = 25
        notificationContainer.addView(iconView, iconParams)

        textContainer = LinearLayout(context)
        textContainer.orientation = LinearLayout.VERTICAL

        titleView = TextView(context)
        titleView.setTextColor(Color.WHITE)
        titleView.textSize = 13f
        titleView.setSingleLine()

        messageView = TextView(context)
        messageView.setTextColor(Color.LTGRAY)
        messageView.textSize = 12f
        messageView.setSingleLine()

        textContainer.addView(titleView)
        textContainer.addView(messageView)

        val textParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT)
        textParams.weight = 1f
        notificationContainer.addView(textContainer, textParams)

        addView(notificationContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // --- Music Layout ---
        musicContainer = LinearLayout(context)
        musicContainer.orientation = LinearLayout.HORIZONTAL
        musicContainer.gravity = Gravity.CENTER_VERTICAL
        musicContainer.alpha = 0f
        musicContainer.setPadding(30, 20, 30, 20)

        // 1. Album Art
        albumArtView = ImageView(context)
        albumArtView.scaleType = ImageView.ScaleType.CENTER_CROP
        val artParams = LinearLayout.LayoutParams(120, 120)
        artParams.rightMargin = 25

        albumArtView.clipToOutline = true
        albumArtView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 60f
            setColor(Color.DKGRAY)
        }
        albumArtView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width / 2f)
            }
        }

        musicContainer.addView(albumArtView, artParams)

        // 2. Info
        musicInfoContainer = LinearLayout(context)
        musicInfoContainer.orientation = LinearLayout.VERTICAL
        musicInfoContainer.gravity = Gravity.CENTER_VERTICAL or Gravity.START

        musicTitle = TextView(context)
        musicTitle.setTextColor(Color.WHITE)
        musicTitle.textSize = 14f
        musicTitle.setSingleLine()
        musicTitle.gravity = Gravity.START

        musicArtist = TextView(context)
        musicArtist.setTextColor(Color.LTGRAY)
        musicArtist.textSize = 12f
        musicArtist.setSingleLine()
        musicArtist.gravity = Gravity.START

        val waveLayout = LinearLayout(context)
        waveLayout.orientation = LinearLayout.HORIZONTAL
        waveLayout.gravity = Gravity.START
        for (i in 0..2) {
             val bar = View(context)
             bar.setBackgroundColor(Color.GREEN)
             val params = LinearLayout.LayoutParams(10, 30)
             params.setMargins(0, 0, 10, 0)
             waveLayout.addView(bar, params)
        }
        musicWaveform = waveLayout

        val vizParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        vizParams.topMargin = 10

        musicInfoContainer.addView(musicTitle)
        musicInfoContainer.addView(musicArtist)
        musicInfoContainer.addView(musicWaveform, vizParams)

        val infoParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT)
        infoParams.weight = 1f
        musicContainer.addView(musicInfoContainer, infoParams)

        // 3. Play/Pause
        playPauseButton = ImageView(context)
        playPauseButton.setImageResource(R.drawable.ic_play_vector)
        playPauseButton.scaleType = ImageView.ScaleType.FIT_CENTER
        val btnParams = LinearLayout.LayoutParams(80, 80)
        btnParams.leftMargin = 25
        musicContainer.addView(playPauseButton, btnParams)

        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Initialize Springs
        widthSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                updateWindowLayout(width = value.toInt())
            }
        }

        heightSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                updateWindowLayout(height = value.toInt())

                // Ensure corner radius is always height / 2 (Perfect Circle/Pill)
                if (value > 0) {
                    cornerRadius = value / 2f
                    backgroundDrawable.cornerRadius = cornerRadius
                }
            }
        }

        // Ensure we are clickable
        isClickable = true
        isFocusable = true
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            // When expanded, we handle touch. When collapsed, maybe ignore?
            // Actually, FLAG_NOT_TOUCH_MODAL handles outside touches.
        }
        // Handle collapse on outside touch if window manager sends ACTION_OUTSIDE
        if (ev?.action == MotionEvent.ACTION_OUTSIDE) {
            if (isExpanded) {
                collapse()
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Allow touch events to propagate so OnClickListener works
        return super.onTouchEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]
                 val safeTop = rect.top

                 // Fix Massive Pill Size: Reduce padding to match hardware cutout
                 collapsedHeight = rect.height() + 4
                 // Force PERFECT CIRCLE when collapsed
                 collapsedWidth = collapsedHeight

                 // Ensure corner radius creates a perfect squircle
                 cornerRadius = collapsedHeight / 2f
                 backgroundDrawable.cornerRadius = cornerRadius

                 post {
                     if (!isExpanded) {
                         // Update Window Layout directly
                         updateWindowLayout(collapsedWidth, collapsedHeight, safeTop)
                     }
                 }
             }
        }
        return super.onApplyWindowInsets(insets)
    }

    fun setDimensions(cW: Int, cH: Int, eW: Int, eH: Int) {
        collapsedWidth = cW
        collapsedHeight = cH
        expandedWidth = eW
        expandedHeight = eH

        post {
            if (!isExpanded) {
                updateWindowLayout(collapsedWidth, collapsedHeight)
            }
        }
    }

    // Helper to update WindowManager layout params
    private fun updateWindowLayout(width: Int? = null, height: Int? = null, topMarginOverride: Int? = null) {
        val wm = windowManager
        val wp = windowParams

        if (wm != null && wp != null) {
            var changed = false
            if (width != null && wp.width != width) {
                wp.width = width
                changed = true
            }
            if (height != null && wp.height != height) {
                wp.height = height
                changed = true
            }
            if (topMarginOverride != null && wp.y != topMarginOverride) {
                wp.y = topMarginOverride // For Gravity.TOP, y is top margin equivalent
                changed = true
            }

            if (changed) {
                try {
                    wm.updateViewLayout(this, wp)
                } catch (e: Exception) {
                    // Handle potential race conditions or detached view
                }
            }
        } else {
            // Fallback for non-WM usage (if any) or initial layout
            val params = layoutParams
            if (params != null) {
                if (width != null) params.width = width
                if (height != null) params.height = height
                layoutParams = params
            }
        }
    }

    // For compatibility, keep updateLayout but delegate
    private fun updateLayout(width: Int, height: Int, topMarginOverride: Int? = null) {
        updateWindowLayout(width, height, topMarginOverride)
    }

    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?) {
        titleView.text = title ?: "Notification"
        messageView.text = text ?: "Tap to view"
        if (icon != null) {
            iconView.setImageIcon(icon)
        } else {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        musicContainer.alpha = 0f
        musicContainer.visibility = View.GONE
        notificationContainer.visibility = View.VISIBLE
        notificationContainer.animate().alpha(1f).duration = 200

        backgroundDrawable.setStroke(0, Color.TRANSPARENT)
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        musicTitle.text = title ?: "Unknown Title"
        musicArtist.text = artist ?: "Unknown Artist"

        if (art != null) {
            albumArtView.setImageBitmap(art)

            Palette.from(art).generate { palette ->
                val vibrant = palette?.getVibrantColor(Color.BLACK) ?: Color.BLACK
                val dominant = palette?.getDominantColor(Color.BLACK) ?: Color.BLACK

                var finalColor = if (vibrant != Color.BLACK) vibrant else dominant

                if (ColorUtils.calculateLuminance(finalColor) < 0.1) {
                    finalColor = Color.LTGRAY
                }

                backgroundDrawable.setStroke(5, finalColor)
            }
        } else {
            albumArtView.setImageResource(android.R.drawable.ic_menu_gallery)
            albumArtView.setBackgroundColor(Color.DKGRAY)
            backgroundDrawable.setStroke(0, Color.TRANSPARENT)
        }
    }

    fun updatePlayPauseState(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause_vector)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_vector)
        }
    }

    fun showMusicVisualizer(show: Boolean) {
        if (show) {
            notificationContainer.alpha = 0f
            notificationContainer.visibility = View.GONE
            musicContainer.visibility = View.VISIBLE
            musicContainer.animate().alpha(1f).duration = 300
        } else {
            musicContainer.animate().alpha(0f).duration = 300
        }
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        // No need to bringToFront() for WindowManager

        widthSpring.cancel()
        // Use current window width as start value if available
        val currentWidth = windowParams?.width?.toFloat() ?: width.toFloat()
        widthSpring.setStartValue(currentWidth)
        widthSpring.animateToFinalPosition(expandedWidth.toFloat())

        heightSpring.cancel()
        val currentHeight = windowParams?.height?.toFloat() ?: height.toFloat()
        heightSpring.setStartValue(currentHeight)
        heightSpring.animateToFinalPosition(expandedHeight.toFloat())

        // Fade in content IMMEDIATELY
        if (notificationContainer.visibility == View.VISIBLE) {
            notificationContainer.animate().alpha(1f).setDuration(150).start()
        }
        if (musicContainer.visibility == View.VISIBLE) {
            musicContainer.animate().alpha(1f).setDuration(150).start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        widthSpring.cancel()
        val currentWidth = windowParams?.width?.toFloat() ?: width.toFloat()
        widthSpring.setStartValue(currentWidth)
        widthSpring.animateToFinalPosition(collapsedWidth.toFloat())

        heightSpring.cancel()
        val currentHeight = windowParams?.height?.toFloat() ?: height.toFloat()
        heightSpring.setStartValue(currentHeight)
        heightSpring.animateToFinalPosition(collapsedHeight.toFloat())

        // Fade out content
        notificationContainer.animate().alpha(0f).setDuration(100).start()
        musicContainer.animate().alpha(0f).setDuration(100).start()
    }
}
