package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Groups ---
    @Query("SELECT * FROM `groups` ORDER BY createdAt DESC")
    fun getAllGroupsFlow(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query("SELECT * FROM `groups` WHERE id = :id")
    fun getGroupByIdFlow(id: Long): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)

    @Query("SELECT COUNT(*) FROM `groups`")
    fun getTotalGroupCountFlow(): Flow<Int>

    // --- Members ---
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    fun getMembersByGroupIdFlow(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    suspend fun getMembersByGroupId(groupId: Long): List<MemberEntity>

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembersFlow(): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members")
    fun getTotalMemberCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM members WHERE groupId = :groupId")
    fun getMemberCountForGroupFlow(groupId: Long): Flow<Int>

    @Query("SELECT * FROM members WHERE id = :memberId")
    suspend fun getMemberById(memberId: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    @Query("UPDATE attendance_records SET memberName = :newName WHERE memberId = :memberId")
    suspend fun updateMemberNameInRecords(memberId: Long, newName: String)

    // --- Attendance Sessions ---
    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId ORDER BY date DESC, timestamp DESC")
    fun getSessionsForGroupFlow(groupId: Long): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC, timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE date = :date")
    fun getSessionsByDateFlow(date: String): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId AND date = :date LIMIT 1")
    suspend fun getSessionByGroupAndDate(groupId: Long, date: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): AttendanceSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity): Long

    @Update
    suspend fun updateSession(session: AttendanceSessionEntity)

    @Delete
    suspend fun deleteSession(session: AttendanceSessionEntity)

    // --- Attendance Records ---
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberName ASC")
    fun getRecordsForSessionFlow(sessionId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberName ASC")
    suspend fun getRecordsForSession(sessionId: Long): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE memberId = :memberId ORDER BY date DESC")
    fun getRecordsForMemberFlow(memberId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE groupId = :groupId ORDER BY date DESC")
    fun getRecordsForGroupFlow(groupId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>): List<Long>

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: Long)

    // Combined atomic save of session & records
    @Transaction
    suspend fun saveFullAttendanceSession(
        session: AttendanceSessionEntity,
        records: List<AttendanceRecordEntity>
    ): Long {
        // Check if session for group & date exists
        val existing = getSessionByGroupAndDate(session.groupId, session.date)
        val targetSessionId: Long
        if (existing != null) {
            val updatedSession = session.copy(id = existing.id)
            updateSession(updatedSession)
            deleteRecordsForSession(existing.id)
            targetSessionId = existing.id
        } else {
            targetSessionId = insertSession(session)
        }

        val updatedRecords = records.map { it.copy(sessionId = targetSessionId) }
        insertRecords(updatedRecords)
        return targetSessionId
    }

    // --- Activity Logs ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentActivitiesFlow(limit: Int = 20): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLogEntity): Long

    @Query("DELETE FROM activity_logs")
    suspend fun clearActivities()
}
