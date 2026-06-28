package com.example.ui

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LudoRepository
import com.example.data.UserStats
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

class GameViewModel(private val repository: LudoRepository) : ViewModel() {

    private val _gameState = MutableStateFlow(LudoGameState())
    val gameState: StateFlow<LudoGameState> = _gameState.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _userVoiceVolume = MutableStateFlow(0f)
    val userVoiceVolume: StateFlow<Float> = _userVoiceVolume.asStateFlow()

    // Matchmaking State
    private val _matchmakingState = MutableStateFlow<MatchmakingState>(MatchmakingState.Idle)
    val matchmakingState: StateFlow<MatchmakingState> = _matchmakingState.asStateFlow()

    // Mic recording state
    private var audioRecord: AudioRecord? = null
    private var isPollingMic = false

    init {
        // Collect user stats from repository
        viewModelScope.launch {
            repository.userStatsFlow.collect { stats ->
                _userStats.value = stats
            }
        }
        
        // Attempt to load any active saved game
        viewModelScope.launch {
            repository.savedGameFlow.firstOrNull()?.let { savedGame ->
                _gameState.value = savedGame
            }
        }

        // Start background tasks for simulated voice chat and chat responses in multiplayer mode
        startSimulationEngine()
    }

    // MATCHMAKING FOR SIMULATED ONLINE MULTIPLAYER
    sealed interface MatchmakingState {
        object Idle : MatchmakingState
        object Searching : MatchmakingState
        data class Found(val players: List<Player>) : MatchmakingState
    }

    fun startOnlineMatchmaking() {
        viewModelScope.launch {
            _matchmakingState.value = MatchmakingState.Searching
            addLog("Searching for real-time players...", null)
            delay(1500)
            
            val randomOpponents = listOf(
                Player(LudoColor.RED, getRandomOpponentName(), isBot = true, avatarIndex = Random.nextInt(1, 8)),
                Player(LudoColor.YELLOW, getRandomOpponentName(), isBot = true, avatarIndex = Random.nextInt(1, 8)),
                Player(LudoColor.BLUE, getRandomOpponentName(), isBot = true, avatarIndex = Random.nextInt(1, 8))
            )
            
            addLog("Players found! Configuring game room...", null)
            delay(1000)
            
            val localUser = createUserPlayer(LudoColor.GREEN)
            
            val allPlayers = listOf(localUser) + randomOpponents
            _matchmakingState.value = MatchmakingState.Found(allPlayers)
            
            // Start the multiplayer game
            startNewGame(GameMode.MULTIPLAYER, allPlayers)
            _matchmakingState.value = MatchmakingState.Idle
        }
    }

    private fun getRandomOpponentName(): String {
        val names = listOf(
            "NeonNinja", "LudoKing_99", "PawnCrusher", "DiceDeity", "CyberCrown", 
            "RetroRider", "CosmicChallenger", "StarStriker", "LuckyRoll", "TokenTamer"
        )
        return names.random() + "#" + Random.nextInt(1000, 9999)
    }

    // NEW GAME INITIALIZATION
    fun startNewGame(mode: GameMode, customPlayers: List<Player>? = null) {
        viewModelScope.launch {
            repository.clearSavedGame()
            val playersList = customPlayers ?: when (mode) {
                GameMode.LOCAL -> listOf(
                    createUserPlayer(LudoColor.GREEN, "Green Player (You)"),
                    Player(LudoColor.RED, "Red Player", isBot = false, avatarIndex = 1),
                    Player(LudoColor.YELLOW, "Yellow Player", isBot = false, avatarIndex = 2),
                    Player(LudoColor.BLUE, "Blue Player", isBot = false, avatarIndex = 3)
                )
                GameMode.AI_PLAY -> listOf(
                    createUserPlayer(LudoColor.GREEN, _userStats.value.nickname + " (You)"),
                    Player(LudoColor.RED, "Bot Red", isBot = true, avatarIndex = 4),
                    Player(LudoColor.YELLOW, "Bot Yellow", isBot = true, avatarIndex = 5),
                    Player(LudoColor.BLUE, "Bot Blue", isBot = true, avatarIndex = 6)
                )
                GameMode.MULTIPLAYER -> throw IllegalStateException("Multiplayer mode must pass opponent list")
            }

            // Create 16 starting tokens
            val tokensList = mutableListOf<LudoToken>()
            LudoColor.values().forEach { color ->
                for (id in 0..3) {
                    tokensList.add(LudoToken(id = id, color = color, step = 0))
                }
            }

            val freshState = LudoGameState(
                id = UUID.randomUUID().toString(),
                mode = mode,
                players = playersList,
                tokens = tokensList,
                currentPlayerIndex = 0,
                diceValue = 1,
                diceState = DiceState.IDLE,
                consecutiveSixes = 0,
                winningSequence = emptyList(),
                logs = listOf(GameLog(message = "Ludo game started in ${mode.name} mode!"))
            )

            _gameState.value = freshState
            repository.saveGame(freshState)
            
            // Trigger first turn if bot
            checkAndTriggerBotTurn()
        }
    }

    // GAME STATE ACTIONS
    fun rollDice() {
        val state = _gameState.value
        if (state.diceState != DiceState.IDLE) return

        viewModelScope.launch {
            _gameState.update { it.copy(diceState = DiceState.ROLLING) }
            
            // Dice roll visual rotation delay
            for (i in 1..6) {
                _gameState.update { it.copy(diceValue = Random.nextInt(1, 7)) }
                delay(100)
            }
            
            val rolledValue = Random.nextInt(1, 7)
            val currentTurnColor = state.currentPlayer.color
            
            addLog("${state.currentPlayer.name} rolled a $rolledValue!", currentTurnColor)

            val hasValid = hasValidMoves(currentTurnColor, rolledValue)
            
            _gameState.update {
                it.copy(
                    diceValue = rolledValue,
                    diceState = if (hasValid) DiceState.ROLLED_WAITING_FOR_MOVE else DiceState.NO_MOVE_AVAILABLE
                )
            }

            repository.saveGame(_gameState.value)

            if (!hasValid) {
                delay(1200)
                addLog("No possible moves for ${state.currentPlayer.name}!", currentTurnColor)
                passTurn()
            } else {
                // If bot/simulated player rolled, trigger automated bot move
                if (state.currentPlayer.isBot) {
                    delay(800)
                    triggerBotMove(rolledValue)
                }
            }
        }
    }

    fun moveToken(color: LudoColor, tokenId: Int) {
        val state = _gameState.value
        if (state.diceState != DiceState.ROLLED_WAITING_FOR_MOVE) return
        if (state.currentPlayer.color != color) return

        // Validate move
        val token = state.tokens.firstOrNull { it.color == color && it.id == tokenId } ?: return
        val roll = state.diceValue
        if (!isValidMove(token, roll)) return

        viewModelScope.launch {
            _gameState.update { it.copy(diceState = DiceState.ROLLING) } // Block interface during animation

            val startStep = token.step
            val endStep = token.step + roll

            // Step-by-step moving animation
            for (step in (startStep + 1)..endStep) {
                _gameState.update { current ->
                    val updatedTokens = current.tokens.map { t ->
                        if (t.color == color && t.id == tokenId) {
                            t.copy(step = step)
                        } else {
                            t
                        }
                    }
                    current.copy(tokens = updatedTokens)
                }
                delay(120) // Movement delay for gorgeous fluid step transitions
            }

            // Post-movement analysis
            val updatedTokensState = _gameState.value.tokens
            val finalToken = updatedTokensState.first { it.color == color && it.id == tokenId }
            val finalCoord = LudoBoardMapper.getCoordinates(color, finalToken.step, tokenId)

            var gotCapture = false
            var capturedColor: LudoColor? = null
            var capturedId = -1

            // Check Capture (only if on track and not safe)
            if (finalToken.step in 1..51 && !LudoBoardMapper.safeCells.contains(finalCoord)) {
                for (opponentToken in updatedTokensState) {
                    if (opponentToken.color != color && opponentToken.step in 1..51) {
                        val oppCoord = LudoBoardMapper.getCoordinates(opponentToken.color, opponentToken.step, opponentToken.id)
                        if (oppCoord == finalCoord) {
                            gotCapture = true
                            capturedColor = opponentToken.color
                            capturedId = opponentToken.id
                            break
                        }
                    }
                }
            }

            if (gotCapture && capturedColor != null) {
                addLog("${state.currentPlayer.name} CAPTURED ${capturedColor.name} Token #${capturedId + 1}!", color)
                
                // Animate captured token flying back home step-by-step
                val capToken = updatedTokensState.first { it.color == capturedColor && it.id == capturedId }
                for (step in (capToken.step - 1) downTo 0) {
                    _gameState.update { current ->
                        val updated = current.tokens.map { t ->
                            if (t.color == capturedColor && t.id == capturedId) t.copy(step = step) else t
                        }
                        current.copy(tokens = updated)
                    }
                    delay(30)
                }

                // Award coins for capture
                if (color == LudoColor.GREEN) {
                    val bonusCoins = 30
                    addLog("Bonus reward: +$bonusCoins Coins!", LudoColor.GREEN)
                    repository.saveUserStats(_userStats.value.copy(coins = _userStats.value.coins + bonusCoins))
                }
            }

            // Check Win Condition
            val allFinished = _gameState.value.tokens.filter { it.color == color }.all { it.step == 57 }
            var isNewWinner = false
            if (allFinished && !_gameState.value.winningSequence.contains(color)) {
                _gameState.update { current ->
                    current.copy(winningSequence = current.winningSequence + color)
                }
                addLog("✨ ${state.currentPlayer.name} HAS COMPLETED THEIR VOYAGE! ✨", color)
                isNewWinner = true
            }

            // Check overall game finish
            val totalPlayersCount = _gameState.value.players.size
            val finishedCount = _gameState.value.winningSequence.size

            if (finishedCount >= totalPlayersCount - 1 || (modeIsSinglePlayerFinished(isNewWinner, color))) {
                // Game Finished!
                addLog("🏆 Game Concluded! 🏆", null)
                val won = _gameState.value.winningSequence.firstOrNull() == LudoColor.GREEN
                val xpEarned = if (won) 150 else 40
                val coinsEarned = if (won) 200 else 50
                
                repository.updateStatsAfterMatch(won, xpEarned, coinsEarned)
                repository.clearSavedGame()
                
                _gameState.update { it.copy(diceState = DiceState.IDLE) }
                return@launch
            }

            // Determine next turn:
            // 6 gives another turn (up to 3 times) OR Capture gives another turn
            val rolledSix = roll == 6
            var nextPlayerIdx = state.currentPlayerIndex
            var consecutive = state.consecutiveSixes

            if (rolledSix && !gotCapture) {
                consecutive++
                if (consecutive >= 3) {
                    addLog("Three consecutive 6s! Turn forfeited.", color)
                    consecutive = 0
                    nextPlayerIdx = (state.currentPlayerIndex + 1) % state.players.size
                } else {
                    addLog("${state.currentPlayer.name} gets a bonus roll for throwing a 6!", color)
                }
            } else if (gotCapture) {
                consecutive = 0
                addLog("${state.currentPlayer.name} gets a bonus roll for capturing an opponent!", color)
            } else {
                consecutive = 0
                nextPlayerIdx = (state.currentPlayerIndex + 1) % state.players.size
            }

            // Ensure we skip finished players
            while (_gameState.value.winningSequence.contains(_gameState.value.players[nextPlayerIdx].color)) {
                nextPlayerIdx = (nextPlayerIdx + 1) % state.players.size
            }

            _gameState.update {
                it.copy(
                    currentPlayerIndex = nextPlayerIdx,
                    consecutiveSixes = consecutive,
                    diceState = DiceState.IDLE
                )
            }

            repository.saveGame(_gameState.value)
            
            // Move to next turn
            checkAndTriggerBotTurn()
        }
    }

    private fun modeIsSinglePlayerFinished(isNewWinner: Boolean, winningColor: LudoColor): Boolean {
        // In local or bot matches, if the human player wins or all humans win, we can end early or let it run.
        // Let's finish the game if the main user (GREEN) wins or finishes, to keep the UI engaging.
        return isNewWinner && winningColor == LudoColor.GREEN
    }

    private fun passTurn() {
        val state = _gameState.value
        var nextIdx = (state.currentPlayerIndex + 1) % state.players.size
        
        while (_gameState.value.winningSequence.contains(_gameState.value.players[nextIdx].color)) {
            nextIdx = (nextIdx + 1) % state.players.size
        }

        _gameState.update {
            it.copy(
                currentPlayerIndex = nextIdx,
                consecutiveSixes = 0,
                diceState = DiceState.IDLE
            )
        }
        
        viewModelScope.launch {
            repository.saveGame(_gameState.value)
            checkAndTriggerBotTurn()
        }
    }

    // BOT AI LOGIC
    private fun checkAndTriggerBotTurn() {
        val state = _gameState.value
        if (state.currentPlayer.isBot && state.winningSequence.size < state.players.size) {
            viewModelScope.launch {
                delay(1200) // Bot "thinking" time
                rollDice()
            }
        }
    }

    private fun triggerBotMove(roll: Int) {
        val state = _gameState.value
        val color = state.currentPlayer.color
        val botTokens = state.tokens.filter { it.color == color }
        val validTokens = botTokens.filter { isValidMove(it, roll) }

        if (validTokens.isEmpty()) {
            passTurn()
            return
        }

        // Bot AI Decision Making
        // 1. Capture priority: Is there any move that results in capturing an opponent?
        val captureMove = validTokens.firstOrNull { token ->
            val hypotheticalStep = token.step + roll
            if (hypotheticalStep in 1..51) {
                val endCoord = LudoBoardMapper.getCoordinates(color, hypotheticalStep, token.id)
                if (!LudoBoardMapper.safeCells.contains(endCoord)) {
                    state.tokens.any { opp -> opp.color != color && opp.step in 1..51 && LudoBoardMapper.getCoordinates(opp.color, opp.step, opp.id) == endCoord }
                } else false
            } else false
        }

        if (captureMove != null) {
            moveToken(color, captureMove.id)
            return
        }

        // 2. Unlock Priority: If we rolled a 6, and have tokens in yard, release a token!
        if (roll == 6) {
            val lockedToken = validTokens.firstOrNull { it.step == 0 }
            if (lockedToken != null) {
                moveToken(color, lockedToken.id)
                return
            }
        }

        // 3. Safety/Home path priority: Prefer tokens that can enter safe houses or are close to scoring
        val homePathMove = validTokens.firstOrNull { token ->
            token.step in 46..51 && token.step + roll >= 52
        }
        if (homePathMove != null) {
            moveToken(color, homePathMove.id)
            return
        }

        // 4. Score priority: Token closest to winning
        val furthestMove = validTokens.maxByOrNull { it.step }
        if (furthestMove != null) {
            moveToken(color, furthestMove.id)
            return
        }

        // Fallback: Random move
        moveToken(color, validTokens.random().id)
    }

    // UTILITIES
    fun isValidMove(token: LudoToken, roll: Int): Boolean {
        if (token.step == 0) return roll == 6
        return token.step + roll <= 57
    }

    private fun hasValidMoves(color: LudoColor, roll: Int): Boolean {
        val playerTokens = _gameState.value.tokens.filter { it.color == color }
        return playerTokens.any { isValidMove(it, roll) }
    }

    private fun addLog(message: String, color: LudoColor?) {
        _gameState.update { current ->
            val updatedLogs = (listOf(GameLog(message = message, color = color)) + current.logs).take(100)
            current.copy(logs = updatedLogs)
        }
    }

    // MULTIPLAYER CHAT / SPEECH SIMULATION
    fun sendLocalQuickChat(phrase: String) {
        viewModelScope.launch {
            // Display speech bubble for local player
            _gameState.update { current ->
                val updatedPlayers = current.players.map { p ->
                    if (p.color == LudoColor.GREEN) p.copy(speechBubble = phrase) else p
                }
                current.copy(players = updatedPlayers)
            }
            addLog("${_userStats.value.nickname} (You): $phrase", LudoColor.GREEN)
            
            delay(3000)
            // Remove speech bubble after 3 seconds
            _gameState.update { current ->
                val updatedPlayers = current.players.map { p ->
                    if (p.color == LudoColor.GREEN && p.speechBubble == phrase) p.copy(speechBubble = null) else p
                }
                current.copy(players = updatedPlayers)
            }
        }
    }

    private fun startSimulationEngine() {
        viewModelScope.launch {
            while (true) {
                delay(Random.nextLong(10000, 20000)) // Periodically trigger chat or speech indicators
                val state = _gameState.value
                
                if (state.mode == GameMode.MULTIPLAYER && state.winningSequence.size < state.players.size) {
                    val activeOpponents = state.players.filter { it.isBot && it.isConnected }
                    if (activeOpponents.isNotEmpty()) {
                        val selectedOpp = activeOpponents.random()
                        
                        // Action 1: Voice Chat Speaking Indicator simulation
                        if (Random.nextBoolean() && _userStats.value.voiceChatEnabled) {
                            simulateOpponentVoiceSpeaking(selectedOpp)
                        } else {
                            // Action 2: Quick Chat text bubble simulation
                            simulateOpponentQuickChat(selectedOpp)
                        }
                    }
                }
            }
        }
    }

    private suspend fun simulateOpponentVoiceSpeaking(player: Player) {
        addLog("🔊 ${player.name} is speaking...", player.color)
        
        // Let them speak for 2-4 seconds with fluctuating mic waves
        for (i in 1..25) {
            _gameState.update { current ->
                val updated = current.players.map { p ->
                    if (p.color == player.color) {
                        p.copy(
                            isSpeaking = true,
                            activeVoiceVolume = Random.nextFloat() * 0.8f + 0.2f
                        )
                    } else p
                }
                current.copy(players = updated)
            }
            delay(120)
        }
        
        // Stop speaking
        _gameState.update { current ->
            val updated = current.players.map { p ->
                if (p.color == player.color) {
                    p.copy(
                        isSpeaking = false,
                        activeVoiceVolume = 0f
                    )
                } else p
            }
            current.copy(players = updated)
        }
    }

    private suspend fun simulateOpponentQuickChat(player: Player) {
        val phrases = listOf(
            "Roll a 6 please! 🙏", "Oops! Hard luck!", "Good game everyone!", 
            "Wow! Nice move!", "Oh no! 😮", "Hurry up!", "Haha! Classic!", "Yes!!"
        )
        val phrase = phrases.random()
        
        _gameState.update { current ->
            val updated = current.players.map { p ->
                if (p.color == player.color) p.copy(speechBubble = phrase) else p
            }
            current.copy(players = updated)
        }
        addLog("${player.name}: $phrase", player.color)
        
        delay(3000)
        
        _gameState.update { current ->
            val updated = current.players.map { p ->
                if (p.color == player.color && p.speechBubble == phrase) p.copy(speechBubble = null) else p
            }
            current.copy(players = updated)
        }
    }

    // LOCAL USER MIC ANALYSIS
    fun toggleLocalVoiceChat(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveUserStats(_userStats.value.copy(voiceChatEnabled = enabled))
            if (!enabled) {
                stopMicPolling()
            }
        }
    }

    fun startMicPolling(context: Context) {
        if (isPollingMic) return
        
        // Double check permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            val minBufSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufSize <= 0) return

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize
            )

            audioRecord?.startRecording()
            isPollingMic = true

            viewModelScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(minBufSize)
                while (isPollingMic && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var max = 0
                        for (i in 0 until readSize) {
                            val absVal = abs(buffer[i].toInt())
                            if (absVal > max) max = absVal
                        }
                        val level = (max / 32768f).coerceIn(0f, 1f)
                        withContext(Dispatchers.Main) {
                            _userVoiceVolume.value = level
                            updateLocalPlayerSpeaking(level > 0.05f, level)
                        }
                    }
                    delay(80)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMicPolling() {
        isPollingMic = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        _userVoiceVolume.value = 0f
        updateLocalPlayerSpeaking(false, 0f)
    }

    private fun updateLocalPlayerSpeaking(speaking: Boolean, volume: Float) {
        _gameState.update { current ->
            val updatedPlayers = current.players.map { p ->
                if (p.color == LudoColor.GREEN) {
                    p.copy(
                        isSpeaking = speaking,
                        activeVoiceVolume = volume
                    )
                } else p
            }
            current.copy(players = updatedPlayers)
        }
    }

    // THEME SHOP & CUSTOMIZATION
    fun selectTheme(themeName: String, price: Int, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            val success = repository.buyOrSelectTheme(themeName, price)
            if (success) {
                onSuccess()
            } else {
                onError()
            }
        }
    }

    fun updateProfile(
        nickname: String,
        avatarIndex: Int,
        useCustomAvatar: Boolean = _userStats.value.useCustomAvatar,
        customAvatarBg: String = _userStats.value.customAvatarBg,
        customAvatarIcon: String = _userStats.value.customAvatarIcon,
        customAvatarIconColor: String = _userStats.value.customAvatarIconColor,
        customAvatarBorder: String = _userStats.value.customAvatarBorder
    ) {
        viewModelScope.launch {
            val updated = _userStats.value.copy(
                nickname = nickname,
                avatarIndex = avatarIndex,
                useCustomAvatar = useCustomAvatar,
                customAvatarBg = customAvatarBg,
                customAvatarIcon = customAvatarIcon,
                customAvatarIconColor = customAvatarIconColor,
                customAvatarBorder = customAvatarBorder
            )
            repository.saveUserStats(updated)
            
            // Sync user details inside active game if active
            _gameState.update { current ->
                val updatedPlayers = current.players.map { p ->
                    if (p.color == LudoColor.GREEN) {
                        p.copy(
                            name = nickname + " (You)",
                            avatarIndex = avatarIndex,
                            useCustomAvatar = useCustomAvatar,
                            customAvatarBg = customAvatarBg,
                            customAvatarIcon = customAvatarIcon,
                            customAvatarIconColor = customAvatarIconColor,
                            customAvatarBorder = customAvatarBorder
                        )
                    } else p
                }
                current.copy(players = updatedPlayers)
            }
        }
    }

    private fun createUserPlayer(color: LudoColor, name: String = _userStats.value.nickname): Player {
        val stats = _userStats.value
        return Player(
            color = color,
            name = name,
            isBot = false,
            avatarIndex = stats.avatarIndex,
            useCustomAvatar = stats.useCustomAvatar,
            customAvatarBg = stats.customAvatarBg,
            customAvatarIcon = stats.customAvatarIcon,
            customAvatarIconColor = stats.customAvatarIconColor,
            customAvatarBorder = stats.customAvatarBorder
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopMicPolling()
    }

    // VIEWMODEL FACTORY PROTOCOL
    class Factory(private val repository: LudoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                return GameViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
