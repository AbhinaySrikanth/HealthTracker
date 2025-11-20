package com.example.healthtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HeartRateDao {

    @Insert
    suspend fun insert(entry: HeartRateEntry)

    @Query("SELECT * FROM heart_rate_table ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecent(): List<HeartRateEntry>
}
