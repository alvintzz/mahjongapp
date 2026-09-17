package com.alvintz.mahjongapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-wide settings (as opposed to per-game state in [GameRepository]), backed by
 * SharedPreferences so they persist across app launches and apply to every new game.
 */
class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hkMinFanToWin = MutableStateFlow(prefs.getInt(KEY_HK_MIN_FAN_TO_WIN, DEFAULT_HK_MIN_FAN_TO_WIN))
    val hkMinFanToWin: StateFlow<Int> = _hkMinFanToWin

    fun setHkMinFanToWin(value: Int) {
        val clamped = value.coerceIn(MIN_HK_MIN_FAN_TO_WIN, MAX_HK_MIN_FAN_TO_WIN)
        prefs.edit().putInt(KEY_HK_MIN_FAN_TO_WIN, clamped).apply()
        _hkMinFanToWin.value = clamped
    }

    companion object {
        const val DEFAULT_HK_MIN_FAN_TO_WIN = 3
        const val MIN_HK_MIN_FAN_TO_WIN = 1
        const val MAX_HK_MIN_FAN_TO_WIN = 5

        private const val PREFS_NAME = "settings"
        private const val KEY_HK_MIN_FAN_TO_WIN = "hk_min_fan_to_win"
    }
}
