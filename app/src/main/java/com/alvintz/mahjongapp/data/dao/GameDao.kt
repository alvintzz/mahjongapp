package com.alvintz.mahjongapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.alvintz.mahjongapp.data.entity.GameEntity
import com.alvintz.mahjongapp.data.entity.PlayerEntity
import com.alvintz.mahjongapp.data.entity.RoundLogEntity
import kotlinx.coroutines.flow.Flow

data class GameWithPlayers(
    val game: GameEntity,
    val players: List<PlayerEntity>
)

@Dao
interface GameDao {

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Insert
    suspend fun insertPlayers(players: List<PlayerEntity>): List<Long>

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: RoundLogEntity)

    @Query("SELECT * FROM games WHERE isFinished = 0 ORDER BY gameId DESC LIMIT 1")
    suspend fun getCurrentGame(): GameEntity?

    @Query("SELECT * FROM players WHERE gameId = :gameId ORDER BY seatIndex ASC")
    suspend fun getPlayers(gameId: Long): List<PlayerEntity>

    @Transaction
    suspend fun getCurrentGameWithPlayers(): GameWithPlayers? {
        val game = getCurrentGame() ?: return null
        return GameWithPlayers(game, getPlayers(game.gameId))
    }

    @Query("SELECT * FROM games WHERE isFinished = 1 ORDER BY endTime DESC")
    fun observeFinishedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE gameId = :gameId")
    suspend fun getGame(gameId: Long): GameEntity?

    @Query("SELECT * FROM round_log WHERE gameId = :gameId ORDER BY sequence ASC")
    suspend fun getLogs(gameId: Long): List<RoundLogEntity>

    @Delete
    suspend fun deleteGame(game: GameEntity)
}
