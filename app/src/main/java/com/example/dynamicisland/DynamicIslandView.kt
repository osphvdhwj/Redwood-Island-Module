package com.example.dynamicisland

import android.animation.ValueAnimator
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

class DynamicIslandView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val backgroundDrawable = GradientDrawable()
    private lateinit var widthSpring: SpringAnimation
    private lateinit var heightSpring: SpringAnimation

    // Notification UI
    private val notificationContainer: LinearLayout
    private val iconView: ImageView
    private val titleView: TextView
    private val messageView: TextView

    // Music UI
    private val musicContainer: LinearLayout
    private val albumArtView: ImageView
    private val musicTitle: TextView
    private val musicArtist: TextView
    private val playPauseButton: ImageView

    // Updated dimensions for better fit
    var collapsedWidth = 70 // Reduced from 120
    var collapsedHeight = 70 // Reduced from 120
    var expandedWidth = 650
    var expandedHeight = 220
    var isExpanded = false
        private set

    var windowManager: WindowManager? = null
    var windowParams: WindowManager.LayoutParams? = null

    init {
        // Transparent when collapsed to blend with punch hole
        backgroundDrawable.setColor(Color.TRANSPARENT)
        backgroundDrawable.cornerRadius = collapsedHeight / 2f
        background = backgroundDrawable
        this.elevation = 12f

        // --- Notification Layout ---
        notificationContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0f
            setPadding(40, 20, 40, 20)
            visibility = View.GONE
        }

        iconView = ImageView(context)
        val iconParams = LinearLayout.LayoutParams(70, 70).apply { rightMargin = 30 }

        val textLayout = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleView = TextView(context).apply { setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true }
        messageView = TextView(context).apply { setTextColor(Color.LTGRAY); textSize = 12f; isSingleLine = true }

        textLayout.addView(titleView)
        textLayout.addView(messageView)
        notificationContainer.addView(iconView, iconParams)
        notificationContainer.addView(textLayout, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(notificationContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // --- Music Layout ---
        musicContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            alpha = 0f
            setPadding(30, 25, 30, 25)
            visibility = View.GONE
        }

        albumArtView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            background = GradientDrawable().apply { cornerRadius = 30f; setColor(Color.DKGRAY) }
        }

        val musicTextLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        musicTitle = TextView(context).apply { setTextColor(Color.WHITE); textSize = 14f; isSingleLine = true }
        musicArtist = TextView(context).apply { setTextColor(Color.LTGRAY); textSize = 12f; isSingleLine = true }

        playPauseButton = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }

        musicContainer.addView(albumArtView, LinearLayout.LayoutParams(130, 130).apply { rightMargin = 30 })
        musicTextLayout.addView(musicTitle)
        musicTextLayout.addView(musicArtist)
        musicContainer.addView(musicTextLayout, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        musicContainer.addView(playPauseButton, LinearLayout.LayoutParams(90, 90))
        addView(musicContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        setupSprings()
    }

    private fun setupSprings() {
        widthSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            addUpdateListener { _, value, _ -> updateWindowLayout(width = value.toInt()) }
        }

        heightSpring = SpringAnimation(FloatValueHolder(0f)).apply {
            spring = SpringForce().apply {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
            addUpdateListener { _, value, _ ->
                updateWindowLayout(height = value.toInt())
                backgroundDrawable.cornerRadius = value / 2f
            }
        }
    }

    private fun updateWindowLayout(width: Int? = null, height: Int? = null) {
        val wm = windowManager ?: return
        val wp = windowParams ?: return
        var changed = false
        if (width != null && wp.width != width) { wp.width = width; changed = true }
        if (height != null && wp.height != height) { wp.height = height; changed = true }
        if (changed) wm.updateViewLayout(this, wp)
    }

    fun expand() {
        if (isExpanded) return
        isExpanded = true

        // Use Black background when expanded for visibility
        backgroundDrawable.setColor(Color.BLACK)

        val wp = windowParams ?: return
        wp.flags = wp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        windowManager?.updateViewLayout(this, wp)

        widthSpring.cancel()
        widthSpring.setStartValue(wp.width.toFloat()).animateToFinalPosition(expandedWidth.toFloat())
        heightSpring.cancel()
        heightSpring.setStartValue(wp.height.toFloat()).animateToFinalPosition(expandedHeight.toFloat())
    }

    fun collapse() {
        if (!isExpanded) return
        isExpanded = false

        // Revert to Transparent when collapsed
        // Delay color change slightly to match animation? No, immediate might be better for "disappearing" effect
        // but let's do it after animation or during?
        // Doing it immediately might look like a glitch. Let's do it at end action?
        // Actually, user wants it "transparent colour (that circle)".
        // If we make it transparent immediately, the shrinking animation will be invisible.
        // So we should animate color or switch at end.
        // For simplicity/performance, let's keep it BLACK during animation and switch to TRANSPARENT at end.

        val wp = windowParams ?: return
        wp.flags = wp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        windowManager?.updateViewLayout(this, wp)

        widthSpring.cancel()
        widthSpring.animateToFinalPosition(collapsedWidth.toFloat())
        heightSpring.cancel()
        heightSpring.animateToFinalPosition(collapsedHeight.toFloat())

        // Add end listener to spring? SpringAnimation doesn't have a simple "withEndAction".
        // Use OneShotOnEndListener equivalent or simple handler delay?
        // Since springs settle based on physics, time isn't fixed.
        // Let's just set it to transparent immediately if the user wants the "circle" to be transparent.
        // Wait, if it's transparent, it's invisible.
        // The user said "make that circle same size... in transparent colour".
        // This implies they want it invisible but present? Or maybe a "hole"?
        // If I make it transparent immediately, the collapsing view disappears instantly.
        // I will trust the "transparent" request for the *idle* state.

        // Post a runnable to change color when "likely" done?
        postDelayed({
            if (!isExpanded) backgroundDrawable.setColor(Color.TRANSPARENT)
        }, 300)

        notificationContainer.animate().alpha(0f).setDuration(150).withEndAction { notificationContainer.visibility = View.GONE }
        musicContainer.animate().alpha(0f).setDuration(150).withEndAction { musicContainer.visibility = View.GONE }
    }

    fun updateNotificationInfo(title: String?, text: String?, icon: Icon?) {
        titleView.text = title ?: "Notification"
        messageView.text = text ?: ""
        iconView.setImageIcon(icon ?: Icon.createWithResource(context, android.R.drawable.sym_def_app_icon))

        musicContainer.visibility = View.GONE
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

    fun showMusicVisualizer(show: Boolean) {
        if (show) {
            notificationContainer.visibility = View.GONE
            musicContainer.visibility = View.VISIBLE
            musicContainer.animate().alpha(1f).duration = 300
        } else {
            musicContainer.animate().alpha(0f).duration = 300
        }
    }
}
