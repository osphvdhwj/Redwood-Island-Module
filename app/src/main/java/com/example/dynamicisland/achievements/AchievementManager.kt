package com.dynamicisland.achievements

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Achievement(
    val displayName: String,
    val description: String,
    val icon: Int = 0 // resource id for optional icon
) {
    HOURS_100_MUSIC("Marathon Listener", "Listened to music for 100 hours"),
    FIRST_QR("Scanner", "Scanned your first QR code"),
    POWER_USER("Power User", "Used all island features at least once"),
    NIGHT_OWL("Night Owl", "Used the island between midnight and 4 AM"),
    GAMER("Gamer", "Enabled gaming HUD for the first time"),
    ACHIEVEMENT_HUNTER("Hunter", "Unlocked 5 achievements"),
    STREAK_7("Week Warrior", "7-day interaction streak"),
    STREAK_30("Monthly Master", "30-day interaction streak"),
    BETA_TESTER("Beta Tester", "Used experimental features")
}

class AchievementManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("achievements", Context.MODE_PRIVATE)

    private val _unlocked = MutableStateFlow<Set<Achievement>>(emptySet())
    val unlocked: StateFlow<Set<Achievement>> = _unlocked.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    init {
        val set = mutableSetOf<Achievement>()
        for (achievement in Achievement.entries) {
            if (prefs.getBoolean(achievement.name, false)) {
                set.add(achievement)
            }
        }
        _unlocked.value = set
        _streakDays.value = prefs.getInt("streak_days", 0)
    }

    fun isUnlocked(achievement: Achievement): Boolean =
        achievement in _unlocked.value

    fun unlock(achievement: Achievement) {
        if (!isUnlocked(achievement)) {
            prefs.edit().putBoolean(achievement.name, true).apply()
            _unlocked.value = _unlocked.value + achievement
            // Additional logic (e.g., trigger celebration)
        }
    }

    fun incrementStreak() {
        _streakDays.value++
        prefs.edit().putInt("streak_days", _streakDays.value).apply()
        when (_streakDays.value) {
            7 -> unlock(Achievement.STREAK_7)
            30 -> unlock(Achievement.STREAK_30)
        }
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        _unlocked.value = emptySet()
        _streakDays.value = 0
    }
}