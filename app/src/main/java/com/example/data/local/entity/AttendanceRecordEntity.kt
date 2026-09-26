package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = AttendanceSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["memberId"]),
        Index(value = ["groupId"]),
        Index(value = ["sessionId", "memberId"], unique = true)
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val groupId: Long,
    val memberId: Long,
    val memberName: String,
    val date: String, // format "yyyy-MM-dd"
    val status: String, // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
