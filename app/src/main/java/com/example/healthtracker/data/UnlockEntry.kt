package com.example.healthtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlock_table")
data class UnlockEntry(
    @PrimaryKey val date: Long,   // yyyy-MM-dd
    val unlocks: Int
)
