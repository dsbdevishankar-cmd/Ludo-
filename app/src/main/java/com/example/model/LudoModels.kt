package com.example.model

import com.squareup.moshi.JsonClass

enum class LudoColor {
    GREEN, RED, YELLOW, BLUE
}

enum class DiceState {
    IDLE,
    ROLLING,
    ROLLED_WAITING_FOR_MOVE,
    NO_MOVE_AVAILABLE
}

enum class GameMode {
    LOCAL,     // Pass & Play
    AI_PLAY,   // Play with 3 smart bots
    MULTIPLAYER // Simulated real-time online multiplayer with dynamic voice chat visuals and reactions
}

@JsonClass(generateAdapter = true)
data class LudoToken(
    val id: Int, // 0..3
    val color: LudoColor,
    val step: Int = 0, // 0 = in Home Yard, 1..51 = track, 52..56 = home path, 57 = finished
    val isSafe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Player(
    val color: LudoColor,
    val name: String,
    val isBot: Boolean = false,
    val avatarIndex: Int = 0,
    val useCustomAvatar: Boolean = false,
    val customAvatarBg: String = "mystic",
    val customAvatarIcon: String = "star",
    val customAvatarIconColor: String = "white",
    val customAvatarBorder: String = "none",
    val speechBubble: String? = null,
    val activeVoiceVolume: Float = 0f, // Simulated voice level (0f to 1f)
    val isConnected: Boolean = true,
    val isSpeaking: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GameLog(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val color: LudoColor? = null
)

@JsonClass(generateAdapter = true)
data class LudoGameState(
    val id: String = "active_game",
    val mode: GameMode = GameMode.LOCAL,
    val players: List<Player> = emptyList(),
    val tokens: List<LudoToken> = emptyList(),
    val currentPlayerIndex: Int = 0,
    val diceValue: Int = 1,
    val diceState: DiceState = DiceState.IDLE,
    val consecutiveSixes: Int = 0,
    val winningSequence: List<LudoColor> = emptyList(),
    val logs: List<GameLog> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
) {
    val currentPlayer: Player
        get() = players[currentPlayerIndex]
}

object LudoBoardMapper {
    val outerTrack = listOf(
        Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5),
        Pair(5, 6), Pair(4, 6), Pair(3, 6), Pair(2, 6), Pair(1, 6), Pair(0, 6),
        Pair(0, 7),
        Pair(0, 8), Pair(1, 8), Pair(2, 8), Pair(3, 8), Pair(4, 8), Pair(5, 8),
        Pair(6, 9), Pair(6, 10), Pair(6, 11), Pair(6, 12), Pair(6, 13), Pair(6, 14),
        Pair(7, 14),
        Pair(8, 14), Pair(8, 13), Pair(8, 12), Pair(8, 11), Pair(8, 10), Pair(8, 9),
        Pair(9, 8), Pair(10, 8), Pair(11, 8), Pair(12, 8), Pair(13, 8), Pair(14, 8),
        Pair(14, 7),
        Pair(14, 6), Pair(13, 6), Pair(12, 6), Pair(11, 6), Pair(10, 6), Pair(9, 6),
        Pair(8, 5), Pair(8, 4), Pair(8, 3), Pair(8, 2), Pair(8, 1), Pair(8, 0),
        Pair(7, 0),
        Pair(6, 0)
    )

    // Star cells/safe squares
    val safeCells = setOf(
        Pair(6, 1),   // Green Start
        Pair(8, 2),   // Safe cell Left
        Pair(1, 8),   // Red Start
        Pair(2, 6),   // Safe cell Top
        Pair(8, 13),  // Yellow Start
        Pair(6, 12),  // Safe cell Right
        Pair(13, 6),  // Blue Start
        Pair(12, 8)   // Safe cell Bottom
    )

    fun getCoordinates(color: LudoColor, step: Int, tokenId: Int): Pair<Int, Int> {
        if (step == 0) {
            return getHomeYardCoordinates(color, tokenId)
        }
        if (step in 1..51) {
            val startIdx = when (color) {
                LudoColor.GREEN -> 0
                LudoColor.RED -> 13
                LudoColor.YELLOW -> 26
                LudoColor.BLUE -> 39
            }
            val trackIdx = (startIdx + (step - 1)) % 52
            return outerTrack[trackIdx]
        }
        if (step in 52..56) {
            val homeStep = step - 52
            return when (color) {
                LudoColor.GREEN -> Pair(7, 1 + homeStep)
                LudoColor.RED -> Pair(1 + homeStep, 7)
                LudoColor.YELLOW -> Pair(7, 13 - homeStep)
                LudoColor.BLUE -> Pair(13 - homeStep, 7)
            }
        }
        // Step 57 is inside center triangle
        return when (color) {
            LudoColor.GREEN -> Pair(7, 6)
            LudoColor.RED -> Pair(6, 7)
            LudoColor.YELLOW -> Pair(7, 8)
            LudoColor.BLUE -> Pair(8, 7)
        }
    }

    private fun getHomeYardCoordinates(color: LudoColor, tokenId: Int): Pair<Int, Int> {
        val offsets = listOf(
            Pair(2, 2), Pair(2, 3),
            Pair(3, 2), Pair(3, 3)
        )
        val offset = offsets[tokenId % 4]
        return when (color) {
            LudoColor.GREEN -> offset
            LudoColor.RED -> Pair(offset.first, offset.second + 9)
            LudoColor.YELLOW -> Pair(offset.first + 9, offset.second + 9)
            LudoColor.BLUE -> Pair(offset.first + 9, offset.second)
        }
    }
}
