package com.example.data

import com.example.model.LudoGameState
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LudoRepository(private val database: LudoDatabase) {
    private val userStatsDao = database.userStatsDao()
    private val savedGameDao = database.savedGameDao()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val gameStateAdapter = moshi.adapter(LudoGameState::class.java)

    // User Stats Flow
    val userStatsFlow: Flow<UserStats> = userStatsDao.getUserStatsFlow()
        .map { it ?: UserStats() }
        .flowOn(Dispatchers.IO)

    // Saved Game Flow
    val savedGameFlow: Flow<LudoGameState?> = savedGameDao.getSavedGameFlow()
        .map { entity ->
            if (entity != null && entity.isGameActive) {
                try {
                    gameStateAdapter.fromJson(entity.gameStateJson)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
        .flowOn(Dispatchers.IO)

    // Get current user stats directly
    suspend fun getUserStats(): UserStats = withContext(Dispatchers.IO) {
        userStatsDao.getUserStats() ?: UserStats()
    }

    // Save user stats
    suspend fun saveUserStats(stats: UserStats) = withContext(Dispatchers.IO) {
        userStatsDao.insertUserStats(stats)
    }

    // Save active game
    suspend fun saveGame(gameState: LudoGameState) = withContext(Dispatchers.IO) {
        val json = gameStateAdapter.toJson(gameState)
        val entity = SavedGameEntity(
            gameStateJson = json,
            isGameActive = true,
            lastSaved = System.currentTimeMillis()
        )
        savedGameDao.saveGame(entity)
    }

    // Clear saved game
    suspend fun clearSavedGame() = withContext(Dispatchers.IO) {
        savedGameDao.deleteSavedGame()
    }

    // Utility to modify stats
    suspend fun updateStatsAfterMatch(won: Boolean, xpEarned: Int, coinsEarned: Int) = withContext(Dispatchers.IO) {
        val current = getUserStats()
        val newMatches = current.matchesPlayed + 1
        val newWins = if (won) current.matchesWon + 1 else current.matchesWon
        val newStreak = if (won) current.winStreak + 1 else 0
        
        var newXp = current.xp + xpEarned
        var newLevel = current.level
        val xpNeeded = newLevel * 200 // Simple leveling curve
        if (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel += 1
        }

        val updated = current.copy(
            matchesPlayed = newMatches,
            matchesWon = newWins,
            winStreak = newStreak,
            xp = newXp,
            level = newLevel,
            coins = current.coins + coinsEarned
        )
        saveUserStats(updated)
    }

    suspend fun buyOrSelectTheme(themeName: String, price: Int): Boolean = withContext(Dispatchers.IO) {
        val current = getUserStats()
        if (current.selectedTheme == themeName) {
            return@withContext true
        }
        if (current.coins >= price) {
            val updated = current.copy(
                coins = current.coins - price,
                selectedTheme = themeName
            )
            saveUserStats(updated)
            true
        } else {
            false
        }
    }
}
