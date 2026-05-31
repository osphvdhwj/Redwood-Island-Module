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
        // === Appearance ===
        USE_LIQUID_GLASS, DYNAMIC_COLORS, ACCENT_COLOR, BLUR_INTENSITY,
        PILL_RADIUS, ANIM_SPEED, RING_IDLE, PILL_SHAPE,
        DYNAMIC_GRADIENT, GLOW_EFFECT, DOT_MODE, ELASTIC_STRETCH,
        SHADOW_CASTING, CONTENT_AWARE_BLUR, TIME_BASED_THEMES,
        HIGH_CONTRAST_MODE, ICON_PACK_INTEGRATION,
        
        // --- Ultimate Refinement: Appearance ---
        ENABLE_METABALL_TEAR, PHYSICS_STYLE, CONTENT_TRANSITION_STYLE,
        RING_PULSE_STYLE, AESTHETIC_STYLE, MONOCHROME_ICONS,

        // === Notifications & Detection ===
        OTP_DETECTION, LINK_INTERCEPT, TRANSLATION, BARCODE, NAVIGATION,
        NOTIFICATION_COALESCING, APP_PERMISSION_CHECKER,

        // === Gaming HUD ===
        GAMING_HUD, SHOW_FPS, SHOW_CPU_TEMP, GAMING_DASHBOARD_OVERLAY,

        // === Media & Audio ===
        MEDIA_ARTWORK_BLUR, WAVEFORM_ENABLED, AMBIENT_REACTIVE,
        AUDIO_SENSITIVITY, BPM_PULSE, LIVE_MUSIC_VISUALIZER,
        NOW_PLAYING, LIVE_CAPTION, VOICE_MEMO_TRANSCRIPTION,
        MUSIC_VISUALIZER_STYLE,

        // === Haptics ===
        HAPTIC_FEEDBACK, HAPTIC_INTENSITY, RING_CADENCE_VIBRATION,
        HAPTIC_MORSE_ALERTS,

        // === Advanced Triggers & Sensors ===
        RING_MEDIA_VISIBLE, RING_BATTERY_VISIBLE, RING_DOWNLOAD_VISIBLE,
        RING_BLUETOOTH_VISIBLE, RING_HOTSPOT_VISIBLE, RING_DATA_VISIBLE,
        INVISIBLE_RING_TOUCH_PASSTHROUGH, ANTI_BURN_IN_ENABLED, ANTI_BURN_IN_INTENSITY,
        WIFI_ALERT_DURATION, BT_ALERT_DURATION, HOTSPOT_ALERT_DURATION, DATA_ALERT_DURATION,
        LIVE_DOWNLOAD_TRACKING, NETWORK_SPEED_RING,
        
        // --- Ultimate Refinement: Sensors ---
        ENABLE_LOCATION_AWARENESS, ENABLE_TIME_THEMES, DAY_THEME_COLORS, NIGHT_THEME_COLORS,

        // === Smart AI Gestures ===
        SMART_GESTURES_ENABLED, SMART_MEDIA_OVERRIDE, 
        SMART_GAMING_OVERRIDE, SMART_CALL_OVERRIDE,

        // === State Constraint Engine ===
        ALLOW_MUSIC_MID, ALLOW_MUSIC_MAX,
        ALLOW_CHARGING_MINI, ALLOW_CHARGING_MID,
        ALLOW_NOTIF_MINI, ALLOW_NOTIF_MID, ALLOW_NOTIF_MAX,
        ALLOW_CALL_MID, ALLOW_CALL_MAX,
        ALLOW_TASK_MINI, ALLOW_TASK_MID,

        // === Floating / Freeform Windows ===
        FREEFORM_LAUNCH_ENABLED, FREEFORM_SMART_GESTURE,
        ENABLE_FREEFORM_PORTAL_ANIM,

        // === Global Controls ===
        ISLAND_ENABLED, ISLAND_ON_LOCKSCREEN, FEATURES_ON_LOCKSCREEN,
        ALLOWED_MUSIC_APPS, ALLOWED_MEDIA_APPS, ALLOWED_NOTES_APPS,
        ALLOWED_NOTIFICATION_APPS,
        SWIPE_LEFT_ACTION, SWIPE_RIGHT_ACTION,
        
        // --- Ultimate Refinement: Focus Mode ---
        ENABLE_FOCUS_MODE, PRODUCTIVE_APPS,

        // === Call & Communication ===
        CALL_SCREEN_TRANSCRIPT, SILENT_RING_MODE_SWITCH,

        // === Weather & Environment ===
        WEATHER_MOOD_RING, AT_A_GLANCE,

        // === Split & Multi-Event ===
        SPLIT_PILL_ENABLED, MERGE_SIMULTANEOUS_EVENTS,
        MULTI_ISLAND_STACK,

        // === Continuity Camera ===
        CONTINUITY_CAMERA_ACTIONS,

        // === Focus & DND ===
        FOCUS_MODE_PILL,

        // === App Roles ===
        ROLE_CALLING_APP, ROLE_GAME_LAUNCHER,

        // === Styles ===
        CALL_STYLE, CHARGING_STYLE, BATTERY_STYLE,
        
        // --- Ultimate Refinement: Dashboard ---
        ENABLE_MAX_WIDGETS, SHOW_VITALS_RAM, SHOW_VITALS_CPU,
        SHOW_VITALS_NET, SHOW_VITALS_FPS, SHOW_VITALS_BAT_CYCLES,
        SHORTCUT_LAYOUT,

        // --- Ultimate Refinement: Data & Storage ---
        AUTO_BACKUP_ENABLED, AUTO_BACKUP_FREQ_DAYS, STASH_STORAGE_PATH,
        ENABLE_CLIPBOARD_PAPERCLIP,
        
        // --- Ultimate Refinement: Performance ---
        ENABLE_LOW_LATENCY_MODE,
        
        // --- Ultimate Refinement: Per-App ---
        HIDE_ISLAND_PER_APP, HIDE_STATES_PER_APP_JSON,
        HIDE_ON_SCREENSHOT, HIDE_ON_SCREEN_RECORD,

        // === The Final 'Aura & Bridge' Core ===
        LIVE_BRIDGE_ENABLED, MAGNETIC_EDGE_DOCKING, GEMINI_AURA_ENABLED,
        ROLLING_TYPOGRAPHY_ENABLED, PARSE_DELIVERY_NOTIFICATIONS,
        WARP_CHARGE_ANIMATION, VELOCITY_SQUISH_ENABLED, INLINE_REPLY_ENABLED,
        
        ASSIST_BRIDGE_ENABLED, ASSIST_BRIDGE_TARGET,
        LENS_BRIDGE_ENABLED, LENS_BRIDGE_TARGET,
        
        PER_APP_GESTURES_JSON, PER_APP_STATES_JSON,
        AI_CONFIDENCE_THRESHOLD, AI_REINFORCEMENT_RATE,

        ICON_PACK
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
        val iconPackName = getString(SettingKey.ICON_PACK, "MATERIAL_YOU") ?: "MATERIAL_YOU"
        return SettingsState(
            // Appearance
            designLanguage = if (getBoolean(SettingKey.USE_LIQUID_GLASS, false)) DesignLanguage.APPLE_LIQUID_GLASS else DesignLanguage.MATERIAL_YOU,
            dynamicColors = getBoolean(SettingKey.DYNAMIC_COLORS, true),
            customAccentColor = Color(getInt(SettingKey.ACCENT_COLOR, 0xFF6750A4.toInt())),
            blurIntensity = getFloat(SettingKey.BLUR_INTENSITY, 15f),
            pillCornerRadius = getFloat(SettingKey.PILL_RADIUS, 100f),
            animationSpeed = try { AnimationSpeed.valueOf(getString(SettingKey.ANIM_SPEED, "NORMAL") ?: "NORMAL") } catch(e: Exception) { AnimationSpeed.NORMAL },
            showRingIdle = getBoolean(SettingKey.RING_IDLE, true),
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

            // --- Ultimate Refinement: Appearance ---
            enableMetaballTear = getBoolean(SettingKey.ENABLE_METABALL_TEAR, true),
            physicsStyle = try { PhysicsStyle.valueOf(getString(SettingKey.PHYSICS_STYLE, "APPLE") ?: "APPLE") } catch(e: Exception) { PhysicsStyle.APPLE },
            contentTransitionStyle = try { ContentTransitionStyle.valueOf(getString(SettingKey.CONTENT_TRANSITION_STYLE, "SLIDE") ?: "SLIDE") } catch(e: Exception) { ContentTransitionStyle.SLIDE },
            ringPulseStyle = try { RingPulseStyle.valueOf(getString(SettingKey.RING_PULSE_STYLE, "BREATH") ?: "BREATH") } catch(e: Exception) { RingPulseStyle.BREATH },
            aestheticStyle = try { AestheticStyle.valueOf(getString(SettingKey.AESTHETIC_STYLE, "GLASS") ?: "GLASS") } catch(e: Exception) { AestheticStyle.GLASS },
            monochromeIcons = getBoolean(SettingKey.MONOCHROME_ICONS, false),

            // Notifications & Detection
            otpDetection = getBoolean(SettingKey.OTP_DETECTION, true),
            linkIntercept = getBoolean(SettingKey.LINK_INTERCEPT, true),
            translation = getBoolean(SettingKey.TRANSLATION, true),
            barcode = getBoolean(SettingKey.BARCODE, true),
            navigation = getBoolean(SettingKey.NAVIGATION, true),
            notificationCoalescing = getBoolean(SettingKey.NOTIFICATION_COALESCING, true),
            appPermissionChecker = getBoolean(SettingKey.APP_PERMISSION_CHECKER, true),

            // Gaming HUD
            gamingHud = getBoolean(SettingKey.GAMING_HUD, true),
            showFps = getBoolean(SettingKey.SHOW_FPS, false),
            showCpuTemp = getBoolean(SettingKey.SHOW_CPU_TEMP, false),
            gamingDashboardOverlay = getBoolean(SettingKey.GAMING_DASHBOARD_OVERLAY, false),

            // Media & Audio
            mediaArtworkBlur = getBoolean(SettingKey.MEDIA_ARTWORK_BLUR, true),
            waveformEnabled = getBoolean(SettingKey.WAVEFORM_ENABLED, true),
            ambientReactiveRing = getBoolean(SettingKey.AMBIENT_REACTIVE, true),
            audioSensitivity = getFloat(SettingKey.AUDIO_SENSITIVITY, 0.5f),
            bpmPulse = getBoolean(SettingKey.BPM_PULSE, true),
            liveMusicVisualizer = getBoolean(SettingKey.LIVE_MUSIC_VISUALIZER, false),
            nowPlaying = getBoolean(SettingKey.NOW_PLAYING, true),
            liveCaption = getBoolean(SettingKey.LIVE_CAPTION, false),
            voiceMemoTranscription = getBoolean(SettingKey.VOICE_MEMO_TRANSCRIPTION, true),
            musicVisualizerStyle = getString(SettingKey.MUSIC_VISUALIZER_STYLE, "NEURAL_CIRCLE") ?: "NEURAL_CIRCLE",

            // Haptics
            hapticFeedback = getBoolean(SettingKey.HAPTIC_FEEDBACK, true),
            hapticIntensity = getFloat(SettingKey.HAPTIC_INTENSITY, 1f),
            ringCadenceVibration = getBoolean(SettingKey.RING_CADENCE_VIBRATION, true),
            hapticMorseAlerts = getBoolean(SettingKey.HAPTIC_MORSE_ALERTS, false),

            // Advanced Triggers & Sensors
            ringMediaVisible = getBoolean(SettingKey.RING_MEDIA_VISIBLE, true),
            ringBatteryVisible = getBoolean(SettingKey.RING_BATTERY_VISIBLE, true),
            ringDownloadVisible = getBoolean(SettingKey.RING_DOWNLOAD_VISIBLE, true),
            ringBluetoothVisible = getBoolean(SettingKey.RING_BLUETOOTH_VISIBLE, true),
            ringHotspotVisible = getBoolean(SettingKey.RING_HOTSPOT_VISIBLE, true),
            ringDataVisible = getBoolean(SettingKey.RING_DATA_VISIBLE, true),
            invisibleRingTouchPassthrough = getBoolean(SettingKey.INVISIBLE_RING_TOUCH_PASSTHROUGH, true),
            antiBurnInEnabled = getBoolean(SettingKey.ANTI_BURN_IN_ENABLED, true),
            antiBurnInIntensity = getFloat(SettingKey.ANTI_BURN_IN_INTENSITY, 1.5f),
            wifiAlertDuration = getInt(SettingKey.WIFI_ALERT_DURATION, 3),
            btAlertDuration = getInt(SettingKey.BT_ALERT_DURATION, 3),
            hotspotAlertDuration = getInt(SettingKey.HOTSPOT_ALERT_DURATION, 5),
            dataAlertDuration = getInt(SettingKey.DATA_ALERT_DURATION, 3),
            liveDownloadTracking = getBoolean(SettingKey.LIVE_DOWNLOAD_TRACKING, true),
            networkSpeedRing = getBoolean(SettingKey.NETWORK_SPEED_RING, true),
            
            // --- Ultimate Refinement: Sensors ---
            enableLocationAwareness = getBoolean(SettingKey.ENABLE_LOCATION_AWARENESS, false),
            enableTimeThemes = getBoolean(SettingKey.ENABLE_TIME_THEMES, false),
            dayThemeColors = getString(SettingKey.DAY_THEME_COLORS, "#6750A4") ?: "#6750A4",
            nightThemeColors = getString(SettingKey.NIGHT_THEME_COLORS, "#1E1E2E") ?: "#1E1E2E",

            // Smart AI Gestures
            smartGesturesEnabled = getBoolean(SettingKey.SMART_GESTURES_ENABLED, true),
            smartMediaOverride = getBoolean(SettingKey.SMART_MEDIA_OVERRIDE, true),
            smartGamingOverride = getBoolean(SettingKey.SMART_GAMING_OVERRIDE, true),
            smartCallOverride = getBoolean(SettingKey.SMART_CALL_OVERRIDE, true),

            // State Constraint Engine
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

            // Floating / Freeform Windows
            freeformLaunchEnabled = getBoolean(SettingKey.FREEFORM_LAUNCH_ENABLED, true),
            freeformSmartGesture = getBoolean(SettingKey.FREEFORM_SMART_GESTURE, true),
            enableFreeformPortalAnim = getBoolean(SettingKey.ENABLE_FREEFORM_PORTAL_ANIM, true),

            // Global Controls
            islandEnabled = getBoolean(SettingKey.ISLAND_ENABLED, true),
            islandOnLockscreen = getBoolean(SettingKey.ISLAND_ON_LOCKSCREEN, true),
            allowedNotificationApps = getStringSet(SettingKey.ALLOWED_NOTIFICATION_APPS, emptySet()),
            swipeLeftAction = getString(SettingKey.SWIPE_LEFT_ACTION, "dismiss") ?: "dismiss",
            swipeRightAction = getString(SettingKey.SWIPE_RIGHT_ACTION, "next_track") ?: "next_track",
            
            // --- Ultimate Refinement: Focus Mode ---
            enableFocusMode = getBoolean(SettingKey.ENABLE_FOCUS_MODE, false),
            productiveApps = getStringSet(SettingKey.PRODUCTIVE_APPS, emptySet()),

            // App Roles
            roleCallingApp = getString(SettingKey.ROLE_CALLING_APP, "") ?: "",
            roleGameLauncher = getString(SettingKey.ROLE_GAME_LAUNCHER, "") ?: "",
            allowedMusicApps = getStringSet(SettingKey.ALLOWED_MUSIC_APPS, emptySet()),
            allowedMediaApps = getStringSet(SettingKey.ALLOWED_MEDIA_APPS, emptySet()),
            allowedNotesApps = getStringSet(SettingKey.ALLOWED_NOTES_APPS, emptySet()),

            // Styles
            callStyle = try { CallStyle.valueOf(getString(SettingKey.CALL_STYLE, "IOS") ?: "IOS") } catch(e: Exception) { CallStyle.IOS },
            chargingStyle = try { ChargingStyle.valueOf(getString(SettingKey.CHARGING_STYLE, "RING") ?: "RING") } catch(e: Exception) { ChargingStyle.RING },
            batteryStyle = try { BatteryStyle.valueOf(getString(SettingKey.BATTERY_STYLE, "PILL") ?: "PILL") } catch(e: Exception) { BatteryStyle.PILL },
            
            // --- Ultimate Refinement: Dashboard ---
            enableMaxWidgets = getBoolean(SettingKey.ENABLE_MAX_WIDGETS, true),
            showVitalsRam = getBoolean(SettingKey.SHOW_VITALS_RAM, true),
            showVitalsCpu = getBoolean(SettingKey.SHOW_VITALS_CPU, true),
            showVitalsNet = getBoolean(SettingKey.SHOW_VITALS_NET, true),
            showVitalsFps = getBoolean(SettingKey.SHOW_VITALS_FPS, true),
            showVitalsBatCycles = getBoolean(SettingKey.SHOW_VITALS_BAT_CYCLES, true),
            shortcutLayout = try { ShortcutLayout.valueOf(getString(SettingKey.SHORTCUT_LAYOUT, "GRID") ?: "GRID") } catch(e: Exception) { ShortcutLayout.GRID },

            // --- Ultimate Refinement: Data & Storage ---
            autoBackupEnabled = getBoolean(SettingKey.AUTO_BACKUP_ENABLED, false),
            autoBackupFreqDays = getInt(SettingKey.AUTO_BACKUP_FREQ_DAYS, 7),
            stashStoragePath = getString(SettingKey.STASH_STORAGE_PATH, "/sdcard/DynamicIsland/Archive") ?: "/sdcard/DynamicIsland/Archive",
            enableClipboardPaperclip = getBoolean(SettingKey.ENABLE_CLIPBOARD_PAPERCLIP, true),
            
            // --- Ultimate Refinement: Performance ---
            enableLowLatencyMode = getBoolean(SettingKey.ENABLE_LOW_LATENCY_MODE, false),
            
            // --- Ultimate Refinement: Privacy ---
            hideOnScreenshot = getBoolean(SettingKey.HIDE_ON_SCREENSHOT, true),
            hideOnScreenRecord = getBoolean(SettingKey.HIDE_ON_SCREEN_RECORD, true),
            hideIslandPerApp = getStringSet(SettingKey.HIDE_ISLAND_PER_APP, emptySet()),
            hideStatesPerAppJson = getString(SettingKey.HIDE_STATES_PER_APP_JSON, "{}") ?: "{}",

            // --- The Final 'Aura & Bridge' Core ---
            liveBridgeEnabled = getBoolean(SettingKey.LIVE_BRIDGE_ENABLED, false),
            magneticEdgeDocking = getBoolean(SettingKey.MAGNETIC_EDGE_DOCKING, true),
            geminiAuraEnabled = getBoolean(SettingKey.GEMINI_AURA_ENABLED, true),
            rollingTypographyEnabled = getBoolean(SettingKey.ROLLING_TYPOGRAPHY_ENABLED, true),
            parseDeliveryNotifications = getBoolean(SettingKey.PARSE_DELIVERY_NOTIFICATIONS, true),
            warpChargeAnimation = getBoolean(SettingKey.WARP_CHARGE_ANIMATION, true),
            velocitySquishEnabled = getBoolean(SettingKey.VELOCITY_SQUISH_ENABLED, true),
            inlineReplyEnabled = getBoolean(SettingKey.INLINE_REPLY_ENABLED, true),

            assistBridgeEnabled = getBoolean(SettingKey.ASSIST_BRIDGE_ENABLED, false),
            assistBridgeTarget = getString(SettingKey.ASSIST_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",
            lensBridgeEnabled = getBoolean(SettingKey.LENS_BRIDGE_ENABLED, false),
            lensBridgeTarget = getString(SettingKey.LENS_BRIDGE_TARGET, "com.brave.browser") ?: "com.brave.browser",

            perAppGesturesJson = getString(SettingKey.PER_APP_GESTURES_JSON, "{}") ?: "{}",
            perAppStatesJson = getString(SettingKey.PER_APP_STATES_JSON, "{}") ?: "{}",
            aiConfidenceThreshold = getInt(SettingKey.AI_CONFIDENCE_THRESHOLD, 10),
            aiReinforcementRate = getFloat(SettingKey.AI_REINFORCEMENT_RATE, 1.0f),

            iconPack = try { IconPack.valueOf(iconPackName) } catch (e: Exception) { IconPack.MATERIAL_YOU }
            )
    }
}
