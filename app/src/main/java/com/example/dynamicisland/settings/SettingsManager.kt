package com.example.dynamicisland.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.dynamicisland.ipc.IslandIPCClient
import org.json.JSONObject

/**
 * Pro-Grade Settings Manager
 * 
 * AUTOMATIC BRIDGE:
 * - In the module app: Reads/Writes to local SharedPreferences.
 * - In SystemUI: Reads from the IslandIPCClient (ContentProvider).
 */
class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

    private val isSystemUI = context.packageName == "com.android.systemui"
    private val ipcClient by lazy { IslandIPCClient.get(context) }

    fun broadcastUpdate() {
        try {
            val intent = android.content.Intent("com.example.dynamicisland.RELOAD_PREFS")
            intent.addFlags(0x01000000)
            intent.setPackage("com.android.systemui")
            context.sendBroadcast(intent)
        } catch (e: Exception) {}
    }

    enum class SettingKey {
        DESIGN_LANGUAGE, DYNAMIC_COLORS, ACCENT_COLOR, BLUR_INTENSITY,
        PILL_RADIUS, ANIM_SPEED, RING_IDLE, PILL_SHAPE,
        DYNAMIC_GRADIENT, GLOW_EFFECT, DOT_MODE, ELASTIC_STRETCH,
        SHADOW_CASTING, CONTENT_AWARE_BLUR, TIME_BASED_THEMES,
        HIGH_CONTRAST_MODE, ICON_PACK_INTEGRATION, AESTHETIC_STYLE,
        MONOCHROME_ICONS, ENABLE_METABALL_TEAR,
        LIVE_BRIDGE_ENABLED, MAGNETIC_EDGE_DOCKING,
        GEMINI_AURA_ENABLED, ROLLING_TYPOGRAPHY_ENABLED,
        PARSE_DELIVERY_NOTIFICATIONS, WARP_CHARGE_ANIMATION,
        BATTERY_AWARE_ANIMATION, NOW_PLAYING, MUSIC_VISUALIZER_STYLE,
        WAVEFORM_ENABLED, MEDIA_ARTWORK_BLUR, BPM_PULSE,
        AMBIENT_REACTIVE_RING, AMBIENT_REACTIVE,
        PHYSICS_STYLE, CONTENT_TRANSITION_STYLE, VELOCITY_SQUISH_ENABLED,
        INLINE_REPLY_ENABLED, ENABLE_MAX_WIDGETS,
        SHOW_VITALS_RAM, SHOW_VITALS_CPU, SHOW_VITALS_NET,
        SHOW_VITALS_FPS, SHOW_VITALS_BAT_CYCLES, SHORTCUT_LAYOUT,
        ASSIST_BRIDGE_ENABLED, ASSIST_BRIDGE_TARGET,
        LENS_BRIDGE_ENABLED, LENS_BRIDGE_TARGET,
        SMART_GESTURES_ENABLED, SMART_CALL_OVERRIDE,
        SMART_MEDIA_OVERRIDE, SMART_GAMING_OVERRIDE,
        PREDICTION_TINT, PREDICTIVE_ACTIONS, AUTO_DISMISS_DELAY,
        CONTEXTUAL_SUGGESTIONS, GESTURE_LEARNING,
        AI_CONFIDENCE_THRESHOLD, AI_REINFORCEMENT_RATE,
        ALLOW_MUSIC_MID, ALLOW_MUSIC_MAX, ALLOW_CHARGING_MINI,
        ALLOW_CHARGING_MID, ALLOW_NOTIF_MINI, ALLOW_NOTIF_MID,
        ALLOW_NOTIF_MAX, ALLOW_CALL_MID, ALLOW_CALL_MAX,
        ALLOW_TASK_MINI, ALLOW_TASK_MID,
        ISLAND_ENABLED, ISLAND_ON_LOCKSCREEN, LOCKSCREEN_FEATURES,
        ALLOWED_NOTIFICATION_APPS, SWIPE_LEFT_ACTION, SWIPE_RIGHT_ACTION,
        HIDE_ON_SCREENSHOT, HIDE_ON_SCREEN_RECORD, HIDE_ISLAND_PER_APP,
        ENABLE_FOCUS_MODE, PRODUCTIVE_APPS, ENABLE_LOW_LATENCY_MODE,
        ENABLE_CLIPBOARD_PAPERCLIP, CLIPBOARD_CLEANER, PRIVACY_DOTS_ENABLED,
        DOZE_MODE_OPTIMISATION, OTP_DETECTION, LINK_INTERCEPT,
        TRANSLATION, BARCODE, NAVIGATION, NOTIFICATION_COALESCING,
        APP_PERMISSION_CHECKER, GAMING_HUD, SHOW_FPS, SHOW_CPU_TEMP,
        WIFI_ALERT_DURATION, BT_ALERT_DURATION, HOTSPOT_ALERT_DURATION,
        DATA_ALERT_DURATION, RING_MEDIA_VISIBLE, RING_BATTERY_VISIBLE,
        RING_DATA_VISIBLE, INVISIBLE_RING_TOUCH_PASSTHROUGH,
        ANTI_BURN_IN_ENABLED, ANTI_BURN_IN_INTENSITY,
        HAPTIC_FEEDBACK, HAPTIC_INTENSITY, RING_CADENCE_VIBRATION,
        HAPTIC_MORSE_ALERTS, ROLE_CALLING_APP, ALLOWED_MUSIC_APPS,
        ALLOWED_MEDIA_APPS, ALLOWED_NOTES_APPS, CALL_STYLE,
        CHARGING_STYLE, BATTERY_STYLE, RING_PULSE_STYLE,
        AUTO_BACKUP_ENABLED, AUTO_BACKUP_FREQ_DAYS, STASH_STORAGE_PATH,
        TALKBACK_INTEGRATION, PROXIMITY_WAKE, TIMER_INTEGRATION,
        SPLIT_PILL_ENABLED, ICON_PACK, ENABLE_FREEFORM_PORTAL_ANIM, TIME_BASED_THEMES,
        HIGH_CONTRAST_MODE, ICON_PACK_INTEGRATION, SHOW_RING_IDLE
    }

    fun getBoolean(key: SettingKey, default: Boolean): Boolean =
        if (isSystemUI) ipcClient.getBoolean(key.name, default)
        else prefs.getBoolean(key.name, default)

    fun putBoolean(key: SettingKey, value: Boolean) {
        prefs.edit().putBoolean(key.name, value).apply()
        ipcClient.putBoolean(key.name, value)
    }

    fun getInt(key: SettingKey, default: Int): Int =
        if (isSystemUI) ipcClient.getInt(key.name, default)
        else prefs.getInt(key.name, default)

    fun putInt(key: SettingKey, value: Int) {
        prefs.edit().putInt(key.name, value).apply()
        ipcClient.putInt(key.name, value)
    }

    fun getFloat(key: SettingKey, default: Float): Float =
        if (isSystemUI) ipcClient.getFloat(key.name, default)
        else prefs.getFloat(key.name, default)

    fun putFloat(key: SettingKey, value: Float) {
        prefs.edit().putFloat(key.name, value).apply()
        ipcClient.putFloat(key.name, value)
    }

    fun getString(key: SettingKey, default: String?): String? =
        if (isSystemUI) ipcClient.getString(key.name, default ?: "")
        else prefs.getString(key.name, default)

    fun getRawString(keyName: String, default: String): String =
        if (isSystemUI) ipcClient.getString(keyName, default)
        else prefs.getString(keyName, default) ?: default

    fun putString(key: SettingKey, value: String) {
        prefs.edit().putString(key.name, value).apply()
        ipcClient.putString(key.name, value)
    }

    fun getStringSet(key: SettingKey, default: Set<String>): Set<String> {
        val raw = ipcClient.getString(key.name, "")
        return if (raw.isEmpty()) {
            if (isSystemUI) default else prefs.getStringSet(key.name, default) ?: default
        } else {
            raw.split(",").toSet()
        }
    }

    fun putStringSet(key: SettingKey, values: Set<String>) {
        prefs.edit().putStringSet(key.name, values).apply()
        ipcClient.putString(key.name, values.joinToString(","))
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    fun clearAiMemory(): Boolean = ipcClient.clearAiMemory()

    fun exportAiData(): String? = ipcClient.exportAiData()

    fun getSettingsState(): SettingsState {
        val iconPackId = getString(SettingKey.ICON_PACK, "MATERIAL_YOU") ?: "MATERIAL_YOU"
        return SettingsState(
            designLanguage = try { DesignLanguage.valueOf(getString(SettingKey.DESIGN_LANGUAGE, "MATERIAL_YOU") ?: "MATERIAL_YOU") } catch(e: Exception) { DesignLanguage.MATERIAL_YOU },
            dynamicColors = getBoolean(SettingKey.DYNAMIC_COLORS, true),
            customAccentColor = Color(getInt(SettingKey.ACCENT_COLOR, 0xFF6750A4.toInt())),
            blurIntensity = getFloat(SettingKey.BLUR_INTENSITY, 15f),
            pillCornerRadius = getFloat(SettingKey.PILL_RADIUS, 100f),
            animationSpeed = try { AnimationSpeed.valueOf(getString(SettingKey.ANIM_SPEED, "NORMAL") ?: "NORMAL") } catch(e: Exception) { AnimationSpeed.NORMAL },
            showRingIdle = getBoolean(SettingKey.SHOW_RING_IDLE, true),
            pillShape = getString(SettingKey.PILL_SHAPE, "pill") ?: "pill",
            dynamicGradient = getBoolean(SettingKey.DYNAMIC_GRADIENT, true),
            glowEffect = getBoolean(SettingKey.GLOW_EFFECT, true),
            dotMode = getBoolean(SettingKey.DOT_MODE, false),
            elasticStretch = getBoolean(SettingKey.ELASTIC_STRETCH, true),
            shadowCasting = getBoolean(SettingKey.SHADOW_CASTING, true),
            contentAwareBlur = getBoolean(SettingKey.CONTENT_AWARE_BLUR, true),
            timeBasedThemes = getBoolean(SettingKey.TIME_BASED_THEMES, false),
            highContrastMode = getBoolean(SettingKey.HIGH_CONTRAST_MODE, false),
            iconPackIntegration = getBoolean(SettingKey.ICON_PACK_INTEGRATION, false),
            aestheticStyle = try { AestheticStyle.valueOf(getString(SettingKey.AESTHETIC_STYLE, "GLASS") ?: "GLASS") } catch(e: Exception) { AestheticStyle.GLASS },
            monochromeIcons = getBoolean(SettingKey.MONOCHROME_ICONS, false),
            enableMetaballTear = getBoolean(SettingKey.ENABLE_METABALL_TEAR, true),
            liveBridgeEnabled = getBoolean(SettingKey.LIVE_BRIDGE_ENABLED, false),
            magneticEdgeDocking = getBoolean(SettingKey.MAGNETIC_EDGE_DOCKING, true),
            geminiAuraEnabled = getBoolean(SettingKey.GEMINI_AURA_ENABLED, true),
            rollingTypographyEnabled = getBoolean(SettingKey.ROLLING_TYPOGRAPHY_ENABLED, true),
            parseDeliveryNotifications = getBoolean(SettingKey.PARSE_DELIVERY_NOTIFICATIONS, true),
            warpChargeAnimation = getBoolean(SettingKey.WARP_CHARGE_ANIMATION, true),
            batteryAwareAnimation = getBoolean(SettingKey.BATTERY_AWARE_ANIMATION, true),
            nowPlaying = getBoolean(SettingKey.NOW_PLAYING, true),
            musicVisualizerStyle = getString(SettingKey.MUSIC_VISUALIZER_STYLE, "NEURAL_CIRCLE") ?: "NEURAL_CIRCLE",
            waveformEnabled = getBoolean(SettingKey.WAVEFORM_ENABLED, true),
            mediaArtworkBlur = getBoolean(SettingKey.MEDIA_ARTWORK_BLUR, true),
            bpmPulse = getBoolean(SettingKey.BPM_PULSE, true),
            ambientReactiveRing = getBoolean(SettingKey.AMBIENT_REACTIVE_RING, true),
            ambientReactive = getBoolean(SettingKey.AMBIENT_REACTIVE, true),
            physicsStyle = try { PhysicsStyle.valueOf(getString(SettingKey.PHYSICS_STYLE, "APPLE") ?: "APPLE") } catch(e: Exception) { PhysicsStyle.APPLE },
            contentTransitionStyle = try { ContentTransitionStyle.valueOf(getString(SettingKey.CONTENT_TRANSITION_STYLE, "SLIDE") ?: "SLIDE") } catch(e: Exception) { ContentTransitionStyle.SLIDE },
            velocitySquishEnabled = getBoolean(SettingKey.VELOCITY_SQUISH_ENABLED, true),
            inlineReplyEnabled = getBoolean(SettingKey.INLINE_REPLY_ENABLED, true),
            enableMaxWidgets = getBoolean(SettingKey.ENABLE_MAX_WIDGETS, true),
            showVitalsRam = getBoolean(SettingKey.SHOW_VITALS_RAM, true),
            showVitalsCpu = getBoolean(SettingKey.SHOW_VITALS_CPU, true),
            showVitalsNet = getBoolean(SettingKey.SHOW_VITALS_NET, true),
            showVitalsFps = getBoolean(SettingKey.SHOW_VITALS_FPS, true),
            showVitalsBatCycles = getBoolean(SettingKey.SHOW_VITALS_BAT_CYCLES, true),
            shortcutLayout = try { ShortcutLayout.valueOf(getString(SettingKey.SHORTCUT_LAYOUT, "GRID") ?: "GRID") } catch(e: Exception) { ShortcutLayout.GRID },
            assistBridgeEnabled = getBoolean(SettingKey.ASSIST_BRIDGE_ENABLED, false),
            assistBridgeTarget = getString(SettingKey.ASSIST_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",
            lensBridgeEnabled = getBoolean(SettingKey.LENS_BRIDGE_ENABLED, false),
            lensBridgeTarget = getString(SettingKey.LENS_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",
            smartGesturesEnabled = getBoolean(SettingKey.SMART_GESTURES_ENABLED, true),
            smartCallOverride = getBoolean(SettingKey.SMART_CALL_OVERRIDE, true),
            smartMediaOverride = getBoolean(SettingKey.SMART_MEDIA_OVERRIDE, true),
            smartGamingOverride = getBoolean(SettingKey.SMART_GAMING_OVERRIDE, true),
            predictionTint = getBoolean(SettingKey.PREDICTION_TINT, true),
            predictiveActions = getBoolean(SettingKey.PREDICTIVE_ACTIONS, true),
            autoDismissDelay = getInt(SettingKey.AUTO_DISMISS_DELAY, 5),
            contextualSuggestions = getBoolean(SettingKey.CONTEXTUAL_SUGGESTIONS, true),
            gestureLearning = getBoolean(SettingKey.GESTURE_LEARNING, true),
            aiConfidenceThreshold = getInt(SettingKey.AI_CONFIDENCE_THRESHOLD, 10),
            aiReinforcementRate = getFloat(SettingKey.AI_REINFORCEMENT_RATE, 1.0f),
            allowMusicMid = getBoolean(SettingKey.ALLOW_MUSIC_MID, true),
            allowMusicMax = getBoolean(SettingKey.ALLOW_MUSIC_MAX, true),
            allowChargingMini = getBoolean(SettingKey.ALLOW_CHARGING_MINI, true),
            allowChargingMid = getBoolean(SettingKey.ALLOW_CHARGING_MID, true),
            allowNotifMini = getBoolean(SettingKey.ALLOW_NOTIF_MINI, true),
            allowNotifMid = getBoolean(SettingKey.ALLOW_NOTIF_MID, true),
            allowNotifMax = getBoolean(SettingKey.ALLOW_NOTIF_MAX, true),
            allowCallMid = getBoolean(SettingKey.ALLOW_CALL_MID, true),
            allowCallMax = getBoolean(SettingKey.ALLOW_CALL_MAX, true),
            allowTaskMini = getBoolean(SettingKey.ALLOW_TASK_MINI, true),
            allowTaskMid = getBoolean(SettingKey.ALLOW_TASK_MID, true),
            islandEnabled = getBoolean(SettingKey.ISLAND_ENABLED, true),
            islandOnLockscreen = getBoolean(SettingKey.ISLAND_ON_LOCKSCREEN, true),
            lockscreenFeatures = getStringSet(SettingKey.LOCKSCREEN_FEATURES, setOf("music", "notifications")),
            allowedNotificationApps = getStringSet(SettingKey.ALLOWED_NOTIFICATION_APPS, emptySet()),
            swipeLeftAction = getString(SettingKey.SWIPE_LEFT_ACTION, "dismiss") ?: "dismiss",
            swipeRightAction = getString(SettingKey.SWIPE_RIGHT_ACTION, "next_track") ?: "next_track",
            hideOnScreenshot = getBoolean(SettingKey.HIDE_ON_SCREENSHOT, true),
            hideOnScreenRecord = getBoolean(SettingKey.HIDE_ON_SCREEN_RECORD, true),
            hideIslandPerApp = getStringSet(SettingKey.HIDE_ISLAND_PER_APP, emptySet()),
            enableFocusMode = getBoolean(SettingKey.ENABLE_FOCUS_MODE, false),
            productiveApps = getStringSet(SettingKey.PRODUCTIVE_APPS, emptySet()),
            enableLowLatencyMode = getBoolean(SettingKey.ENABLE_LOW_LATENCY_MODE, false),
            enableClipboardPaperclip = getBoolean(SettingKey.ENABLE_CLIPBOARD_PAPERCLIP, true),
            clipboardCleaner = getBoolean(SettingKey.CLIPBOARD_CLEANER, true),
            privacyDotsEnabled = getBoolean(SettingKey.PRIVACY_DOTS_ENABLED, false),
            dozeModeOptimisation = getBoolean(SettingKey.DOZE_MODE_OPTIMISATION, true),
            otpDetection = getBoolean(SettingKey.OTP_DETECTION, true),
            linkIntercept = getBoolean(SettingKey.LINK_INTERCEPT, true),
            translation = getBoolean(SettingKey.TRANSLATION, true),
            barcode = getBoolean(SettingKey.BARCODE, true),
            navigation = getBoolean(SettingKey.NAVIGATION, true),
            notificationCoalescing = getBoolean(SettingKey.NOTIFICATION_COALESCING, true),
            appPermissionChecker = getBoolean(SettingKey.APP_PERMISSION_CHECKER, true),
            gamingHud = getBoolean(SettingKey.GAMING_HUD, true),
            showFps = getBoolean(SettingKey.SHOW_FPS, false),
            showCpuTemp = getBoolean(SettingKey.SHOW_CPU_TEMP, false),
            wifiAlertDuration = getInt(SettingKey.WIFI_ALERT_DURATION, 3),
            btAlertDuration = getInt(SettingKey.BT_ALERT_DURATION, 3),
            hotspotAlertDuration = getInt(SettingKey.HOTSPOT_ALERT_DURATION, 5),
            dataAlertDuration = getInt(SettingKey.DATA_ALERT_DURATION, 3),
            ringMediaVisible = getBoolean(SettingKey.RING_MEDIA_VISIBLE, true),
            ringBatteryVisible = getBoolean(SettingKey.RING_BATTERY_VISIBLE, true),
            ringDataVisible = getBoolean(SettingKey.RING_DATA_VISIBLE, true),
            invisibleRingTouchPassthrough = getBoolean(SettingKey.INVISIBLE_RING_TOUCH_PASSTHROUGH, true),
            antiBurnInEnabled = getBoolean(SettingKey.ANTI_BURN_IN_ENABLED, true),
            antiBurnInIntensity = getFloat(SettingKey.ANTI_BURN_IN_INTENSITY, 1.5f),
            hapticFeedback = getBoolean(SettingKey.HAPTIC_FEEDBACK, true),
            hapticIntensity = getFloat(SettingKey.HAPTIC_INTENSITY, 1f),
            ringCadenceVibration = getBoolean(SettingKey.RING_CADENCE_VIBRATION, true),
            hapticMorseAlerts = getBoolean(SettingKey.HAPTIC_MORSE_ALERTS, false),
            roleCallingApp = getString(SettingKey.ROLE_CALLING_APP, "") ?: "",
            allowedMusicApps = getStringSet(SettingKey.ALLOWED_MUSIC_APPS, emptySet()),
            allowedMediaApps = getStringSet(SettingKey.ALLOWED_MEDIA_APPS, emptySet()),
            allowedNotesApps = getStringSet(SettingKey.ALLOWED_NOTES_APPS, emptySet()),
            callStyle = try { CallStyle.valueOf(getString(SettingKey.CALL_STYLE, "IOS") ?: "IOS") } catch(e: Exception) { CallStyle.IOS },
            chargingStyle = try { ChargingStyle.valueOf(getString(SettingKey.CHARGING_STYLE, "RING") ?: "RING") } catch(e: Exception) { ChargingStyle.RING },
            batteryStyle = try { BatteryStyle.valueOf(getString(SettingKey.BATTERY_STYLE, "PILL") ?: "PILL") } catch(e: Exception) { BatteryStyle.PILL },
            ringPulseStyle = try { RingPulseStyle.valueOf(getString(SettingKey.RING_PULSE_STYLE, "BREATH") ?: "BREATH") } catch(e: Exception) { RingPulseStyle.BREATH },
            autoBackupEnabled = getBoolean(SettingKey.AUTO_BACKUP_ENABLED, false),
            autoBackupFreqDays = getInt(SettingKey.AUTO_BACKUP_FREQ_DAYS, 7),
            stashStoragePath = getString(SettingKey.STASH_STORAGE_PATH, "/sdcard/DynamicIsland/Archive") ?: "/sdcard/DynamicIsland/Archive",
            talkbackIntegration = getBoolean(SettingKey.TALKBACK_INTEGRATION, true),
            proximityWake = getBoolean(SettingKey.PROXIMITY_WAKE, false),
            timerIntegration = getBoolean(SettingKey.TIMER_INTEGRATION, true),
            splitPillEnabled = getBoolean(SettingKey.SPLIT_PILL_ENABLED, true),
            iconPack = IconPack.fromString(iconPackId),
            enableFreeformPortalAnim = getBoolean(SettingKey.ENABLE_FREEFORM_PORTAL_ANIM, true)
        )
    }
}
