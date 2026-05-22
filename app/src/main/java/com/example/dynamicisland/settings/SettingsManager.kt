package com.example.dynamicisland.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("island_prefs", Context.MODE_PRIVATE)

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
        ALLOWED_MUSIC_APPS, ALLOWED_NOTIFICATION_APPS,
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
        FOCUS_MODE_PILL
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
            islandEnabled = getBoolean(SettingKey.ISLAND_ENABLED, true),
            bpmPulse = getBoolean(SettingKey.BPM_PULSE, true),
            ambientReactiveRing = getBoolean(SettingKey.AMBIENT_REACTIVE, true),
            proximityWake = getBoolean(SettingKey.PROXIMITY_WAKE, false),
            talkbackIntegration = getBoolean(SettingKey.TALKBACK_INTEGRATION, true),
            islandStreaks = getBoolean(SettingKey.ISLAND_STREAKS, true),
            achievementsEnabled = getBoolean(SettingKey.ACHIEVEMENTS_ENABLED, true),
            iconPack = IconPack.fromString(iconPackName)
        )
    }
}
