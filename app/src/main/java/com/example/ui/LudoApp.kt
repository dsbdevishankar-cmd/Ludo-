package com.example.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameMode
import com.example.data.LudoDatabase
import com.example.data.LudoRepository
import androidx.compose.ui.platform.LocalContext

sealed interface Screen {
    object Main : Screen
    object Game : Screen
}

@Composable
fun LudoApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // Initialize Database and Repository
    val database = remember { LudoDatabase.getDatabase(context) }
    val repository = remember { LudoRepository(database) }
    
    // Initialize ViewModel
    val gameViewModel: GameViewModel = viewModel(
        factory = GameViewModel.Factory(repository)
    )
    
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    val gameState by gameViewModel.gameState.collectAsState()

    when (currentScreen) {
        Screen.Main -> {
            MainScreen(
                viewModel = gameViewModel,
                onStartGame = { mode ->
                    // For resume, if mode is chosen and match already exists in VM, we just transition
                    if (gameState.players.isNotEmpty() && gameState.mode == mode && gameState.winningSequence.isEmpty()) {
                        currentScreen = Screen.Game
                    } else {
                        gameViewModel.startNewGame(mode)
                        currentScreen = Screen.Game
                    }
                }
            )
        }
        Screen.Game -> {
            GameScreen(
                viewModel = gameViewModel,
                onBackToMenu = {
                    currentScreen = Screen.Main
                }
            )
        }
    }
}
