package com.example.dynamicisland.core.manager

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

// File: app/src/main/java/com/example/dynamicisland/manager/PerAppProfileManager.kt
import com.example.dynamicisland.core.ui.mvi.IslandViewModel
import com.example.dynamicisland.core.settings.SettingsViewModel
import com.example.dynamicisland.core.manager.NewConfigManager
import com.example.dynamicisland.core.ui.components.IslandContainer
import com.example.dynamicisland.core.ui.design.AppMD3Theme
import com.example.dynamicisland.shared.settings.*

import com.example.dynamicisland.shared.model.*

/**
 * BATCH 4: Per-App Island Behaviour Profiles
 *
 * Maps every package to an [IslandBehaviorProfile] that controls exactly
 * how the island behaves while that app is in the foreground.
 *
 * Default profiles are seeded for common apps based on sensible UX principles.
 * The user can override any profile from a new "App Rules" tab in ConfigActivity.
 *
 * Profile fields:
 *   islandVisibility      — VISIBLE | HIDDEN | RING_ONLY | UNMODIFIED
 *   allowedEventTypes     — which event categories can break through (bitmask)
 *   maxExpandState        — ceiling on how big the island can get (e.g. MINI only in camera)
 *   blockAccessibilityReads — opt-out of accessibility content analysis for privacy
 *   hapticSuppression     — suppress haptics for apps that generate their own (e.g. games)
 *   defaultGesture        — override the default tap action for this app context
 *
 * Persistence: stored in ContentProvider under keys "app_profile_{pkg}".
 * In-memory cache allows zero-latency reads during event routing.
 */
object PerAppProfileManager {

    // ── Data model ─────────────────────────────────────────────────────────────

    enum class Visibility { VISIBLE, HIDDEN, RING_ONLY, UNMODIFIED }

    object Events {
        const val CALLS       = 1 shl 0
        const val MEDIA       = 1 shl 1
        const val CHARGING    = 1 shl 2
        const val ALERTS      = 1 shl 3
        const val CONNECTIVITY = 1 shl 4
        const val NAVIGATION  = 1 shl 5
        const val ALL         = 0xFF
        const val CRITICAL_ONLY = CALLS or ALERTS   // CALLS + system alerts
    }

    data class IslandBehaviorProfile(
        val packageName:          String,
        val visibility:           Visibility   = Visibility.UNMODIFIED,
        val allowedEventMask:     Int          = Events.ALL,
        val maxExpandState:       IslandState  = IslandState.TYPE_3_MAX,
        val blockAccessibilityReads: Boolean   = false,
        val hapticSuppression:    Boolean      = false,
        val noteToUser:           String       = ""    // shown in ConfigActivity UI
    ) {
        fun allowsEvent(eventFlag: Int)  = (allowedEventMask and eventFlag) != 0
        fun toJson(): JSONObject = JSONObject().apply {
            put("pkg",          packageName)
            put("visibility",   visibility.name)
            put("eventMask",    allowedEventMask)
            put("maxState",     maxExpandState.name)
            put("blockA11y",    blockAccessibilityReads)
            put("hapticSuppress", hapticSuppression)
            put("note",         noteToUser)
        }

        companion object {
            fun fromJson(obj: JSONObject): IslandBehaviorProfile = IslandBehaviorProfile(
                packageName          = obj.getString("pkg"),
                visibility           = Visibility.valueOf(obj.optString("visibility", "UNMODIFIED")),
                allowedEventMask     = obj.optInt("eventMask", Events.ALL),
                maxExpandState       = runCatching { IslandState.valueOf(obj.getString("maxState")) }.getOrDefault(IslandState.TYPE_3_MAX),
                blockAccessibilityReads = obj.optBoolean("blockA11y", false),
                hapticSuppression    = obj.optBoolean("hapticSuppress", false),
                noteToUser           = obj.optString("note", "")
            )
        }
    }

    // ── Seeded defaults ───────────────────────────────────────────────────────

    /**
     * These defaults are applied the first time a package is seen.
     * User overrides are saved to storage and take priority on the next read.
     */
    private val DEFAULT_PROFILES = listOf(
        // Camera — island hidden to avoid obstructing the viewfinder
        IslandBehaviorProfile("com.android.camera2",
            visibility = Visibility.HIDDEN,
            allowedEventMask = Events.CRITICAL_ONLY,
            blockAccessibilityReads = true,
            noteToUser = "Hidden during camera use"
        ),
        IslandBehaviorProfile("com.google.android.GoogleCamera",
            visibility = Visibility.HIDDEN,
            allowedEventMask = Events.CRITICAL_ONLY,
            blockAccessibilityReads = true
        ),

        // Navigation — always show nav instruction, suppress media/charging
        IslandBehaviorProfile("com.google.android.apps.maps",
            visibility = Visibility.VISIBLE,
            allowedEventMask = Events.CALLS or Events.NAVIGATION or Events.ALERTS,
            maxExpandState = IslandState.TYPE_2_MID,
            blockAccessibilityReads = true,
            noteToUser = "Navigation mode: only calls & turn instructions shown"
        ),
        IslandBehaviorProfile("com.waze",
            visibility = Visibility.VISIBLE,
            allowedEventMask = Events.CALLS or Events.NAVIGATION or Events.ALERTS,
            maxExpandState = IslandState.TYPE_2_MID
        ),

        // Banking — total privacy: hidden, no accessibility reads, no content
        IslandBehaviorProfile("com.phonepe.app",
            visibility = Visibility.HIDDEN,
            allowedEventMask = Events.CALLS,
            blockAccessibilityReads = true,
            noteToUser = "Privacy mode: hidden during payments"
        ),
        IslandBehaviorProfile("net.one97.paytm",
            visibility = Visibility.HIDDEN, allowedEventMask = Events.CALLS, blockAccessibilityReads = true
        ),
        IslandBehaviorProfile("com.google.android.apps.nbu.paisa.user",
            visibility = Visibility.HIDDEN, allowedEventMask = Events.CALLS, blockAccessibilityReads = true
        ),
        IslandBehaviorProfile("com.amazon.mShop.android.shopping",
            visibility = Visibility.HIDDEN, allowedEventMask = Events.CALLS, blockAccessibilityReads = true
        ),

        // Gaming — ring only, no haptics (game generates its own), critical events only
        IslandBehaviorProfile("com.pubg.imobile",
            visibility = Visibility.RING_ONLY,
            allowedEventMask = Events.CRITICAL_ONLY,
            hapticSuppression = true,
            maxExpandState = IslandState.TYPE_1_MINI,
            noteToUser = "Gaming mode: minimal interruption"
        ),
        IslandBehaviorProfile("com.tencent.ig",
            visibility = Visibility.RING_ONLY, allowedEventMask = Events.CRITICAL_ONLY, hapticSuppression = true, maxExpandState = IslandState.TYPE_1_MINI
        ),
        IslandBehaviorProfile("com.activision.callofduty.shooter",
            visibility = Visibility.RING_ONLY, allowedEventMask = Events.CRITICAL_ONLY, hapticSuppression = true, maxExpandState = IslandState.TYPE_1_MINI
        ),

        // Video — hidden during playback (Xposed video detection is the primary signal;
        // this is a belt-and-braces fallback based on package)
        IslandBehaviorProfile("com.netflix.mediaclient",
            visibility = Visibility.HIDDEN, allowedEventMask = Events.CRITICAL_ONLY, blockAccessibilityReads = true
        ),
        IslandBehaviorProfile("com.google.android.youtube",
            visibility = Visibility.HIDDEN, allowedEventMask = Events.CRITICAL_ONLY
        ),

        // Spotify — allow media, calls. Block charging/connectivity noise.
        IslandBehaviorProfile("com.spotify.music",
            visibility = Visibility.VISIBLE,
            allowedEventMask = Events.CALLS or Events.MEDIA or Events.ALERTS
        ),

        // Maps Lite, Ola, Uber — navigation context
        IslandBehaviorProfile("com.olacabs.customer",
            visibility = Visibility.VISIBLE, allowedEventMask = Events.CALLS or Events.NAVIGATION or Events.ALERTS, maxExpandState = IslandState.TYPE_2_MID
        ),
        IslandBehaviorProfile("com.ubercab",
            visibility = Visibility.VISIBLE, allowedEventMask = Events.CALLS or Events.NAVIGATION or Events.ALERTS, maxExpandState = IslandState.TYPE_2_MID
        )
    ).associateBy { it.packageName }

    // ── In-memory cache ───────────────────────────────────────────────────────

    private val cache = ConcurrentHashMap<String, IslandBehaviorProfile>()
    private var prefs: SharedPreferences? = null

    private val FALLBACK_PROFILE = IslandBehaviorProfile("*")

    // ── API ───────────────────────────────────────────────────────────────────

    /**
     * Initialise with the app's SharedPreferences.
     * Call from ConfigActivity.onCreate() or IslandController.init().
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("island_app_profiles", Context.MODE_PRIVATE)
        loadAllFromPrefs()
    }

    /**
     * Returns the profile for [packageName].
     * Priority: user override → seeded default → fallback (all-visible).
     */
    fun getProfile(packageName: String): IslandBehaviorProfile {
        cache[packageName]?.let { return it }
        return (DEFAULT_PROFILES[packageName] ?: FALLBACK_PROFILE).also { cache[packageName] = it }
    }

    /**
     * Save a user-defined profile override. Persists across reboots.
     */
    fun saveProfile(profile: IslandBehaviorProfile) {
        cache[profile.packageName] = profile
        prefs?.edit()
            ?.putString("app_profile_${profile.packageName}", profile.toJson().toString())
            ?.apply()
    }

    /**
     * Remove a user override for [packageName], reverting to the seeded default.
     */
    fun resetProfile(packageName: String) {
        prefs?.edit()?.remove("app_profile_$packageName")?.apply()
        val seeded = DEFAULT_PROFILES[packageName] ?: FALLBACK_PROFILE
        cache[packageName] = seeded
    }

    /** Returns all packages that have been assigned a non-default profile. */
    fun getAllCustomProfiles(): List<IslandBehaviorProfile> =
        prefs?.all
            ?.filter { it.key.startsWith("app_profile_") }
            ?.mapNotNull { (_, v) ->
                runCatching { IslandBehaviorProfile.fromJson(JSONObject(v.toString())) }.getOrNull()
            } ?: emptyList()

    /**
     * Convenience check used by IslandPriorityEngineV2 to gate event routing.
     *
     * Returns false if the current foreground app has blocked this event category.
     */
    fun eventAllowedForPackage(packageName: String, eventFlag: Int): Boolean =
        getProfile(packageName).allowsEvent(eventFlag)

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadAllFromPrefs() {
        prefs?.all?.forEach { (key, value) ->
            if (key.startsWith("app_profile_") && value is String) {
                runCatching {
                    val profile = IslandBehaviorProfile.fromJson(JSONObject(value))
                    cache[profile.packageName] = profile
                }
            }
        }
    }
}