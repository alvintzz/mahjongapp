package com.alvintz.mahjong.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HongKongScoringEngineTest {

    private val allSimpleSequenceHand = listOf(
        Tile.man(2), Tile.man(3), Tile.man(4),
        Tile.man(5), Tile.man(6), Tile.man(7),
        Tile.pin(3), Tile.pin(4), Tile.pin(5),
        Tile.sou(6), Tile.sou(7), Tile.sou(8),
        Tile.pin(5), Tile.pin(5)
    )

    @Test
    fun `concealed self-drawn all-simple hand scores 4 fan`() {
        val input = HongKongWinInput(
            handTiles = allSimpleSequenceHand,
            winningTile = Tile.sou(8),
            winType = WinType.TSUMO,
            isClosed = true,
            seatWind = Wind.SOUTH,
            roundWind = Wind.EAST
        )
        val outcome = HongKongScoringEngine.score(input)
        assertTrue(outcome is HongKongScoringOutcome.Success)
        val result = (outcome as HongKongScoringOutcome.Success).result
        assertEquals(4, result.totalFan)
        assertEquals(16, result.pointsPerPayer) // 2 * 2^(4-1)
        assertEquals(48, result.totalGain(WinType.TSUMO))
    }

    @Test
    fun `open low-fan hand is rejected below minimum`() {
        val input = HongKongWinInput(
            handTiles = allSimpleSequenceHand,
            winningTile = Tile.sou(8),
            winType = WinType.RON,
            isClosed = false,
            seatWind = Wind.SOUTH,
            roundWind = Wind.EAST
        )
        val outcome = HongKongScoringEngine.score(input)
        assertTrue(outcome is HongKongScoringOutcome.BelowMinimumFan)
    }

    @Test
    fun `thirteen orphans is a limit hand`() {
        val hand = listOf(
            Tile.man(1), Tile.man(9), Tile.pin(1), Tile.pin(9), Tile.sou(1), Tile.sou(9),
            Tile.honor(1), Tile.honor(2), Tile.honor(3), Tile.honor(4),
            Tile.honor(5), Tile.honor(6), Tile.honor(7), Tile.honor(7)
        )
        val input = HongKongWinInput(hand, Tile.honor(7), WinType.RON, true, Wind.SOUTH, Wind.EAST)
        val outcome = HongKongScoringEngine.score(input)
        assertTrue(outcome is HongKongScoringOutcome.Success)
        val result = (outcome as HongKongScoringOutcome.Success).result
        assertTrue(result.isLimitHand)
        assertEquals(10, result.totalFan)
    }
}
