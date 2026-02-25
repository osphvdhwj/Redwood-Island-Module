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
import kotlin.math.max

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
    private val liveProgress: ProgressBar

    // Music Elements
    private val albumArtView: ImageView
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val playPauseButton: ImageView
    private val visualizerView: LinearLayout // Placeholder for visualizer

    // Dimensions
    var collapsedWidth = 70
    var collapsedHeight = 70
    var expandedWidth = 650
    var expandedHeight = 220
    var isExpanded = false
        private set

    private var cornerRadius = collapsedHeight / 2f

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    init {
        // Initialize Background (Transparent by default as per request)
        backgroundDrawable.setColor(Color.TRANSPARENT)
        backgroundDrawable.cornerRadius = collapsedHeight / 2f
        background = backgroundDrawable
        this.elevation = 12f

        // --- Notification Layout ---
        notificationContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0f
            visibility = View.GONE
            setPadding(30, 0, 30, 0)
        }

        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(60, 60)
        }
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 20
            }
        }
        titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        messageView = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 12f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textLayout.addView(titleView)
        textLayout.addView(messageView)

        notificationContainer.addView(iconView)
        notificationContainer.addView(textLayout)

        // --- Music Layout ---
        musicContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0f
            visibility = View.GONE
            setPadding(30, 20, 30, 20)
        }

        albumArtView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(90, 90)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            background = GradientDrawable().apply { cornerRadius = 20f; setColor(Color.DKGRAY) }
        }

        val musicInfoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 25
            }
        }
        musicTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isSelected = true // For marquee
        }
        musicArtist = TextView(context).apply {
            setTextColor(Color.GRAY)
            textSize = 13f
        }
        musicInfoLayout.addView(musicTitle)
        musicInfoLayout.addView(musicArtist)

        playPauseButton = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(60, 60)
            setImageResource(android.R.drawable.ic_media_play)
        }

        visualizerView = LinearLayout(context).apply {
            // Placeholder
        }

        musicContainer.addView(albumArtView)
        musicContainer.addView(musicInfoLayout)
        musicContainer.addView(playPauseButton)

        // --- Live Activity Layout ---
        liveActivityContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = 0f
            visibility = View.GONE
            setPadding(40, 20, 40, 20)
        }
        liveTitleView = TextView(context).apply {
            setTextColor(Color.CYAN)
            textSize = 14f
        }
        liveDataView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        liveProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 10)
            progressDrawable.setColorFilter(Color.CYAN, android.graphics.PorterDuff.Mode.SRC_IN)
        }
        liveActivityContainer.addView(liveTitleView)
        liveActivityContainer.addView(liveDataView)
        liveActivityContainer.addView(liveProgress)

        addView(notificationContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(liveActivityContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // --- Animations ---
        widthSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            addUpdateListener { _, value, _ ->
                updateWindowLayout(width = value.toInt())
            }
        }
        heightSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
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
                     collapsedHeight = max(70, cutoutHeight + 4) // Ensure at least 70px
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

        // Use Black background when expanded for visibility
        backgroundDrawable.setColor(Color.BLACK)

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

        // Revert to Transparent when collapsed after animation
        postDelayed({
            if (!isExpanded) backgroundDrawable.setColor(Color.TRANSPARENT)
        }, 300)

        notificationContainer.animate().alpha(0f).setDuration(150).withEndAction { notificationContainer.visibility = View.GONE }
        musicContainer.animate().alpha(0f).setDuration(150).withEndAction { musicContainer.visibility = View.GONE }
        liveActivityContainer.animate().alpha(0f).setDuration(150).withEndAction { liveActivityContainer.visibility = View.GONE }
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

    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?) {
        titleView.text = title ?: "Notification"
        messageView.text = text ?: ""
        iconView.setImageIcon(icon ?: Icon.createWithResource(context, android.R.drawable.sym_def_app_icon))

        musicContainer.visibility = View.GONE
        liveActivityContainer.visibility = View.GONE
        notificationContainer.visibility = View.VISIBLE
        notificationContainer.animate().alpha(1f).duration = 300
    }

    fun updateMusicInfo(title: String?, artist: String?, art: Bitmap?) {
        musicTitle.text = title ?: "Unknown Title"
        musicArtist.text = artist ?: "Unknown Artist"
        if (art != null) {
            albumArtView.setImageBitmap(art)
        } else {
            albumArtView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        notificationContainer.visibility = View.GONE
        liveActivityContainer.visibility = View.GONE
        musicContainer.visibility = View.VISIBLE
        musicContainer.animate().alpha(1f).duration = 300
    }

    fun updatePlayPauseState(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause_vector)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_vector)
        }
    }

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int) {
        liveTitleView.text = title
        liveTitleView.setTextColor(color)
        liveDataView.text = data
        if (progress != null) {
            liveProgress.visibility = View.VISIBLE
            liveProgress.progress = (progress * 100).toInt()
            liveProgress.progressDrawable.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        } else {
            liveProgress.visibility = View.GONE
        }

        notificationContainer.visibility = View.GONE
        musicContainer.visibility = View.GONE
        liveActivityContainer.visibility = View.VISIBLE
        liveActivityContainer.animate().alpha(1f).duration = 300
    }

    fun setContextGlow(bitmap: Bitmap?) {
        if (bitmap != null) {
            Palette.from(bitmap).generate { palette ->
                val color = palette?.getVibrantColor(Color.DKGRAY) ?: Color.DKGRAY
                // Apply subtle glow or border if needed.
                // For now, let's tint the background stroke slightly if expanded
                if (isExpanded) {
                    backgroundDrawable.setStroke(2, color)
                } else {
                    backgroundDrawable.setStroke(0, 0)
                }
            }
        } else {
            backgroundDrawable.setStroke(0, 0)
        }
    }
}
