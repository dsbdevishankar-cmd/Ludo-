package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "Ludo Master",
    val avatarIndex: Int = 0,
    val useCustomAvatar: Boolean = false,
    val customAvatarBg: String = "mystic",
    val customAvatarIcon: String = "star",
    val customAvatarIconColor: String = "white",
    val customAvatarBorder: String = "none",
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 1000,
    val selectedTheme: String = "classic",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val winStreak: Int = 0,
    val voiceChatEnabled: Boolean = true
)

@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val id: String = "active_game",
    val gameStateJson: String,
    val isGameActive: Boolean = false,
    val lastSaved: Long = System.currentTimeMillis()
)
