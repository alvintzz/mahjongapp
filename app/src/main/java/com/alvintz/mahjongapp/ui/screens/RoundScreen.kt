package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjongapp.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundScreen(
    state: GameState,
    onWin: (winnerSeat: Int) -> Unit,
    onCheckWinningTile: () -> Unit,
    onDraw: () -> Unit,
    onDeclareRiichi: (seatIndex: Int) -> Unit,
    onEditDora: () -> Unit,
    onEndGame: () -> Unit
) {
    var showEndConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("${state.roundLabel}${if (state.ruleSet == RuleSet.JAPANESE) "  •  Honba ${state.honba}" else ""}") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.ruleSet == RuleSet.JAPANESE) {
                DoraAndStickRow(state, onEditDora)
            }

            state.players.forEach { player ->
                PlayerCard(
                    name = player.name,
                    wind = state.windOf(player.seatIndex).name,
                    score = player.score,
                    isDealer = player.seatIndex == state.dealerSeatIndex,
                    canDeclareRiichi = state.ruleSet == RuleSet.JAPANESE && player.seatIndex !in state.riichiDeclaredSeats && player.score >= 1000,
                    hasDeclaredRiichi = player.seatIndex in state.riichiDeclaredSeats,
                    showRiichi = state.ruleSet == RuleSet.JAPANESE,
                    onWin = { onWin(player.seatIndex) },
                    onDeclareRiichi = { onDeclareRiichi(player.seatIndex) }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onCheckWinningTile, modifier = Modifier.fillMaxWidth()) {
                Text("Check Winning Tile")
            }
            OutlinedButton(onClick = onDraw, modifier = Modifier.fillMaxWidth()) {
                Text("Exhaustive Draw (no winner)")
            }
            Button(
                onClick = { showEndConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("End Game")
            }
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End game?") },
            text = { Text("Final scores will be saved to history and this game will be closed.") },
            confirmButton = {
                TextButton(onClick = { showEndConfirm = false; onEndGame() }) { Text("End Game") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DoraAndStickRow(state: GameState, onEditDora: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Dora indicators", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (state.doraIndicators.isEmpty()) "None revealed" else state.doraIndicators.joinToString(", ") { it.toString() },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("Riichi sticks: ${state.riichiSticks}", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onEditDora) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    name: String,
    wind: String,
    score: Int,
    isDealer: Boolean,
    canDeclareRiichi: Boolean,
    hasDeclaredRiichi: Boolean,
    showRiichi: Boolean,
    onWin: () -> Unit,
    onDeclareRiichi: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$name${if (isDealer) " (Dealer)" else ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(wind, style = MaterialTheme.typography.bodySmall)
                Text("$score pts", style = MaterialTheme.typography.headlineSmall)
                if (hasDeclaredRiichi) Text("RIICHI", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Button(onClick = onWin) { Text("WIN!") }
                if (showRiichi && canDeclareRiichi) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDeclareRiichi) { Text("Riichi") }
                }
            }
        }
    }
}
