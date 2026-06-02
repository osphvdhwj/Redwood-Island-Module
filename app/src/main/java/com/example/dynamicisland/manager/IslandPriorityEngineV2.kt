package com.example.dynamicisland.manager

import android.content.Context
import android.view.WindowManager
import com.example.dynamicisland.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import com.example.dynamicisland.ipc.IslandState

/**
 * PRO-GRADE PRIORITY ENGINE (V2.1)
 *
 * Weighted Scoring Engine with Situational Context Awareness.
 * Optimized for both Camera-Cutout and bottom-anchored Nav Island paradigms.
 */
object IslandPriorityEngineV2 {

    // -------------------------------------------------------------------------
    // Weighted Priority Scores
    // -------------------------------------------------------------------------
    private const val SCORE_PHONE_CALL_RINGING    = 10_000f
    private const val SCORE_PHONE_CALL_ACTIVE     = 8_000f
    private const val SCORE_CRITICAL_ALERT        = 7_500f   // Battery 5%, OTP
    private const val SCORE_NAVIGATION_IMMINENT   = 7_000f   // Turn in < 100m
    private const val SCORE_NAVIGATION_UPCOMING   = 5_500f
    private const val SCORE_LOW_BATTERY_20        = 4_500f
    private const val SCORE_APP_TIMER_EXPIRY      = 4_000f
    private const val SCORE_MUSIC_PLAYING         = 3_500f
    private const val SCORE_CHARGING_JUST_PLUGGED = 3_000f
    private const val SCORE_HARDWARE_TOGGLE       = 2_500f
    private const val SCORE_DOWNLOAD_PROGRESS     = 2_000f
    private const val SCORE_CONNECTIVITY_EVENT    = 1_800f
    private const val SCORE_MUSIC_PAUSED          = 1_200f
    private const val SCORE_SCREENSHOT_SAVED      = 1_000f
    private const val SCORE_CLIPBOARD_COPIED      = 900f
    private const val SCORE_DASHBOARD_OPEN        = 500f     
    private const val SCORE_NAV_LAUNCHER_IDLE     = 100f     // Persistent bottom launcher
    private const val SCORE_IDLE_RING             = 0f

    data class IslandEvent(
        val id: String,
        val model: LiveActivityModel?,
        val baseScore: Float,
        val createdAtMs: Long = System.currentTimeMillis(),
        val ttlMs: Long = 5_000L,
        val isSticky: Boolean = false,
        val isSuppressable: Boolean = true,
        val targetState: IslandState,
        val contextModifiers: List<ContextModifier> = emptyList()
    ) {
        fun finalScore(): Float {
            if (!isSticky && System.currentTimeMillis() - createdAtMs > ttlMs) return -1f
            var score = baseScore
            contextModifiers.forEach { score *= it.multiplier }
            return score
        }
        fun isExpired(): Boolean = !isSticky && (System.currentTimeMillis() - createdAtMs > ttlMs)
    }

    data class ContextModifier(val name: String, val multiplier: Float)

    // Multipliers
    private const val MOD_USER_WATCHING    = 1.6f
    private const val MOD_GAMING           = 0.1f
    private const val MOD_VIDEO_FULLSCREEN = 0.05f
    private const val MOD_LANDSCAPE        = 0.75f
    private const val MOD_PANEL_OPEN       = 0.0f
    private const val MOD_BATTERY_CRITICAL = 2.0f
    private const val MOD_REPEATED_EVENT   = 0.6f

    // Internal State
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
    private var navIslandMode  = false
    private var pinnedApps     = emptyList<String>()
    private var moduleIndex    = 0

    fun post(event: IslandEvent) {
        activeEvents[event.id] = event
        eventSeenCount[event.id] = (eventSeenCount[event.id] ?: 0) + 1
    }

    fun dismiss(id: String) { activeEvents.remove(id) }

    fun updateContext(
        screenOn: Boolean? = null,
        gaming: Boolean? = null,
        videoFullscreen: Boolean? = null,
        panelExpanded: Boolean? = null,
        landscape: Boolean? = null,
        battery: Int? = null,
        charging: Boolean? = null,
        navMode: Boolean? = null,
        pinned: List<String>? = null,
        mIndex: Int? = null
    ) {
        screenOn?.let       { isScreenOn       = it }
        gaming?.let         { isGaming         = it }
        videoFullscreen?.let { isVideoPlaying  = it }
        panelExpanded?.let  { isPanelExpanded  = it }
        landscape?.let      { isLandscape      = it }
        battery?.let        { batteryLevel     = it }
        charging?.let       { isCharging       = it }
        navMode?.let        { navIslandMode    = it }
        pinned?.let         { pinnedApps       = it }
        mIndex?.let         { moduleIndex      = it }
    }

    fun evaluate(): EvaluationResult {
        // Step 1: Cleanup
        activeEvents.entries.filter { it.value.isExpired() }.forEach { activeEvents.remove(it.key) }

        if (isPanelExpanded) return EvaluationResult(null, IslandState.HIDDEN, null, "shade_open")

        // Step 2: Inject Nav Launcher if in Nav Mode
        if (navIslandMode) {
            post(buildNavLauncherEvent())
        } else {
            dismiss("nav_launcher")
        }

        // Step 3: Score and Rank
        val contextMultiplier = buildContextMultiplier()
        val scoredEvents = activeEvents.values
            .map { event ->
                val repeatPenalty = if ((eventSeenCount[event.id] ?: 0) >= 3 && !event.isSticky) MOD_REPEATED_EVENT else 1f
                val userAttentionBoost = if (System.currentTimeMillis() - lastUserInteractionMs < 10_000L) MOD_USER_WATCHING else 1f
                val adjustedScore = event.finalScore() * contextMultiplier * repeatPenalty * userAttentionBoost
                event to adjustedScore
            }
            .filter { it.second >= 0f }
            .sortedByDescending { it.second }

        if (scoredEvents.isEmpty()) return EvaluationResult(null, IslandState.TYPE_0_RING, null, null)

        // Step 4: Handle Module Cycling (Nav Island Switcher)
        var winner = scoredEvents.first().first
        if (navIslandMode && moduleIndex != 0 && scoredEvents.size > 1) {
            val idx = abs(moduleIndex) % scoredEvents.size
            winner = scoredEvents[idx].first
        }

        // Step 5: Split Determination
        val split = scoredEvents.drop(1).firstOrNull { (e, _) -> 
            e.id != winner.id && (e.model is LiveActivityModel.Music || e.model is LiveActivityModel.Charging)
        }?.first

        return EvaluationResult(
            winner, 
            resolveTargetState(winner), 
            split, 
            null, 
            scoredEvents.take(5).associate { it.first.id to it.second }
        )
    }

    private fun buildContextMultiplier(): Float {
        var m = 1.0f
        if (!isScreenOn) m *= 0.5f
        if (isGaming) m *= MOD_GAMING
        if (isVideoPlaying) m *= MOD_VIDEO_FULLSCREEN
        if (isLandscape) m *= MOD_LANDSCAPE
        return m
    }

    private fun resolveTargetState(event: IslandEvent): IslandState {
        if ((isVideoPlaying || isGaming) && event.isSuppressable) return IslandState.HIDDEN
        if (isLandscape && event.isSuppressable && event.targetState == IslandState.TYPE_2_MID) return IslandState.TYPE_1_MINI
        return event.targetState
    }

    // Builder Methods
    fun buildCallEvent(call: LiveActivityModel.Call) = IslandEvent("sys_call", call, if (call.state == "RINGING") SCORE_PHONE_CALL_RINGING else SCORE_PHONE_CALL_ACTIVE, isSticky = true, isSuppressable = false, targetState = IslandState.TYPE_2_MID)
    fun buildMusicEvent(music: LiveActivityModel.Music) = IslandEvent("media_main", music, if (music.isPlaying) SCORE_MUSIC_PLAYING else SCORE_MUSIC_PAUSED, isSticky = true, ttlMs = Long.MAX_VALUE, targetState = if (music.isPlaying) IslandState.TYPE_2_MID else IslandState.TYPE_0_RING)
    fun buildChargingEvent(c: LiveActivityModel.Charging) = IslandEvent("sys_battery", c, if (c.isPluggedIn) SCORE_CHARGING_JUST_PLUGGED else if (c.level <= 5) SCORE_CRITICAL_ALERT else SCORE_LOW_BATTERY_20, targetState = if (c.level <= 5 && !c.isPluggedIn) IslandState.TYPE_2_MID else IslandState.TYPE_CUBE)
    fun buildNavLauncherEvent() = IslandEvent("nav_launcher", LiveActivityModel.Dashboard(pinnedApps = pinnedApps), SCORE_NAV_LAUNCHER_IDLE, isSticky = true, ttlMs = Long.MAX_VALUE, targetState = IslandState.TYPE_1_MINI)
}

data class EvaluationResult(
    val winningEvent:   IslandPriorityEngineV2.IslandEvent?,
    val islandState:    IslandState,
    val splitEvent:     IslandPriorityEngineV2.IslandEvent?,
    val suppressReason: String?,
    val allScores:      Map<String, Float> = emptyMap()
)
