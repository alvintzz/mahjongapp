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

/**
 * Lets a player check, before committing to a win, whether a candidate 14-tile hand actually
 * qualifies as a win under the current rule set — Hong Kong's minimum-fan requirement, or
 * Japanese's requirement of at least one yaku. This only evaluates; it never changes scores,
 * riichi state, or anything else about the game (unlike [WinEntryScreen]'s "Confirm & Apply").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckWinningTileScreen(
    state: GameState,
    viewModel: GameViewModel,
    onCancel: () -> Unit
) {
    var checkingSeat by remember { mutableStateOf(state.players.first().seatIndex) }
    var winType by remember { mutableStateOf(WinType.TSUMO) }
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

    Scaffold(topBar = { TopAppBar(title = { Text("Check Winning Tile") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val validityHint = if (state.ruleSet == RuleSet.HONG_KONG) {
                "at least ${state.hkMinFanToWin} fan"
            } else {
                "at least one yaku"
            }
            Text(
                "Build a candidate hand to see if it would actually be a valid win ($validityHint) " +
                    "before declaring — nothing here affects scores.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text("Checking for", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.players.forEach { p ->
                    FilterChip(
                        selected = checkingSeat == p.seatIndex,
                        onClick = { checkingSeat = p.seatIndex; japaneseOutcome = null; hongKongOutcome = null },
                        label = { Text(p.name) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = winType == WinType.TSUMO, onClick = { winType = WinType.TSUMO; isHoutei = false; japaneseOutcome = null; hongKongOutcome = null }, label = { Text("Self-draw (Tsumo)") })
                FilterChip(selected = winType == WinType.RON, onClick = { winType = WinType.RON; isHaitei = false; japaneseOutcome = null; hongKongOutcome = null }, label = { Text("Discard (Ron)") })
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
                if (checkingSeat in state.riichiDeclaredSeats) {
                    Text("This player already declared Riichi this hand — it will be counted automatically.", style = MaterialTheme.typography.bodySmall)
                }
            }

            TileGridPicker(handState)

            Button(
                onClick = {
                    if (state.ruleSet == RuleSet.JAPANESE) {
                        japaneseOutcome = viewModel.evaluateJapaneseWin(
                            GameViewModel.JapaneseWinRequest(
                                winnerSeat = checkingSeat,
                                loserSeat = null,
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
                                winnerSeat = checkingSeat,
                                loserSeat = null,
                                handTiles = handState.selected.toList(),
                                winningTile = handState.winningTile!!,
                                winType = winType,
                                isClosed = isClosed
                            )
                        )
                    }
                },
                enabled = handState.isComplete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check")
            }

            japaneseOutcome?.let { CheckResultCard(it) }
            hongKongOutcome?.let { CheckResultCard(it, state.hkMinFanToWin) }

            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

@Composable
private fun CheckResultCard(outcome: JapaneseScoringOutcome) {
    val (containerColor, headline) = when (outcome) {
        is JapaneseScoringOutcome.Success -> MaterialTheme.colorScheme.primaryContainer to "✓ Valid win"
        JapaneseScoringOutcome.NoYaku -> MaterialTheme.colorScheme.errorContainer to "✗ Not a valid win — no yaku"
        JapaneseScoringOutcome.InvalidHand -> MaterialTheme.colorScheme.errorContainer to "✗ Not a valid 14-tile hand"
    }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(headline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            when (outcome) {
                is JapaneseScoringOutcome.Success -> {
                    val r = outcome.result
                    Text("${r.totalHan} han / ${r.fu} fu — ${r.limitTier}")
                    r.yaku.forEach { Text("${it.name}: ${it.han} han") }
                    Text("Would gain: ${r.totalGain()} pts", fontWeight = FontWeight.Bold)
                }
                JapaneseScoringOutcome.NoYaku -> Text("This hand has no yaku with the current flags, so it can't legally win — check your closed/open flag and situational flags.")
                JapaneseScoringOutcome.InvalidHand -> Text("These 14 tiles don't decompose into 4 sets + a pair (or seven pairs / thirteen orphans).")
            }
        }
    }
}

@Composable
private fun CheckResultCard(outcome: HongKongScoringOutcome, minFanToWin: Int) {
    val (containerColor, headline) = when (outcome) {
        is HongKongScoringOutcome.Success -> MaterialTheme.colorScheme.primaryContainer to "✓ Valid win"
        is HongKongScoringOutcome.BelowMinimumFan -> MaterialTheme.colorScheme.errorContainer to "✗ Not enough fan"
        HongKongScoringOutcome.InvalidHand -> MaterialTheme.colorScheme.errorContainer to "✗ Not a valid 14-tile hand"
    }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(headline, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            when (outcome) {
                is HongKongScoringOutcome.Success -> {
                    val r = outcome.result
                    Text("${r.totalFan} fan${if (r.isLimitHand) " (Limit hand)" else ""} — meets the $minFanToWin fan minimum")
                    r.fanBreakdown.forEach { Text("${it.name}: ${it.fan} fan") }
                    Text("Would gain: ${r.pointsPerPayer} pts per payer", fontWeight = FontWeight.Bold)
                }
                is HongKongScoringOutcome.BelowMinimumFan ->
                    Text("Only ${outcome.actualFan} fan — needs at least ${outcome.required} fan to win.")
                HongKongScoringOutcome.InvalidHand -> Text("These 14 tiles don't decompose into 4 sets + a pair (or seven pairs / thirteen orphans).")
            }
        }
    }
}
