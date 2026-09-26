package com.example.data.model

enum class AttendanceStatus(val label: String, val shortCode: String) {
    PRESENT("Present", "P"),
    ABSENT("Absent", "A"),
    LATE("Late", "L"),
    EXCUSED("Excused", "E")
}

data class Group(
    val id: String,
    val name: String,
    val description: String = "",
    val schedule: String = "Mon, Wed, Fri",
    val colorHex: String = "#1E3A8A",
    val passingThreshold: Int = 75, // Percentage required
    val createdAt: Long = System.currentTimeMillis()
)

data class Member(
    val id: String,
    val groupId: String,
    val fullName: String,
    val identifier: String = "", // e.g. Student ID, Roll No, or Email
    val phone: String = "",
    val role: String = "Student", // Student, Member, Lead, Staff, Guest
    val notes: String = "",
    val avatarColor: String = "#1E3A8A"
)

data class MemberAttendanceRecord(
    val memberId: String,
    val status: AttendanceStatus,
    val note: String = "",
    val checkInTime: String = ""
)

data class AttendanceSession(
    val id: String,
    val groupId: String,
    val sessionName: String,
    val dateMillis: Long,
    val sessionType: String = "Lecture", // Lecture, Lab, Exam, Meeting, Workshop, Practice
    val notes: String = "",
    val records: Map<String, MemberAttendanceRecord> = emptyMap()
)

data class GroupAnalytics(
    val totalSessions: Int,
    val totalMembers: Int,
    val averageAttendanceRate: Float, // 0.0 to 1.0
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val atRiskCount: Int
)

data class MemberAnalytics(
    val member: Member,
    val totalSessions: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val attendanceRate: Float,
    val currentStreak: Int,
    val isAtRisk: Boolean
)
