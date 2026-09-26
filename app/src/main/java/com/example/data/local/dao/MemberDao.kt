package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.MemberEntity
import com.example.data.local.model.MemberWithAttendanceHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>): List<Long>

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Query("SELECT * FROM members WHERE id = :id")
    fun getMemberByIdFlow(id: Long): Flow<MemberEntity?>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    suspend fun getMembersByGroupId(groupId: Long): List<MemberEntity>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    fun getMembersByGroupIdFlow(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembersFlow(): Flow<List<MemberEntity>>

    @Query("""
        SELECT * FROM members 
        WHERE groupId = :groupId 
          AND (name LIKE '%' || :query || '%' OR identifier LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchMembersInGroup(groupId: Long, query: String): Flow<List<MemberEntity>>

    @Query("SELECT COUNT(*) FROM members WHERE groupId = :groupId")
    fun getMemberCountForGroupFlow(groupId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM members")
    fun getTotalMemberCountFlow(): Flow<Int>

    // Relations with Attendance Records
    @Transaction
    @Query("SELECT * FROM members WHERE id = :memberId")
    fun getMemberWithHistoryFlow(memberId: Long): Flow<MemberWithAttendanceHistory?>

    @Transaction
    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY name ASC")
    fun getGroupMembersWithHistoryFlow(groupId: Long): Flow<List<MemberWithAttendanceHistory>>
}
