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
import androidx.compose.foundation.layout.size
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

private fun tileLabel(suit: Suit, rank: Int): String = when (suit) {
    Suit.HONOR -> listOf("E", "S", "W", "N", "Wh", "Gr", "Rd")[rank - 1]
    else -> rank.toString()
}

private fun suitSuffix(suit: Suit): String = when (suit) {
    Suit.MAN -> "m"
    Suit.PIN -> "p"
    Suit.SOU -> "s"
    Suit.HONOR -> ""
}

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
            TileChip(
                text = (if (tile.isRedFive) "0" else tileLabel(tile.suit, tile.rank)) + suitSuffix(tile.suit),
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
                text = "$rank${suitSuffix(suit)}",
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
                text = "0${suitSuffix(suit)}",
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
                text = tileLabel(Suit.HONOR, rank),
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
            val label = tileLabel(suit, rank) + suitSuffix(suit)
            val isSelected = state.winningTile?.let { it.suit == suit && it.rank == rank } == true
            TileChip(text = label, highlighted = isSelected, onClick = { state.winningTile = Tile(suit, rank) })
        }
    }
}

@Composable
private fun TileButton(text: String, count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .background(
                if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(6.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (count > 0) Text("x$count", fontSize = 9.sp)
        }
    }
}

@Composable
private fun TileChip(text: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (highlighted) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 13.sp, color = if (highlighted) MaterialTheme.colorScheme.onTertiaryContainer else Color.Unspecified)
    }
}
