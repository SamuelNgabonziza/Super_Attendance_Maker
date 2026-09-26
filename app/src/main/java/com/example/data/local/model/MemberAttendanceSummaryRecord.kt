package com.example.data.local.model

data class MemberAttendanceSummaryRecord(
    val memberId: Long,
    val memberName: String,
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int
) {
    val attendancePercentage: Int
        get() = if (totalSessions > 0) {
            ((presentCount + lateCount) * 100) / totalSessions
        } else {
            0
        }
}
