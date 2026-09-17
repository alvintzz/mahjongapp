package com.alvintz.mahjong.scoring

data class HongKongFanEntry(val name: String, val fan: Int)

data class HongKongWinInput(
    /** All 14 tiles of the completed hand (concealed tiles + winning tile; called melds included). */
    val handTiles: List<Tile>,
    val winningTile: Tile,
    val winType: WinType,
    /** False if the hand contains any called (pon/chi/open-kan) meld. */
    val isClosed: Boolean,
    val seatWind: Wind,
    val roundWind: Wind,
    /** Minimum fan required to declare a win. Common "old style" Hong Kong games require 3. */
    val minFanToWin: Int = 3,
    /** Points doubled per fan: score = baseUnit * 2^(fan-1), capped at [limitFan]. */
    val baseUnit: Int = 2,
    val limitFan: Int = 10
)

data class HongKongWinResult(
    val fanBreakdown: List<HongKongFanEntry>,
    val totalFan: Int,
    val isLimitHand: Boolean,
    /** Points the winner receives from each paying opponent (discarder only on ron, all three on self-draw). */
    val pointsPerPayer: Int
) {
    fun totalGain(winType: WinType): Int = if (winType == WinType.TSUMO) pointsPerPayer * 3 else pointsPerPayer
}

sealed interface HongKongScoringOutcome {
    data class Success(val result: HongKongWinResult) : HongKongScoringOutcome
    data class BelowMinimumFan(val actualFan: Int, val required: Int) : HongKongScoringOutcome
    data object InvalidHand : HongKongScoringOutcome
}

private val limitHandNames = setOf(
    "Thirteen Orphans (Kokushi)", "Big Three Dragons (Daisangen)", "All Honors (Tsuuiisou)",
    "All Terminals (Chinroutou)", "Four Concealed Triplets (Suuankou)", "Big Four Winds (Daisuushii)",
    "Small Four Winds (Shousuushii)", "Nine Gates (Chuurenpoutou)", "All Green (Ryuuiisou)"
)

object HongKongScoringEngine {

    fun score(input: HongKongWinInput): HongKongScoringOutcome {
        require(input.handTiles.size == 14) { "Hand must have 14 tiles" }

        val candidates = mutableListOf<List<HongKongFanEntry>>()

        if (HandDecomposer.isKokushi(input.handTiles)) {
            candidates += listOf(HongKongFanEntry("Thirteen Orphans (Kokushi)", input.limitFan))
        }

        if (HandDecomposer.isChiitoitsu(input.handTiles)) {
            val fan = mutableListOf(HongKongFanEntry("Seven Pairs", 4))
            fan += contextFan(input)
            candidates += fan
        }

        for (decomposition in HandDecomposer.decomposeStandard(input.handTiles)) {
            candidates += analyzeStandard(decomposition, input)
        }

        if (candidates.isEmpty()) return HongKongScoringOutcome.InvalidHand

        val scored = candidates.map { entries ->
            val isLimit = entries.any { it.name in limitHandNames }
            val total = if (isLimit) input.limitFan else entries.sumOf { it.fan }.coerceAtMost(input.limitFan)
            Triple(entries, total, isLimit)
        }
        val best = scored.maxByOrNull { it.second } ?: return HongKongScoringOutcome.InvalidHand

        if (best.second < input.minFanToWin) {
            return HongKongScoringOutcome.BelowMinimumFan(best.second, input.minFanToWin)
        }

        val pointsPerPayer = input.baseUnit * (1 shl (best.second - 1))
        return HongKongScoringOutcome.Success(
            HongKongWinResult(
                fanBreakdown = best.first,
                totalFan = best.second,
                isLimitHand = best.third,
                pointsPerPayer = pointsPerPayer
            )
        )
    }

    private fun analyzeStandard(decomposition: HandDecomposition, input: HongKongWinInput): List<HongKongFanEntry> {
        val melds = decomposition.melds
        val pair = decomposition.pair
        val allTiles = melds.flatMap { it.tiles } + pair
        val allSequences = melds.all { it.isSequence }
        val allTriplets = melds.all { it.isTriplet }
        val concealedTriplets = melds.count { meld ->
            meld.isTriplet && input.isClosed && !(meld.representative.suit == input.winningTile.suit &&
                meld.representative.rank == input.winningTile.rank && input.winType == WinType.RON)
        }

        val fan = mutableListOf<HongKongFanEntry>()

        val dragonTriplets = melds.count { it.isTriplet && it.representative.isDragon }
        val windTriplets = melds.count { it.isTriplet && it.representative.isWind }

        if (dragonTriplets == 3) return listOf(HongKongFanEntry("Big Three Dragons (Daisangen)", input.limitFan))
        if (allTiles.all { it.isHonor }) return listOf(HongKongFanEntry("All Honors (Tsuuiisou)", input.limitFan))
        if (allTiles.all { it.isTerminal } && allTriplets) return listOf(HongKongFanEntry("All Terminals (Chinroutou)", input.limitFan))
        val greenTiles = setOf(2, 3, 4, 6, 8)
        if (allTiles.all { (it.suit == Suit.SOU && it.rank in greenTiles) || (it.suit == Suit.HONOR && it.rank == 6) }) {
            return listOf(HongKongFanEntry("All Green (Ryuuiisou)", input.limitFan))
        }
        if (concealedTriplets == 4) return listOf(HongKongFanEntry("Four Concealed Triplets (Suuankou)", input.limitFan))
        if (windTriplets == 4) return listOf(HongKongFanEntry("Big Four Winds (Daisuushii)", input.limitFan))
        if (windTriplets == 3 && pair.isWind) return listOf(HongKongFanEntry("Small Four Winds (Shousuushii)", input.limitFan))
        if (input.isClosed && isChuurenPoutou(input.handTiles)) return listOf(HongKongFanEntry("Nine Gates (Chuurenpoutou)", input.limitFan))

        if (allSequences) fan += HongKongFanEntry("All Sequences (Ping Wu)", 1)
        if (allTiles.none { it.isTerminalOrHonor }) fan += HongKongFanEntry("All Simples (Tanyao)", 1)
        for (meld in melds.filter { it.isTriplet }) {
            val t = meld.representative
            when {
                t.isDragon -> fan += HongKongFanEntry("Dragon Triplet", 1)
                t.isWind && t.rank == input.seatWind.ordinal + 1 -> fan += HongKongFanEntry("Seat Wind Triplet", 1)
                else -> {}
            }
            if (t.isWind && t.rank == input.roundWind.ordinal + 1) fan += HongKongFanEntry("Round Wind Triplet", 1)
        }
        if (allTriplets) fan += HongKongFanEntry("All Triplets (Toitoi)", 3)
        if (concealedTriplets == 3) fan += HongKongFanEntry("Three Concealed Triplets", 5)
        if (dragonTriplets == 2 && pair.isDragon) fan += HongKongFanEntry("Small Three Dragons", 3)
        if (windTriplets == 2 && pair.isWind) fan += HongKongFanEntry("Small Three Winds", 3)

        val suitsUsed = allTiles.filterNot { it.isHonor }.map { it.suit }.toSet()
        val hasHonor = allTiles.any { it.isHonor }
        if (suitsUsed.size == 1) {
            fan += if (hasHonor) HongKongFanEntry("Half Flush (Honitsu)", 3) else HongKongFanEntry("Full Flush (Chinitsu)", 7)
        }

        val allGroupsHaveTerminalOrHonor = melds.all { m -> m.tiles.any { it.isTerminalOrHonor } } && pair.isTerminalOrHonor
        if (allGroupsHaveTerminalOrHonor) fan += HongKongFanEntry("Terminal/Honor in Every Set", 3)

        val seqRanksBySuit = melds.filter { it.isSequence }
            .groupBy { it.representative.suit }
            .mapValues { e -> e.value.map { it.representative.rank }.toSet() }
        val m = seqRanksBySuit[Suit.MAN]; val p = seqRanksBySuit[Suit.PIN]; val s = seqRanksBySuit[Suit.SOU]
        if (m != null && p != null && s != null && m.intersect(p).intersect(s).isNotEmpty()) {
            fan += HongKongFanEntry("Mixed Triple Chow", 2)
        }

        fan += contextFan(input)

        if (fan.isEmpty()) fan += HongKongFanEntry("Chicken Hand", 1)

        return fan
    }

    private fun contextFan(input: HongKongWinInput): List<HongKongFanEntry> {
        val list = mutableListOf<HongKongFanEntry>()
        if (input.isClosed) list += HongKongFanEntry("Concealed Hand", 1)
        if (input.winType == WinType.TSUMO) list += HongKongFanEntry("Self-Drawn", 1)
        return list
    }

    private fun isChuurenPoutou(tiles: List<Tile>): Boolean {
        val suits = tiles.map { it.suit }.toSet()
        if (suits.size != 1 || suits.first() == Suit.HONOR) return false
        val counts = IntArray(9)
        for (t in tiles) counts[t.rank - 1]++
        val required = intArrayOf(3, 1, 1, 1, 1, 1, 1, 1, 3)
        for (i in 0..8) if (counts[i] < required[i]) return false
        return true
    }
}
