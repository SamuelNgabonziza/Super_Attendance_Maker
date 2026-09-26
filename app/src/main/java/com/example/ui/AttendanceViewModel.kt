package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.data.model.Group
import com.example.data.model.GroupAnalytics
import com.example.data.model.Member
import com.example.data.model.MemberAnalytics
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    GROUPS("Groups"),
    SESSIONS("Roll Call"),
    ANALYTICS("Analytics"),
    EXPORT("Export & Share")
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application.applicationContext)

    val groups: StateFlow<List<Group>> = repository.groups
    val members: StateFlow<List<Member>> = repository.members
    val sessions: StateFlow<List<AttendanceSession>> = repository.sessions

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.GROUPS)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _rollCallFilter = MutableStateFlow<AttendanceStatus?>(null)
    val rollCallFilter: StateFlow<AttendanceStatus?> = _rollCallFilter.asStateFlow()

    init {
        if (groups.value.isNotEmpty()) {
            _selectedGroupId.value = groups.value.first().id
        }
    }

    val selectedGroup: StateFlow<Group?> = combine(groups, selectedGroupId) { grps, id ->
        grps.find { it.id == id } ?: grps.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentGroupMembers: StateFlow<List<Member>> = combine(members, selectedGroupId, searchQuery) { mbrs, gId, query ->
        val effectiveGid = gId ?: groups.value.firstOrNull()?.id
        val list = mbrs.filter { it.groupId == effectiveGid }
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.identifier.contains(query, ignoreCase = true) ||
                it.role.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentGroupSessions: StateFlow<List<AttendanceSession>> = combine(sessions, selectedGroupId) { sess, gId ->
        val effectiveGid = gId ?: groups.value.firstOrNull()?.id
        sess.filter { it.groupId == effectiveGid }.sortedByDescending { it.dateMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<AttendanceSession?> = combine(sessions, activeSessionId) { sess, sId ->
        sess.find { it.id == sId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentGroupAnalytics: StateFlow<GroupAnalytics?> = combine(selectedGroupId, groups, sessions, members) { gId, grps, _, _ ->
        val effectiveGid = gId ?: grps.firstOrNull()?.id ?: return@combine null
        repository.getGroupAnalytics(effectiveGid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMemberAnalytics: StateFlow<List<MemberAnalytics>> = combine(selectedGroupId, groups, sessions, members) { gId, grps, _, _ ->
        val effectiveGid = gId ?: grps.firstOrNull()?.id ?: return@combine emptyList()
        repository.getMemberAnalytics(effectiveGid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectGroup(groupId: String) {
        _selectedGroupId.value = groupId
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRollCallFilter(status: AttendanceStatus?) {
        _rollCallFilter.value = status
    }

    fun setActiveSession(sessionId: String?) {
        _activeSessionId.value = sessionId
    }

    // --- Action Methods ---
    fun addGroup(name: String, description: String, schedule: String, colorHex: String, passingThreshold: Int = 75) {
        val newGrp = repository.addGroup(name, description, schedule, colorHex, passingThreshold)
        _selectedGroupId.value = newGrp.id
    }

    fun updateGroup(group: Group) {
        repository.updateGroup(group)
    }

    fun deleteGroup(groupId: String) {
        repository.deleteGroup(groupId)
        if (_selectedGroupId.value == groupId) {
            _selectedGroupId.value = groups.value.firstOrNull { it.id != groupId }?.id
        }
    }

    fun addMember(fullName: String, identifier: String, phone: String, role: String) {
        val gId = selectedGroupId.value ?: groups.value.firstOrNull()?.id ?: return
        repository.addMember(gId, fullName, identifier, phone, role)
    }

    fun addBatchMembers(items: List<Triple<String, String, String>>) {
        val gId = selectedGroupId.value ?: groups.value.firstOrNull()?.id ?: return
        repository.addBatchMembers(gId, items)
    }

    fun updateMember(member: Member) {
        repository.updateMember(member)
    }

    fun deleteMember(memberId: String) {
        repository.deleteMember(memberId)
    }

    fun createNewSession(sessionName: String, sessionType: String = "Lecture", notes: String = ""): AttendanceSession? {
        val gId = selectedGroupId.value ?: groups.value.firstOrNull()?.id ?: return null
        val session = repository.createSession(
            groupId = gId,
            sessionName = sessionName,
            dateMillis = System.currentTimeMillis(),
            sessionType = sessionType,
            notes = notes
        )
        _activeSessionId.value = session.id
        _currentTab.value = AppTab.SESSIONS
        return session
    }

    fun updateAttendance(
        sessionId: String,
        memberId: String,
        status: AttendanceStatus,
        note: String = "",
        checkInTime: String = ""
    ) {
        repository.updateAttendanceRecord(sessionId, memberId, status, note, checkInTime)
    }

    fun markAllInActiveSession(status: AttendanceStatus) {
        val sId = _activeSessionId.value ?: return
        repository.markAllInSession(sId, status)
    }

    fun deleteSession(sessionId: String) {
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = null
        }
        repository.deleteSession(sessionId)
    }

    // --- Reports & Alerts ---
    fun generateAbsenteeNotificationText(): String {
        val session = activeSession.value ?: return "No active session."
        val group = selectedGroup.value ?: return "No group selected."
        val absentees = currentGroupMembers.value.filter {
            session.records[it.id]?.status == AttendanceStatus.ABSENT
        }

        if (absentees.isEmpty()) {
            return "🎉 Perfect attendance for ${session.sessionName}! No members absent."
        }

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("⚠️ ABSENTEE NOTICE - ${group.name}\n")
        sb.append("Session: ${session.sessionName} (${sdf.format(Date(session.dateMillis))})\n")
        sb.append("Total Absent: ${absentees.size}\n\n")
        sb.append("Absent Members:\n")
        absentees.forEachIndexed { i, m ->
            val note = session.records[m.id]?.note
            val noteText = if (!note.isNullOrEmpty()) " (Reason: $note)" else ""
            sb.append("${i + 1}. ${m.fullName} [${m.identifier}]$noteText\n")
        }
        sb.append("\nPlease contact administration or follow up regarding your absence.")
        return sb.toString()
    }

    fun generateReportText(): String {
        val group = selectedGroup.value ?: return "No group selected."
        val sessionsList = currentGroupSessions.value
        val membersList = members.value.filter { it.groupId == group.id }
        val analytics = currentGroupAnalytics.value

        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("📋 ATTENDANCE REPORT: ${group.name.uppercase()}\n")
        sb.append("📅 Generated: ${sdf.format(Date())}\n")
        sb.append("👥 Total Members: ${membersList.size}\n")
        sb.append("📊 Total Sessions: ${sessionsList.size}\n")
        sb.append("🎯 Passing Threshold: ${group.passingThreshold}%\n")
        if (analytics != null) {
            sb.append("📈 Average Attendance: ${(analytics.averageAttendanceRate * 100).toInt()}%\n")
            sb.append("⚠️ At-Risk Members (<${group.passingThreshold}%): ${analytics.atRiskCount}\n")
        }
        sb.append("=========================================\n\n")

        sb.append("MEMBER ROSTER & SUMMARY:\n")
        val memberStats = currentMemberAnalytics.value
        memberStats.forEach { stat ->
            val ratePct = (stat.attendanceRate * 100).toInt()
            val atRiskMark = if (stat.isAtRisk) " [⚠️ AT RISK]" else ""
            val streakMark = if (stat.currentStreak >= 3) " [🔥 ${stat.currentStreak} streak]" else ""
            sb.append("• ${stat.member.fullName} [${stat.member.identifier}] - ${ratePct}% (${stat.presentCount}P / ${stat.absentCount}A / ${stat.lateCount}L)$atRiskMark$streakMark\n")
        }

        sb.append("\nRECENT SESSIONS:\n")
        sessionsList.take(5).forEach { sess ->
            val dateStr = sdf.format(Date(sess.dateMillis))
            sb.append("\nSession: ${sess.sessionName} [$dateStr] (Type: ${sess.sessionType})\n")
            val pCount = sess.records.values.count { it.status == AttendanceStatus.PRESENT }
            val aCount = sess.records.values.count { it.status == AttendanceStatus.ABSENT }
            val lCount = sess.records.values.count { it.status == AttendanceStatus.LATE }
            val eCount = sess.records.values.count { it.status == AttendanceStatus.EXCUSED }
            sb.append("Status: $pCount Present, $aCount Absent, $lCount Late, $eCount Excused\n")
        }

        return sb.toString()
    }

    fun generateReportCsv(): String {
        val group = selectedGroup.value ?: return ""
        val sessionsList = currentGroupSessions.value
        val membersList = members.value.filter { it.groupId == group.id }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val sb = StringBuilder()
        // Header
        sb.append("Member ID,Full Name,Role,Current Streak,At Risk")
        sessionsList.forEach { sess ->
            val formattedDate = sdf.format(Date(sess.dateMillis))
            sb.append(",\"${sess.sessionName} ($formattedDate)\"")
        }
        sb.append(",Attendance Rate\n")

        // Rows
        val memberStats = currentMemberAnalytics.value.associateBy { it.member.id }
        membersList.forEach { member ->
            val stat = memberStats[member.id]
            val rate = stat?.attendanceRate ?: 0f
            val streak = stat?.currentStreak ?: 0
            val atRisk = if (stat?.isAtRisk == true) "YES" else "NO"

            sb.append("\"${member.identifier}\",\"${member.fullName}\",\"${member.role}\",$streak,$atRisk")
            sessionsList.forEach { sess ->
                val record = sess.records[member.id]
                val code = record?.status?.shortCode ?: "-"
                val time = if (!record?.checkInTime.isNullOrEmpty()) " (${record?.checkInTime})" else ""
                sb.append(",\"$code$time\"")
            }
            sb.append(",\"${(rate * 100).toInt()}%\"\n")
        }

        return sb.toString()
    }
}
