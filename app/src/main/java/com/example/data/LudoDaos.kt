package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)
}

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_game WHERE id = 'active_game'")
    fun getSavedGameFlow(): Flow<SavedGameEntity?>

    @Query("SELECT * FROM saved_game WHERE id = 'active_game'")
    suspend fun getSavedGame(): SavedGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(game: SavedGameEntity)

    @Query("DELETE FROM saved_game WHERE id = 'active_game'")
    suspend fun deleteSavedGame()
}
