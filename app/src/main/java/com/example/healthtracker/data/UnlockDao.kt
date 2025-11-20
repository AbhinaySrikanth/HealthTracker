package com.example.healthtracker.data

import androidx.room.*

@Dao
interface UnlockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: UnlockEntry)

    @Query("SELECT * FROM unlock_table WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): UnlockEntry?

    @Query("UPDATE unlock_table SET unlocks = unlocks + 1 WHERE date = :date")
    suspend fun increment(date: String)
}
