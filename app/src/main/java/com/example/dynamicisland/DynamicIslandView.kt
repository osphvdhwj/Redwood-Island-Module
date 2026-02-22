package com.example.dynamicisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.DisplayCutout
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.View
import android.view.WindowInsets
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundDrawable = GradientDrawable()
    private var currentAnimator: ValueAnimator? = null
    private val contentText: TextView
    private val musicContainer: LinearLayout
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val musicVisualizer: View // Placeholder for visualizer

    // Default dimensions (in pixels) - will be updated by config
    var collapsedWidth = 100
    var collapsedHeight = 50
    var expandedWidth = 600
    var expandedHeight = 200
    var cornerRadius = 50f

    var isExpanded = false
        private set

    init {
        // True Black for OLED
        backgroundDrawable.setColor(Color.BLACK)
        backgroundDrawable.cornerRadius = cornerRadius
        background = backgroundDrawable

        // Generic Content (Notifications)
        contentText = TextView(context)
        contentText.text = "Notification"
        contentText.setTextColor(Color.WHITE)
        contentText.gravity = Gravity.CENTER
        contentText.alpha = 0f
        addView(contentText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // Music Content
        musicContainer = LinearLayout(context)
        musicContainer.orientation = LinearLayout.VERTICAL
        musicContainer.gravity = Gravity.CENTER
        musicContainer.alpha = 0f

        musicTitle = TextView(context)
        musicTitle.setTextColor(Color.WHITE)
        musicTitle.textSize = 14f
        musicTitle.gravity = Gravity.CENTER

        musicArtist = TextView(context)
        musicArtist.setTextColor(Color.LTGRAY)
        musicArtist.textSize = 12f
        musicArtist.gravity = Gravity.CENTER

        musicVisualizer = View(context)
        musicVisualizer.setBackgroundColor(Color.GREEN) // Placeholder visualizer
        val vizParams = LinearLayout.LayoutParams(100, 10)
        vizParams.topMargin = 20

        musicContainer.addView(musicTitle)
        musicContainer.addView(musicArtist)
        musicContainer.addView(musicVisualizer, vizParams)

        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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
        if (isExpanded) {
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]

                 collapsedHeight = rect.height() + 10
                 collapsedWidth = rect.width() + 40

                 post {
                     if (!isExpanded) {
                        updateLayout(collapsedWidth, collapsedHeight)
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

        if (!isExpanded && layoutParams != null) {
            updateLayout(collapsedWidth, collapsedHeight)
        }
    }

    private fun updateLayout(width: Int, height: Int) {
        val params = layoutParams
        if (params != null) {
            params.width = width
            params.height = height
            layoutParams = params
        }
    }

    fun updateMusicInfo(title: String?, artist: String?) {
        musicTitle.text = title ?: "Unknown Title"
        musicArtist.text = artist ?: "Unknown Artist"
    }

    fun showMusicVisualizer(show: Boolean) {
        // Toggle between music view and default view?
        // For now, if music is playing, we show music container
        if (show) {
            contentText.alpha = 0f
            musicContainer.animate().alpha(1f).duration = 300
        } else {
            musicContainer.animate().alpha(0f).duration = 300
        }
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        currentAnimator?.cancel()

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 400
        anim.interpolator = OvershootInterpolator()
        anim.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val currentWidth = (collapsedWidth + (expandedWidth - collapsedWidth) * fraction).toInt()
            val currentHeight = (collapsedHeight + (expandedHeight - collapsedHeight) * fraction).toInt()
            updateLayout(currentWidth, currentHeight)
        }
        currentAnimator = anim
        anim.start()
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        currentAnimator?.cancel()

        val anim = ValueAnimator.ofFloat(1f, 0f)
        anim.duration = 300
        anim.interpolator = OvershootInterpolator()
        anim.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val currentWidth = (collapsedWidth + (expandedWidth - collapsedWidth) * fraction).toInt()
            val currentHeight = (collapsedHeight + (expandedHeight - collapsedHeight) * fraction).toInt()
            updateLayout(currentWidth, currentHeight)

            // Fade out all content
            contentText.alpha = 0f
            musicContainer.alpha = 0f
        }
        currentAnimator = anim
        anim.start()
    }
}
