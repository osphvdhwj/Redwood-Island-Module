package com.example.dynamicisland.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

class SettingsManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

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

        // === Notifications & Detection ===
        OTP_DETECTION, LINK_INTERCEPT, TRANSLATION, BARCODE, NAVIGATION,
        NOTIFICATION_COALESCING, APP_PERMISSION_CHECKER,

        // === Gaming HUD ===
        GAMING_HUD, SHOW_FPS, SHOW_CPU_TEMP, GAMING_DASHBOARD_OVERLAY,

        // === Media & Audio ===
        MEDIA_ARTWORK_BLUR, WAVEFORM_ENABLED, AMBIENT_REACTIVE,
        AUDIO_SENSITIVITY, BPM_PULSE, LIVE_MUSIC_VISUALIZER,
        NOW_PLAYING, LIVE_CAPTION, VOICE_MEMO_TRANSCRIPTION,

        // === Haptics ===
        HAPTIC_FEEDBACK, HAPTIC_INTENSITY, RING_CADENCE_VIBRATION,
        HAPTIC_MORSE_ALERTS,

        // === Prediction & Smart Features ===
        PREDICTION_TINT, PREDICTIVE_ACTIONS, AUTO_DISMISS_DELAY,
        CONTEXTUAL_SUGGESTIONS, GESTURE_LEARNING, VOICE_TRIGGER,
        ADAPTIVE_BRIGHTNESS_VOLUME, APP_PREDICTION_SUGGESTION,
        CONTEXTUAL_ROUTINE_LAUNCHER,

        // === Cross-Device & Continuity ===
        CLIPBOARD_SYNC, UNIVERSAL_CONTROL, QUICK_NOTE,
        PHONE_TO_TABLET_HANDOFF, NEARBY_SHARE_PROGRESS,
        MULTI_DEVICE_CLIPBOARD, WEAR_OS_REMOTE,
        AIRPODS_POPUP, AIRPLAY_CAST_INDICATOR, HOME_POD_CONTROL,

        // === iOS-Inspired ===
        LIVE_ACTIVITIES_API, FOCUS_FILTER_INTEGRATION,
        UNIVERSAL_CLIPBOARD_PREVIEWS, ALWAYS_ON_DISPLAY_COMPANION,
        FACE_ID_PADLOCK, RING_MODE_SWITCH, TIMER_INTEGRATION,
        MAGSAFE_CHARGING_ANIMATION, PROXIMITY_WAKE,

        // === Android Ecosystem ===
        MATERIAL_YOU_DYNAMIC_CONTRAST, QUICK_SETTINGS_TILE,
        DIGITAL_WELLBEING_INTEGRATION, ROOT_ADB_FEATURES, ICON_PACK,

        // === Accessibility ===
        TALKBACK_INTEGRATION, ONE_HAND_MODE,
        DEDICATED_ONE_HAND_PLACEMENT,

        // === Customisation ===
        CUSTOM_PILL_ANIMATIONS, THIRD_PARTY_WIDGET_API,

        // === Battery & Performance ===
        BATTERY_AWARE_ANIMATION, DOZE_MODE_OPTIMISATION,
        QUICK_PERFORMANCE_PROFILE, DATA_SAVER,

        // === Gamification ===
        ISLAND_STREAKS, LEADERBOARD, EXCLUSIVE_THEMES,
        ACHIEVEMENTS_ENABLED, ACHIEVEMENTS_DISPLAY,

        // === Privacy & Security ===
        CLIPBOARD_CLEANER, VPN_TOR_INDICATOR,

        // === Experimental ===
        AR_ISLAND, MINDFULNESS_BREATH_PACER, MORSE_CODE_INPUT,
        MULTI_USER_PROFILE_SWITCHING, CRYPTO_STOCK_TICKER,

        // === Developer Tools ===
        ADB_COMMAND_INJECTOR, TASKER_PLUGIN, LOG_DEBUG_OVERLAY,
        OPEN_SOURCE_SDK,

        // === Global Controls ===
        ISLAND_ENABLED, ISLAND_ON_LOCKSCREEN, FEATURES_ON_LOCKSCREEN,
        ALLOWED_MUSIC_APPS, ALLOWED_MEDIA_APPS, ALLOWED_NOTES_APPS,
        ALLOWED_NOTIFICATION_APPS,
        SWIPE_LEFT_ACTION, SWIPE_RIGHT_ACTION,

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
        CALL_STYLE, CHARGING_STYLE, BATTERY_STYLE
    }

    // Type-safe getters and setters
    fun getBoolean(key: SettingKey, default: Boolean): Boolean =
        prefs.getBoolean(key.name, default)

    fun putBoolean(key: SettingKey, value: Boolean) =
        prefs.edit().putBoolean(key.name, value).apply()

    fun getInt(key: SettingKey, default: Int): Int =
        prefs.getInt(key.name, default)

    fun putInt(key: SettingKey, value: Int) =
        prefs.edit().putInt(key.name, value).apply()

    fun getFloat(key: SettingKey, default: Float): Float =
        prefs.getFloat(key.name, default)

    fun putFloat(key: SettingKey, value: Float) =
        prefs.edit().putFloat(key.name, value).apply()

    fun getString(key: SettingKey, default: String?): String? =
        prefs.getString(key.name, default)

    fun putString(key: SettingKey, value: String) =
        prefs.edit().putString(key.name, value).apply()

    fun getStringSet(key: SettingKey, default: Set<String>): Set<String> =
        prefs.getStringSet(key.name, default) ?: default

    fun putStringSet(key: SettingKey, values: Set<String>) =
        prefs.edit().putStringSet(key.name, values).apply()

    // Bulk operations
    fun getAll(): Map<String, *> = prefs.all

    fun resetAll() = prefs.edit().clear().apply()

    fun getSettingsState(): SettingsState {
        val iconPackName = prefs.getString("icon_pack", "MATERIAL_YOU") ?: "MATERIAL_YOU"
        return SettingsState(
            // Appearance
            designLanguage = if (getBoolean(SettingKey.USE_LIQUID_GLASS, false)) DesignLanguage.APPLE_LIQUID_GLASS else DesignLanguage.MATERIAL_YOU,
            dynamicColors = getBoolean(SettingKey.DYNAMIC_COLORS, true),
            customAccentColor = Color(getInt(SettingKey.ACCENT_COLOR, 0xFF6750A4.toInt())),
            blurIntensity = getFloat(SettingKey.BLUR_INTENSITY, 15f),
            pillCornerRadius = getFloat(SettingKey.PILL_RADIUS, 100f),
            animationSpeed = AnimationSpeed.valueOf(getString(SettingKey.ANIM_SPEED, "NORMAL") ?: "NORMAL"),
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

            // Haptics
            hapticFeedback = getBoolean(SettingKey.HAPTIC_FEEDBACK, true),
            hapticIntensity = getFloat(SettingKey.HAPTIC_INTENSITY, 1f),
            ringCadenceVibration = getBoolean(SettingKey.RING_CADENCE_VIBRATION, true),
            hapticMorseAlerts = getBoolean(SettingKey.HAPTIC_MORSE_ALERTS, false),

            // Prediction & Smart Features
            predictionTint = getBoolean(SettingKey.PREDICTION_TINT, true),
            predictiveActions = getBoolean(SettingKey.PREDICTIVE_ACTIONS, true),
            autoDismissDelay = getInt(SettingKey.AUTO_DISMISS_DELAY, 5),
            contextualSuggestions = getBoolean(SettingKey.CONTEXTUAL_SUGGESTIONS, true),
            gestureLearning = getBoolean(SettingKey.GESTURE_LEARNING, true),
            voiceTrigger = getBoolean(SettingKey.VOICE_TRIGGER, false),
            adaptiveBrightnessVolume = getBoolean(SettingKey.ADAPTIVE_BRIGHTNESS_VOLUME, false),
            appPredictionSuggestion = getBoolean(SettingKey.APP_PREDICTION_SUGGESTION, true),
            contextualRoutineLauncher = getBoolean(SettingKey.CONTEXTUAL_ROUTINE_LAUNCHER, true),

            // Cross-Device & Continuity
            clipboardSync = getBoolean(SettingKey.CLIPBOARD_SYNC, false),
            universalControl = getBoolean(SettingKey.UNIVERSAL_CONTROL, false),
            quickNote = getBoolean(SettingKey.QUICK_NOTE, true),
            phoneToTabletHandoff = getBoolean(SettingKey.PHONE_TO_TABLET_HANDOFF, false),
            nearbyShareProgress = getBoolean(SettingKey.NEARBY_SHARE_PROGRESS, true),
            multiDeviceClipboard = getBoolean(SettingKey.MULTI_DEVICE_CLIPBOARD, false),
            wearOsRemote = getBoolean(SettingKey.WEAR_OS_REMOTE, false),
            airpodsPopup = getBoolean(SettingKey.AIRPODS_POPUP, true),
            airplayCastIndicator = getBoolean(SettingKey.AIRPLAY_CAST_INDICATOR, true),
            homePodControl = getBoolean(SettingKey.HOME_POD_CONTROL, false),

            // iOS-Inspired
            liveActivitiesApi = getBoolean(SettingKey.LIVE_ACTIVITIES_API, true),
            focusFilterIntegration = getBoolean(SettingKey.FOCUS_FILTER_INTEGRATION, true),
            universalClipboardPreviews = getBoolean(SettingKey.UNIVERSAL_CLIPBOARD_PREVIEWS, true),
            alwaysOnDisplayCompanion = getBoolean(SettingKey.ALWAYS_ON_DISPLAY_COMPANION, true),
            faceIDPadlock = getBoolean(SettingKey.FACE_ID_PADLOCK, true),
            ringModeSwitch = getBoolean(SettingKey.RING_MODE_SWITCH, true),
            timerIntegration = getBoolean(SettingKey.TIMER_INTEGRATION, true),
            magsafeChargingAnimation = getBoolean(SettingKey.MAGSAFE_CHARGING_ANIMATION, true),
            proximityWake = getBoolean(SettingKey.PROXIMITY_WAKE, false),

            // Android Ecosystem
            materialYouDynamicContrast = getBoolean(SettingKey.MATERIAL_YOU_DYNAMIC_CONTRAST, true),
            quickSettingsTile = getBoolean(SettingKey.QUICK_SETTINGS_TILE, true),
            digitalWellbeingIntegration = getBoolean(SettingKey.DIGITAL_WELLBEING_INTEGRATION, false),
            rootAdbFeatures = getBoolean(SettingKey.ROOT_ADB_FEATURES, false),
            iconPack = IconPack.fromString(iconPackName),

            // Accessibility
            talkbackIntegration = getBoolean(SettingKey.TALKBACK_INTEGRATION, true),
            oneHandMode = getBoolean(SettingKey.ONE_HAND_MODE, false),
            dedicatedOneHandPlacement = getBoolean(SettingKey.DEDICATED_ONE_HAND_PLACEMENT, false),

            // Customisation
            customPillAnimations = getString(SettingKey.CUSTOM_PILL_ANIMATIONS, null),
            thirdPartyWidgetApi = getBoolean(SettingKey.THIRD_PARTY_WIDGET_API, false),

            // Battery & Performance
            batteryAwareAnimation = getBoolean(SettingKey.BATTERY_AWARE_ANIMATION, true),
            dozeModeOptimisation = getBoolean(SettingKey.DOZE_MODE_OPTIMISATION, true),
            quickPerformanceProfile = getBoolean(SettingKey.QUICK_PERFORMANCE_PROFILE, true),
            dataSaver = getBoolean(SettingKey.DATA_SAVER, false),

            // Gamification
            islandStreaks = getBoolean(SettingKey.ISLAND_STREAKS, true),
            leaderboard = getBoolean(SettingKey.LEADERBOARD, false),
            exclusiveThemes = getBoolean(SettingKey.EXCLUSIVE_THEMES, false),
            achievementsEnabled = getBoolean(SettingKey.ACHIEVEMENTS_ENABLED, true),
            showAchievementBadge = getBoolean(SettingKey.ACHIEVEMENTS_DISPLAY, true),

            // Privacy & Security
            clipboardCleaner = getBoolean(SettingKey.CLIPBOARD_CLEANER, true),
            vpnTorIndicator = getBoolean(SettingKey.VPN_TOR_INDICATOR, true),

            // Experimental
            arIsland = getBoolean(SettingKey.AR_ISLAND, false),
            mindfulnessBreathPacer = getBoolean(SettingKey.MINDFULNESS_BREATH_PACER, false),
            morseCodeInput = getBoolean(SettingKey.MORSE_CODE_INPUT, false),
            multiUserProfileSwitching = getBoolean(SettingKey.MULTI_USER_PROFILE_SWITCHING, false),
            cryptoStockTicker = getBoolean(SettingKey.CRYPTO_STOCK_TICKER, false),

            // Developer Tools
            adbCommandInjector = getBoolean(SettingKey.ADB_COMMAND_INJECTOR, false),
            taskerPlugin = getBoolean(SettingKey.TASKER_PLUGIN, true),
            logDebugOverlay = getBoolean(SettingKey.LOG_DEBUG_OVERLAY, false),
            openSourceSdk = getBoolean(SettingKey.OPEN_SOURCE_SDK, true),

            // Global Controls
            islandEnabled = getBoolean(SettingKey.ISLAND_ENABLED, true),
            islandOnLockscreen = getBoolean(SettingKey.ISLAND_ON_LOCKSCREEN, true),
            lockscreenFeatures = getStringSet(SettingKey.FEATURES_ON_LOCKSCREEN, setOf("music", "notifications")),
            allowedNotificationApps = getStringSet(SettingKey.ALLOWED_NOTIFICATION_APPS, emptySet()),
            swipeLeftAction = getString(SettingKey.SWIPE_LEFT_ACTION, "dismiss") ?: "dismiss",
            swipeRightAction = getString(SettingKey.SWIPE_RIGHT_ACTION, "next_track") ?: "next_track",

            // Call & Communication
            callScreenTranscript = getBoolean(SettingKey.CALL_SCREEN_TRANSCRIPT, true),
            silentRingModeSwitch = getBoolean(SettingKey.SILENT_RING_MODE_SWITCH, true),

            // Weather & Environment
            weatherMoodRing = getBoolean(SettingKey.WEATHER_MOOD_RING, true),
            atAGlance = getBoolean(SettingKey.AT_A_GLANCE, true),

            // Split & Multi-Event
            splitPillEnabled = getBoolean(SettingKey.SPLIT_PILL_ENABLED, true),
            mergeSimultaneousEvents = getBoolean(SettingKey.MERGE_SIMULTANEOUS_EVENTS, true),
            multiIslandStack = getBoolean(SettingKey.MULTI_ISLAND_STACK, false),

            // Continuity Camera
            continuityCameraActions = getBoolean(SettingKey.CONTINUITY_CAMERA_ACTIONS, true),

            // Focus & DND
            focusModePill = getBoolean(SettingKey.FOCUS_MODE_PILL, true),

            // App Roles
            roleCallingApp = getString(SettingKey.ROLE_CALLING_APP, "") ?: "",
            roleGameLauncher = getString(SettingKey.ROLE_GAME_LAUNCHER, "") ?: "",
            allowedMusicApps = getStringSet(SettingKey.ALLOWED_MUSIC_APPS, emptySet()),
            allowedMediaApps = getStringSet(SettingKey.ALLOWED_MEDIA_APPS, emptySet()),
            allowedNotesApps = getStringSet(SettingKey.ALLOWED_NOTES_APPS, emptySet()),

            // Styles
            callStyle = CallStyle.valueOf(getString(SettingKey.CALL_STYLE, "IOS") ?: "IOS"),
            chargingStyle = ChargingStyle.valueOf(getString(SettingKey.CHARGING_STYLE, "RING") ?: "RING"),
            batteryStyle = BatteryStyle.valueOf(getString(SettingKey.BATTERY_STYLE, "PILL") ?: "PILL")
        )
    }
}
