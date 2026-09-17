package com.alvintz.mahjong.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandDecomposerTest {

    private fun tiles(spec: String): List<Tile> {
        // e.g. "234m 456p 789s 22p 678s" -> list of tiles
        val result = mutableListOf<Tile>()
        for (group in spec.split(" ")) {
            val suitChar = group.last()
            val ranks = group.dropLast(1)
            val suit = when (suitChar) {
                'm' -> Suit.MAN
                'p' -> Suit.PIN
                's' -> Suit.SOU
                'z' -> Suit.HONOR
                else -> error("bad suit $suitChar")
            }
            for (c in ranks) result += Tile(suit, c.toString().toInt())
        }
        return result
    }

    @Test
    fun `simple hand decomposes into exactly one shape`() {
        val hand = tiles("123m 456m 789m 22p 345s")
        val results = HandDecomposer.decomposeStandard(hand)
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.pair.suit == Suit.PIN && it.pair.rank == 2 })
    }

    @Test
    fun `ambiguous hand yields multiple decompositions`() {
        // 111222333m can be read as three triplets, or as three identical 123m sequences (iipeiko).
        val hand = tiles("111222333m 444p 55s")
        val results = HandDecomposer.decomposeStandard(hand)
        assertTrue(results.size >= 2)
        assertTrue(results.any { d -> d.melds.count { it.isTriplet } == 4 })
        assertTrue(results.any { d -> d.melds.count { it.isSequence } == 3 })
    }

    @Test
    fun `seven pairs detected`() {
        val hand = listOf(1, 2, 3, 4, 5, 6, 7).flatMap { r -> listOf(Tile.man(r), Tile.man(r)) }
        assertTrue(HandDecomposer.isChiitoitsu(hand))
    }

    @Test
    fun `four of a kind is not seven pairs`() {
        val hand = listOf(1, 2, 3, 4, 5).flatMap { r -> listOf(Tile.man(r), Tile.man(r)) } +
            listOf(Tile.man(6), Tile.man(6), Tile.man(6), Tile.man(6))
        assertEquals(14, hand.size)
        assertTrue(!HandDecomposer.isChiitoitsu(hand))
    }

    @Test
    fun `kokushi detected`() {
        val hand = listOf(
            Tile.man(1), Tile.man(9), Tile.pin(1), Tile.pin(9), Tile.sou(1), Tile.sou(9),
            Tile.honor(1), Tile.honor(2), Tile.honor(3), Tile.honor(4),
            Tile.honor(5), Tile.honor(6), Tile.honor(7), Tile.honor(7)
        )
        assertTrue(HandDecomposer.isKokushi(hand))
    }
}
