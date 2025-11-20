package com.example.healthtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HeartRateEntry::class,
        StepEntry::class,
        PhoneStepEntry::class,
        UnlockEntry::class,
        ScreenTimeEntry::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun heartRateDao(): HeartRateDao
    abstract fun stepDao(): StepDao
    abstract fun unlockDao(): UnlockDao
    abstract fun screenTimeDao(): ScreenTimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
