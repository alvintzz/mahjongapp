package com.alvintz.mahjongapp

import android.app.Application
import com.alvintz.mahjongapp.data.AppDatabase
import com.alvintz.mahjongapp.data.GameRepository

class MahjongApplication : Application() {
    val repository: GameRepository by lazy { GameRepository(AppDatabase.get(this)) }
}
