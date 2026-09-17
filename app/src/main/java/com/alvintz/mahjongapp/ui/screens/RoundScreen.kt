package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjong.scoring.Wind
import com.alvintz.mahjongapp.model.GameState
import com.alvintz.mahjongapp.model.PlayerState

private val dealerYellow = Color(0xFFFFD54F)
private val dealerOnYellow = Color(0xFF3E2723)

/**
 * A 2x2 grid of seat cards (North/East on top, West/South on bottom) with a full-width round
 * info card above it. All four seats use the same card design; only the dealer's yellow
 * background and the rank number distinguish them.
 */
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

    val rankOf = state.players
        .sortedWith(compareByDescending<PlayerState> { it.score }.thenBy { it.seatIndex })
        .withIndex()
        .associate { (index, p) -> p.seatIndex to index + 1 }
    val byWind = state.players.associateBy { state.windOf(it.seatIndex) }

    Scaffold(topBar = { TopAppBar(title = { Text("MahjongCalc") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoundInfoCard(state = state, onEditDora = onEditDora)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                byWind[Wind.NORTH]?.let { player ->
                    SeatCard(
                        state = state,
                        player = player,
                        wind = Wind.NORTH,
                        rank = rankOf.getValue(player.seatIndex),
                        onWin = { onWin(player.seatIndex) },
                        onDeclareRiichi = { onDeclareRiichi(player.seatIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
                byWind[Wind.EAST]?.let { player ->
                    SeatCard(
                        state = state,
                        player = player,
                        wind = Wind.EAST,
                        rank = rankOf.getValue(player.seatIndex),
                        onWin = { onWin(player.seatIndex) },
                        onDeclareRiichi = { onDeclareRiichi(player.seatIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                byWind[Wind.WEST]?.let { player ->
                    SeatCard(
                        state = state,
                        player = player,
                        wind = Wind.WEST,
                        rank = rankOf.getValue(player.seatIndex),
                        onWin = { onWin(player.seatIndex) },
                        onDeclareRiichi = { onDeclareRiichi(player.seatIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
                byWind[Wind.SOUTH]?.let { player ->
                    SeatCard(
                        state = state,
                        player = player,
                        wind = Wind.SOUTH,
                        rank = rankOf.getValue(player.seatIndex),
                        onWin = { onWin(player.seatIndex) },
                        onDeclareRiichi = { onDeclareRiichi(player.seatIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheckWinningTile, modifier = Modifier.fillMaxWidth()) {
                    Text("Check Winning Tile")
                }
                OutlinedButton(onClick = onDraw, modifier = Modifier.fillMaxWidth()) {
                    Text("Exhaustive Draw (no winner)")
                }
                Button(
                    onClick = { showEndConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("End Game")
                }
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

private fun windKanji(wind: Wind): String = when (wind) {
    Wind.EAST -> "東"
    Wind.SOUTH -> "南"
    Wind.WEST -> "西"
    Wind.NORTH -> "北"
}

@Composable
private fun RoundInfoCard(state: GameState, onEditDora: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ROUND", style = MaterialTheme.typography.labelSmall)
                Text(
                    "${windKanji(state.roundWind)}${state.handNumber}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (state.honba > 0) {
                    Text("Honba: ${state.honba}", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (state.ruleSet == RuleSet.JAPANESE) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (state.doraIndicators.isEmpty()) "Dora: none" else "Dora: ${state.doraIndicators.joinToString(", ") { it.toString() }}",
                        style = MaterialTheme.typography.labelSmall
                    )
                    TextButton(onClick = onEditDora) { Text("Edit Dora") }
                }
            }
        }
    }
}

@Composable
private fun SeatCard(
    state: GameState,
    player: PlayerState,
    wind: Wind,
    rank: Int,
    onWin: () -> Unit,
    onDeclareRiichi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDealer = player.seatIndex == state.dealerSeatIndex
    val canDeclareRiichi = state.ruleSet == RuleSet.JAPANESE &&
        player.seatIndex !in state.riichiDeclaredSeats &&
        player.score >= 1000

    Card(
        modifier = modifier,
        colors = if (isDealer) {
            CardDefaults.cardColors(containerColor = dealerYellow)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        CompositionLocalProvider(LocalContentColor provides if (isDealer) dealerOnYellow else LocalContentColor.current) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                windKanji(wind),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isDealer) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "DEALER",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(if (rank == 1) "#1 Leader" else "#$rank", style = MaterialTheme.typography.labelSmall)
                }

                Text(player.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${player.score} pts", style = MaterialTheme.typography.headlineSmall)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onWin, modifier = Modifier.weight(1f)) { Text("WIN") }
                    if (canDeclareRiichi) {
                        OutlinedButton(onClick = onDeclareRiichi) { Text("Riichi") }
                    }
                }
            }
        }
    }
}
