package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjongapp.model.GameState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(
    state: GameState,
    onConfirm: (tenpaiSeats: Set<Int>) -> Unit,
    onCancel: () -> Unit
) {
    val tenpai = remember { mutableStateOf(setOf<Int>()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Exhaustive Draw") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (state.ruleSet == RuleSet.JAPANESE) {
                    "Select which players were tenpai (ready) when the wall ran out. Noten players pay tenpai players; the dealer keeps dealing only if they were tenpai."
                } else {
                    "Select which players were tenpai (ready). This only affects whether the dealer keeps dealing — Hong Kong rules exchange no points on a draw."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            state.players.forEach { player ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(
                        checked = player.seatIndex in tenpai.value,
                        onCheckedChange = { checked ->
                            tenpai.value = if (checked) tenpai.value + player.seatIndex else tenpai.value - player.seatIndex
                        }
                    )
                    Text(player.name)
                }
            }
            Button(onClick = { onConfirm(tenpai.value) }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirm Draw")
            }
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}
