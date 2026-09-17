package com.alvintz.mahjong.scoring

import kotlin.math.ceil

enum class LimitTier { NONE, MANGAN, HANEMAN, BAIMAN, SANBAIMAN, YAKUMAN }

data class YakuEntry(val name: String, val han: Int)

data class JapaneseWinInput(
    /** All 14 tiles of the completed hand (concealed tiles + winning tile; called melds included). */
    val handTiles: List<Tile>,
    val winningTile: Tile,
    val winType: WinType,
    /** False if the hand contains any called (pon/chi/open-kan) meld. */
    val isClosed: Boolean,
    val seatWind: Wind,
    val roundWind: Wind,
    val isRiichi: Boolean = false,
    val isDoubleRiichi: Boolean = false,
    val isIppatsu: Boolean = false,
    val isHaitei: Boolean = false,
    val isHoutei: Boolean = false,
    val isRinshan: Boolean = false,
    val isChankan: Boolean = false,
    val doraIndicators: List<Tile> = emptyList(),
    val uraDoraIndicators: List<Tile> = emptyList(),
    val honba: Int = 0,
    val riichiSticksOnTable: Int = 0
)

data class JapaneseWinResult(
    val yaku: List<YakuEntry>,
    val totalHan: Int,
    val fu: Int,
    val limitTier: LimitTier,
    val isYakuman: Boolean,
    /** Total points the winner receives from the single discarder, excluding honba/riichi sticks. Ron only. */
    val ronPayment: Int? = null,
    /** Amount the dealer pays. Tsumo only. */
    val tsumoDealerPayment: Int? = null,
    /** Amount each non-dealer pays. Tsumo only. */
    val tsumoNonDealerPayment: Int? = null,
    val honbaTotal: Int,
    val riichiStickTotal: Int
) {
    val pointsFromEachLoser: Int get() = ronPayment ?: tsumoNonDealerPayment ?: 0

    /** Total points gained by the winner, including honba and riichi sticks. */
    fun totalGain(): Int {
        val core = ronPayment ?: ((tsumoDealerPayment ?: 0) + 2 * (tsumoNonDealerPayment ?: 0))
        return core + honbaTotal + riichiStickTotal
    }
}

sealed interface JapaneseScoringOutcome {
    data class Success(val result: JapaneseWinResult) : JapaneseScoringOutcome
    data object NoYaku : JapaneseScoringOutcome
    data object InvalidHand : JapaneseScoringOutcome
}

private enum class WaitType { RYANMEN, KANCHAN, PENCHAN, TANKI, SHANPON }

object JapaneseScoringEngine {

    fun score(input: JapaneseWinInput): JapaneseScoringOutcome {
        require(input.handTiles.size == 14) { "Hand must have 14 tiles" }

        val candidates = mutableListOf<Candidate>()

        if (HandDecomposer.isKokushi(input.handTiles)) {
            candidates += Candidate(listOf(YakuEntry("Kokushi Musou", 13)), 0, isYakuman = true)
        }

        if (HandDecomposer.isChiitoitsu(input.handTiles)) {
            val yaku = mutableListOf(YakuEntry("Chiitoitsu (Seven Pairs)", 2))
            yaku += honorFlushYaku(input.handTiles, isClosed = true)
            yaku += contextYaku(input, isMenzenTsumoEligible = input.isClosed)
            yaku += doraYaku(input)
            if (yaku.any { it.han > 0 && !it.name.startsWith("Dora") && !it.name.startsWith("Aka") && !it.name.startsWith("Ura") }) {
                candidates += Candidate(yaku, 25)
            }
        }

        for (decomposition in HandDecomposer.decomposeStandard(input.handTiles)) {
            val analysis = analyzeStandard(decomposition, input) ?: continue
            candidates += analysis
        }

        if (candidates.isEmpty()) return JapaneseScoringOutcome.InvalidHand

        val scored = candidates.map { it to computeResult(it, input) }
        val best = scored.maxByOrNull { it.second.totalGain() } ?: return JapaneseScoringOutcome.InvalidHand

        val nonBonusHan = best.first.yaku.filterNot { it.name.startsWith("Dora") || it.name.startsWith("Aka Dora") || it.name.startsWith("Ura Dora") }
        if (nonBonusHan.isEmpty() && !best.first.isYakuman) return JapaneseScoringOutcome.NoYaku

        return JapaneseScoringOutcome.Success(best.second)
    }

    private data class Candidate(
        val yaku: List<YakuEntry>,
        val fu: Int,
        val isYakuman: Boolean = false
    )

    private fun analyzeStandard(decomposition: HandDecomposition, input: JapaneseWinInput): Candidate? {
        val melds = decomposition.melds
        val pair = decomposition.pair
        val winningKind = input.winningTile.suit to input.winningTile.rank

        val winningGroupIsPair = pair.suit to pair.rank == winningKind
        val winningMeld = melds.firstOrNull { meld -> meld.tiles.any { it.suit == input.winningTile.suit && it.rank == input.winningTile.rank } }
        if (!winningGroupIsPair && winningMeld == null) return null

        val waitType = when {
            winningGroupIsPair -> WaitType.TANKI
            winningMeld!!.isTriplet -> WaitType.SHANPON
            else -> sequenceWaitType(winningMeld.representative.rank, input.winningTile.rank)
        }

        val concealedTriplets = melds.count { meld ->
            meld.isTriplet && (input.isClosed && !(meld === winningMeld && input.winType == WinType.RON))
        }
        val allSequences = melds.all { it.isSequence }
        val allTriplets = melds.all { it.isTriplet }

        val isPinfu = input.isClosed && allSequences && waitType == WaitType.RYANMEN &&
            !isYakuhaiTile(pair, input.seatWind, input.roundWind)

        var fu = 20
        if (input.isClosed && input.winType == WinType.RON) fu += 10
        if (input.winType == WinType.TSUMO && !isPinfu) fu += 2
        for (meld in melds) {
            if (!meld.isTriplet) continue
            val concealed = input.isClosed && !(meld === winningMeld && input.winType == WinType.RON)
            val base = if (meld.representative.isTerminalOrHonor) 4 else 2
            fu += if (concealed) base * 2 else base
        }
        if (pair.isDragon) fu += 2
        if (pair.isWind && pair.rank == input.seatWind.ordinal + 1) fu += 2
        if (pair.isWind && pair.rank == input.roundWind.ordinal + 1) fu += 2
        if (waitType == WaitType.KANCHAN || waitType == WaitType.PENCHAN || waitType == WaitType.TANKI) fu += 2

        fu = if (isPinfu) {
            if (input.winType == WinType.TSUMO) 20 else 30
        } else {
            ceil(fu / 10.0).toInt() * 10
        }

        val yaku = mutableListOf<YakuEntry>()

        // Yakuman checks first.
        val allTiles = decomposition.melds.flatMap { it.tiles } + pair
        if (concealedTriplets == 4) {
            yaku += YakuEntry("Suuankou (Four Concealed Triplets)", 13)
        }
        val dragonTriplets = melds.count { it.isTriplet && it.representative.isDragon }
        if (dragonTriplets == 3) yaku += YakuEntry("Daisangen (Big Three Dragons)", 13)
        if (allTiles.all { it.isHonor }) yaku += YakuEntry("Tsuuiisou (All Honors)", 13)
        if (allTiles.all { it.isTerminal } && allTriplets) yaku += YakuEntry("Chinroutou (All Terminals)", 13)
        val greenTiles = setOf(2, 3, 4, 6, 8)
        if (allTiles.all { (it.suit == Suit.SOU && it.rank in greenTiles) || (it.suit == Suit.HONOR && it.rank == 6) }) {
            yaku += YakuEntry("Ryuuiisou (All Green)", 13)
        }
        val windTriplets = melds.count { it.isTriplet && it.representative.isWind }
        if (windTriplets == 4) yaku += YakuEntry("Daisuushii (Four Winds)", 13)
        else if (windTriplets == 3 && pair.isWind) yaku += YakuEntry("Shousuushii (Three Winds + Pair)", 13)
        if (input.isClosed && isChuurenPoutou(input.handTiles)) yaku += YakuEntry("Chuuren Poutou (Nine Gates)", 13)

        if (yaku.isNotEmpty()) {
            return Candidate(yaku + doraYaku(input), 0, isYakuman = true)
        }

        // Regular yaku.
        if (allTiles.none { it.isTerminalOrHonor }) yaku += YakuEntry("Tanyao (All Simples)", 1)
        if (isPinfu) yaku += YakuEntry("Pinfu (All Sequences)", 1)

        for (meld in melds.filter { it.isTriplet }) {
            val t = meld.representative
            when {
                t.isDragon -> yaku += YakuEntry("Yakuhai (${dragonName(t.rank)})", 1)
                t.isWind && t.rank == input.seatWind.ordinal + 1 -> yaku += YakuEntry("Yakuhai (Seat Wind)", 1)
                else -> {}
            }
            if (t.isWind && t.rank == input.roundWind.ordinal + 1) yaku += YakuEntry("Yakuhai (Round Wind)", 1)
        }

        val identicalSequencePairs = melds.filter { it.isSequence }
            .groupBy { it.representative.suit to it.representative.rank }
            .values.sumOf { it.size / 2 }
        if (input.isClosed && identicalSequencePairs == 2) {
            yaku += YakuEntry("Ryanpeikou (Two Identical Sequence Pairs)", 3)
        } else if (input.isClosed && identicalSequencePairs == 1) {
            yaku += YakuEntry("Iipeiko (Identical Sequence)", 1)
        }

        val seqRanksBySuit = melds.filter { it.isSequence }
            .groupBy { it.representative.suit }
            .mapValues { e -> e.value.map { it.representative.rank }.toSet() }
        if (commonSanshokuRank(seqRanksBySuit) != null) {
            yaku += YakuEntry("Sanshoku Doujun (Mixed Triple Sequence)", if (input.isClosed) 2 else 1)
        }
        val tripletRanksBySuit = melds.filter { it.isTriplet && it.representative.suit != Suit.HONOR }
            .groupBy { it.representative.suit }
            .mapValues { e -> e.value.map { it.representative.rank }.toSet() }
        val commonTripletRank = tripletRanksBySuit[Suit.MAN]?.intersect(tripletRanksBySuit[Suit.PIN] ?: emptySet())
            ?.intersect(tripletRanksBySuit[Suit.SOU] ?: emptySet())?.firstOrNull()
        if (commonTripletRank != null) yaku += YakuEntry("Sanshoku Doukou (Mixed Triple Triplet)", 2)

        for (suit in listOf(Suit.MAN, Suit.PIN, Suit.SOU)) {
            val ranks = seqRanksBySuit[suit] ?: emptySet()
            if (setOf(1, 4, 7).all { it in ranks }) {
                yaku += YakuEntry("Ittsu (Pure Straight)", if (input.isClosed) 2 else 1)
            }
        }

        val allGroupsHaveTerminalOrHonor = melds.all { m -> m.tiles.any { it.isTerminalOrHonor } } && pair.isTerminalOrHonor
        val hasHonor = allTiles.any { it.isHonor }
        if (allGroupsHaveTerminalOrHonor) {
            if (!hasHonor) {
                yaku += YakuEntry("Junchan (Terminals in Every Set)", if (input.isClosed) 3 else 2)
            } else {
                yaku += YakuEntry("Chanta (Terminal/Honor in Every Set)", if (input.isClosed) 2 else 1)
            }
        }

        if (allTriplets) yaku += YakuEntry("Toitoi (All Triplets)", 2)
        if (concealedTriplets == 3) yaku += YakuEntry("Sanankou (Three Concealed Triplets)", 2)
        if (allTiles.all { it.isTerminalOrHonor } && allTriplets) yaku += YakuEntry("Honroutou (All Terminals and Honors)", 2)
        if (dragonTriplets == 2 && pair.isDragon) yaku += YakuEntry("Shousangen (Small Three Dragons)", 2)

        yaku += honorFlushYaku(allTiles, input.isClosed)
        yaku += contextYaku(input, isMenzenTsumoEligible = input.isClosed)
        yaku += doraYaku(input)

        return Candidate(yaku, fu)
    }

    private fun commonSanshokuRank(bySuit: Map<Suit, Set<Int>>): Int? {
        val m = bySuit[Suit.MAN] ?: return null
        val p = bySuit[Suit.PIN] ?: return null
        val s = bySuit[Suit.SOU] ?: return null
        return m.intersect(p).intersect(s).firstOrNull()
    }

    private fun sequenceWaitType(seqStartRank: Int, winRank: Int): WaitType = when (winRank) {
        seqStartRank -> if (seqStartRank + 2 == 9) WaitType.PENCHAN else WaitType.RYANMEN
        seqStartRank + 2 -> if (seqStartRank == 1) WaitType.PENCHAN else WaitType.RYANMEN
        seqStartRank + 1 -> WaitType.KANCHAN
        else -> WaitType.RYANMEN
    }

    private fun isYakuhaiTile(tile: Tile, seatWind: Wind, roundWind: Wind): Boolean =
        tile.isDragon || (tile.isWind && (tile.rank == seatWind.ordinal + 1 || tile.rank == roundWind.ordinal + 1))

    private fun dragonName(rank: Int) = when (rank) { 5 -> "White"; 6 -> "Green"; 7 -> "Red"; else -> "Dragon" }

    private fun honorFlushYaku(tiles: List<Tile>, isClosed: Boolean): List<YakuEntry> {
        val suits = tiles.filterNot { it.isHonor }.map { it.suit }.toSet()
        val hasHonor = tiles.any { it.isHonor }
        if (suits.size != 1) return emptyList()
        return if (hasHonor) {
            listOf(YakuEntry("Honitsu (Half Flush)", if (isClosed) 3 else 2))
        } else {
            listOf(YakuEntry("Chinitsu (Full Flush)", if (isClosed) 6 else 5))
        }
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

    private fun contextYaku(input: JapaneseWinInput, isMenzenTsumoEligible: Boolean): List<YakuEntry> {
        val list = mutableListOf<YakuEntry>()
        if (input.isDoubleRiichi) list += YakuEntry("Double Riichi", 2)
        else if (input.isRiichi) list += YakuEntry("Riichi", 1)
        if (input.isIppatsu) list += YakuEntry("Ippatsu", 1)
        if (isMenzenTsumoEligible && input.winType == WinType.TSUMO) list += YakuEntry("Menzen Tsumo", 1)
        if (input.isHaitei) list += YakuEntry("Haitei Raoyue (Last Tile Tsumo)", 1)
        if (input.isHoutei) list += YakuEntry("Houtei Raoyui (Last Tile Ron)", 1)
        if (input.isRinshan) list += YakuEntry("Rinshan Kaihou (After Kan)", 1)
        if (input.isChankan) list += YakuEntry("Chankan (Robbing a Kan)", 1)
        return list
    }

    private fun doraYaku(input: JapaneseWinInput): List<YakuEntry> {
        val list = mutableListOf<YakuEntry>()
        val doraTiles = input.doraIndicators.map { Tile.doraFrom(it) }
        val doraCount = input.handTiles.count { tile -> doraTiles.any { tile.suit == it.suit && tile.rank == it.rank } }
        if (doraCount > 0) list += YakuEntry("Dora", doraCount)
        if (input.isRiichi || input.isDoubleRiichi) {
            val uraTiles = input.uraDoraIndicators.map { Tile.doraFrom(it) }
            val uraCount = input.handTiles.count { tile -> uraTiles.any { tile.suit == it.suit && tile.rank == it.rank } }
            if (uraCount > 0) list += YakuEntry("Ura Dora", uraCount)
        }
        val akaCount = input.handTiles.count { it.isRedFive }
        if (akaCount > 0) list += YakuEntry("Aka Dora (Red Five)", akaCount)
        return list
    }

    private fun computeResult(candidate: Candidate, input: JapaneseWinInput): JapaneseWinResult {
        val totalHan = candidate.yaku.sumOf { it.han }
        val isDealer = input.seatWind == Wind.EAST
        val (tier, base) = pointTier(totalHan, candidate.fu, candidate.isYakuman)

        val honbaTotal: Int
        var ronPayment: Int? = null
        var tsumoDealer: Int? = null
        var tsumoNonDealer: Int? = null

        when (input.winType) {
            WinType.RON -> {
                val multiplier = if (isDealer) 6 else 4
                ronPayment = roundUp100(base * multiplier)
                honbaTotal = input.honba * 300
            }
            WinType.TSUMO -> {
                if (isDealer) {
                    tsumoNonDealer = roundUp100(base * 2)
                    tsumoDealer = tsumoNonDealer
                } else {
                    tsumoDealer = roundUp100(base * 2)
                    tsumoNonDealer = roundUp100(base * 1)
                }
                honbaTotal = input.honba * 100 * 3
            }
        }

        return JapaneseWinResult(
            yaku = candidate.yaku,
            totalHan = totalHan,
            fu = candidate.fu,
            limitTier = tier,
            isYakuman = candidate.isYakuman,
            ronPayment = ronPayment,
            tsumoDealerPayment = tsumoDealer,
            tsumoNonDealerPayment = tsumoNonDealer,
            honbaTotal = honbaTotal,
            riichiStickTotal = input.riichiSticksOnTable * 1000
        )
    }

    private fun pointTier(han: Int, fu: Int, isYakuman: Boolean): Pair<LimitTier, Int> {
        if (isYakuman || han >= 13) return LimitTier.YAKUMAN to 8000
        return when {
            han >= 11 -> LimitTier.SANBAIMAN to 6000
            han >= 8 -> LimitTier.BAIMAN to 4000
            han >= 6 -> LimitTier.HANEMAN to 3000
            han == 5 -> LimitTier.MANGAN to 2000
            else -> {
                val base = fu * (1 shl (2 + han))
                if (base >= 2000) LimitTier.MANGAN to 2000 else LimitTier.NONE to base
            }
        }
    }

    private fun roundUp100(value: Int): Int = ceil(value / 100.0).toInt() * 100
}
