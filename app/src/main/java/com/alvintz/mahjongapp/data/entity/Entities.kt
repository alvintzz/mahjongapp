package com.alvintz.mahjongapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One row per game. While [isFinished] is false this is the single "current game" the app
 * resumes into on relaunch (autosaved after every mutation so an accidental close never loses
 * the in-progress score sheet). Dora indicators / riichi flags are Japanese-only and unused
 * for Hong Kong games.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val gameId: Long = 0,
    val ruleSet: String, // RuleSet.name
    val startTime: Long,
    val endTime: Long? = null,
    val isFinished: Boolean = false,
    val roundWindOrdinal: Int = 0, // Wind.ordinal
    val handNumber: Int = 1,
    val dealerSeatIndex: Int = 0,
    val honba: Int = 0,
    val riichiSticks: Int = 0,
    /** Comma separated "suit:rank" tokens, e.g. "PIN:1,HONOR:3". Japanese only. */
    val doraIndicators: String = "",
    /** Comma separated seat indices (0..3) that have declared riichi this hand. Japanese only. */
    val riichiDeclaredSeats: String = "",
    val startingScore: Int,
    val hkMinFanToWin: Int = 3,
    val hkBaseUnit: Int = 2
)

@Entity(
    tableName = "players",
    foreignKeys = [
        ForeignKey(entity = GameEntity::class, parentColumns = ["gameId"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val playerId: Long = 0,
    val gameId: Long,
    /** Fixed physical seat 0..3, assigned at setup; seat 0 starts as East. */
    val seatIndex: Int,
    val name: String,
    val score: Int
)

/** Human-readable log of scoring events, shown as a running history within a game and in the post-game summary. */
@Entity(
    tableName = "round_log",
    foreignKeys = [
        ForeignKey(entity = GameEntity::class, parentColumns = ["gameId"], childColumns = ["gameId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class RoundLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val gameId: Long,
    val sequence: Int,
    val description: String,
    val timestamp: Long
)
