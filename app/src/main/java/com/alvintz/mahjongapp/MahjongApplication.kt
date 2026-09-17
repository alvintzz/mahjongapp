package com.alvintz.mahjongapp

import android.app.Application
import com.alvintz.mahjongapp.data.AppDatabase
import com.alvintz.mahjongapp.data.GameRepository
import com.alvintz.mahjongapp.data.SettingsRepository

class MahjongApplication : Application() {
    val repository: GameRepository by lazy { GameRepository(AppDatabase.get(this)) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
