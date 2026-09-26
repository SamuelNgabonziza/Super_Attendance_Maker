package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.model.SessionWithRecords
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AttendanceSessionEntity): Long

    @Update
    suspend fun updateSession(session: AttendanceSessionEntity)

    @Delete
    suspend fun deleteSession(session: AttendanceSessionEntity)

    @Query("DELETE FROM attendance_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM attendance_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId AND date = :date LIMIT 1")
    suspend fun getSessionByGroupAndDate(groupId: Long, date: String): AttendanceSessionEntity?

    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId ORDER BY date DESC, timestamp DESC")
    fun getSessionsForGroupFlow(groupId: Long): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions ORDER BY date DESC, timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<AttendanceSessionEntity>>

    @Query("SELECT * FROM attendance_sessions WHERE date = :date ORDER BY timestamp DESC")
    fun getSessionsByDateFlow(date: String): Flow<List<AttendanceSessionEntity>>

    @Transaction
    @Query("SELECT * FROM attendance_sessions WHERE id = :sessionId")
    fun getSessionWithRecordsFlow(sessionId: Long): Flow<SessionWithRecords?>

    @Transaction
    @Query("SELECT * FROM attendance_sessions WHERE groupId = :groupId AND date = :date LIMIT 1")
    fun getSessionWithRecordsByGroupAndDateFlow(groupId: Long, date: String): Flow<SessionWithRecords?>
}
