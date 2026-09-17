package com.alvintz.mahjongapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvintz.mahjong.scoring.HongKongScoringEngine
import com.alvintz.mahjong.scoring.HongKongScoringOutcome
import com.alvintz.mahjong.scoring.HongKongWinInput
import com.alvintz.mahjong.scoring.JapaneseScoringEngine
import com.alvintz.mahjong.scoring.JapaneseScoringOutcome
import com.alvintz.mahjong.scoring.JapaneseWinInput
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjong.scoring.Tile
import com.alvintz.mahjong.scoring.WinType
import com.alvintz.mahjongapp.data.GameRepository
import com.alvintz.mahjongapp.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Non-null while a game is in progress; the app resumes straight into it on relaunch. */
class GameViewModel(private val repository: GameRepository) : ViewModel() {

    private val _game = MutableStateFlow<GameState?>(null)
    val game: StateFlow<GameState?> = _game.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var logSequence = 0

    init {
        viewModelScope.launch {
            _game.value = repository.loadCurrentGame()
            _loading.value = false
        }
    }

    fun startNewGame(
        ruleSet: RuleSet,
        playerNames: List<String>,
        startingScore: Int,
        hkMinFanToWin: Int = 3,
        hkBaseUnit: Int = 2
    ) {
        viewModelScope.launch {
            _game.value = repository.startGame(ruleSet, playerNames, startingScore, hkMinFanToWin, hkBaseUnit)
            logSequence = 0
        }
    }

    fun setDoraIndicators(indicators: List<Tile>) {
        val current = _game.value ?: return
        updateAndPersist(current.copy(doraIndicators = indicators))
    }

    fun declareRiichi(seatIndex: Int) {
        val current = _game.value ?: return
        if (seatIndex in current.riichiDeclaredSeats) return
        val player = current.players.first { it.seatIndex == seatIndex }
        if (player.score < 1000) return
        val updatedPlayers = current.players.map { if (it.seatIndex == seatIndex) it.copy(score = it.score - 1000) else it }
        updateAndPersist(
            current.copy(
                players = updatedPlayers,
                riichiSticks = current.riichiSticks + 1,
                riichiDeclaredSeats = current.riichiDeclaredSeats + seatIndex
            )
        )
        appendLog(current.gameId, "${player.name} declares Riichi")
    }

    data class JapaneseWinRequest(
        val winnerSeat: Int,
        val loserSeat: Int?, // null for tsumo
        val handTiles: List<Tile>,
        val winningTile: Tile,
        val winType: WinType,
        val isClosed: Boolean,
        val isDoubleRiichi: Boolean,
        val isIppatsu: Boolean,
        val isHaitei: Boolean,
        val isHoutei: Boolean,
        val isRinshan: Boolean,
        val isChankan: Boolean,
        val uraDoraIndicators: List<Tile>
    )

    fun evaluateJapaneseWin(request: JapaneseWinRequest): JapaneseScoringOutcome {
        val g = _game.value ?: return JapaneseScoringOutcome.InvalidHand
        val isRiichi = request.winnerSeat in g.riichiDeclaredSeats || request.isDoubleRiichi
        val input = JapaneseWinInput(
            handTiles = request.handTiles,
            winningTile = request.winningTile,
            winType = request.winType,
            isClosed = request.isClosed,
            seatWind = g.windOf(request.winnerSeat),
            roundWind = g.roundWind,
            isRiichi = isRiichi,
            isDoubleRiichi = request.isDoubleRiichi,
            isIppatsu = request.isIppatsu,
            isHaitei = request.isHaitei,
            isHoutei = request.isHoutei,
            isRinshan = request.isRinshan,
            isChankan = request.isChankan,
            doraIndicators = g.doraIndicators,
            uraDoraIndicators = request.uraDoraIndicators,
            honba = g.honba,
            riichiSticksOnTable = g.riichiSticks
        )
        return JapaneseScoringEngine.score(input)
    }

    fun applyJapaneseWin(request: JapaneseWinRequest, result: com.alvintz.mahjong.scoring.JapaneseWinResult) {
        val g = _game.value ?: return
        val winner = g.players.first { it.seatIndex == request.winnerSeat }
        val isDealerWinner = request.winnerSeat == g.dealerSeatIndex

        val deltas = IntArray(4)
        when (request.winType) {
            WinType.RON -> {
                val loserSeat = requireNotNull(request.loserSeat) { "Ron requires a discarder" }
                val amount = (result.ronPayment ?: 0) + result.honbaTotal
                deltas[request.winnerSeat] += amount + result.riichiStickTotal
                deltas[loserSeat] -= amount
            }
            WinType.TSUMO -> {
                for (p in g.players) {
                    if (p.seatIndex == request.winnerSeat) continue
                    val isPayerDealer = p.seatIndex == g.dealerSeatIndex
                    val base = if (isPayerDealer) (result.tsumoDealerPayment ?: 0) else (result.tsumoNonDealerPayment ?: 0)
                    val honbaShare = g.honba * 100
                    deltas[p.seatIndex] -= (base + honbaShare)
                    deltas[request.winnerSeat] += (base + honbaShare)
                }
                deltas[request.winnerSeat] += result.riichiStickTotal
            }
        }

        val updatedPlayers = g.players.map { it.copy(score = it.score + deltas[it.seatIndex]) }
        val rotated = rotateAfterWin(g.copy(players = updatedPlayers, riichiSticks = 0, riichiDeclaredSeats = emptySet()), isDealerWinner)
        updateAndPersist(rotated)
        appendLog(g.gameId, "${winner.name} wins (${result.totalHan}han/${result.fu}fu, ${result.limitTier}) — ${result.yaku.joinToString { it.name }}")
    }

    data class HongKongWinRequest(
        val winnerSeat: Int,
        val loserSeat: Int?,
        val handTiles: List<Tile>,
        val winningTile: Tile,
        val winType: WinType,
        val isClosed: Boolean
    )

    fun evaluateHongKongWin(request: HongKongWinRequest): HongKongScoringOutcome {
        val g = _game.value ?: return HongKongScoringOutcome.InvalidHand
        val input = HongKongWinInput(
            handTiles = request.handTiles,
            winningTile = request.winningTile,
            winType = request.winType,
            isClosed = request.isClosed,
            seatWind = g.windOf(request.winnerSeat),
            roundWind = g.roundWind,
            minFanToWin = g.hkMinFanToWin,
            baseUnit = g.hkBaseUnit
        )
        return HongKongScoringEngine.score(input)
    }

    fun applyHongKongWin(request: HongKongWinRequest, result: com.alvintz.mahjong.scoring.HongKongWinResult) {
        val g = _game.value ?: return
        val winner = g.players.first { it.seatIndex == request.winnerSeat }
        val isDealerWinner = request.winnerSeat == g.dealerSeatIndex

        val deltas = IntArray(4)
        when (request.winType) {
            WinType.RON -> {
                val loserSeat = requireNotNull(request.loserSeat) { "Discard win requires a discarder" }
                deltas[request.winnerSeat] += result.pointsPerPayer
                deltas[loserSeat] -= result.pointsPerPayer
            }
            WinType.TSUMO -> {
                for (p in g.players) {
                    if (p.seatIndex == request.winnerSeat) continue
                    deltas[p.seatIndex] -= result.pointsPerPayer
                    deltas[request.winnerSeat] += result.pointsPerPayer
                }
            }
        }

        val updatedPlayers = g.players.map { it.copy(score = it.score + deltas[it.seatIndex]) }
        val rotated = rotateAfterWin(g.copy(players = updatedPlayers, riichiDeclaredSeats = emptySet()), isDealerWinner)
        updateAndPersist(rotated)
        appendLog(g.gameId, "${winner.name} wins (${result.totalFan} fan) — ${result.fanBreakdown.joinToString { it.name }}")
    }

    fun recordDraw(tenpaiSeats: Set<Int>) {
        val g = _game.value ?: return
        val dealerTenpai = g.dealerSeatIndex in tenpaiSeats

        var players = g.players
        if (g.ruleSet == RuleSet.JAPANESE && tenpaiSeats.isNotEmpty() && tenpaiSeats.size < 4) {
            val pot = 3000
            val gain = pot / tenpaiSeats.size
            val loss = pot / (4 - tenpaiSeats.size)
            players = g.players.map {
                if (it.seatIndex in tenpaiSeats) it.copy(score = it.score + gain) else it.copy(score = it.score - loss)
            }
        }

        val rotated = rotateAfterDraw(g.copy(players = players, riichiDeclaredSeats = emptySet()), dealerTenpai)
        updateAndPersist(rotated)
        appendLog(g.gameId, "Exhaustive draw (tenpai: ${tenpaiSeats.joinToString { g.players[it].name }})")
    }

    fun endGame() {
        val g = _game.value ?: return
        viewModelScope.launch {
            repository.finishGame(g)
            _game.value = null
        }
    }

    private fun rotateAfterWin(state: GameState, dealerWon: Boolean): GameState = if (dealerWon) {
        state.copy(honba = state.honba + 1)
    } else {
        advanceDealer(state)
    }

    private fun rotateAfterDraw(state: GameState, dealerTenpai: Boolean): GameState = if (dealerTenpai) {
        state.copy(honba = state.honba + 1)
    } else {
        advanceDealer(state)
    }

    private fun advanceDealer(state: GameState): GameState {
        val nextDealer = (state.dealerSeatIndex + 1) % 4
        val wrapped = nextDealer == 0
        return state.copy(
            dealerSeatIndex = nextDealer,
            honba = 0,
            roundWind = if (wrapped) state.roundWind.next() else state.roundWind,
            handNumber = if (wrapped) 1 else state.handNumber + 1
        )
    }

    private fun updateAndPersist(state: GameState) {
        _game.value = state
        viewModelScope.launch { repository.saveGameState(state) }
    }

    private fun appendLog(gameId: Long, description: String) {
        val seq = logSequence++
        viewModelScope.launch { repository.appendLog(gameId, seq, description) }
    }
}
