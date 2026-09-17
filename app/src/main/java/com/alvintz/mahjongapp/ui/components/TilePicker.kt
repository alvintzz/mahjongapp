@file:OptIn(ExperimentalLayoutApi::class)

package com.alvintz.mahjongapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvintz.mahjongapp.R
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

private val MAN_DRAWABLES = intArrayOf(
    R.drawable.tile_man_1, R.drawable.tile_man_2, R.drawable.tile_man_3,
    R.drawable.tile_man_4, R.drawable.tile_man_5, R.drawable.tile_man_6,
    R.drawable.tile_man_7, R.drawable.tile_man_8, R.drawable.tile_man_9
)
private val PIN_DRAWABLES = intArrayOf(
    R.drawable.tile_pin_1, R.drawable.tile_pin_2, R.drawable.tile_pin_3,
    R.drawable.tile_pin_4, R.drawable.tile_pin_5, R.drawable.tile_pin_6,
    R.drawable.tile_pin_7, R.drawable.tile_pin_8, R.drawable.tile_pin_9
)
private val SOU_DRAWABLES = intArrayOf(
    R.drawable.tile_sou_1, R.drawable.tile_sou_2, R.drawable.tile_sou_3,
    R.drawable.tile_sou_4, R.drawable.tile_sou_5, R.drawable.tile_sou_6,
    R.drawable.tile_sou_7, R.drawable.tile_sou_8, R.drawable.tile_sou_9
)

/** SVG artwork from https://github.com/samoheen/mahjong-tiles (hongkong/svg), converted to
 * Android vector drawables. Red fives reuse the plain 5 tile's artwork, distinguished only by
 * the "0" caption, per the same convention used elsewhere in this app's notation. */
private fun tileDrawable(suit: Suit, rank: Int): Int = when (suit) {
    Suit.MAN -> MAN_DRAWABLES[rank - 1]
    Suit.PIN -> PIN_DRAWABLES[rank - 1]
    Suit.SOU -> SOU_DRAWABLES[rank - 1]
    Suit.HONOR -> when (rank) {
        1 -> R.drawable.tile_wind_east
        2 -> R.drawable.tile_wind_south
        3 -> R.drawable.tile_wind_west
        4 -> R.drawable.tile_wind_north
        5 -> R.drawable.tile_dragon_white
        6 -> R.drawable.tile_dragon_green
        else -> R.drawable.tile_dragon_red
    }
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

/**
 * The tile artwork is ink drawn for a physical ivory tile face, so its background must stay a
 * fixed light color regardless of the app's light/dark theme — using theme-adaptive Material
 * colors here made the ink nearly invisible in dark mode once the background went dark too.
 */
private val TileFaceColor = Color(0xFFF3ECDA)
private val TileFaceSelectedColor = Color(0xFFE3D6A8)
private val TileFaceHighlightColor = Color(0xFFFFD54F)
private val TileBorderColor = Color(0xFF8A7A52)
private val TileTextColor = Color(0xFF3E2E17)

@Composable
fun TileGridPicker(state: HandBuilderState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Tap tiles to build the 14-tile hand (${state.selected.size}/14)", style = MaterialTheme.typography.labelLarge)
        Box(Modifier.padding(vertical = 8.dp)) {
            SelectedHandTray(state)
        }
        Divider()

        TileSection(title = "Chinese Number", tiles = (1..9).map { Suit.MAN to it }, state = state)
        TileSection(title = "Bamboo", tiles = (1..9).map { Suit.SOU to it }, state = state)
        TileSection(title = "Dot", tiles = (1..9).map { Suit.PIN to it }, state = state)
        TileSection(title = "Dragon", tiles = listOf(Suit.HONOR to 5, Suit.HONOR to 6, Suit.HONOR to 7), state = state)
        TileSection(title = "Winds", tiles = (1..4).map { Suit.HONOR to it }, state = state)
        RedFiveSection(state)

        if (state.selected.size == 14) {
            Divider(Modifier.padding(vertical = 8.dp))
            Text("Which tile did you win on?", style = MaterialTheme.typography.labelLarge)
            WinningTilePicker(state)
        }
    }
}

/** Renders a labeled group of tiles, wrapped into rows of at most 5. */
@Composable
private fun TileSection(title: String, tiles: List<Pair<Suit, Int>>, state: HandBuilderState) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        tiles.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                row.forEach { (suit, rank) ->
                    val count = state.totalCountOf(suit, rank)
                    TileButton(
                        drawableRes = tileDrawable(suit, rank),
                        caption = tileCaption(suit, rank),
                        count = count,
                        onClick = { state.add(Tile(suit, rank)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RedFiveSection(state: HandBuilderState) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text("Red 5", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
            listOf(Suit.MAN, Suit.PIN, Suit.SOU).forEach { suit ->
                val count = state.countOf(suit, 5, red = true)
                TileButton(
                    drawableRes = tileDrawable(suit, 5),
                    caption = "0${suitSuffix(suit)}",
                    count = count,
                    onClick = { state.add(Tile(suit, 5, isRedFive = true)) }
                )
            }
        }
    }
}

@Composable
private fun SelectedHandTray(state: HandBuilderState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        state.selected.sortedWith(compareBy({ it.suit.ordinal }, { it.rank }, { !it.isRedFive })).forEach { tile ->
            val isWinning = state.winningTile?.let { it.suit == tile.suit && it.rank == tile.rank } == true
            TileChip(
                drawableRes = tileDrawable(tile.suit, tile.rank),
                caption = if (tile.isRedFive) "0${suitSuffix(tile.suit)}" else tileCaption(tile.suit, tile.rank),
                highlighted = isWinning,
                onClick = { state.removeOne(tile.suit, tile.rank, tile.isRedFive) }
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
                drawableRes = tileDrawable(suit, rank),
                caption = tileCaption(suit, rank),
                highlighted = isSelected,
                onClick = { state.winningTile = Tile(suit, rank) }
            )
        }
    }
}

@Composable
private fun TileButton(drawableRes: Int, caption: String, count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .clickable(onClick = onClick)
            .background(
                if (count > 0) TileFaceSelectedColor else TileFaceColor,
                RoundedCornerShape(6.dp)
            )
            .border(1.dp, TileBorderColor, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = caption,
                modifier = Modifier.size(width = 28.dp, height = 39.dp)
            )
            Text(caption, color = TileTextColor, fontSize = 10.sp)
            if (count > 0) Text("x$count", color = TileTextColor, fontSize = 9.sp)
        }
    }
}

@Composable
private fun TileChip(drawableRes: Int, caption: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (highlighted) TileFaceHighlightColor else TileFaceColor,
                RoundedCornerShape(6.dp)
            )
            .border(1.dp, TileBorderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = caption,
                modifier = Modifier.size(width = 24.dp, height = 34.dp)
            )
            Text(caption, color = TileTextColor, fontSize = 9.sp)
        }
    }
}
