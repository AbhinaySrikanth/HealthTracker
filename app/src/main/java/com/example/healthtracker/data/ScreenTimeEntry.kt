package com.example.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time_table")
data class ScreenTimeEntry(
    @PrimaryKey val date: String,
    val seconds: Long
)
