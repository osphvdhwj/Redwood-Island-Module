package com.example.dynamicisland

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.palette.graphics.Palette
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.FloatValueHolder
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // ADDED: LONG_PRESS
    enum class GestureAction { SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, SINGLE_TAP, DOUBLE_TAP, LONG_PRESS }
    var onGestureListener: ((GestureAction) -> Unit)? = null

    private val backgroundDrawable = GradientDrawable()
    private lateinit var widthSpring: SpringAnimation
    private lateinit var heightSpring: SpringAnimation
    private val gestureDetector: GestureDetector

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

    // Music Progress Elements
    private val musicCurrentTime: TextView
    private val musicTotalTime: TextView
    private val musicProgressBar: ProgressBar

    // Scaled Dimensions
    var collapsedWidth = 0
    var collapsedHeight = 0
    var expandedWidth = 0
    var expandedHeight = 0

    var isExpanded = false
        private set

    private var cornerRadius = 0f

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    // System Font
    private val systemFont = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val systemFontRegular = Typeface.create("sans-serif", Typeface.NORMAL)

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    init {
        // Optimized Dimensions for Poco X5 Pro
        collapsedWidth = dpToPx(108)
        collapsedHeight = dpToPx(34)
        expandedWidth = dpToPx(350)
        expandedHeight = dpToPx(146)

        // Initialize Gesture Detector
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onGestureListener?.invoke(GestureAction.SINGLE_TAP)
                return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onGestureListener?.invoke(GestureAction.DOUBLE_TAP)
                return true
            }
            // ADDED: Long Press Support
            override fun onLongPress(e: MotionEvent) {
                onGestureListener?.invoke(GestureAction.LONG_PRESS)
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > 100 && Math.abs(velocityX) > 100) {
                        if (diffX > 0) onGestureListener?.invoke(GestureAction.SWIPE_RIGHT)
                        else onGestureListener?.invoke(GestureAction.SWIPE_LEFT)
                        return true
                    }
                } else {
                    if (Math.abs(diffY) > 100 && Math.abs(velocityY) > 100) {
                        if (diffY > 0) onGestureListener?.invoke(GestureAction.SWIPE_DOWN)
                        else onGestureListener?.invoke(GestureAction.SWIPE_UP)
                        return true
                    }
                }
                return false
            }
        })

        // Initialize Background
        backgroundDrawable.setColor(Color.TRANSPARENT)
        backgroundDrawable.cornerRadius = dpToPx(16).toFloat()
        background = backgroundDrawable
        this.elevation = dpToPx(6).toFloat()

        // --- Notification Layout ---
        notificationContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0f
            visibility = View.GONE
            setPadding(dpToPx(12), 0, dpToPx(12), 0)
        }

        iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24))
        }
        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(10)
            }
        }
        titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = systemFont
        }
        messageView = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 12f
            typeface = systemFontRegular
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textLayout.addView(titleView)
        textLayout.addView(messageView)

        notificationContainer.addView(iconView)
        notificationContainer.addView(textLayout)

        // --- Music Layout ---
        musicContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            alpha = 0f
            visibility = View.GONE
            setPadding(dpToPx(18), dpToPx(14), dpToPx(18), dpToPx(14))
        }

        val musicTopRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        albumArtView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(50), dpToPx(50))
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            background = GradientDrawable().apply { cornerRadius = dpToPx(10).toFloat(); setColor(Color.DKGRAY) }
        }

        val musicInfoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(14)
                rightMargin = dpToPx(14)
            }
        }
        musicTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = systemFont
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        musicArtist = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            typeface = systemFontRegular
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        musicInfoLayout.addView(musicTitle)
        musicInfoLayout.addView(musicArtist)

        playPauseButton = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(34), dpToPx(34))
            setImageResource(R.drawable.ic_play_vector)
        }

        musicTopRow.addView(albumArtView)
        musicTopRow.addView(musicInfoLayout)
        musicTopRow.addView(playPauseButton)

        // Progress Bar Row
        val musicBottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(14)
            }
        }

        musicCurrentTime = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 11f
            typeface = systemFontRegular
            text = "0:00"
        }

        musicProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(4), 1f).apply {
                leftMargin = dpToPx(10)
                rightMargin = dpToPx(10)
            }
            progressDrawable.colorFilter = BlendModeColorFilter(Color.WHITE, BlendMode.SRC_IN)
            max = 1000
        }

        musicTotalTime = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 11f
            typeface = systemFontRegular
            text = "0:00"
        }

        musicBottomRow.addView(musicCurrentTime)
        musicBottomRow.addView(musicProgressBar)
        musicBottomRow.addView(musicTotalTime)

        musicContainer.addView(musicTopRow)
        musicContainer.addView(musicBottomRow)

        // --- Live Activity Layout ---
        liveActivityContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = 0f
            visibility = View.GONE
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
        }
        liveTitleView = TextView(context).apply {
            setTextColor(Color.CYAN)
            textSize = 14f
            typeface = systemFont
        }
        liveDataView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = systemFont
        }
        liveProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(4))
            progressDrawable.colorFilter = BlendModeColorFilter(Color.CYAN, BlendMode.SRC_IN)
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val cutout = insets.displayCutout
        if (cutout != null) {
             val rects = cutout.boundingRects
             if (rects.isNotEmpty()) {
                 val rect = rects[0]
                 val safeTop = rect.top
                 val cutoutHeight = rect.height()

                 if (cutoutHeight > 0) {
                     val minHeight = dpToPx(34)
                     collapsedHeight = max(minHeight, cutoutHeight)
                     collapsedWidth = dpToPx(108)

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

        backgroundDrawable.setColor(Color.BLACK)
        backgroundDrawable.setStroke(0, 0)

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
            wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
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

        postDelayed({
            if (!isExpanded) {
                backgroundDrawable.setColor(Color.TRANSPARENT)
                backgroundDrawable.setStroke(0, 0)
            }
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
        musicTitle.text = title ?: "Unknown"
        musicArtist.text = artist ?: "Unknown"
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

    fun updateMusicProgress(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return

        val progressPercent = ((positionMs.toFloat() / durationMs.toFloat()) * 1000).toInt()
        musicProgressBar.progress = progressPercent

        musicCurrentTime.text = formatTime(positionMs)
        musicTotalTime.text = formatTime(durationMs)
    }

    private fun formatTime(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    fun updateLiveActivity(title: String, data: String, progress: Float?, color: Int) {
        liveTitleView.text = title
        liveTitleView.setTextColor(color)
        liveDataView.text = data
        if (progress != null) {
            liveProgress.visibility = View.VISIBLE
            liveProgress.progress = (progress * 100).toInt()
            liveProgress.progressDrawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
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
                if (isExpanded) {
                    backgroundDrawable.setStroke(dpToPx(1), color)
                } else {
                    backgroundDrawable.setStroke(0, 0)
                }
            }
        } else {
            backgroundDrawable.setStroke(0, 0)
        }
    }
}
