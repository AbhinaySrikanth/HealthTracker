package com.example.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fit_steps_table")
data class StepEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val steps: Int,
    val timestamp: Long = System.currentTimeMillis()
)
