package com.example.dynamicisland

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
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

        // Centered text for testing
        contentText = TextView(context)
        contentText.text = "Island"
        contentText.setTextColor(Color.WHITE)
        contentText.gravity = Gravity.CENTER
        contentText.alpha = 0f // Hidden initially
        addView(contentText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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
