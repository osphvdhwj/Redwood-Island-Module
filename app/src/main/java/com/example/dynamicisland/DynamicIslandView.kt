package com.example.dynamicisland

import android.content.Context
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.RelativeLayout
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
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val musicWaveform: View

    var collapsedWidth = 100
    var collapsedHeight = 50
    var expandedWidth = 600
    var expandedHeight = 200
    var cornerRadius = 50f

    var isExpanded = false
        private set

    init {
        backgroundDrawable.setColor(Color.BLACK)
        backgroundDrawable.cornerRadius = cornerRadius
        background = backgroundDrawable

        // Elevation WAR: Set extremely high elevation
        this.elevation = 9999f
        this.translationZ = 9999f

        // --- Notification Layout ---
        notificationContainer = LinearLayout(context)
        notificationContainer.orientation = LinearLayout.HORIZONTAL
        notificationContainer.gravity = Gravity.CENTER_VERTICAL
        notificationContainer.alpha = 0f
        notificationContainer.setPadding(35, 15, 35, 15) // Increased padding

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
        musicContainer.orientation = LinearLayout.VERTICAL
        musicContainer.gravity = Gravity.CENTER
        musicContainer.alpha = 0f
        musicContainer.setPadding(20, 20, 20, 20)

        musicTitle = TextView(context)
        musicTitle.setTextColor(Color.WHITE)
        musicTitle.textSize = 14f
        musicTitle.gravity = Gravity.CENTER

        musicArtist = TextView(context)
        musicArtist.setTextColor(Color.LTGRAY)
        musicArtist.textSize = 12f
        musicArtist.gravity = Gravity.CENTER

        val waveLayout = LinearLayout(context)
        waveLayout.orientation = LinearLayout.HORIZONTAL
        waveLayout.gravity = Gravity.CENTER
        for (i in 0..2) {
             val bar = View(context)
             bar.setBackgroundColor(Color.GREEN)
             val params = LinearLayout.LayoutParams(10, 30)
             params.setMargins(5, 0, 5, 0)
             waveLayout.addView(bar, params)
        }
        musicWaveform = waveLayout

        val vizParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        vizParams.topMargin = 25

        musicContainer.addView(musicTitle)
        musicContainer.addView(musicArtist)
        musicContainer.addView(musicWaveform, vizParams)

        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Initialize Springs
        widthSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                val params = layoutParams
                if (params != null) {
                    params.width = value.toInt()
                    layoutParams = params
                }
            }
        }

        heightSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
            addUpdateListener { _, value, _ ->
                val params = layoutParams
                if (params != null) {
                    params.height = value.toInt()
                    layoutParams = params
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            if (isExpanded) {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isExpanded) { return true }
        return super.onTouchEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]

                 // Add vertical padding to avoid hugging the top edge too tight
                 // If the user says it touches the top, let's add a small margin
                 val safeTop = rect.top
                 collapsedHeight = rect.height() + 20 // More height buffer
                 collapsedWidth = rect.width() + 50

                 post {
                     if (!isExpanded) {
                         updateLayout(collapsedWidth, collapsedHeight, safeTop)
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
            if (!isExpanded && layoutParams != null) {
                updateLayout(collapsedWidth, collapsedHeight)
            }
        }
    }

    private fun updateLayout(width: Int, height: Int, topMarginOverride: Int? = null) {
        val params = layoutParams
        if (params != null) {
            params.width = width
            params.height = height

            // If passed a specific top margin (from cutout), apply it
            if (params is MarginLayoutParams && topMarginOverride != null) {
                params.topMargin = topMarginOverride
            }

            layoutParams = params
        }

        // Force Z-Order on layout update
        this.elevation = 9999f
        this.translationZ = 9999f
        this.bringToFront()
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

        this.bringToFront()
    }

    fun updateMusicInfo(title: String?, artist: String?) {
        musicTitle.text = title ?: "Unknown Title"
        musicArtist.text = artist ?: "Unknown Artist"
    }

    fun showMusicVisualizer(show: Boolean) {
        if (show) {
            notificationContainer.alpha = 0f
            notificationContainer.visibility = View.GONE
            musicContainer.visibility = View.VISIBLE
            musicContainer.animate().alpha(1f).duration = 300
            this.bringToFront()
        } else {
            musicContainer.animate().alpha(0f).duration = 300
        }
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        this.bringToFront()

        widthSpring.cancel()
        widthSpring.setStartValue(width.toFloat())
        widthSpring.animateToFinalPosition(expandedWidth.toFloat())

        heightSpring.cancel()
        heightSpring.setStartValue(height.toFloat())
        heightSpring.animateToFinalPosition(expandedHeight.toFloat())

        // Fade in content
        if (notificationContainer.visibility == View.VISIBLE) {
            notificationContainer.animate().alpha(1f).setDuration(150).setStartDelay(50).start()
        }
        if (musicContainer.visibility == View.VISIBLE) {
            musicContainer.animate().alpha(1f).setDuration(150).setStartDelay(50).start()
        }
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        widthSpring.cancel()
        widthSpring.setStartValue(width.toFloat())
        widthSpring.animateToFinalPosition(collapsedWidth.toFloat())

        heightSpring.cancel()
        heightSpring.setStartValue(height.toFloat())
        heightSpring.animateToFinalPosition(collapsedHeight.toFloat())

        // Fade out content
        notificationContainer.animate().alpha(0f).setDuration(100).start()
        musicContainer.animate().alpha(0f).setDuration(100).start()
    }
}
