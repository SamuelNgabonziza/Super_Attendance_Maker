package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    EXCUSED
}

data class DashboardMetrics(
    val totalGroups: Int = 0,
    val totalMembers: Int = 0,
    val groupsCheckedInToday: Int = 0,
    val todayAttendanceRate: Int = 0,
    val todayPresentCount: Int = 0,
    val todayAbsentCount: Int = 0,
    val todayLateCount: Int = 0
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = AttendanceRepository(
            dao = db.attendanceDao(),
            memberDao = db.memberDao(),
            recordDao = db.attendanceRecordDao()
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedSampleDataIfEmpty()
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString: String = dateFormat.format(Date())

    // Personalized User Profile
    private val _userName = MutableStateFlow("Samuel Prosper")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userRole = MutableStateFlow("Lead Coordinator")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    fun updateProfile(name: String, role: String) {
        _userName.value = name.ifBlank { "Organizer" }
        _userRole.value = role.ifBlank { "Daily Attendance Lead" }
    }

    // Data sources
    val allGroups: StateFlow<List<GroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGroupCount: StateFlow<Int> = repository.totalGroupCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMemberCount: StateFlow<Int> = repository.totalMemberCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentActivities: StateFlow<List<ActivityLogEntity>> = repository.recentActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<AttendanceSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed Dashboard Metrics
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        allGroups,
        totalMemberCount,
        allSessions
    ) { groups, membersCount, sessions ->
        val todaySessions = sessions.filter { it.date == todayString }
        val checkedInGroups = todaySessions.map { it.groupId }.distinct().size
        val totalPresent = todaySessions.sumOf { it.presentCount }
        val totalLate = todaySessions.sumOf { it.lateCount }
        val totalAbsent = todaySessions.sumOf { it.absentCount }
        val totalExcused = todaySessions.sumOf { it.excusedCount }
        val totalMarked = totalPresent + totalLate + totalAbsent + totalExcused

        val rate = if (totalMarked > 0) {
            ((totalPresent + totalLate) * 100) / totalMarked
        } else 0

        DashboardMetrics(
            totalGroups = groups.size,
            totalMembers = membersCount,
            groupsCheckedInToday = checkedInGroups,
            todayAttendanceRate = rate,
            todayPresentCount = totalPresent,
            todayAbsentCount = totalAbsent,
            todayLateCount = totalLate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Active Group & Check-in Session State
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayString)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _currentGroup = MutableStateFlow<GroupEntity?>(null)
    val currentGroup: StateFlow<GroupEntity?> = _currentGroup.asStateFlow()

    private val _currentGroupMembers = MutableStateFlow<List<MemberEntity>>(emptyList())
    val currentGroupMembers: StateFlow<List<MemberEntity>> = _currentGroupMembers.asStateFlow()

    private val _currentGroupSessions = MutableStateFlow<List<AttendanceSessionEntity>>(emptyList())
    val currentGroupSessions: StateFlow<List<AttendanceSessionEntity>> = _currentGroupSessions.asStateFlow()

    // Attendance draft state for active check-in
    private val _attendanceDraft = MutableStateFlow<Map<Long, AttendanceStatus>>(emptyMap())
    val attendanceDraft: StateFlow<Map<Long, AttendanceStatus>> = _attendanceDraft.asStateFlow()

    private val _notesDraft = MutableStateFlow<Map<Long, String>>(emptyMap())
    val notesDraft: StateFlow<Map<Long, String>> = _notesDraft.asStateFlow()

    private val _sessionNoteDraft = MutableStateFlow("")
    val sessionNoteDraft: StateFlow<String> = _sessionNoteDraft.asStateFlow()

    private val _isSessionSavedToday = MutableStateFlow(false)
    val isSessionSavedToday: StateFlow<Boolean> = _isSessionSavedToday.asStateFlow()

    fun selectGroup(groupId: Long) {
        _selectedGroupId.value = groupId
        loadGroupData(groupId, _selectedDate.value)
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
        val groupId = _selectedGroupId.value
        if (groupId != null) {
            loadGroupData(groupId, date)
        }
    }

    private fun loadGroupData(groupId: Long, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = repository.getGroupById(groupId)
            _currentGroup.value = group

            val members = repository.getMembersForGroup(groupId)
            _currentGroupMembers.value = members

            // Check if a session exists for this date
            val session = repository.getSessionByGroupAndDate(groupId, date)
            val newDraft = mutableMapOf<Long, AttendanceStatus>()
            val newNotes = mutableMapOf<Long, String>()

            if (session != null) {
                _isSessionSavedToday.value = true
                _sessionNoteDraft.value = session.notes
                val records = repository.getRecordsForSession(session.id)
                val recordMap = records.associateBy { it.memberId }

                members.forEach { member ->
                    val rec = recordMap[member.id]
                    if (rec != null) {
                        newDraft[member.id] = when (rec.status) {
                            "PRESENT" -> AttendanceStatus.PRESENT
                            "ABSENT" -> AttendanceStatus.ABSENT
                            "LATE" -> AttendanceStatus.LATE
                            "EXCUSED" -> AttendanceStatus.EXCUSED
                            else -> AttendanceStatus.PRESENT
                        }
                        if (rec.note.isNotBlank()) {
                            newNotes[member.id] = rec.note
                        }
                    } else {
                        newDraft[member.id] = AttendanceStatus.PRESENT
                    }
                }
            } else {
                _isSessionSavedToday.value = false
                _sessionNoteDraft.value = ""
                // Default all to PRESENT for quick convenience
                members.forEach { member ->
                    newDraft[member.id] = AttendanceStatus.PRESENT
                }
            }
            _attendanceDraft.value = newDraft
            _notesDraft.value = newNotes

            // Also load group session history
            repository.getSessionsForGroupFlow(groupId).collect { sessions ->
                _currentGroupSessions.value = sessions
            }
        }
    }

    fun setMemberStatus(memberId: Long, status: AttendanceStatus) {
        val updated = _attendanceDraft.value.toMutableMap()
        updated[memberId] = status
        _attendanceDraft.value = updated
    }

    fun setMemberNote(memberId: Long, note: String) {
        val updated = _notesDraft.value.toMutableMap()
        updated[memberId] = note
        _notesDraft.value = updated
    }

    fun setSessionNote(note: String) {
        _sessionNoteDraft.value = note
    }

    fun markAllAs(status: AttendanceStatus) {
        val updated = _attendanceDraft.value.toMutableMap()
        _currentGroupMembers.value.forEach { member ->
            updated[member.id] = status
        }
        _attendanceDraft.value = updated
    }

    fun saveCurrentAttendance(onSuccess: (savedSessionId: Long) -> Unit) {
        val groupId = _selectedGroupId.value ?: return
        val group = _currentGroup.value ?: return
        val date = _selectedDate.value
        val members = _currentGroupMembers.value
        val statuses = _attendanceDraft.value
        val notes = _notesDraft.value
        val sessionNotes = _sessionNoteDraft.value

        viewModelScope.launch(Dispatchers.IO) {
            val records = members.map { member ->
                val status = statuses[member.id] ?: AttendanceStatus.PRESENT
                val note = notes[member.id] ?: ""
                AttendanceRecordEntity(
                    sessionId = 0,
                    groupId = groupId,
                    memberId = member.id,
                    memberName = member.name,
                    date = date,
                    status = status.name,
                    note = note
                )
            }

            val sessionId = repository.saveAttendanceSession(
                groupId = groupId,
                groupName = group.name,
                date = date,
                records = records,
                sessionNotes = sessionNotes
            )

            _isSessionSavedToday.value = true
            loadGroupData(groupId, date)

            launch(Dispatchers.Main) {
                onSuccess(sessionId)
            }
        }
    }

    fun createGroup(name: String, description: String, category: String, colorHex: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createGroup(name, description, category, colorHex)
            launch(Dispatchers.Main) {
                onSuccess(id)
            }
        }
    }

    fun deleteGroup(group: GroupEntity, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroup(group)
            if (_selectedGroupId.value == group.id) {
                _selectedGroupId.value = null
            }
            launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun addMemberToCurrentGroup(
        name: String,
        identifier: String,
        notes: String,
        avatarColorHex: String,
        onSuccess: ((Long) -> Unit)? = null
    ) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.addMember(groupId, name, identifier, notes, avatarColorHex)
            loadGroupData(groupId, _selectedDate.value)
            onSuccess?.let { cb ->
                launch(Dispatchers.Main) { cb(id) }
            }
        }
    }

    fun updateMember(
        member: MemberEntity,
        onSuccess: (() -> Unit)? = null
    ) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMember(member)
            loadGroupData(groupId, _selectedDate.value)
            onSuccess?.let { cb ->
                launch(Dispatchers.Main) { cb() }
            }
        }
    }

    fun deleteMember(
        member: MemberEntity,
        onSuccess: (() -> Unit)? = null
    ) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMember(member)
            _attendanceDraft.update { it - member.id }
            _notesDraft.update { it - member.id }
            loadGroupData(groupId, _selectedDate.value)
            onSuccess?.let { cb ->
                launch(Dispatchers.Main) { cb() }
            }
        }
    }

    fun generateShareableSummary(session: AttendanceSessionEntity, group: GroupEntity): String {
        val rate = if (session.totalCount > 0) ((session.presentCount + session.lateCount) * 100 / session.totalCount) else 0
        return buildString {
            appendLine("📋 Attendance Report: ${group.name}")
            appendLine("📅 Date: ${session.date}")
            appendLine("📊 Overall Rate: $rate%")
            appendLine("✅ Present: ${session.presentCount}")
            appendLine("❌ Absent: ${session.absentCount}")
            appendLine("⏳ Late: ${session.lateCount}")
            appendLine("📝 Excused: ${session.excusedCount}")
            appendLine("👥 Total Enrolled: ${session.totalCount}")
            if (session.notes.isNotBlank()) {
                appendLine("💬 Notes: ${session.notes}")
            }
            appendLine("\nGenerated via AttendEase")
        }
    }
}
