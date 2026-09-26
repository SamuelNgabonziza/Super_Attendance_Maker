package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val type: String, // "CHECK_IN", "MEMBER_ADDED", "GROUP_CREATED", "RESET"
    val timestamp: Long = System.currentTimeMillis()
)
