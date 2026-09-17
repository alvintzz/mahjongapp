package com.alvintz.mahjongapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvintz.mahjong.scoring.RuleSet
import androidx.compose.foundation.clickable

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RuleSelectScreen(onSelected: (RuleSet) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Choose Scoring Method") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RuleOptionCard(
                title = "Japanese (Riichi)",
                description = "Han/fu scoring, riichi sticks, dora, honba, dealer repeats. Starts at 25,000 points.",
                onClick = { onSelected(RuleSet.JAPANESE) }
            )
            RuleOptionCard(
                title = "Hong Kong",
                description = "Fan scoring, doubling points table. Starts at 0 points.",
                onClick = { onSelected(RuleSet.HONG_KONG) }
            )
        }
    }
}

@Composable
private fun RuleOptionCard(title: String, description: String, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
