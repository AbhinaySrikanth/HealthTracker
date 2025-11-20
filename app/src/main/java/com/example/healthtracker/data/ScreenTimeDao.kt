package com.example.healthtracker.data

import androidx.room.*

@Dao
interface ScreenTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ScreenTimeEntry)

    @Query("SELECT * FROM screen_time_table WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): ScreenTimeEntry?

    @Query("UPDATE screen_time_table SET seconds = seconds + :extra WHERE date = :date")
    suspend fun updateTime(date: String, extra: Long)
}
