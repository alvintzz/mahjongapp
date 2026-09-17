package com.alvintz.mahjongapp.data

import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjong.scoring.Wind
import com.alvintz.mahjongapp.data.entity.GameEntity
import com.alvintz.mahjongapp.data.entity.PlayerEntity
import com.alvintz.mahjongapp.data.entity.RoundLogEntity
import com.alvintz.mahjongapp.model.GameState
import com.alvintz.mahjongapp.model.PlayerState
import com.alvintz.mahjongapp.model.decodeTiles
import com.alvintz.mahjongapp.model.encodeTiles
import kotlinx.coroutines.flow.Flow

class GameRepository(private val db: AppDatabase) {

    private val dao get() = db.gameDao()

    suspend fun loadCurrentGame(): GameState? {
        val withPlayers = dao.getCurrentGameWithPlayers() ?: return null
        return withPlayers.toGameState()
    }

    suspend fun startGame(
        ruleSet: RuleSet,
        playerNames: List<String>,
        startingScore: Int,
        hkMinFanToWin: Int = 3,
        hkBaseUnit: Int = 2
    ): GameState {
        val entity = GameEntity(
            ruleSet = ruleSet.name,
            startTime = System.currentTimeMillis(),
            startingScore = startingScore,
            hkMinFanToWin = hkMinFanToWin,
            hkBaseUnit = hkBaseUnit
        )
        val gameId = dao.insertGame(entity)
        val players = playerNames.mapIndexed { index, name ->
            PlayerEntity(gameId = gameId, seatIndex = index, name = name, score = startingScore)
        }
        dao.insertPlayers(players)
        val saved = dao.getGame(gameId)!!
        val savedPlayers = dao.getPlayers(gameId)
        return GameWithPlayersMapper.toGameState(saved, savedPlayers)
    }

    suspend fun saveGameState(state: GameState) {
        dao.updateGame(state.toEntity())
        dao.updatePlayers(state.players.map { it.toEntity(state.gameId) })
    }

    suspend fun appendLog(gameId: Long, sequence: Int, description: String) {
        dao.insertLog(RoundLogEntity(gameId = gameId, sequence = sequence, description = description, timestamp = System.currentTimeMillis()))
    }

    suspend fun getLogs(gameId: Long) = dao.getLogs(gameId)

    suspend fun finishGame(state: GameState) {
        val entity = state.toEntity().copy(isFinished = true, endTime = System.currentTimeMillis())
        dao.updateGame(entity)
        dao.updatePlayers(state.players.map { it.toEntity(state.gameId) })
    }

    fun observeFinishedGames(): Flow<List<GameEntity>> = dao.observeFinishedGames()

    suspend fun getPlayersFor(gameId: Long): List<PlayerEntity> = dao.getPlayers(gameId)
}

private object GameWithPlayersMapper {
    fun toGameState(game: GameEntity, players: List<PlayerEntity>): GameState = GameState(
        gameId = game.gameId,
        ruleSet = RuleSet.valueOf(game.ruleSet),
        startTime = game.startTime,
        players = players.sortedBy { it.seatIndex }.map { it.toModel() },
        roundWind = Wind.entries[game.roundWindOrdinal],
        handNumber = game.handNumber,
        dealerSeatIndex = game.dealerSeatIndex,
        honba = game.honba,
        riichiSticks = game.riichiSticks,
        doraIndicators = game.doraIndicators.decodeTiles(),
        riichiDeclaredSeats = game.riichiDeclaredSeats.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
        startingScore = game.startingScore,
        hkMinFanToWin = game.hkMinFanToWin,
        hkBaseUnit = game.hkBaseUnit
    )
}

private fun com.alvintz.mahjongapp.data.dao.GameWithPlayers.toGameState() =
    GameWithPlayersMapper.toGameState(game, players)

private fun PlayerEntity.toModel() = PlayerState(playerId = playerId, seatIndex = seatIndex, name = name, score = score)

private fun PlayerState.toEntity(gameId: Long) = PlayerEntity(playerId = playerId, gameId = gameId, seatIndex = seatIndex, name = name, score = score)

private fun GameState.toEntity() = GameEntity(
    gameId = gameId,
    ruleSet = ruleSet.name,
    startTime = startTime,
    isFinished = false,
    roundWindOrdinal = roundWind.ordinal,
    handNumber = handNumber,
    dealerSeatIndex = dealerSeatIndex,
    honba = honba,
    riichiSticks = riichiSticks,
    doraIndicators = doraIndicators.encodeTiles(),
    riichiDeclaredSeats = riichiDeclaredSeats.joinToString(","),
    startingScore = startingScore,
    hkMinFanToWin = hkMinFanToWin,
    hkBaseUnit = hkBaseUnit
)
