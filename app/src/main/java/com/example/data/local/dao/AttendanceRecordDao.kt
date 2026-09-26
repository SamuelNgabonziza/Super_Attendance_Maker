package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.model.MemberAttendanceSummaryRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>): List<Long>

    @Update
    suspend fun updateRecord(record: AttendanceRecordEntity)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: Long)

    @Query("DELETE FROM attendance_records WHERE memberId = :memberId")
    suspend fun deleteRecordsForMember(memberId: Long)

    @Query("UPDATE attendance_records SET memberName = :newName WHERE memberId = :memberId")
    suspend fun updateMemberNameInRecords(memberId: Long, newName: String)

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberName ASC")
    suspend fun getRecordsForSession(sessionId: Long): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY memberName ASC")
    fun getRecordsForSessionFlow(sessionId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE memberId = :memberId ORDER BY date DESC")
    fun getRecordsForMemberFlow(memberId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE groupId = :groupId ORDER BY date DESC, memberName ASC")
    fun getRecordsForGroupFlow(groupId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY groupId ASC, memberName ASC")
    fun getRecordsByDateFlow(date: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE memberId = :memberId AND date = :date LIMIT 1")
    suspend fun getRecordForMemberAndDate(memberId: Long, date: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecordEntity>>

    // SQL-level attendance statistics computation per member
    @Query("""
        SELECT 
            memberId,
            memberName,
            COUNT(*) as totalSessions,
            SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount,
            SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absentCount,
            SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) as lateCount,
            SUM(CASE WHEN status = 'EXCUSED' THEN 1 ELSE 0 END) as excusedCount
        FROM attendance_records 
        WHERE memberId = :memberId
        GROUP BY memberId
    """)
    fun getMemberStatsFlow(memberId: Long): Flow<MemberAttendanceSummaryRecord?>

    // Aggregated stats for all members in a given group
    @Query("""
        SELECT 
            memberId,
            memberName,
            COUNT(*) as totalSessions,
            SUM(CASE WHEN status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount,
            SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) as absentCount,
            SUM(CASE WHEN status = 'LATE' THEN 1 ELSE 0 END) as lateCount,
            SUM(CASE WHEN status = 'EXCUSED' THEN 1 ELSE 0 END) as excusedCount
        FROM attendance_records 
        WHERE groupId = :groupId
        GROUP BY memberId, memberName
        ORDER BY memberName ASC
    """)
    fun getGroupMembersStatsFlow(groupId: Long): Flow<List<MemberAttendanceSummaryRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE date = :date AND status = 'PRESENT'")
    fun getTotalPresentCountForDateFlow(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE date = :date AND status = 'ABSENT'")
    fun getTotalAbsentCountForDateFlow(date: String): Flow<Int>
}
