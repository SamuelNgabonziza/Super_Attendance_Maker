package com.example.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity

data class SessionWithRecords(
    @Embedded
    val session: AttendanceSessionEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val records: List<AttendanceRecordEntity> = emptyList()
)
