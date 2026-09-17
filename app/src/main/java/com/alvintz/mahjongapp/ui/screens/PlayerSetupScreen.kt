package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvintz.mahjong.scoring.RuleSet

private val windLabels = listOf("East (Dealer)", "South", "West", "North")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSetupScreen(
    ruleSet: RuleSet,
    onSubmit: (names: List<String>, startingScore: Int) -> Unit
) {
    val names = remember { mutableStateOf(listOf("", "", "", "")) }
    val startingScore = if (ruleSet == RuleSet.JAPANESE) 25000 else 0

    Scaffold(topBar = { TopAppBar(title = { Text("Enter Player Names") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Each player starts with $startingScore points. Seating below sets who deals first (East).",
                style = MaterialTheme.typography.bodyMedium
            )
            windLabels.forEachIndexed { index, label ->
                OutlinedTextField(
                    value = names.value[index],
                    onValueChange = { new -> names.value = names.value.toMutableList().also { it[index] = new } },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    val finalNames = names.value.mapIndexed { i, n -> n.ifBlank { windLabels[i].substringBefore(" ") } }
                    onSubmit(finalNames, startingScore)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Game")
            }
        }
    }
}
