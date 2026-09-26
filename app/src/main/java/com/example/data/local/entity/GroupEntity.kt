package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "Class", // Class, Team, Club, Workplace, Event
    val colorHex: String = "#3B82F6",
    val iconName: String = "group",
    val createdAt: Long = System.currentTimeMillis()
)
