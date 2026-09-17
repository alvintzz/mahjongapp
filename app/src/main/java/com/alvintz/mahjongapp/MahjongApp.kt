package com.alvintz.mahjongapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alvintz.mahjong.scoring.RuleSet
import com.alvintz.mahjongapp.data.GameRepository
import com.alvintz.mahjongapp.data.SettingsRepository
import com.alvintz.mahjongapp.ui.screens.CheckWinningTileScreen
import com.alvintz.mahjongapp.ui.screens.DoraEditScreen
import com.alvintz.mahjongapp.ui.screens.DrawScreen
import com.alvintz.mahjongapp.ui.screens.HistoryScreen
import com.alvintz.mahjongapp.ui.screens.HomeScreen
import com.alvintz.mahjongapp.ui.screens.PlayerSetupScreen
import com.alvintz.mahjongapp.ui.screens.RoundScreen
import com.alvintz.mahjongapp.ui.screens.RuleSelectScreen
import com.alvintz.mahjongapp.ui.screens.SettingsScreen
import com.alvintz.mahjongapp.ui.screens.WinEntryScreen
import com.alvintz.mahjongapp.viewmodel.GameViewModel
import com.alvintz.mahjongapp.viewmodel.GameViewModelFactory

@Composable
fun MahjongApp(repository: GameRepository, settingsRepository: SettingsRepository) {
    val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(repository))
    val navController = rememberNavController()

    val loading by viewModel.loading.collectAsState()
    val game by viewModel.game.collectAsState()

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Navigate to the round screen whenever a (new or resumed) game becomes current - covers both
    // resuming into an in-progress game after the app was closed mid-round, and jumping straight
    // into "round" once startNewGame()'s async DB insert actually completes.
    LaunchedEffect(game?.gameId) {
        if (game != null && navController.currentDestination?.route != "round") {
            navController.navigate("round") {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNewGame = { navController.navigate("rule_select") },
                onHistory = { navController.navigate("history") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable("rule_select") {
            RuleSelectScreen(onSelected = { ruleSet -> navController.navigate("player_setup/${ruleSet.name}") })
        }
        composable("player_setup/{ruleSet}") { backStackEntry ->
            val ruleSet = RuleSet.valueOf(backStackEntry.arguments?.getString("ruleSet") ?: RuleSet.JAPANESE.name)
            PlayerSetupScreen(
                ruleSet = ruleSet,
                onSubmit = { names, startingScore ->
                    // Navigation to "round" happens reactively once the new game's async DB
                    // insert completes and `game` becomes non-null (see LaunchedEffect above).
                    viewModel.startNewGame(
                        ruleSet = ruleSet,
                        playerNames = names,
                        startingScore = startingScore,
                        hkMinFanToWin = settingsRepository.hkMinFanToWin.value
                    )
                }
            )
        }
        composable("round") {
            val current = game
            if (current == null) {
                navigateHomeOnce(navController)
            } else {
                RoundScreen(
                    state = current,
                    onWin = { seat -> navController.navigate("win_entry/$seat") },
                    onCheckWinningTile = { navController.navigate("check_winning_tile") },
                    onDraw = { navController.navigate("draw") },
                    onDeclareRiichi = { seat -> viewModel.declareRiichi(seat) },
                    onEditDora = { navController.navigate("dora_edit") },
                    onEndGame = {
                        viewModel.endGame()
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    }
                )
            }
        }
        composable("check_winning_tile") {
            val current = game
            if (current == null) {
                navigateHomeOnce(navController)
            } else {
                CheckWinningTileScreen(
                    state = current,
                    viewModel = viewModel,
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable("win_entry/{seat}") { backStackEntry ->
            val seat = backStackEntry.arguments?.getString("seat")?.toIntOrNull() ?: 0
            val current = game
            if (current == null) {
                navigateHomeOnce(navController)
            } else {
                WinEntryScreen(
                    state = current,
                    winnerSeat = seat,
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable("draw") {
            val current = game
            if (current == null) {
                navigateHomeOnce(navController)
            } else {
                DrawScreen(
                    state = current,
                    onConfirm = { tenpaiSeats ->
                        viewModel.recordDraw(tenpaiSeats)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable("dora_edit") {
            val current = game
            if (current == null) {
                navigateHomeOnce(navController)
            } else {
                DoraEditScreen(
                    initial = current.doraIndicators,
                    onConfirm = { indicators ->
                        viewModel.setDoraIndicators(indicators)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable("history") {
            val finishedGames by repository.observeFinishedGames().collectAsState(initial = emptyList())
            HistoryScreen(games = finishedGames)
        }
    }
}

@Composable
private fun navigateHomeOnce(navController: NavHostController) {
    LaunchedEffect(Unit) {
        navController.navigate("home") { popUpTo("home") { inclusive = true } }
    }
}
