package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvintz.mahjong.scoring.HongKongScoringOutcome
import com.alvintz.mahjong.scoring.JapaneseScoringOutcome
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjong.scoring.WinType
import com.alvintz.mahjongapp.model.GameState
import com.alvintz.mahjongapp.ui.components.HandBuilderState
import com.alvintz.mahjongapp.ui.components.TileGridPicker
import com.alvintz.mahjongapp.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinEntryScreen(
    state: GameState,
    winnerSeat: Int,
    viewModel: GameViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val winner = state.players.first { it.seatIndex == winnerSeat }
    val others = state.players.filter { it.seatIndex != winnerSeat }

    var winType by remember { mutableStateOf(WinType.TSUMO) }
    var loserSeat by remember { mutableStateOf<Int?>(others.firstOrNull()?.seatIndex) }
    var isClosed by remember { mutableStateOf(true) }
    var isDoubleRiichi by remember { mutableStateOf(false) }
    var isIppatsu by remember { mutableStateOf(false) }
    var isHaitei by remember { mutableStateOf(false) }
    var isHoutei by remember { mutableStateOf(false) }
    var isRinshan by remember { mutableStateOf(false) }
    var isChankan by remember { mutableStateOf(false) }
    val handState = remember { HandBuilderState() }

    var japaneseOutcome by remember { mutableStateOf<JapaneseScoringOutcome?>(null) }
    var hongKongOutcome by remember { mutableStateOf<HongKongScoringOutcome?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("${winner.name} wins") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = winType == WinType.TSUMO, onClick = { winType = WinType.TSUMO; isHoutei = false; japaneseOutcome = null; hongKongOutcome = null }, label = { Text("Self-draw (Tsumo)") })
                FilterChip(selected = winType == WinType.RON, onClick = { winType = WinType.RON; isHaitei = false; japaneseOutcome = null; hongKongOutcome = null }, label = { Text("Discard (Ron)") })
            }

            if (winType == WinType.RON) {
                Text("Who discarded?", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    others.forEach { p ->
                        FilterChip(
                            selected = loserSeat == p.seatIndex,
                            onClick = { loserSeat = p.seatIndex },
                            label = { Text(p.name) }
                        )
                    }
                }
            }

            FilterChip(selected = isClosed, onClick = { isClosed = !isClosed }, label = { Text(if (isClosed) "Closed hand" else "Open hand (has calls)") })

            if (state.ruleSet == RuleSet.JAPANESE) {
                Text("Situational flags", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isDoubleRiichi, onClick = { isDoubleRiichi = !isDoubleRiichi }, label = { Text("Double Riichi") })
                    FilterChip(selected = isIppatsu, onClick = { isIppatsu = !isIppatsu }, label = { Text("Ippatsu") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isRinshan, onClick = { isRinshan = !isRinshan }, label = { Text("Rinshan") })
                    FilterChip(selected = isChankan, onClick = { isChankan = !isChankan }, label = { Text("Chankan") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (winType == WinType.TSUMO) {
                        FilterChip(selected = isHaitei, onClick = { isHaitei = !isHaitei }, label = { Text("Haitei (last tile)") })
                    } else {
                        FilterChip(selected = isHoutei, onClick = { isHoutei = !isHoutei }, label = { Text("Houtei (last discard)") })
                    }
                }
                if (winnerSeat in state.riichiDeclaredSeats) {
                    Text("This player already declared Riichi this hand — it will be counted automatically.", style = MaterialTheme.typography.bodySmall)
                }
            }

            TileGridPicker(handState)

            Button(
                onClick = {
                    if (state.ruleSet == RuleSet.JAPANESE) {
                        japaneseOutcome = viewModel.evaluateJapaneseWin(
                            GameViewModel.JapaneseWinRequest(
                                winnerSeat = winnerSeat,
                                loserSeat = if (winType == WinType.RON) loserSeat else null,
                                handTiles = handState.selected.toList(),
                                winningTile = handState.winningTile!!,
                                winType = winType,
                                isClosed = isClosed,
                                isDoubleRiichi = isDoubleRiichi,
                                isIppatsu = isIppatsu,
                                isHaitei = isHaitei,
                                isHoutei = isHoutei,
                                isRinshan = isRinshan,
                                isChankan = isChankan,
                                uraDoraIndicators = emptyList()
                            )
                        )
                    } else {
                        hongKongOutcome = viewModel.evaluateHongKongWin(
                            GameViewModel.HongKongWinRequest(
                                winnerSeat = winnerSeat,
                                loserSeat = if (winType == WinType.RON) loserSeat else null,
                                handTiles = handState.selected.toList(),
                                winningTile = handState.winningTile!!,
                                winType = winType,
                                isClosed = isClosed
                            )
                        )
                    }
                },
                enabled = handState.isComplete && (winType == WinType.TSUMO || loserSeat != null),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate Score")
            }

            japaneseOutcome?.let { outcome ->
                JapaneseResultCard(outcome) {
                    if (outcome is JapaneseScoringOutcome.Success) {
                        viewModel.applyJapaneseWin(
                            GameViewModel.JapaneseWinRequest(
                                winnerSeat, if (winType == WinType.RON) loserSeat else null,
                                handState.selected.toList(), handState.winningTile!!, winType, isClosed,
                                isDoubleRiichi, isIppatsu, isHaitei, isHoutei, isRinshan, isChankan, emptyList()
                            ),
                            outcome.result
                        )
                        onDone()
                    }
                }
            }
            hongKongOutcome?.let { outcome ->
                HongKongResultCard(outcome) {
                    if (outcome is HongKongScoringOutcome.Success) {
                        viewModel.applyHongKongWin(
                            GameViewModel.HongKongWinRequest(
                                winnerSeat, if (winType == WinType.RON) loserSeat else null,
                                handState.selected.toList(), handState.winningTile!!, winType, isClosed
                            ),
                            outcome.result
                        )
                        onDone()
                    }
                }
            }

            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun JapaneseResultCard(outcome: JapaneseScoringOutcome, onConfirm: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (outcome) {
                is JapaneseScoringOutcome.Success -> {
                    val r = outcome.result
                    Text("${r.totalHan} han / ${r.fu} fu — ${r.limitTier}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    r.yaku.forEach { Text("${it.name}: ${it.han} han") }
                    Text("Total gain: ${r.totalGain()} pts", fontWeight = FontWeight.Bold)
                    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm & Apply") }
                }
                JapaneseScoringOutcome.NoYaku -> Text("No yaku — this hand cannot win. Check flags or tiles.")
                JapaneseScoringOutcome.InvalidHand -> Text("These 14 tiles don't form a valid winning hand.")
            }
        }
    }
}

@Composable
private fun HongKongResultCard(outcome: HongKongScoringOutcome, onConfirm: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (outcome) {
                is HongKongScoringOutcome.Success -> {
                    val r = outcome.result
                    Text("${r.totalFan} fan${if (r.isLimitHand) " (Limit hand)" else ""}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    r.fanBreakdown.forEach { Text("${it.name}: ${it.fan} fan") }
                    Text("Points per payer: ${r.pointsPerPayer}", fontWeight = FontWeight.Bold)
                    Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm & Apply") }
                }
                is HongKongScoringOutcome.BelowMinimumFan -> Text("Only ${outcome.actualFan} fan, needs at least ${outcome.required} to win.")
                HongKongScoringOutcome.InvalidHand -> Text("These 14 tiles don't form a valid winning hand.")
            }
        }
    }
}
