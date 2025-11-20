package com.example.healthtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface StepDao {

    @Insert
    suspend fun insertFitSteps(entry: StepEntry)

    @Insert
    suspend fun insertPhoneSteps(entry: PhoneStepEntry)

    @Query("SELECT * FROM fit_steps_table ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentFitSteps(): List<StepEntry>

    @Query("SELECT * FROM phone_steps_table ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentPhoneSteps(): List<PhoneStepEntry>
}
