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
import android.view.WindowInsets
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundDrawable = GradientDrawable()
    private var currentAnimator: ValueAnimator? = null
    private val contentText: TextView

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
        background = backgroundDrawable

        // Centered text for testing
        contentText = TextView(context)
        contentText.text = "Island"
        contentText.setTextColor(Color.WHITE)
        contentText.gravity = Gravity.CENTER
        contentText.alpha = 0f // Hidden initially
        addView(contentText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // If expanded, consume touch events to prevent clicks passing through
        // If collapsed, only consume if touching the island itself, but mostly we want pass-through behavior
        // for areas "around" the island if we were full screen, but since our LayoutParams size *is* the island size,
        // standard View behavior applies.

        // HOWEVER, if we are in a large container (PhoneStatusBarView), we might be blocking touches if we were larger.
        // Since we resize our LayoutParams, we only occupy the space we see.

        if (isExpanded) {
            // When expanded, we definitely want to catch touches
            return true
        }

        // When collapsed, we might want to allow tapping "through" the notch area if it was just dead space,
        // but it is the camera, so users rarely tap there.
        // Returning false would let the touch propagate to the status bar (pull down shade).
        return super.onTouchEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]

                 // Update collapsed size to match cutout roughly + padding
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

        // Apply initial collapsed state if not expanded
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

            // Fade in content
            contentText.alpha = fraction
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
             // Fade out content
            contentText.alpha = fraction
        }
        currentAnimator = anim
        anim.start()
    }
}
