package com.example.data.repository

import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AttendanceRecordDao
import com.example.data.local.dao.MemberDao
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceRepository(
    private val dao: AttendanceDao,
    private val memberDao: MemberDao,
    private val recordDao: AttendanceRecordDao
) {

    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroupsFlow()
    val totalGroupCount: Flow<Int> = dao.getTotalGroupCountFlow()
    val totalMemberCount: Flow<Int> = dao.getTotalMemberCountFlow()
    val recentActivities: Flow<List<ActivityLogEntity>> = dao.getRecentActivitiesFlow(20)
    val allSessions: Flow<List<AttendanceSessionEntity>> = dao.getAllSessionsFlow()

    suspend fun getGroupById(groupId: Long): GroupEntity? = dao.getGroupById(groupId)

    suspend fun getMembersForGroup(groupId: Long): List<MemberEntity> = memberDao.getMembersByGroupId(groupId)

    fun getMembersForGroupFlow(groupId: Long): Flow<List<MemberEntity>> = memberDao.getMembersByGroupIdFlow(groupId)

    suspend fun getSessionByGroupAndDate(groupId: Long, date: String): AttendanceSessionEntity? =
        dao.getSessionByGroupAndDate(groupId, date)

    suspend fun getRecordsForSession(sessionId: Long): List<AttendanceRecordEntity> =
        recordDao.getRecordsForSession(sessionId)

    fun getRecordsForGroupFlow(groupId: Long): Flow<List<AttendanceRecordEntity>> =
        recordDao.getRecordsForGroupFlow(groupId)

    suspend fun getRecordsForGroup(groupId: Long): List<AttendanceRecordEntity> =
        recordDao.getRecordsForGroupFlow(groupId).first()

    suspend fun getSessionsForGroup(groupId: Long): List<AttendanceSessionEntity> =
        dao.getSessionsForGroupFlow(groupId).first()

    fun getSessionsForGroupFlow(groupId: Long): Flow<List<AttendanceSessionEntity>> =
        dao.getSessionsForGroupFlow(groupId)

    suspend fun saveAttendanceSession(
        groupId: Long,
        groupName: String,
        date: String,
        records: List<AttendanceRecordEntity>,
        sessionNotes: String
    ): Long {
        val total = records.size
        val present = records.count { it.status == "PRESENT" }
        val absent = records.count { it.status == "ABSENT" }
        val late = records.count { it.status == "LATE" }
        val excused = records.count { it.status == "EXCUSED" }

        val session = AttendanceSessionEntity(
            groupId = groupId,
            date = date,
            timestamp = System.currentTimeMillis(),
            totalCount = total,
            presentCount = present,
            absentCount = absent,
            lateCount = late,
            excusedCount = excused,
            notes = sessionNotes
        )

        val savedSessionId = dao.saveFullAttendanceSession(session, records)

        dao.insertActivityLog(
            ActivityLogEntity(
                title = "Roll Call Saved: $groupName",
                description = "$present present, $late late, $absent absent on $date",
                type = "CHECK_IN"
            )
        )

        return savedSessionId
    }

    suspend fun createGroup(
        name: String,
        description: String,
        category: String,
        colorHex: String
    ): Long {
        val group = GroupEntity(
            name = name,
            description = description,
            category = category,
            colorHex = colorHex
        )
        val id = dao.insertGroup(group)
        dao.insertActivityLog(
            ActivityLogEntity(
                title = "New Group Created",
                description = "Group '$name' ($category) was created",
                type = "GROUP_CREATED"
            )
        )
        return id
    }

    suspend fun updateGroup(group: GroupEntity) {
        dao.updateGroup(group)
    }

    suspend fun deleteGroup(group: GroupEntity) {
        dao.deleteGroup(group)
        dao.insertActivityLog(
            ActivityLogEntity(
                title = "Group Removed",
                description = "Group '${group.name}' and related records were deleted",
                type = "RESET"
            )
        )
    }

    suspend fun addMember(
        groupId: Long,
        name: String,
        identifier: String,
        notes: String,
        avatarColorHex: String
    ): Long {
        val member = MemberEntity(
            groupId = groupId,
            name = name,
            identifier = identifier,
            notes = notes,
            avatarColorHex = avatarColorHex
        )
        val id = memberDao.insertMember(member)
        dao.insertActivityLog(
            ActivityLogEntity(
                title = "Member Added",
                description = "Added $name ($identifier) to roster",
                type = "MEMBER_ADDED"
            )
        )
        return id
    }

    suspend fun insertMembers(members: List<MemberEntity>): List<Long> {
        val ids = memberDao.insertMembers(members)
        dao.insertActivityLog(
            ActivityLogEntity(
                title = "Batch Members Added",
                description = "Imported ${members.size} members to group roster",
                type = "MEMBER_ADDED"
            )
        )
        return ids
    }

    suspend fun updateMember(member: MemberEntity) {
        memberDao.updateMember(member)
        dao.updateMemberNameInRecords(member.id, member.name)
    }

    suspend fun deleteMember(member: MemberEntity) {
        memberDao.deleteMember(member)
    }

    suspend fun seedSampleDataIfEmpty() {
        val existingGroups = dao.getAllGroupsFlow().first()
        if (existingGroups.isNotEmpty()) return

        val csGroup = GroupEntity(
            name = "CS401: Mobile Architecture",
            description = "Advanced Android & Kotlin system design lectures and hands-on lab",
            category = "Class",
            colorHex = "#2563EB",
            iconName = "computer"
        )
        val bioGroup = GroupEntity(
            name = "Bio 204: Molecular Biology",
            description = "Microscopy practicals, lab assays and genetics seminar",
            category = "Laboratory",
            colorHex = "#059669",
            iconName = "science"
        )
        val designGroup = GroupEntity(
            name = "Design Sprint: UI/UX Studio",
            description = "Product discovery, prototyping, and accessibility critique",
            category = "Workshop",
            colorHex = "#7C3AED",
            iconName = "palette"
        )
        val trackGroup = GroupEntity(
            name = "Varsity Track & Athletics",
            description = "Morning conditioning, sprint drills, and tournament prep",
            category = "Team",
            colorHex = "#D97706",
            iconName = "sports"
        )

        val csId = dao.insertGroup(csGroup)
        val bioId = dao.insertGroup(bioGroup)
        val designId = dao.insertGroup(designGroup)
        val trackId = dao.insertGroup(trackGroup)

        // Seed CS Members
        val csStudents = listOf(
            MemberEntity(groupId = csId, name = "Alexander Chen", identifier = "CS-2024-001", avatarColorHex = "#3B82F6"),
            MemberEntity(groupId = csId, name = "Maya Lin", identifier = "CS-2024-002", avatarColorHex = "#10B981"),
            MemberEntity(groupId = csId, name = "David Kim", identifier = "CS-2024-003", avatarColorHex = "#F59E0B"),
            MemberEntity(groupId = csId, name = "Sarah Jenkins", identifier = "CS-2024-004", avatarColorHex = "#8B5CF6"),
            MemberEntity(groupId = csId, name = "Elena Rostova", identifier = "CS-2024-005", avatarColorHex = "#EC4899"),
            MemberEntity(groupId = csId, name = "Marcus Brody", identifier = "CS-2024-006", avatarColorHex = "#06B6D4"),
            MemberEntity(groupId = csId, name = "Lucas Silva", identifier = "CS-2024-007", avatarColorHex = "#14B8A6"),
            MemberEntity(groupId = csId, name = "Aaliyah Patel", identifier = "CS-2024-008", avatarColorHex = "#F97316"),
            MemberEntity(groupId = csId, name = "Noah Becker", identifier = "CS-2024-009", avatarColorHex = "#6366F1"),
            MemberEntity(groupId = csId, name = "Chloe Zhao", identifier = "CS-2024-010", avatarColorHex = "#84CC16")
        )
        val csMemberIds = memberDao.insertMembers(csStudents)

        // Seed Bio Members
        val bioStudents = listOf(
            MemberEntity(groupId = bioId, name = "Olivia Martinez", identifier = "BIO-8801", avatarColorHex = "#059669"),
            MemberEntity(groupId = bioId, name = "Ethan Wright", identifier = "BIO-8802", avatarColorHex = "#10B981"),
            MemberEntity(groupId = bioId, name = "Sophia Nakamura", identifier = "BIO-8803", avatarColorHex = "#34D399"),
            MemberEntity(groupId = bioId, name = "Liam O'Connor", identifier = "BIO-8804", avatarColorHex = "#047857"),
            MemberEntity(groupId = bioId, name = "Amara Okafor", identifier = "BIO-8805", avatarColorHex = "#065F46")
        )
        memberDao.insertMembers(bioStudents)

        // Seed Design Members
        val designStudents = listOf(
            MemberEntity(groupId = designId, name = "Jordan Hayes", identifier = "DS-101", avatarColorHex = "#7C3AED"),
            MemberEntity(groupId = designId, name = "Camila Rodriguez", identifier = "DS-102", avatarColorHex = "#A855F7"),
            MemberEntity(groupId = designId, name = "Tyler Brooks", identifier = "DS-103", avatarColorHex = "#9333EA"),
            MemberEntity(groupId = designId, name = "Zoe Washington", identifier = "DS-104", avatarColorHex = "#C084FC")
        )
        memberDao.insertMembers(designStudents)

        // Seed Track Members
        val trackStudents = listOf(
            MemberEntity(groupId = trackId, name = "DeMarcus King", identifier = "ATH-01", avatarColorHex = "#D97706"),
            MemberEntity(groupId = trackId, name = "Hannah Scott", identifier = "ATH-02", avatarColorHex = "#F59E0B"),
            MemberEntity(groupId = trackId, name = "Gabriel Santos", identifier = "ATH-03", avatarColorHex = "#B45309")
        )
        memberDao.insertMembers(trackStudents)

        // Generate past sessions for CS401 so charts and analytics are immediately populated
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Past 3 sessions + Today
        for (daysAgo in 3 downTo 0) {
            val sessionCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val dateStr = sdf.format(sessionCal.time)

            val sessionRecords = csStudents.mapIndexed { idx, student ->
                val assignedId = csMemberIds.getOrElse(idx) { (idx + 1).toLong() }
                val status = when {
                    daysAgo == 0 && idx == 2 -> "LATE"
                    daysAgo == 0 && idx == 5 -> "ABSENT"
                    daysAgo == 1 && idx == 4 -> "EXCUSED"
                    daysAgo == 1 && idx == 7 -> "ABSENT"
                    daysAgo == 2 && idx == 3 -> "LATE"
                    daysAgo == 3 && idx == 8 -> "ABSENT"
                    else -> "PRESENT"
                }
                val note = when (status) {
                    "LATE" -> "10 mins late - transit delay"
                    "EXCUSED" -> "Medical note verified"
                    "ABSENT" -> "Unexcused"
                    else -> ""
                }
                AttendanceRecordEntity(
                    sessionId = 0,
                    groupId = csId,
                    memberId = assignedId,
                    memberName = student.name,
                    date = dateStr,
                    status = status,
                    note = note
                )
            }

            val present = sessionRecords.count { it.status == "PRESENT" }
            val absent = sessionRecords.count { it.status == "ABSENT" }
            val late = sessionRecords.count { it.status == "LATE" }
            val excused = sessionRecords.count { it.status == "EXCUSED" }

            val session = AttendanceSessionEntity(
                groupId = csId,
                date = dateStr,
                timestamp = sessionCal.timeInMillis,
                totalCount = sessionRecords.size,
                presentCount = present,
                absentCount = absent,
                lateCount = late,
                excusedCount = excused,
                notes = if (daysAgo == 0) "Lecture on Kotlin Flow & Jetpack Room caching" else "Weekly architectural review"
            )
            dao.saveFullAttendanceSession(session, sessionRecords)
        }

        dao.insertActivityLog(
            ActivityLogEntity(
                title = "Database Initialized",
                description = "AttendEase initialized with sample academic groups and rosters",
                type = "INFO"
            )
        )
    }
}
