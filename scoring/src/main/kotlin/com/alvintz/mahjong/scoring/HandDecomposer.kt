package com.alvintz.mahjong.scoring

enum class MeldType { SEQUENCE, TRIPLET }

/** A completed group of 3 tiles. Kans are not modeled; a called quad is treated as a triplet (see README). */
data class Meld(val type: MeldType, val tiles: List<Tile>) {
    val isTriplet get() = type == MeldType.TRIPLET
    val isSequence get() = type == MeldType.SEQUENCE
    val representative: Tile get() = tiles.first()
    val containsRedFive: Boolean get() = tiles.any { it.isRedFive }
}

/** One valid way to break a 14-tile winning hand into 4 melds + a pair, or a special shape. */
data class HandDecomposition(
    val melds: List<Meld>,
    val pair: Tile,
    val isChiitoitsu: Boolean = false,
    val isKokushi: Boolean = false
)

object HandDecomposer {

    /** Returns every valid standard (4 melds + pair) decomposition of a 14-tile hand. Empty if none exist. */
    fun decomposeStandard(tiles: List<Tile>): List<HandDecomposition> {
        require(tiles.size == 14) { "A winning hand must have 14 tiles, got ${tiles.size}" }

        val bySuit = mapOf(
            Suit.MAN to IntArray(9),
            Suit.PIN to IntArray(9),
            Suit.SOU to IntArray(9),
            Suit.HONOR to IntArray(7)
        )
        // Track one representative red-five tile per suit/rank so melds carry the red flag through.
        val redFive = mutableMapOf<Pair<Suit, Int>, Boolean>()
        for (t in tiles) {
            bySuit.getValue(t.suit)[t.rank - 1]++
            if (t.isRedFive) redFive[t.suit to t.rank] = true
        }

        fun tileOf(suit: Suit, rank: Int) = Tile(suit, rank, redFive[suit to rank] == true)

        val pairCandidates = bySuit.entries
            .flatMap { (suit, counts) -> counts.indices.filter { counts[it] >= 2 }.map { suit to (it + 1) } }
            .distinct()

        val results = mutableListOf<HandDecomposition>()
        for ((pairSuit, pairRank) in pairCandidates) {
            val working = bySuit.mapValues { it.value.copyOf() }
            working.getValue(pairSuit)[pairRank - 1] -= 2

            val perSuitOptions = Suit.entries.map { suit -> decomposeSuit(working.getValue(suit), suit, ::tileOf) }
            if (perSuitOptions.any { it.isEmpty() }) continue

            for (combo in cartesianProduct(perSuitOptions)) {
                val melds = combo.flatten()
                if (melds.size == 4) {
                    results.add(HandDecomposition(melds, tileOf(pairSuit, pairRank)))
                }
            }
        }
        return results.distinctBy { d -> d.melds.map { it.type to it.tiles.map { t -> t.suit to t.rank } }.sortedBy { it.toString() } to d.pair }
    }

    private fun decomposeSuit(
        counts: IntArray,
        suit: Suit,
        tileOf: (Suit, Int) -> Tile
    ): List<List<Meld>> {
        val i = counts.indexOfFirst { it > 0 }
        if (i == -1) return listOf(emptyList())

        val results = mutableListOf<List<Meld>>()

        if (counts[i] >= 3) {
            counts[i] -= 3
            for (sub in decomposeSuit(counts, suit, tileOf)) {
                val triplet = Meld(MeldType.TRIPLET, List(3) { tileOf(suit, i + 1) })
                results.add(listOf(triplet) + sub)
            }
            counts[i] += 3
        }

        if (suit != Suit.HONOR && i <= counts.size - 3 && counts[i + 1] > 0 && counts[i + 2] > 0) {
            counts[i]--; counts[i + 1]--; counts[i + 2]--
            for (sub in decomposeSuit(counts, suit, tileOf)) {
                val sequence = Meld(MeldType.SEQUENCE, listOf(tileOf(suit, i + 1), tileOf(suit, i + 2), tileOf(suit, i + 3)))
                results.add(listOf(sequence) + sub)
            }
            counts[i]++; counts[i + 1]++; counts[i + 2]++
        }

        return results
    }

    private fun cartesianProduct(lists: List<List<List<Meld>>>): List<List<List<Meld>>> {
        var acc: List<List<List<Meld>>> = listOf(emptyList())
        for (list in lists) {
            acc = acc.flatMap { prefix -> list.map { prefix + listOf(it) } }
        }
        return acc
    }

    /** Seven distinct pairs. Most rulesets forbid using the same tile as two of the pairs. */
    fun isChiitoitsu(tiles: List<Tile>): Boolean {
        if (tiles.size != 14) return false
        val counts = tiles.groupBy { it.suit to it.rank }.mapValues { it.value.size }
        return counts.size == 7 && counts.values.all { it == 2 }
    }

    private val kokushiKinds: Set<Pair<Suit, Int>> = setOf(
        Suit.MAN to 1, Suit.MAN to 9, Suit.PIN to 1, Suit.PIN to 9, Suit.SOU to 1, Suit.SOU to 9,
        Suit.HONOR to 1, Suit.HONOR to 2, Suit.HONOR to 3, Suit.HONOR to 4,
        Suit.HONOR to 5, Suit.HONOR to 6, Suit.HONOR to 7
    )

    /** Thirteen orphans: one of each terminal/honor, plus a duplicate of one of them. */
    fun isKokushi(tiles: List<Tile>): Boolean {
        if (tiles.size != 14) return false
        if (tiles.any { (it.suit to it.rank) !in kokushiKinds }) return false
        val counts = tiles.groupBy { it.suit to it.rank }
        return counts.size == 13 && counts.values.count { it.size == 2 } == 1
    }
}
