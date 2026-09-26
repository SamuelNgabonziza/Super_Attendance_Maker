package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.data.model.Member
import com.example.ui.AttendanceViewModel
import com.example.ui.components.QuickScannerModal
import com.example.ui.components.RandomStudentPickerModal
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RollCallScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val sessions by viewModel.currentGroupSessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val members by viewModel.currentGroupMembers.collectAsState()
    val rollCallFilter by viewModel.rollCallFilter.collectAsState()

    var showNewSessionDialog by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var showRandomPickerModal by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(sessions) {
        if (activeSessionId == null && sessions.isNotEmpty()) {
            viewModel.setActiveSession(sessions.first().id)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Sessions Header & Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Roll Call Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedGroup?.name ?: "No group",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Random Student Cold Caller
                FilledTonalIconButton(
                    onClick = { showRandomPickerModal = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Pick Random Student",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Scanner Modal
                FilledTonalIconButton(
                    onClick = { showScannerModal = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Quick Check-in Scanner",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = { showNewSessionDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("new_session_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", fontSize = 12.sp)
                }
            }
        }

        // Sessions Horizontal Pill Selector
        if (sessions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(sessions) { sess ->
                    val isSelected = sess.id == activeSessionId
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setActiveSession(sess.id) },
                        label = {
                            Column {
                                Text(
                                    text = sess.sessionName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "${sess.sessionType} • " + SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(sess.dateMillis)),
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val currentSession = activeSession
        if (currentSession == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No sessions recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap 'New' to start taking attendance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            val records = currentSession.records
            val presentCount = records.values.count { it.status == AttendanceStatus.PRESENT }
            val absentCount = records.values.count { it.status == AttendanceStatus.ABSENT }
            val lateCount = records.values.count { it.status == AttendanceStatus.LATE }
            val excusedCount = records.values.count { it.status == AttendanceStatus.EXCUSED }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSession.sessionName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = currentSession.sessionType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = sdf.format(Date(currentSession.dateMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSession(currentSession.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Session",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stats row counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCounterBadge("Present", presentCount, StatusPresent)
                        StatCounterBadge("Late", lateCount, StatusLate)
                        StatCounterBadge("Absent", absentCount, StatusAbsent)
                        StatCounterBadge("Excused", excusedCount, StatusExcused)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Bulk & Followup Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.markAllInActiveSession(AttendanceStatus.PRESENT) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, tint = StatusPresent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("All Present", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.markAllInActiveSession(AttendanceStatus.ABSENT) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = StatusAbsent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("All Absent", fontSize = 11.sp)
                        }

                        if (absentCount > 0) {
                            FilledTonalButton(
                                onClick = {
                                    val alertText = viewModel.generateAbsenteeNotificationText()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, alertText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Notify Absentees"))
                                },
                                modifier = Modifier.weight(1.2f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Notify ($absentCount)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Roster Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = rollCallFilter == null,
                        onClick = { viewModel.setRollCallFilter(null) },
                        label = { Text("All (${members.size})", fontSize = 11.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = rollCallFilter == AttendanceStatus.PRESENT,
                        onClick = { viewModel.setRollCallFilter(if (rollCallFilter == AttendanceStatus.PRESENT) null else AttendanceStatus.PRESENT) },
                        label = { Text("Present ($presentCount)", fontSize = 11.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = rollCallFilter == AttendanceStatus.LATE,
                        onClick = { viewModel.setRollCallFilter(if (rollCallFilter == AttendanceStatus.LATE) null else AttendanceStatus.LATE) },
                        label = { Text("Late ($lateCount)", fontSize = 11.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = rollCallFilter == AttendanceStatus.ABSENT,
                        onClick = { viewModel.setRollCallFilter(if (rollCallFilter == AttendanceStatus.ABSENT) null else AttendanceStatus.ABSENT) },
                        label = { Text("Absent ($absentCount)", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val displayedMembers = remember(members, rollCallFilter, currentSession.records) {
                if (rollCallFilter == null) {
                    members
                } else {
                    members.filter {
                        currentSession.records[it.id]?.status == rollCallFilter
                    }
                }
            }

            // Member Attendance Cards List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedMembers, key = { it.id }) { member ->
                    val record = currentSession.records[member.id]
                    val status = record?.status ?: AttendanceStatus.PRESENT

                    MemberRollCallCard(
                        member = member,
                        currentStatus = status,
                        note = record?.note ?: "",
                        checkInTime = record?.checkInTime ?: "",
                        onStatusChanged = { newStatus ->
                            val timeStr = if (newStatus == AttendanceStatus.PRESENT || newStatus == AttendanceStatus.LATE) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            } else ""
                            viewModel.updateAttendance(currentSession.id, member.id, newStatus, checkInTime = timeStr)
                        },
                        onNoteChanged = { newNote ->
                            viewModel.updateAttendance(currentSession.id, member.id, status, newNote)
                        }
                    )
                }
            }
        }
    }

    if (showNewSessionDialog) {
        CreateSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onConfirm = { name, type, note ->
                viewModel.createNewSession(name, type, note)
                showNewSessionDialog = false
            }
        )
    }

    if (showScannerModal) {
        activeSession?.let { sess ->
            QuickScannerModal(
                members = members,
                onCheckInMember = { memberId, status, timeStr ->
                    viewModel.updateAttendance(sess.id, memberId, status, checkInTime = timeStr)
                },
                onDismiss = { showScannerModal = false }
            )
        }
    }

    if (showRandomPickerModal) {
        RandomStudentPickerModal(
            members = members,
            onDismiss = { showRandomPickerModal = false }
        )
    }
}

@Composable
fun StatCounterBadge(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MemberRollCallCard(
    member: Member,
    currentStatus: AttendanceStatus,
    note: String,
    checkInTime: String,
    onStatusChanged: (AttendanceStatus) -> Unit,
    onNoteChanged: (String) -> Unit
) {
    var showNoteInput by remember { mutableStateOf(false) }
    var tempNote by remember(note) { mutableStateOf(note) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (member.identifier.isNotEmpty()) {
                            Text(
                                text = "${member.role} • ${member.identifier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        if (checkInTime.isNotEmpty()) {
                            Text(
                                text = "⏱ $checkInTime",
                                fontSize = 11.sp,
                                color = StatusPresent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showNoteInput = !showNoteInput },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (note.isNotEmpty()) Icons.Default.Chat else Icons.Default.ChatBubbleOutline,
                        contentDescription = "Add Note",
                        tint = if (note.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (note.isNotEmpty() && !showNoteInput) {
                Text(
                    text = "📝 $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (showNoteInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tempNote,
                        onValueChange = { tempNote = it },
                        placeholder = { Text("Note (e.g. excused doctor)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            onNoteChanged(tempNote)
                            showNoteInput = false
                        }
                    ) {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4 Single-Tap Status Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    status = AttendanceStatus.PRESENT,
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    color = StatusPresent,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChanged(AttendanceStatus.PRESENT) }
                )
                StatusPill(
                    status = AttendanceStatus.LATE,
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    color = StatusLate,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChanged(AttendanceStatus.LATE) }
                )
                StatusPill(
                    status = AttendanceStatus.ABSENT,
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    color = StatusAbsent,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChanged(AttendanceStatus.ABSENT) }
                )
                StatusPill(
                    status = AttendanceStatus.EXCUSED,
                    isSelected = currentStatus == AttendanceStatus.EXCUSED,
                    color = StatusExcused,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChanged(AttendanceStatus.EXCUSED) }
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    status: AttendanceStatus,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) color else color.copy(alpha = 0.08f),
        label = "pill_bg"
    )
    val textColor = if (isSelected) Color.White else color

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)) else null,
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun CreateSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (sessionName: String, sessionType: String, notes: String) -> Unit
) {
    val defaultName = remember { "Session - ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date())}" }
    var name by remember { mutableStateOf(defaultName) }
    var type by remember { mutableStateOf("Lecture") }
    var notes by remember { mutableStateOf("") }

    val types = listOf("Lecture", "Lab", "Exam", "Meeting", "Workshop", "Practice")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Roll Call Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Session Name / Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Session Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Session Agenda / Notes") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), type, notes.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Start Roll Call")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
