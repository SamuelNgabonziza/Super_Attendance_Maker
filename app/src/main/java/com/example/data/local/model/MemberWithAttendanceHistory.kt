package com.example.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.MemberEntity

data class MemberWithAttendanceHistory(
    @Embedded
    val member: MemberEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "memberId"
    )
    val attendanceRecords: List<AttendanceRecordEntity> = emptyList()
) {
    val totalRecords: Int get() = attendanceRecords.size
    val presentCount: Int get() = attendanceRecords.count { it.status == "PRESENT" }
    val absentCount: Int get() = attendanceRecords.count { it.status == "ABSENT" }
    val lateCount: Int get() = attendanceRecords.count { it.status == "LATE" }
    val excusedCount: Int get() = attendanceRecords.count { it.status == "EXCUSED" }

    val attendanceRate: Int
        get() = if (totalRecords > 0) {
            ((presentCount + lateCount) * 100) / totalRecords
        } else {
            0
        }
}
