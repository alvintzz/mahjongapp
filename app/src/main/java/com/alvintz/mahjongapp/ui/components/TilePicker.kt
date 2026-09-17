@file:OptIn(ExperimentalLayoutApi::class)

package com.alvintz.mahjongapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvintz.mahjong.scoring.Suit
import com.alvintz.mahjong.scoring.Tile

/** Holds the in-progress 14-tile hand selection for the win-entry screen. */
class HandBuilderState {
    val selected = mutableStateListOf<Tile>()
    var winningTile by mutableStateOf<Tile?>(null)

    val isComplete: Boolean get() = selected.size == 14 && winningTile != null

    fun countOf(suit: Suit, rank: Int, red: Boolean): Int =
        selected.count { it.suit == suit && it.rank == rank && it.isRedFive == red }

    fun totalCountOf(suit: Suit, rank: Int): Int = selected.count { it.suit == suit && it.rank == rank }

    fun add(tile: Tile) {
        if (selected.size >= 14) return
        if (totalCountOf(tile.suit, tile.rank) >= 4) return
        selected.add(tile)
    }

    fun removeOne(suit: Suit, rank: Int, red: Boolean) {
        val idx = selected.indexOfFirst { it.suit == suit && it.rank == rank && it.isRedFive == red }
        if (idx >= 0) {
            val removed = selected.removeAt(idx)
            if (winningTile != null && winningTile!!.suit == removed.suit && winningTile!!.rank == removed.rank && totalCountOf(removed.suit, removed.rank) == 0) {
                winningTile = null
            }
        }
    }

    fun clear() {
        selected.clear()
        winningTile = null
    }
}

/**
 * The real Unicode Mahjong Tile glyphs (U+1F000-U+1F021), e.g. 🀇🀈🀉 for characters,
 * 🀐🀑🀒 for bamboos, 🀙🀚🀛 for circles — same tiles pictured on
 * https://www.alt-codes.net/mahjong-tiles-symbols.php. Shown big as the primary visual,
 * with a small text caption underneath for players who don't yet recognize them by eye.
 */
private fun tileGlyph(suit: Suit, rank: Int): String {
    val codepoint = when (suit) {
        Suit.MAN -> 0x1F006 + rank // 1F007 (1m) .. 1F00F (9m)
        Suit.SOU -> 0x1F00F + rank // 1F010 (1s) .. 1F018 (9s)
        Suit.PIN -> 0x1F018 + rank // 1F019 (1p) .. 1F021 (9p)
        Suit.HONOR -> when (rank) {
            1 -> 0x1F000 // East
            2 -> 0x1F001 // South
            3 -> 0x1F002 // West
            4 -> 0x1F003 // North
            5 -> 0x1F006 // White dragon
            6 -> 0x1F005 // Green dragon
            else -> 0x1F004 // Red dragon (rank 7)
        }
    }
    return String(Character.toChars(codepoint))
}

private fun tileCaption(suit: Suit, rank: Int): String = when (suit) {
    Suit.HONOR -> listOf("E", "S", "W", "N", "Wh", "Gr", "Rd")[rank - 1]
    else -> "$rank${suitSuffix(suit)}"
}

private fun suitSuffix(suit: Suit): String = when (suit) {
    Suit.MAN -> "m"
    Suit.PIN -> "p"
    Suit.SOU -> "s"
    Suit.HONOR -> ""
}

/** A distinct color per suit so tiles are easy to tell apart at a glance, independent of the glyph. */
private fun suitTint(suit: Suit): Color = when (suit) {
    Suit.MAN -> Color(0xFFC62828) // red — characters
    Suit.PIN -> Color(0xFF1565C0) // blue — circles/dots
    Suit.SOU -> Color(0xFF2E7D32) // green — bamboos
    Suit.HONOR -> Color(0xFF424242) // dark grey — winds/dragons
}

private val redFiveTint = Color(0xFFD50000)

@Composable
fun TileGridPicker(state: HandBuilderState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Tap tiles to build the 14-tile hand (${state.selected.size}/14)", style = MaterialTheme.typography.labelLarge)
        Box(Modifier.padding(vertical = 8.dp)) {
            SelectedHandTray(state)
        }
        Divider()
        SuitRow(Suit.MAN, state)
        SuitRow(Suit.PIN, state)
        SuitRow(Suit.SOU, state)
        RedFiveRow(state)
        HonorRow(state)

        if (state.selected.size == 14) {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Which tile did you win on?", style = MaterialTheme.typography.labelLarge)
            WinningTilePicker(state)
        }
    }
}

@Composable
private fun SelectedHandTray(state: HandBuilderState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.selected.sortedWith(compareBy({ it.suit.ordinal }, { it.rank }, { !it.isRedFive })).forEach { tile ->
            val isWinning = state.winningTile?.let { it.suit == tile.suit && it.rank == tile.rank } == true
            val tint = if (tile.isRedFive) redFiveTint else suitTint(tile.suit)
            TileChip(
                glyph = tileGlyph(tile.suit, tile.rank),
                caption = if (tile.isRedFive) "0${suitSuffix(tile.suit)}" else tileCaption(tile.suit, tile.rank),
                tint = tint,
                highlighted = isWinning,
                onClick = { state.removeOne(tile.suit, tile.rank, tile.isRedFive) }
            )
        }
    }
}

@Composable
private fun SuitRow(suit: Suit, state: HandBuilderState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        for (rank in 1..9) {
            val count = state.totalCountOf(suit, rank)
            TileButton(
                glyph = tileGlyph(suit, rank),
                caption = tileCaption(suit, rank),
                tint = suitTint(suit),
                count = count,
                onClick = { state.add(Tile(suit, rank)) }
            )
        }
    }
}

@Composable
private fun RedFiveRow(state: HandBuilderState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Text("Red 5s:", modifier = Modifier.padding(end = 4.dp), style = MaterialTheme.typography.labelMedium)
        for (suit in listOf(Suit.MAN, Suit.PIN, Suit.SOU)) {
            val count = state.countOf(suit, 5, red = true)
            TileButton(
                glyph = tileGlyph(suit, 5),
                caption = "0${suitSuffix(suit)}",
                tint = redFiveTint,
                count = count,
                onClick = { state.add(Tile(suit, 5, isRedFive = true)) }
            )
        }
    }
}

@Composable
private fun HonorRow(state: HandBuilderState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        for (rank in 1..7) {
            val count = state.totalCountOf(Suit.HONOR, rank)
            TileButton(
                glyph = tileGlyph(Suit.HONOR, rank),
                caption = tileCaption(Suit.HONOR, rank),
                tint = suitTint(Suit.HONOR),
                count = count,
                onClick = { state.add(Tile(Suit.HONOR, rank)) }
            )
        }
    }
}

@Composable
private fun WinningTilePicker(state: HandBuilderState) {
    val distinctKinds = state.selected.map { it.suit to it.rank }.distinct()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        distinctKinds.forEach { (suit, rank) ->
            val isSelected = state.winningTile?.let { it.suit == suit && it.rank == rank } == true
            TileChip(
                glyph = tileGlyph(suit, rank),
                caption = tileCaption(suit, rank),
                tint = suitTint(suit),
                highlighted = isSelected,
                onClick = { state.winningTile = Tile(suit, rank) }
            )
        }
    }
}

@Composable
private fun TileButton(glyph: String, caption: String, tint: Color, count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .clickable(onClick = onClick)
            .background(
                if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(6.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, fontSize = 26.sp, color = tint)
            Text(caption, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            if (count > 0) Text("x$count", fontSize = 9.sp)
        }
    }
}

@Composable
private fun TileChip(glyph: String, caption: String, tint: Color, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(glyph, fontSize = 22.sp, color = tint)
            Text(caption, fontSize = 9.sp)
        }
    }
}
