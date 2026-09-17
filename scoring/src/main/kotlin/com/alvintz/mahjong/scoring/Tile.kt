package com.alvintz.mahjong.scoring

enum class Suit {
    MAN, PIN, SOU, HONOR
}

/**
 * Honor ranks: 1=East, 2=South, 3=West, 4=North, 5=White (Haku), 6=Green (Hatsu), 7=Red (Chun).
 * Suited ranks: 1-9.
 */
data class Tile(val suit: Suit, val rank: Int, val isRedFive: Boolean = false) : Comparable<Tile> {

    init {
        val maxRank = if (suit == Suit.HONOR) 7 else 9
        require(rank in 1..maxRank) { "Invalid rank $rank for suit $suit" }
        require(!isRedFive || (suit != Suit.HONOR && rank == 5)) { "Only 5m/5p/5s tiles can be red fives" }
    }

    val isTerminal: Boolean get() = suit != Suit.HONOR && (rank == 1 || rank == 9)
    val isHonor: Boolean get() = suit == Suit.HONOR
    val isWind: Boolean get() = suit == Suit.HONOR && rank in 1..4
    val isDragon: Boolean get() = suit == Suit.HONOR && rank in 5..7
    val isTerminalOrHonor: Boolean get() = isTerminal || isHonor

    /** Identity ignoring the red-five flag; melds/pairs are grouped by this. */
    fun sameKind(other: Tile): Boolean = suit == other.suit && rank == other.rank

    override fun compareTo(other: Tile): Int {
        if (suit != other.suit) return suit.ordinal - other.suit.ordinal
        return rank - other.rank
    }

    override fun toString(): String {
        val suffix = when (suit) {
            Suit.MAN -> "m"
            Suit.PIN -> "p"
            Suit.SOU -> "s"
            Suit.HONOR -> "z"
        }
        return "$rank$suffix${if (isRedFive) "*" else ""}"
    }

    companion object {
        fun man(rank: Int, red: Boolean = false) = Tile(Suit.MAN, rank, red)
        fun pin(rank: Int, red: Boolean = false) = Tile(Suit.PIN, rank, red)
        fun sou(rank: Int, red: Boolean = false) = Tile(Suit.SOU, rank, red)
        fun honor(rank: Int) = Tile(Suit.HONOR, rank)

        val EAST = honor(1)
        val SOUTH = honor(2)
        val WEST = honor(3)
        val NORTH = honor(4)
        val WHITE = honor(5)
        val GREEN = honor(6)
        val RED = honor(7)

        /** All 34 distinct tile kinds (no red-five variants). */
        fun allKinds(): List<Tile> =
            (1..9).map { man(it) } + (1..9).map { pin(it) } + (1..9).map { sou(it) } + (1..7).map { honor(it) }

        /** The dora tile that follows [indicator], per standard Riichi dora-indicator rules. */
        fun doraFrom(indicator: Tile): Tile = when (indicator.suit) {
            Suit.HONOR -> if (indicator.rank in 1..4) {
                honor(if (indicator.rank == 4) 1 else indicator.rank + 1)
            } else {
                honor(if (indicator.rank == 7) 5 else indicator.rank + 1)
            }
            else -> Tile(indicator.suit, if (indicator.rank == 9) 1 else indicator.rank + 1)
        }
    }
}

enum class Wind {
    EAST, SOUTH, WEST, NORTH;

    fun next(): Wind = entries[(ordinal + 1) % entries.size]

    fun toTile(): Tile = Tile.honor(ordinal + 1)
}
