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
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.FloatValueHolder
import java.util.concurrent.TimeUnit
import kotlin.math.max

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class GestureAction { SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN, SINGLE_TAP, DOUBLE_TAP }
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

    // New Music Progress Elements
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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    init {
        // Initialize Dimensions based on Density
        collapsedWidth = dpToPx(100) // Pill width
        collapsedHeight = dpToPx(32) // Thinner pill height
        expandedWidth = dpToPx(340)  // ~650px on xhdpi
        expandedHeight = dpToPx(140) // Adjusted for new progress bar row

        // Initialize Gesture Detector
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onGestureListener?.invoke(GestureAction.SINGLE_TAP)
                return true
            }
            // NEW: Double Tap Support
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onGestureListener?.invoke(GestureAction.DOUBLE_TAP)
                return true
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

        // Initialize Background (Transparent by default as per request)
        backgroundDrawable.setColor(Color.TRANSPARENT)
        // Squircle corner radius (starting)
        backgroundDrawable.cornerRadius = dpToPx(14).toFloat()
        background = backgroundDrawable
        this.elevation = dpToPx(4).toFloat()

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
                leftMargin = dpToPx(8)
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

        // --- Music Layout (Rebuilt for Progress Bar) ---
        musicContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            alpha = 0f
            visibility = View.GONE
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
        }

        val musicTopRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }

        albumArtView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48))
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            background = GradientDrawable().apply { cornerRadius = dpToPx(8).toFloat(); setColor(Color.DKGRAY) }
        }

        val musicInfoLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dpToPx(12)
                rightMargin = dpToPx(12)
            }
        }
        musicTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        musicArtist = TextView(context).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        musicInfoLayout.addView(musicTitle)
        musicInfoLayout.addView(musicArtist)

        playPauseButton = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32))
            setImageResource(R.drawable.ic_play_vector) // Use vector if available
        }

        musicTopRow.addView(albumArtView)
        musicTopRow.addView(musicInfoLayout)
        musicTopRow.addView(playPauseButton)

        // NEW: Progress Bar Row
        val musicBottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(12)
            }
        }

        musicCurrentTime = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
            text = "0:00"
        }

        musicProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(4), 1f).apply {
                leftMargin = dpToPx(8)
                rightMargin = dpToPx(8)
            }
            progressDrawable.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
            max = 1000
        }

        musicTotalTime = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 12f
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
        }
        liveDataView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        liveProgress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(4))
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
                    // Update corner radius dynamically during animation if expanding
                    // Or keep it fixed if user prefers squircle
                    // For squircle: we keep it relative to height but capped?
                    // Let's stick to height/2 for pill look, or fixed for squircle look.
                    // The request asked for "squircle shape". Usually means a superellipse,
                    // but for Android views, a fixed corner radius that isn't fully height/2 often works.
                    // Let's keep height/2 for collapsed to match cutout, and fixed for expanded?
                    // Let's stick to height/2 for smooth transition.
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
                     // Ensure we cover the cutout but respect the "thinner" look
                     // collapsedHeight = max(dpToPx(32), cutoutHeight + dpToPx(2))
                     // Actually, if cutout is huge, we must expand to cover it.
                     val minHeight = dpToPx(32)
                     collapsedHeight = max(minHeight, cutoutHeight)

                     // Keep width pill-shaped
                     collapsedWidth = dpToPx(100)

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
        backgroundDrawable.setStroke(0, 0) // Clear stroke on expand initially

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

        // Revert to Transparent when collapsed after animation
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
