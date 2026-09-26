package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.data.model.Group
import com.example.data.model.GroupAnalytics
import com.example.data.model.Member
import com.example.data.model.MemberAnalytics
import com.example.data.model.MemberAttendanceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AttendanceRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("attend_ease_prefs", Context.MODE_PRIVATE)

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _sessions = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val sessions: StateFlow<List<AttendanceSession>> = _sessions.asStateFlow()

    init {
        loadData()
        if (_groups.value.isEmpty()) {
            seedSampleData()
        }
    }

    // --- Groups ---
    fun addGroup(name: String, description: String, schedule: String, colorHex: String, passingThreshold: Int = 75): Group {
        val group = Group(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            schedule = schedule,
            colorHex = colorHex,
            passingThreshold = passingThreshold
        )
        _groups.value = _groups.value + group
        saveData()
        return group
    }

    fun updateGroup(group: Group) {
        _groups.value = _groups.value.map { if (it.id == group.id) group else it }
        saveData()
    }

    fun deleteGroup(groupId: String) {
        _groups.value = _groups.value.filter { it.id != groupId }
        _members.value = _members.value.filter { it.groupId != groupId }
        _sessions.value = _sessions.value.filter { it.groupId != groupId }
        saveData()
    }

    // --- Members ---
    fun addMember(groupId: String, fullName: String, identifier: String, phone: String, role: String): Member {
        val member = Member(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            fullName = fullName,
            identifier = identifier,
            phone = phone,
            role = role
        )
        _members.value = _members.value + member
        saveData()
        return member
    }

    fun addBatchMembers(groupId: String, items: List<Triple<String, String, String>>): List<Member> {
        val newMembers = items.map { (name, id, role) ->
            Member(
                id = UUID.randomUUID().toString(),
                groupId = groupId,
                fullName = name,
                identifier = id,
                phone = "",
                role = role
            )
        }
        _members.value = _members.value + newMembers
        saveData()
        return newMembers
    }

    fun updateMember(member: Member) {
        _members.value = _members.value.map { if (it.id == member.id) member else it }
        saveData()
    }

    fun deleteMember(memberId: String) {
        _members.value = _members.value.filter { it.id != memberId }
        _sessions.value = _sessions.value.map { session ->
            session.copy(records = session.records - memberId)
        }
        saveData()
    }

    // --- Sessions & Attendance ---
    fun createSession(
        groupId: String,
        sessionName: String,
        dateMillis: Long,
        sessionType: String = "Lecture",
        notes: String = ""
    ): AttendanceSession {
        val groupMembers = _members.value.filter { it.groupId == groupId }
        val initialRecords = groupMembers.associate {
            it.id to MemberAttendanceRecord(
                memberId = it.id,
                status = AttendanceStatus.PRESENT
            )
        }
        val session = AttendanceSession(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            sessionName = sessionName,
            dateMillis = dateMillis,
            sessionType = sessionType,
            notes = notes,
            records = initialRecords
        )
        _sessions.value = listOf(session) + _sessions.value
        saveData()
        return session
    }

    fun updateAttendanceRecord(
        sessionId: String,
        memberId: String,
        status: AttendanceStatus,
        note: String = "",
        checkInTime: String = ""
    ) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                val updatedRecords = session.records.toMutableMap()
                val currentRecord = updatedRecords[memberId]
                val updatedRecord = MemberAttendanceRecord(
                    memberId = memberId,
                    status = status,
                    note = if (note.isNotEmpty()) note else (currentRecord?.note ?: ""),
                    checkInTime = if (checkInTime.isNotEmpty()) checkInTime else (currentRecord?.checkInTime ?: "")
                )
                updatedRecords[memberId] = updatedRecord
                session.copy(records = updatedRecords)
            } else {
                session
            }
        }
        saveData()
    }

    fun markAllInSession(sessionId: String, status: AttendanceStatus) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                val updatedRecords = session.records.mapValues { (_, record) ->
                    record.copy(status = status)
                }
                session.copy(records = updatedRecords)
            } else {
                session
            }
        }
        saveData()
    }

    fun deleteSession(sessionId: String) {
        _sessions.value = _sessions.value.filter { it.id != sessionId }
        saveData()
    }

    // --- Analytics ---
    fun getGroupAnalytics(groupId: String): GroupAnalytics {
        val group = _groups.value.find { it.id == groupId }
        val thresholdFraction = (group?.passingThreshold ?: 75) / 100f
        val groupSessions = _sessions.value.filter { it.groupId == groupId }
        val groupMembers = _members.value.filter { it.groupId == groupId }

        var totalPresent = 0
        var totalAbsent = 0
        var totalLate = 0
        var totalExcused = 0

        groupSessions.forEach { session ->
            session.records.values.forEach { record ->
                when (record.status) {
                    AttendanceStatus.PRESENT -> totalPresent++
                    AttendanceStatus.ABSENT -> totalAbsent++
                    AttendanceStatus.LATE -> totalLate++
                    AttendanceStatus.EXCUSED -> totalExcused++
                }
            }
        }

        val totalRecords = totalPresent + totalAbsent + totalLate + totalExcused
        val attendanceRate = if (totalRecords > 0) {
            val numerator = totalPresent.toFloat() + (totalLate * 0.75f)
            (numerator / (totalPresent + totalAbsent + totalLate).coerceAtLeast(1)).coerceIn(0f, 1f)
        } else {
            1.0f
        }

        val memberStats = getMemberAnalytics(groupId)
        val atRisk = memberStats.count { it.isAtRisk }

        return GroupAnalytics(
            totalSessions = groupSessions.size,
            totalMembers = groupMembers.size,
            averageAttendanceRate = attendanceRate,
            presentCount = totalPresent,
            absentCount = totalAbsent,
            lateCount = totalLate,
            excusedCount = totalExcused,
            atRiskCount = atRisk
        )
    }

    fun getMemberAnalytics(groupId: String): List<MemberAnalytics> {
        val group = _groups.value.find { it.id == groupId }
        val thresholdFraction = (group?.passingThreshold ?: 75) / 100f
        val groupMembers = _members.value.filter { it.groupId == groupId }
        val groupSessions = _sessions.value.filter { it.groupId == groupId }.sortedByDescending { it.dateMillis }

        return groupMembers.map { member ->
            var p = 0
            var a = 0
            var l = 0
            var e = 0
            groupSessions.forEach { session ->
                when (session.records[member.id]?.status) {
                    AttendanceStatus.PRESENT -> p++
                    AttendanceStatus.ABSENT -> a++
                    AttendanceStatus.LATE -> l++
                    AttendanceStatus.EXCUSED -> e++
                    null -> {}
                }
            }
            val activeSessions = p + a + l
            val rate = if (activeSessions > 0) {
                ((p.toFloat() + (l * 0.75f)) / activeSessions).coerceIn(0f, 1f)
            } else {
                1.0f
            }

            // Calculate consecutive current streak of present/late sessions
            var streak = 0
            for (session in groupSessions) {
                val stat = session.records[member.id]?.status
                if (stat == AttendanceStatus.PRESENT || stat == AttendanceStatus.LATE) {
                    streak++
                } else if (stat == AttendanceStatus.ABSENT) {
                    break
                }
            }

            val isAtRisk = groupSessions.isNotEmpty() && (rate < thresholdFraction)

            MemberAnalytics(
                member = member,
                totalSessions = groupSessions.size,
                presentCount = p,
                absentCount = a,
                lateCount = l,
                excusedCount = e,
                attendanceRate = rate,
                currentStreak = streak,
                isAtRisk = isAtRisk
            )
        }.sortedByDescending { it.attendanceRate }
    }

    // --- Persistence via JSON ---
    private fun saveData() {
        val groupsJson = JSONArray()
        _groups.value.forEach { group ->
            val obj = JSONObject()
            obj.put("id", group.id)
            obj.put("name", group.name)
            obj.put("description", group.description)
            obj.put("schedule", group.schedule)
            obj.put("colorHex", group.colorHex)
            obj.put("passingThreshold", group.passingThreshold)
            obj.put("createdAt", group.createdAt)
            groupsJson.put(obj)
        }

        val membersJson = JSONArray()
        _members.value.forEach { member ->
            val obj = JSONObject()
            obj.put("id", member.id)
            obj.put("groupId", member.groupId)
            obj.put("fullName", member.fullName)
            obj.put("identifier", member.identifier)
            obj.put("phone", member.phone)
            obj.put("role", member.role)
            membersJson.put(obj)
        }

        val sessionsJson = JSONArray()
        _sessions.value.forEach { session ->
            val obj = JSONObject()
            obj.put("id", session.id)
            obj.put("groupId", session.groupId)
            obj.put("sessionName", session.sessionName)
            obj.put("dateMillis", session.dateMillis)
            obj.put("sessionType", session.sessionType)
            obj.put("notes", session.notes)

            val recordsObj = JSONObject()
            session.records.forEach { (mId, rec) ->
                val recObj = JSONObject()
                recObj.put("memberId", rec.memberId)
                recObj.put("status", rec.status.name)
                recObj.put("note", rec.note)
                recObj.put("checkInTime", rec.checkInTime)
                recordsObj.put(mId, recObj)
            }
            obj.put("records", recordsObj)
            sessionsJson.put(obj)
        }

        prefs.edit()
            .putString("groups_data", groupsJson.toString())
            .putString("members_data", membersJson.toString())
            .putString("sessions_data", sessionsJson.toString())
            .apply()
    }

    private fun loadData() {
        try {
            val groupsStr = prefs.getString("groups_data", null)
            if (!groupsStr.isNullOrEmpty()) {
                val groupsArr = JSONArray(groupsStr)
                val list = mutableListOf<Group>()
                for (i in 0 until groupsArr.length()) {
                    val obj = groupsArr.getJSONObject(i)
                    list.add(
                        Group(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            schedule = obj.optString("schedule", "Mon, Wed, Fri"),
                            colorHex = obj.optString("colorHex", "#1E3A8A"),
                            passingThreshold = obj.optInt("passingThreshold", 75),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _groups.value = list
            }

            val membersStr = prefs.getString("members_data", null)
            if (!membersStr.isNullOrEmpty()) {
                val membersArr = JSONArray(membersStr)
                val list = mutableListOf<Member>()
                for (i in 0 until membersArr.length()) {
                    val obj = membersArr.getJSONObject(i)
                    list.add(
                        Member(
                            id = obj.getString("id"),
                            groupId = obj.getString("groupId"),
                            fullName = obj.getString("fullName"),
                            identifier = obj.optString("identifier", ""),
                            phone = obj.optString("phone", ""),
                            role = obj.optString("role", "Member")
                        )
                    )
                }
                _members.value = list
            }

            val sessionsStr = prefs.getString("sessions_data", null)
            if (!sessionsStr.isNullOrEmpty()) {
                val sessionsArr = JSONArray(sessionsStr)
                val list = mutableListOf<AttendanceSession>()
                for (i in 0 until sessionsArr.length()) {
                    val obj = sessionsArr.getJSONObject(i)
                    val recsObj = obj.getJSONObject("records")
                    val recordsMap = mutableMapOf<String, MemberAttendanceRecord>()
                    val keys = recsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val recJson = recsObj.getJSONObject(key)
                        val statusStr = recJson.optString("status", AttendanceStatus.PRESENT.name)
                        val status = try {
                            AttendanceStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            AttendanceStatus.PRESENT
                        }
                        recordsMap[key] = MemberAttendanceRecord(
                            memberId = recJson.getString("memberId"),
                            status = status,
                            note = recJson.optString("note", ""),
                            checkInTime = recJson.optString("checkInTime", "")
                        )
                    }
                    list.add(
                        AttendanceSession(
                            id = obj.getString("id"),
                            groupId = obj.getString("groupId"),
                            sessionName = obj.getString("sessionName"),
                            dateMillis = obj.getLong("dateMillis"),
                            sessionType = obj.optString("sessionType", "Lecture"),
                            notes = obj.optString("notes", ""),
                            records = recordsMap
                        )
                    )
                }
                _sessions.value = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedSampleData() {
        val group1 = Group(
            id = "group_cs101",
            name = "Computer Science 101",
            description = "Intro to Algorithms and Data Structures",
            schedule = "Mon, Wed, Fri 10:00 AM",
            colorHex = "#1E3A8A",
            passingThreshold = 75
        )
        val group2 = Group(
            id = "group_design",
            name = "Product Design Sprint",
            description = "UX/UI Design and Prototyping Workshop",
            schedule = "Tue, Thu 2:00 PM",
            colorHex = "#0D9488",
            passingThreshold = 80
        )
        _groups.value = listOf(group1, group2)

        val members = listOf(
            Member("m1", group1.id, "Alice Walker", "CS-2026-01", "+1 (555) 234-5678", "Student"),
            Member("m2", group1.id, "Benjamin Foster", "CS-2026-02", "+1 (555) 345-6789", "Student"),
            Member("m3", group1.id, "Catherine Chen", "CS-2026-03", "+1 (555) 456-7890", "Class Lead"),
            Member("m4", group1.id, "David O'Connor", "CS-2026-04", "+1 (555) 567-8901", "Student"),
            Member("m5", group1.id, "Elena Rostova", "CS-2026-05", "+1 (555) 678-9012", "Student"),
            Member("m6", group1.id, "Farhan Malik", "CS-2026-06", "+1 (555) 789-0123", "Student"),

            Member("m7", group2.id, "Grace Hopper", "DES-01", "+1 (555) 111-2222", "Facilitator"),
            Member("m8", group2.id, "Henry Cavill", "DES-02", "+1 (555) 333-4444", "Designer"),
            Member("m9", group2.id, "Isabella Rossi", "DES-03", "+1 (555) 555-6666", "Researcher")
        )
        _members.value = members

        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        val session1 = AttendanceSession(
            id = "sess_01",
            groupId = group1.id,
            sessionName = "Lecture 1: Graph Traversal",
            dateMillis = now - (oneDay * 2),
            sessionType = "Lecture",
            notes = "Covered BFS and DFS fundamentals",
            records = mapOf(
                "m1" to MemberAttendanceRecord("m1", AttendanceStatus.PRESENT, checkInTime = "10:02 AM"),
                "m2" to MemberAttendanceRecord("m2", AttendanceStatus.PRESENT, checkInTime = "10:05 AM"),
                "m3" to MemberAttendanceRecord("m3", AttendanceStatus.PRESENT, checkInTime = "09:58 AM"),
                "m4" to MemberAttendanceRecord("m4", AttendanceStatus.LATE, "Arrived 10m late", checkInTime = "10:12 AM"),
                "m5" to MemberAttendanceRecord("m5", AttendanceStatus.ABSENT, "Sick"),
                "m6" to MemberAttendanceRecord("m6", AttendanceStatus.PRESENT, checkInTime = "10:00 AM")
            )
        )

        val session2 = AttendanceSession(
            id = "sess_02",
            groupId = group1.id,
            sessionName = "Lecture 2: Shortest Path & Dijkstra",
            dateMillis = now,
            sessionType = "Lab",
            notes = "Live coding session",
            records = mapOf(
                "m1" to MemberAttendanceRecord("m1", AttendanceStatus.PRESENT, checkInTime = "10:01 AM"),
                "m2" to MemberAttendanceRecord("m2", AttendanceStatus.LATE, checkInTime = "10:15 AM"),
                "m3" to MemberAttendanceRecord("m3", AttendanceStatus.PRESENT, checkInTime = "09:55 AM"),
                "m4" to MemberAttendanceRecord("m4", AttendanceStatus.PRESENT, checkInTime = "10:04 AM"),
                "m5" to MemberAttendanceRecord("m5", AttendanceStatus.EXCUSED, "Doctor appointment"),
                "m6" to MemberAttendanceRecord("m6", AttendanceStatus.PRESENT, checkInTime = "10:00 AM")
            )
        )

        _sessions.value = listOf(session2, session1)
        saveData()
    }
}
