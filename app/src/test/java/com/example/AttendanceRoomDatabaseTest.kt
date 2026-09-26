package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.dao.AttendanceRecordDao
import com.example.data.local.dao.AttendanceSessionDao
import com.example.data.local.dao.GroupDao
import com.example.data.local.dao.MemberDao
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceRoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var memberDao: MemberDao
    private lateinit var sessionDao: AttendanceSessionDao
    private lateinit var recordDao: AttendanceRecordDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        groupDao = db.groupDao()
        memberDao = db.memberDao()
        sessionDao = db.attendanceSessionDao()
        recordDao = db.attendanceRecordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testMemberPersistenceAndRetrieval() = runBlocking {
        val groupId = groupDao.insertGroup(
            GroupEntity(name = "Math 101", category = "Class")
        )

        val memberId = memberDao.insertMember(
            MemberEntity(groupId = groupId, name = "Alice Johnson", identifier = "MTH-01")
        )

        val retrieved = memberDao.getMemberById(memberId)
        assertNotNull(retrieved)
        assertEquals("Alice Johnson", retrieved?.name)
        assertEquals("MTH-01", retrieved?.identifier)

        val membersInGroup = memberDao.getMembersByGroupId(groupId)
        assertEquals(1, membersInGroup.size)
    }

    @Test
    fun testAttendanceRecordAndStatsComputation() = runBlocking {
        val groupId = groupDao.insertGroup(GroupEntity(name = "Physics Lab", category = "Class"))
        val member1Id = memberDao.insertMember(MemberEntity(groupId = groupId, name = "Bob Smith", identifier = "PHY-01"))
        val member2Id = memberDao.insertMember(MemberEntity(groupId = groupId, name = "Carol Danvers", identifier = "PHY-02"))

        val sessionId = sessionDao.insertSession(
            AttendanceSessionEntity(
                groupId = groupId,
                date = "2026-09-24",
                totalCount = 2,
                presentCount = 1,
                absentCount = 1
            )
        )

        val records = listOf(
            AttendanceRecordEntity(
                sessionId = sessionId,
                groupId = groupId,
                memberId = member1Id,
                memberName = "Bob Smith",
                date = "2026-09-24",
                status = "PRESENT"
            ),
            AttendanceRecordEntity(
                sessionId = sessionId,
                groupId = groupId,
                memberId = member2Id,
                memberName = "Carol Danvers",
                date = "2026-09-24",
                status = "ABSENT"
            )
        )
        recordDao.insertRecords(records)

        val sessionRecords = recordDao.getRecordsForSession(sessionId)
        assertEquals(2, sessionRecords.size)

        // Check SQL-level statistics computation
        val bobStats = recordDao.getMemberStatsFlow(member1Id).first()
        assertNotNull(bobStats)
        assertEquals(1, bobStats?.totalSessions)
        assertEquals(1, bobStats?.presentCount)
        assertEquals(0, bobStats?.absentCount)
        assertEquals(100, bobStats?.attendancePercentage)

        val carolStats = recordDao.getMemberStatsFlow(member2Id).first()
        assertNotNull(carolStats)
        assertEquals(1, carolStats?.totalSessions)
        assertEquals(0, carolStats?.presentCount)
        assertEquals(1, carolStats?.absentCount)
        assertEquals(0, carolStats?.attendancePercentage)
    }

    @Test
    fun testMemberWithAttendanceHistoryRelation() = runBlocking {
        val groupId = groupDao.insertGroup(GroupEntity(name = "Design Team", category = "Team"))
        val memberId = memberDao.insertMember(MemberEntity(groupId = groupId, name = "Diana Prince"))

        val sessionId1 = sessionDao.insertSession(AttendanceSessionEntity(groupId = groupId, date = "2026-09-22"))
        val sessionId2 = sessionDao.insertSession(AttendanceSessionEntity(groupId = groupId, date = "2026-09-23"))

        recordDao.insertRecords(
            listOf(
                AttendanceRecordEntity(sessionId = sessionId1, groupId = groupId, memberId = memberId, memberName = "Diana Prince", date = "2026-09-22", status = "PRESENT"),
                AttendanceRecordEntity(sessionId = sessionId2, groupId = groupId, memberId = memberId, memberName = "Diana Prince", date = "2026-09-23", status = "LATE")
            )
        )

        val memberWithHistory = memberDao.getMemberWithHistoryFlow(memberId).first()
        assertNotNull(memberWithHistory)
        assertEquals(2, memberWithHistory?.attendanceRecords?.size)
        assertEquals(100, memberWithHistory?.attendanceRate) // 1 present + 1 late out of 2 = 100% active
    }

    @Test
    fun testAddEditRemoveMemberInGroup() = runBlocking {
        // 1. Create group
        val groupId = groupDao.insertGroup(GroupEntity(name = "Robotics Club", category = "Club"))

        // 2. Add members
        val member1 = MemberEntity(
            groupId = groupId,
            name = "Edward Elric",
            identifier = "ROBO-01",
            notes = "Hardware lead",
            avatarColorHex = "#3B82F6"
        )
        val member1Id = memberDao.insertMember(member1)
        val member2Id = memberDao.insertMember(
            MemberEntity(groupId = groupId, name = "Alphonse Elric", identifier = "ROBO-02")
        )

        var members = memberDao.getMembersByGroupId(groupId)
        assertEquals(2, members.size)

        // 3. Edit member details
        val storedMember = memberDao.getMemberById(member1Id)
        assertNotNull(storedMember)
        val updatedMember = storedMember!!.copy(
            name = "Edward Fullmetal",
            identifier = "ROBO-99",
            notes = "State Alchemist & Lead",
            avatarColorHex = "#F59E0B"
        )
        memberDao.updateMember(updatedMember)

        val retrievedUpdated = memberDao.getMemberById(member1Id)
        assertNotNull(retrievedUpdated)
        assertEquals("Edward Fullmetal", retrievedUpdated?.name)
        assertEquals("ROBO-99", retrievedUpdated?.identifier)
        assertEquals("State Alchemist & Lead", retrievedUpdated?.notes)
        assertEquals("#F59E0B", retrievedUpdated?.avatarColorHex)

        // 4. Remove member
        memberDao.deleteMember(retrievedUpdated!!)
        members = memberDao.getMembersByGroupId(groupId)
        assertEquals(1, members.size)
        assertEquals(member2Id, members[0].id)
        assertEquals("Alphonse Elric", members[0].name)
    }
}
