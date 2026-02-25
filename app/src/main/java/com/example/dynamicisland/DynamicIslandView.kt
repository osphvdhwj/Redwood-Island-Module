package com.example.dynamicisland

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.palette.graphics.Palette
import androidx.core.graphics.ColorUtils
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.FloatValueHolder
import kotlin.math.abs
import com.example.dynamicisland.R

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class GestureAction { SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, SINGLE_TAP }
    var onGestureListener: ((GestureAction) -> Unit)? = null

    private val backgroundDrawable = GradientDrawable()
    private lateinit var widthSpring: SpringAnimation
    private lateinit var heightSpring: SpringAnimation

    // UI Containers
    private val notificationContainer: LinearLayout
    private val musicContainer: LinearLayout
    private val liveActivityContainer: LinearLayout

    // Notification Elements
    private val iconView: ImageView
    private val titleView: TextView
    private val messageView: TextView

    // Live Activity Elements
    private val liveTitleView: TextView
    private val liveDataView: TextView
    private val liveProgressBar: ProgressBar

    // Music Elements
    private val albumArtView: ImageView
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val playPauseButton: ImageView

    // Configuration
    var collapsedWidth = 130
    var collapsedHeight = 130
    var expandedWidth = 700
    var expandedHeight = 220
    var isExpanded = false
        private set

    private var cornerRadius = collapsedHeight / 2f

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    init {
        // OLED True Black Base
        backgroundDrawable.setColor(Color.BLACK)
        backgroundDrawable.cornerRadius = collapsedHeight / 2f
        background = backgroundDrawable
        elevation = 15f

        // --- Container Setups ---
        notificationContainer = createBaseContainer()
        musicContainer = createBaseContainer()
        liveActivityContainer = createBaseContainer()

        // Notification Setup
        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(70, 70).apply { rightMargin = 30 }
        }
        val textLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleView = TextView(context).apply { setTextColor(Color.WHITE); textSize = 15f; isSingleLine = true; setTypeface(null, android.graphics.Typeface.BOLD) }
        messageView = TextView(context).apply { setTextColor(Color.LTGRAY); textSize = 13f; isSingleLine = true }
        textLayout.addView(titleView)
        textLayout.addView(messageView)
        notificationContainer.addView(iconView)
        notificationContainer.addView(textLayout, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        // Live Activity Setup
        val liveTextLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f) }
        liveTitleView = TextView(context).apply { setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true; setTypeface(null, android.graphics.Typeface.BOLD) }
        liveDataView = TextView(context).apply { setTextColor(Color.parseColor("#FF9800")); textSize = 18f; isSingleLine = true; setTypeface(android.graphics.Typeface.MONOSPACE) }
        liveProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 10).apply { topMargin = 10 }
            visibility = View.GONE
        }
        liveTextLayout.addView(liveTitleView)
        liveTextLayout.addView(liveDataView)
        liveTextLayout.addView(liveProgressBar)
        liveActivityContainer.addView(liveTextLayout)

        // Music Setup
        albumArtView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(80, 80).apply { rightMargin = 25 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val musicInfoLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f) }
        musicTitle = TextView(context).apply { setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true; setTypeface(null, android.graphics.Typeface.BOLD) }
        musicArtist = TextView(context).apply { setTextColor(Color.LTGRAY); textSize = 12f; isSingleLine = true }
        musicInfoLayout.addView(musicTitle)
        musicInfoLayout.addView(musicArtist)

        playPauseButton = ImageView(context).apply {
             layoutParams = LinearLayout.LayoutParams(60, 60).apply { leftMargin = 20 }
             setImageResource(R.drawable.ic_play_vector)
        }

        musicContainer.addView(albumArtView)
        musicContainer.addView(musicInfoLayout)
        musicContainer.addView(playPauseButton)

        addView(notificationContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(liveActivityContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setupSprings()
    }

    private fun createBaseContainer() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        alpha = 0f
        setPadding(45, 25, 45, 25)
        visibility = View.GONE
    }

    // --- Gesture Detection ---
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 80
        private val VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onGestureListener?.invoke(GestureAction.SINGLE_TAP)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (abs(diffX) > abs(diffY)) { // Horizontal
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > VELOCITY_THRESHOLD) {
                    onGestureListener?.invoke(if (diffX > 0) GestureAction.SWIPE_RIGHT else GestureAction.SWIPE_LEFT)
                    return true
                }
            } else { // Vertical
                if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > VELOCITY_THRESHOLD) {
                    onGestureListener?.invoke(if (diffY > 0) GestureAction.SWIPE_DOWN else GestureAction.SWIPE_UP)
                    return true
                }
            }
            return false
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    // --- UI Updaters ---

    fun setContextGlow(bitmap: Bitmap?) {
        if (bitmap == null) {
            backgroundDrawable.setStroke(0, Color.TRANSPARENT)
            return
        }
        Palette.from(bitmap).generate { palette ->
            val dominant = palette?.getDominantColor(Color.DKGRAY) ?: Color.DKGRAY
            val glowColor = ColorUtils.setAlphaComponent(dominant, 180)
            backgroundDrawable.setStroke(4, glowColor)
        }
    }

    fun updateLiveActivity(title: String, dataText: String, progress: Float? = null, tintColor: Int = Color.WHITE) {
        liveTitleView.text = title
        liveDataView.text = dataText
        liveDataView.setTextColor(tintColor)

        if (progress != null) {
            liveProgressBar.visibility = View.VISIBLE
            liveProgressBar.progress = (progress * 100).toInt()
        } else {
            liveProgressBar.visibility = View.GONE
        }

        switchContainer(liveActivityContainer)
    }

    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?) {
        titleView.text = title ?: "Notification"
        messageView.text = text ?: ""
        if (icon != null) {
            iconView.setImageIcon(icon)
        } else {
            iconView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        switchContainer(notificationContainer)
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        musicTitle.text = title ?: "Unknown Title"
        musicArtist.text = artist ?: "Unknown Artist"

        if (art != null) {
            albumArtView.setImageBitmap(art)
            setContextGlow(art)
        } else {
            albumArtView.setImageResource(android.R.drawable.ic_menu_gallery)
            setContextGlow(null)
        }
        switchContainer(musicContainer)
    }

    fun updatePlayPauseState(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause_vector)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_vector)
        }
    }

    private fun switchContainer(target: View) {
        listOf(notificationContainer, musicContainer, liveActivityContainer).forEach {
            if (it == target) {
                it.visibility = View.VISIBLE
                it.animate().alpha(1f).duration = 200L
            } else {
                it.animate().alpha(0f).setDuration(150L).withEndAction { it.visibility = View.GONE }
            }
        }
    }

    // --- Animation & Window Management ---

    private fun setupSprings() {
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

                if (value > 0) {
                    cornerRadius = value / 2f
                    backgroundDrawable.cornerRadius = cornerRadius
                }
            }
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]
                 val safeTop = rect.top
                 val cutoutHeight = rect.height()

                 if (cutoutHeight > 10) {
                     collapsedHeight = cutoutHeight + 4
                     collapsedWidth = collapsedHeight // Force perfect circle
                     cornerRadius = collapsedHeight / 2f
                     backgroundDrawable.cornerRadius = cornerRadius
                 }

                 post {
                     if (!isExpanded) {
                         updateWindowLayout(collapsedWidth, collapsedHeight, safeTop)
                     }
                 }
             }
        }
        return super.onApplyWindowInsets(insets)
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        val wp = windowParams
        if (wp != null && windowManager != null) {
            wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            windowManager?.updateViewLayout(this, wp)
        }

        widthSpring.cancel()
        val currentWidth = windowParams?.width?.toFloat() ?: width.toFloat()
        widthSpring.setStartValue(currentWidth)
        widthSpring.animateToFinalPosition(expandedWidth.toFloat())

        heightSpring.cancel()
        val currentHeight = windowParams?.height?.toFloat() ?: height.toFloat()
        heightSpring.setStartValue(currentHeight)
        heightSpring.animateToFinalPosition(expandedHeight.toFloat())
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        val wp = windowParams
        if (wp != null && windowManager != null) {
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            windowManager?.updateViewLayout(this, wp)
        }

        widthSpring.cancel()
        val currentWidth = windowParams?.width?.toFloat() ?: width.toFloat()
        widthSpring.setStartValue(currentWidth)
        widthSpring.animateToFinalPosition(collapsedWidth.toFloat())

        heightSpring.cancel()
        val currentHeight = windowParams?.height?.toFloat() ?: height.toFloat()
        heightSpring.setStartValue(currentHeight)
        heightSpring.animateToFinalPosition(collapsedHeight.toFloat())
    }

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
                wp.y = topMarginOverride
                changed = true
            }

            if (changed) {
                try {
                    wm.updateViewLayout(this, wp)
                } catch (e: Exception) {}
            }
        } else {
             val params = layoutParams
             if (params != null) {
                 if (width != null) params.width = width
                 if (height != null) params.height = height
                 layoutParams = params
             }
        }
    }
}
