package com.example.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_usage")
data class PhoneUsage(
    @PrimaryKey val day: String, // yyyy-MM-dd
    val unlocks: Int = 0,
    val screenTimeMillis: Long = 0L
)
