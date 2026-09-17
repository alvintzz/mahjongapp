package com.alvintz.mahjong.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaneseScoringEngineTest {

    @Test
    fun `pinfu tsumo scores 20fu 2han - 400 700`() {
        // 123m 456p 678s 345s + 22p pair; not 123+456+789 in one suit, so no accidental Ittsu.
        val hand = listOf(
            Tile.man(1), Tile.man(2), Tile.man(3),
            Tile.pin(2), Tile.pin(2),
            Tile.pin(4), Tile.pin(5), Tile.pin(6),
            Tile.sou(3), Tile.sou(4), Tile.sou(5),
            Tile.sou(6), Tile.sou(7), Tile.sou(8)
        )
        val input = JapaneseWinInput(
            handTiles = hand,
            winningTile = Tile.sou(3),
            winType = WinType.TSUMO,
            isClosed = true,
            seatWind = Wind.SOUTH,
            roundWind = Wind.EAST
        )
        val outcome = JapaneseScoringEngine.score(input)
        assertTrue(outcome is JapaneseScoringOutcome.Success)
        val result = (outcome as JapaneseScoringOutcome.Success).result
        assertEquals(2, result.totalHan)
        assertEquals(20, result.fu)
        assertEquals(700, result.tsumoDealerPayment)
        assertEquals(400, result.tsumoNonDealerPayment)
        assertTrue(result.yaku.any { it.name.startsWith("Pinfu") })
        assertTrue(result.yaku.any { it.name.startsWith("Menzen Tsumo") })
    }

    @Test
    fun `riichi tanyao kanchan ron scores 40fu 2han - 2600`() {
        val hand = listOf(
            Tile.man(2), Tile.man(3), Tile.man(4),
            Tile.man(5), Tile.man(6), Tile.man(7),
            Tile.pin(3), Tile.pin(4), Tile.pin(5),
            Tile.sou(5), Tile.sou(6), Tile.sou(7),
            Tile.sou(2), Tile.sou(2)
        )
        val input = JapaneseWinInput(
            handTiles = hand,
            winningTile = Tile.sou(6),
            winType = WinType.RON,
            isClosed = true,
            seatWind = Wind.SOUTH,
            roundWind = Wind.EAST,
            isRiichi = true
        )
        val outcome = JapaneseScoringEngine.score(input)
        assertTrue(outcome is JapaneseScoringOutcome.Success)
        val result = (outcome as JapaneseScoringOutcome.Success).result
        assertEquals(2, result.totalHan)
        assertEquals(40, result.fu)
        assertEquals(2600, result.ronPayment)
    }

    @Test
    fun `kokushi yakuman ron pays 32000 from non-dealer, 48000 from dealer`() {
        val hand = listOf(
            Tile.man(1), Tile.man(9), Tile.pin(1), Tile.pin(9), Tile.sou(1), Tile.sou(9),
            Tile.honor(1), Tile.honor(2), Tile.honor(3), Tile.honor(4),
            Tile.honor(5), Tile.honor(6), Tile.honor(7), Tile.honor(7)
        )
        val nonDealer = JapaneseScoringEngine.score(
            JapaneseWinInput(hand, Tile.honor(7), WinType.RON, true, Wind.SOUTH, Wind.EAST)
        )
        val dealer = JapaneseScoringEngine.score(
            JapaneseWinInput(hand, Tile.honor(7), WinType.RON, true, Wind.EAST, Wind.EAST)
        )
        assertTrue(nonDealer is JapaneseScoringOutcome.Success)
        assertTrue(dealer is JapaneseScoringOutcome.Success)
        assertEquals(32000, (nonDealer as JapaneseScoringOutcome.Success).result.ronPayment)
        assertEquals(48000, (dealer as JapaneseScoringOutcome.Success).result.ronPayment)
        assertEquals(LimitTier.YAKUMAN, nonDealer.result.limitTier)
    }

    @Test
    fun `hand with no yaku cannot win`() {
        // Open hand (a call was made), all sequences, no tanyao (has a West triplet - not seat/round wind
        // for either player below), no riichi possible since open: nothing to claim a yaku with.
        val noYakuHand = listOf(
            Tile.man(2), Tile.man(3), Tile.man(4),
            Tile.man(6), Tile.man(7), Tile.man(8),
            Tile.pin(3), Tile.pin(4), Tile.pin(5),
            Tile.sou(4), Tile.sou(5), Tile.sou(6),
            Tile.honor(3), Tile.honor(3)
        )
        val outcome = JapaneseScoringEngine.score(
            JapaneseWinInput(noYakuHand, Tile.sou(6), WinType.RON, isClosed = false, seatWind = Wind.SOUTH, roundWind = Wind.EAST)
        )
        assertTrue(outcome is JapaneseScoringOutcome.NoYaku)
    }

    @Test
    fun `dora increases han count`() {
        val hand = listOf(
            Tile.man(1), Tile.man(2), Tile.man(3),
            Tile.pin(2), Tile.pin(2),
            Tile.pin(4), Tile.pin(5), Tile.pin(6),
            Tile.sou(3), Tile.sou(4), Tile.sou(5),
            Tile.sou(6), Tile.sou(7), Tile.sou(8)
        )
        val input = JapaneseWinInput(
            handTiles = hand,
            winningTile = Tile.sou(3),
            winType = WinType.TSUMO,
            isClosed = true,
            seatWind = Wind.SOUTH,
            roundWind = Wind.EAST,
            doraIndicators = listOf(Tile.pin(1)) // dora is 2p, hand has two of them
        )
        val outcome = JapaneseScoringEngine.score(input)
        assertTrue(outcome is JapaneseScoringOutcome.Success)
        val result = (outcome as JapaneseScoringOutcome.Success).result
        assertEquals(4, result.totalHan) // pinfu + tsumo + 2 dora
        assertTrue(result.yaku.any { it.name == "Dora" && it.han == 2 })
    }
}
