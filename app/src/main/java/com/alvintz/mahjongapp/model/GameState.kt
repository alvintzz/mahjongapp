package com.alvintz.mahjongapp.model

import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjong.scoring.Suit
import com.alvintz.mahjong.scoring.Tile
import com.alvintz.mahjong.scoring.Wind

data class PlayerState(
    val playerId: Long,
    val seatIndex: Int,
    val name: String,
    val score: Int
)

data class GameState(
    val gameId: Long,
    val ruleSet: RuleSet,
    val startTime: Long,
    val players: List<PlayerState>, // ordered by seatIndex 0..3; seat 0 started as East
    val roundWind: Wind,
    val handNumber: Int,
    val dealerSeatIndex: Int,
    val honba: Int,
    val riichiSticks: Int,
    val doraIndicators: List<Tile>,
    val riichiDeclaredSeats: Set<Int>,
    val startingScore: Int,
    val hkMinFanToWin: Int,
    val hkBaseUnit: Int
) {
    /** Current wind of the player sitting in [seatIndex], given how far the dealership has rotated. */
    fun windOf(seatIndex: Int): Wind = Wind.entries[(seatIndex - dealerSeatIndex + 4) % 4]

    val dealer: PlayerState get() = players[dealerSeatIndex]

    val roundLabel: String get() {
        val windName = roundWind.name.lowercase().replaceFirstChar { it.uppercase() }
        val base = "$windName $handNumber"
        return if (honba > 0) "$base-$honba" else base
    }
}

/** Encodes dora indicators as "SUIT:rank" tokens, e.g. "PIN:1,HONOR:3". */
fun List<Tile>.encodeTiles(): String = joinToString(",") { "${it.suit}:${it.rank}" }

fun String.decodeTiles(): List<Tile> {
    if (isBlank()) return emptyList()
    return split(",").map { token ->
        val (suit, rank) = token.split(":")
        Tile(Suit.valueOf(suit), rank.toInt())
    }
}
