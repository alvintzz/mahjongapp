package com.alvintz.mahjongapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.alvintz.mahjongapp.data.dao.GameDao
import com.alvintz.mahjongapp.data.entity.GameEntity
import com.alvintz.mahjongapp.data.entity.PlayerEntity
import com.alvintz.mahjongapp.data.entity.RoundLogEntity

@Database(
    entities = [GameEntity::class, PlayerEntity::class, RoundLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mahjong.db")
                .build()
                .also { instance = it }
        }
    }
}
