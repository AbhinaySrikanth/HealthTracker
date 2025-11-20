package com.example.healthtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhoneUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: PhoneUsage)

    @Query("SELECT * FROM phone_usage WHERE day = :day LIMIT 1")
    suspend fun getForDay(day: String): PhoneUsage?

    @Query("UPDATE phone_usage SET unlocks = unlocks + :inc WHERE day = :day")
    suspend fun incrementUnlocks(day: String, inc: Int = 1)

    @Query("UPDATE phone_usage SET screenTimeMillis = screenTimeMillis + :millis WHERE day = :day")
    suspend fun addScreenTime(day: String, millis: Long)

    @Query("SELECT unlocks FROM phone_usage WHERE day = :day LIMIT 1")
    suspend fun getUnlocks(day: String): Int?

    @Query("SELECT screenTimeMillis FROM phone_usage WHERE day = :day LIMIT 1")
    suspend fun getScreenTimeMillis(day: String): Long?
}
