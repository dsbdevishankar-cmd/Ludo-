package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserStats::class, SavedGameEntity::class],
    version = 2,
    exportSchema = false
)
abstract class LudoDatabase : RoomDatabase() {
    abstract fun userStatsDao(): UserStatsDao
    abstract fun savedGameDao(): SavedGameDao

    companion object {
        @Volatile
        private var INSTANCE: LudoDatabase? = null

        fun getDatabase(context: Context): LudoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LudoDatabase::class.java,
                    "ludo_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
