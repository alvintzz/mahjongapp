@file:OptIn(ExperimentalLayoutApi::class)

package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvintz.mahjong.scoring.Suit
import com.alvintz.mahjong.scoring.Tile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoraEditScreen(
    initial: List<Tile>,
    onConfirm: (List<Tile>) -> Unit,
    onCancel: () -> Unit
) {
    val indicators = remember { mutableStateListOf(*initial.toTypedArray()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Dora Indicators") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(
                "The dora indicator is revealed at the start of each hand; add another when a kan is called.",
                style = MaterialTheme.typography.bodyMedium
            )
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Current indicators:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (indicators.isEmpty()) Text("None")
                    indicators.forEachIndexed { index, tile ->
                        Chip(text = tile.toString()) { indicators.removeAt(index) }
                    }
                }
            }
            Divider()
            Text("Tap a tile to add it as an indicator:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            for (suit in listOf(Suit.MAN, Suit.PIN, Suit.SOU)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    for (rank in 1..9) {
                        SmallTileButton("$rank${suitSuffixFor(suit)}") { indicators.add(Tile(suit, rank)) }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                for (rank in 1..7) {
                    SmallTileButton(honorLabel(rank)) { indicators.add(Tile(Suit.HONOR, rank)) }
                }
            }

            Column(Modifier.padding(top = 16.dp)) {
                Button(onClick = { onConfirm(indicators.toList()) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
                Button(onClick = onCancel, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Cancel") }
            }
        }
    }
}

private fun suitSuffixFor(suit: Suit) = when (suit) { Suit.MAN -> "m"; Suit.PIN -> "p"; Suit.SOU -> "s"; Suit.HONOR -> "" }
private fun honorLabel(rank: Int) = listOf("E", "S", "W", "N", "Wh", "Gr", "Rd")[rank - 1]

@Composable
private fun SmallTileButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
private fun Chip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text)
    }
}
