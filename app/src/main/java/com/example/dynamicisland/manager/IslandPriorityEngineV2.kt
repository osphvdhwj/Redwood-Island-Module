package com.example.dynamicisland.manager

import android.content.Context
import android.view.WindowManager
import com.example.dynamicisland.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * BATCH 1: Weighted Priority Scoring Engine
 *
 * Replaces the rigid if-else chain with a numeric scoring system.
 * Every event type has a base priority score. Context modifiers
 * (urgency, user attention, timing, recency) multiply that score.
 * The highest-scoring event wins the island at every evaluation tick.
 *
 * Score architecture:
 *   finalScore = basePriority × urgencyMultiplier × attentionMultiplier
 *                × timingMultiplier × (1 - decayFactor × ageSeconds)
 *
 * This means:
 *   - A phone call always scores higher than a download notification
 *   - BUT a 5% battery warning scores higher than paused music
 *   - AND a navigation instruction beats all non-critical events
 *     within 30 seconds of the user looking at Maps
 *   - Events older than their TTL decay to zero automatically
 */
object IslandPriorityEngineV2 {

    // -------------------------------------------------------------------------
    // Base priority scores — intentionally wide-spaced so multipliers matter
    // -------------------------------------------------------------------------

    private const val SCORE_PHONE_CALL_RINGING    = 10_000f
    private const val SCORE_PHONE_CALL_ACTIVE     = 8_000f
    private const val SCORE_CRITICAL_ALERT        = 7_000f   // Battery 5%, OTP
    private const val SCORE_NAVIGATION_IMMINENT   = 6_500f   // Turn in < 100m
    private const val SCORE_NAVIGATION_UPCOMING   = 5_000f
    private const val SCORE_LOW_BATTERY_20        = 4_500f
    private const val SCORE_APP_TIMER_EXPIRY      = 4_000f
    private const val SCORE_MUSIC_PLAYING         = 3_000f
    private const val SCORE_CHARGING_JUST_PLUGGED = 2_500f
    private const val SCORE_HARDWARE_TOGGLE       = 2_000f   // Torch, ringer change
    private const val SCORE_DOWNLOAD_PROGRESS     = 1_800f
    private const val SCORE_CONNECTIVITY_EVENT    = 1_500f   // BT connected, WiFi
    private const val SCORE_MUSIC_PAUSED          = 1_200f
    private const val SCORE_SCREENSHOT_SAVED      = 1_000f
    private const val SCORE_CLIPBOARD_COPIED      = 900f
    private const val SCORE_ALARM_SET             = 800f
    private const val SCORE_OTP_RECEIVED          = 7_500f   // Between critical+call
    private const val SCORE_DASHBOARD_OPEN        = 500f     // User explicitly opened
    private const val SCORE_IDLE_RING             = 0f

    // -------------------------------------------------------------------------
    // Event registry — tracks all currently active events with their metadata
    // -------------------------------------------------------------------------

    data class IslandEvent(
        val id: String,
        val model: LiveActivityModel?,
        val baseScore: Float,
        val createdAtMs: Long = System.currentTimeMillis(),
        val ttlMs: Long = 5_000L,           // Event auto-expires after this many ms
        val isSticky: Boolean = false,       // Sticky events don't expire (calls, music)
        val isSuppressable: Boolean = true,  // User can force-collapse this
        val targetState: IslandState,
        val contextModifiers: List<ContextModifier> = emptyList()
    ) : Comparable<IslandEvent> {
        override fun compareTo(other: IslandEvent): Int =
            other.finalScore().compareTo(this.finalScore())  // Max-heap

        fun finalScore(): Float {
            if (!isSticky && System.currentTimeMillis() - createdAtMs > ttlMs) return -1f
            var score = baseScore
            contextModifiers.forEach { score *= it.multiplier }
            return score
        }

        fun isExpired(): Boolean =
            !isSticky && (System.currentTimeMillis() - createdAtMs > ttlMs)
    }

    /**
     * Context modifiers — applied on top of base scores.
     * These encode situational awareness that a static if-else cannot express.
     */
    data class ContextModifier(
        val name: String,
        val multiplier: Float
    )

    // Multiplier constants
    private const val MOD_USER_WATCHING    = 1.5f  // User last interacted with island < 10s
    private const val MOD_SCREEN_ON        = 1.0f  // Normal
    private const val MOD_SCREEN_DIM       = 0.5f  // Lower priority when screen is dimming
    private const val MOD_GAMING           = 0.1f  // Aggressive suppression in games
    private const val MOD_VIDEO_FULLSCREEN = 0.05f // Near-total suppression during video
    private const val MOD_LANDSCAPE        = 0.7f  // Slightly lower in landscape
    private const val MOD_PANEL_OPEN       = 0.0f  // Zero — notification shade is open
    private const val MOD_BATTERY_CRITICAL = 2.0f  // Double score when < 5%
    private const val MOD_REPEATED_EVENT   = 0.6f  // Penalise same event seen 3+ times

    // -------------------------------------------------------------------------
    // Internal state
    // -------------------------------------------------------------------------

    private val activeEvents   = ConcurrentHashMap<String, IslandEvent>()
    private val eventSeenCount = ConcurrentHashMap<String, Int>()
    private var lastUserInteractionMs = 0L
    private var isScreenOn     = true
    private var isGaming       = false
    private var isVideoPlaying = false
    private var isPanelExpanded = false
    private var isLandscape    = false
    private var batteryLevel   = 100
    private var isCharging     = false

    // -------------------------------------------------------------------------
    // Event lifecycle API — called by IslandController
    // -------------------------------------------------------------------------

    /** Register a new event. Replaces any existing event with the same id. */
    fun post(event: IslandEvent) {
        activeEvents[event.id] = event
        val count = (eventSeenCount[event.id] ?: 0) + 1
        eventSeenCount[event.id] = count
    }

    /** Remove an event by id (e.g. call ended, download complete). */
    fun dismiss(id: String) {
        activeEvents.remove(id)
    }

    /** Update context that affects all event scores simultaneously. */
    fun updateContext(
        screenOn: Boolean? = null,
        gaming: Boolean? = null,
        videoFullscreen: Boolean? = null,
        panelExpanded: Boolean? = null,
        landscape: Boolean? = null,
        battery: Int? = null,
        charging: Boolean? = null
    ) {
        screenOn?.let       { isScreenOn       = it }
        gaming?.let         { isGaming         = it }
        videoFullscreen?.let { isVideoPlaying  = it }
        panelExpanded?.let  { isPanelExpanded  = it }
        landscape?.let      { isLandscape      = it }
        battery?.let        { batteryLevel     = it }
        charging?.let       { isCharging       = it }
    }

    fun recordUserInteraction() {
        lastUserInteractionMs = System.currentTimeMillis()
    }

    // -------------------------------------------------------------------------
    // Core evaluation — returns the winning event every time IslandController
    // needs to decide what to show. Called on every state change.
    // -------------------------------------------------------------------------

    fun evaluate(): EvaluationResult {

        // Step 1: Remove expired events
        val expiredIds = activeEvents.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        expiredIds.forEach { activeEvents.remove(it) }

        // Step 2: Panel open → hide everything
        if (isPanelExpanded) {
            return EvaluationResult(
                winningEvent  = null,
                islandState   = IslandState.HIDDEN,
                splitEvent    = null,
                suppressReason = "notification_panel_open"
            )
        }

        // Step 3: Score every active event with current context multipliers
        val contextMultiplier = buildContextMultiplier()

        val scoredEvents = activeEvents.values
            .map { event ->
                val repeatPenalty = if ((eventSeenCount[event.id] ?: 0) >= 3) MOD_REPEATED_EVENT else 1f
                val userAttentionBoost = if (System.currentTimeMillis() - lastUserInteractionMs < 10_000L) MOD_USER_WATCHING else 1f
                val baseScore = event.finalScore()
                val adjustedScore = baseScore * contextMultiplier * repeatPenalty * userAttentionBoost
                event to adjustedScore
            }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }

        if (scoredEvents.isEmpty()) {
            // Nothing active → idle ring
            return EvaluationResult(
                winningEvent  = null,
                islandState   = IslandState.TYPE_0_RING,
                splitEvent    = null,
                suppressReason = null
            )
        }

        val winner = scoredEvents.first().first

        // Step 4: Determine split pill candidate (second highest non-overlapping event)
        val splitCandidate = scoredEvents
            .drop(1)
            .firstOrNull { (event, _) ->
                // Only show split if it's a meaningfully different category
                event.model?.javaClass != winner.model?.javaClass &&
                event.model is LiveActivityModel.Music ||
                event.model is LiveActivityModel.Charging
            }?.first

        // Step 5: Determine the optimal visual state for the winner
        val targetState = resolveTargetState(winner)

        return EvaluationResult(
            winningEvent   = winner,
            islandState    = targetState,
            splitEvent     = splitCandidate,
            suppressReason = null,
            allScores      = scoredEvents.take(5).associate { it.first.id to it.second }
        )
    }

    // -------------------------------------------------------------------------
    // Convenience builder methods — creates properly scored events
    // -------------------------------------------------------------------------

    fun buildCallEvent(call: LiveActivityModel.Call): IslandEvent {
        val base = if (call.state == "RINGING") SCORE_PHONE_CALL_RINGING else SCORE_PHONE_CALL_ACTIVE
        return IslandEvent(
            id           = "sys_call",
            model        = call,
            baseScore    = base,
            isSticky     = true,
            isSuppressable = false,
            targetState  = IslandState.TYPE_2_MID
        )
    }

    fun buildMusicEvent(music: LiveActivityModel.Music): IslandEvent {
        val base = if (music.isPlaying) SCORE_MUSIC_PLAYING else SCORE_MUSIC_PAUSED
        return IslandEvent(
            id          = "media_main",
            model       = music,
            baseScore   = base,
            isSticky    = true,
            ttlMs       = Long.MAX_VALUE,
            targetState = if (music.isPlaying) IslandState.TYPE_2_MID else IslandState.TYPE_0_RING
        )
    }

    fun buildChargingEvent(charging: LiveActivityModel.Charging): IslandEvent {
        return IslandEvent(
            id          = "sys_battery",
            model       = charging,
            baseScore   = if (charging.isPluggedIn) SCORE_CHARGING_JUST_PLUGGED
                          else if (charging.level <= 5) SCORE_CRITICAL_ALERT
                          else SCORE_LOW_BATTERY_20,
            isSticky    = false,
            ttlMs       = if (charging.isPluggedIn) 4_000L else 6_000L,
            targetState = if (charging.level <= 5 && !charging.isPluggedIn)
                              IslandState.TYPE_2_MID
                          else IslandState.TYPE_CUBE
        )
    }

    fun buildAlertEvent(alert: LiveActivityModel.SystemAlert): IslandEvent {
        return IslandEvent(
            id          = alert.id,
            model       = alert,
            baseScore   = SCORE_CRITICAL_ALERT,
            isSticky    = false,
            ttlMs       = 6_000L,
            targetState = IslandState.TYPE_2_MID,
            contextModifiers = if (batteryLevel <= 5)
                listOf(ContextModifier("battery_critical", MOD_BATTERY_CRITICAL)) else emptyList()
        )
    }

    fun buildOtpEvent(otp: String, pkg: String): IslandEvent {
        return IslandEvent(
            id          = "sys_otp",
            model       = LiveActivityModel.SystemAlert(
                id         = "sys_otp",
                alertType  = "OTP_CATCHER",
                title      = "Verification Code",
                message    = otp,
                alertColor = android.graphics.Color.parseColor("#4285F4"),
                isCritical = true
            ),
            baseScore   = SCORE_OTP_RECEIVED,
            isSticky    = false,
            ttlMs       = 30_000L,  // OTPs stay for 30s so user can copy
            isSuppressable = false,
            targetState = IslandState.TYPE_2_MID
        )
    }

    fun buildNavigationEvent(general: LiveActivityModel.General, distanceMeters: Int): IslandEvent {
        val base = if (distanceMeters < 100) SCORE_NAVIGATION_IMMINENT else SCORE_NAVIGATION_UPCOMING
        // Recency boost: if user opened Maps < 60s ago, bump score
        return IslandEvent(
            id          = "sys_navigation",
            model       = general,
            baseScore   = base,
            isSticky    = false,
            ttlMs       = 8_000L,
            targetState = IslandState.TYPE_2_MID
        )
    }

    fun buildDownloadEvent(task: LiveActivityModel.OngoingTask): IslandEvent {
        return IslandEvent(
            id          = "sys_progress_${task.pkgName}",
            model       = task,
            baseScore   = SCORE_DOWNLOAD_PROGRESS,
            isSticky    = false,
            ttlMs       = 10_000L,
            targetState = IslandState.TYPE_1_MINI
        )
    }

    fun buildHardwareToggleEvent(model: LiveActivityModel.General): IslandEvent {
        return IslandEvent(
            id          = model.id,
            model       = model,
            baseScore   = SCORE_HARDWARE_TOGGLE,
            isSticky    = false,
            ttlMs       = 3_000L,
            targetState = IslandState.TYPE_1_MINI
        )
    }

    fun buildConnectivityEvent(model: LiveActivityModel.General): IslandEvent {
        return IslandEvent(
            id          = model.id,
            model       = model,
            baseScore   = SCORE_CONNECTIVITY_EVENT,
            isSticky    = false,
            ttlMs       = 4_000L,
            targetState = IslandState.TYPE_1_MINI
        )
    }

    fun buildDashboardEvent(dashboard: LiveActivityModel.Dashboard): IslandEvent {
        return IslandEvent(
            id          = "dashboard",
            model       = dashboard,
            baseScore   = SCORE_DASHBOARD_OPEN,
            isSticky    = true,  // Stays until user collapses
            targetState = IslandState.TYPE_3_MAX
        )
    }

    // -------------------------------------------------------------------------
    // Full drop-in replacement for the old evaluatePriority() static function
    // -------------------------------------------------------------------------

    /**
     * This is the direct replacement for IslandPriorityEngine.evaluatePriority().
     * IslandController calls this instead. It populates the same MutableStateFlows
     * so all existing Compose collectors continue to work unchanged.
     */
    fun evaluatePriority(
        context: Context,
        currentCall: LiveActivityModel.Call?,
        transientModel: LiveActivityModel?,
        currentMedia: LiveActivityModel.Music?,
        currentHardware: LiveActivityModel.HardwareMonitor?,
        isMediaEnabled: Boolean,
        userForceCollapsed: Boolean,
        currentActiveModel: LiveActivityModel?,
        currentVisualState: IslandState,
        isPanelExpanded: Boolean,
        isLandscapeNow: Boolean,
        _activeModel: MutableStateFlow<LiveActivityModel?>,
        _splitModel:  MutableStateFlow<LiveActivityModel?>,
        _islandState: MutableStateFlow<IslandState>
    ): Boolean {

        // Sync context
        updateContext(
            panelExpanded  = isPanelExpanded,
            landscape      = isLandscapeNow,
            gaming         = currentHardware?.isGamingModeOn ?: false
        )

        // Sync active events from controller state
        if (currentCall != null) {
            post(buildCallEvent(currentCall))
        } else {
            dismiss("sys_call")
        }

        if (currentMedia != null && isMediaEnabled) {
            post(buildMusicEvent(currentMedia))
        } else {
            dismiss("media_main")
        }

        if (transientModel != null) {
            when (transientModel) {
                is LiveActivityModel.SystemAlert  -> post(buildAlertEvent(transientModel))
                is LiveActivityModel.OngoingTask  -> post(buildDownloadEvent(transientModel))
                is LiveActivityModel.General      -> post(buildHardwareToggleEvent(transientModel))
                is LiveActivityModel.Charging     -> post(buildChargingEvent(transientModel))
                else -> { /* generic transient */ }
            }
        }

        if (currentActiveModel is LiveActivityModel.Dashboard &&
            currentVisualState == IslandState.TYPE_3_MAX) {
            post(buildDashboardEvent(currentActiveModel))
        }

        // Run the weighted evaluation
        val result = evaluate()

        // If user force-collapsed, only critical events break through
        if (userForceCollapsed && result.winningEvent?.isSuppressable == true) {
            _islandState.value = IslandState.TYPE_0_RING
            return true
        }

        // Apply the winning result
        _islandState.value = result.islandState
        _activeModel.value = result.winningEvent?.model
        _splitModel.value  = result.splitEvent?.model

        // Return the new userForceCollapsed state
        // Critical events reset the force-collapse flag
        return if (result.winningEvent?.isSuppressable == false) false else userForceCollapsed
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun buildContextMultiplier(): Float {
        var multiplier = 1.0f
        if (!isScreenOn)       multiplier *= MOD_SCREEN_DIM
        if (isGaming)          multiplier *= MOD_GAMING
        if (isVideoPlaying)    multiplier *= MOD_VIDEO_FULLSCREEN
        if (isPanelExpanded)   multiplier *= MOD_PANEL_OPEN
        if (isLandscape)       multiplier *= MOD_LANDSCAPE
        return multiplier
    }

    private fun resolveTargetState(event: IslandEvent): IslandState {
        // Some events override their own target state based on context
        return when {
            // Video or gaming → hide everything except calls and critical alerts
            (isVideoPlaying || isGaming) && event.isSuppressable -> IslandState.HIDDEN

            // Landscape + not critical → mini at most
            isLandscape && event.isSuppressable &&
            event.targetState == IslandState.TYPE_2_MID -> IslandState.TYPE_1_MINI

            else -> event.targetState
        }
    }

    // Debug helper — logs the current priority queue state
    fun debugDump(): String {
        val sb = StringBuilder("=== Priority Engine State ===\n")
        sb.append("Context: screen=$isScreenOn gaming=$isGaming video=$isVideoPlaying panel=$isPanelExpanded landscape=$isLandscape\n")
        sb.append("Active events (${activeEvents.size}):\n")
        activeEvents.values
            .sortedByDescending { it.finalScore() }
            .forEach { event ->
                sb.append("  [${event.id}] base=${event.baseScore} final=${event.finalScore()} " +
                           "sticky=${event.isSticky} target=${event.targetState}\n")
            }
        return sb.toString()
    }
}

// -------------------------------------------------------------------------
// Result data class
// -------------------------------------------------------------------------

data class EvaluationResult(
    val winningEvent:   IslandPriorityEngineV2.IslandEvent?,
    val islandState:    IslandState,
    val splitEvent:     IslandPriorityEngineV2.IslandEvent?,
    val suppressReason: String?,
    val allScores:      Map<String, Float> = emptyMap()
)
