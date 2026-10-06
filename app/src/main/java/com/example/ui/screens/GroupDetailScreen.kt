package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.AttendanceSegmentedPicker
import com.example.ui.components.AttendanceStatusBadge
import com.example.ui.components.BatchImportDialog
import com.example.ui.components.CategoryBadge
import com.example.ui.components.DigitalIdCardDialog
import com.example.ui.components.EditMemberDialog
import com.example.ui.components.MemberNoteDialog
import com.example.ui.components.QuickScannerModal
import com.example.ui.components.RandomStudentPickerModal
import com.example.ui.components.SessionAttendanceBarChart
import com.example.ui.components.UserAvatar
import com.example.ui.theme.AmberLate
import com.example.ui.theme.AmberLateBg
import com.example.ui.theme.AmberLateText
import com.example.ui.theme.EmeraldPresent
import com.example.ui.theme.EmeraldPresentBg
import com.example.ui.theme.EmeraldPresentText
import com.example.ui.theme.IndigoExcused
import com.example.ui.theme.IndigoExcusedBg
import com.example.ui.theme.IndigoExcusedText
import com.example.ui.theme.RoseAbsent
import com.example.ui.theme.RoseAbsentBg
import com.example.ui.theme.RoseAbsentText
import com.example.ui.viewmodel.AttendanceStatus
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: Long,
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(groupId) {
        viewModel.selectGroup(groupId)
    }

    val group by viewModel.currentGroup.collectAsStateWithLifecycle()
    val members by viewModel.currentGroupMembers.collectAsStateWithLifecycle()
    val sessions by viewModel.currentGroupSessions.collectAsStateWithLifecycle()
    val attendanceDraft by viewModel.attendanceDraft.collectAsStateWithLifecycle()
    val notesDraft by viewModel.notesDraft.collectAsStateWithLifecycle()
    val sessionNote by viewModel.sessionNoteDraft.collectAsStateWithLifecycle()
    val isSessionSavedToday by viewModel.isSessionSavedToday.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Check-in", "Members (${members.size})", "History (${sessions.size})")

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var activeMemberForNote by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToEdit by remember { mutableStateOf<MemberEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showScannerModal by remember { mutableStateOf(false) }
    var showRandomPickerModal by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var selectedMemberForIdCard by remember { mutableStateOf<MemberEntity?>(null) }

    // Quick Date Options
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = viewModel.todayString
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = dateFormat.format(calendar.time)

    // Calculate live status counts
    val presentCount = members.count { (attendanceDraft[it.id] ?: AttendanceStatus.PRESENT) == AttendanceStatus.PRESENT }
    val absentCount = members.count { attendanceDraft[it.id] == AttendanceStatus.ABSENT }
    val lateCount = members.count { attendanceDraft[it.id] == AttendanceStatus.LATE }
    val excusedCount = members.count { attendanceDraft[it.id] == AttendanceStatus.EXCUSED }
    val totalCount = members.size
    val liveRate = if (totalCount > 0) ((presentCount + lateCount) * 100) / totalCount else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group?.name ?: "Attendance Group",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        group?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(category = it.category)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${members.size} member${if (members.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showScannerModal = true },
                        modifier = Modifier.testTag("btn_top_bar_scanner")
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Quick QR Scanner")
                    }
                    IconButton(
                        onClick = { showRandomPickerModal = true },
                        modifier = Modifier.testTag("btn_top_bar_random_picker")
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Pick Random Student")
                    }
                    IconButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier.testTag("btn_top_bar_add_person")
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Person")
                    }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("btn_top_bar_menu")
                    ) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Quick Scanner") },
                            onClick = {
                                showMenu = false
                                showScannerModal = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Random Student Picker") },
                            onClick = {
                                showMenu = false
                                showRandomPickerModal = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Casino, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Batch Import Members") },
                            onClick = {
                                showMenu = false
                                showBatchImportDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.GroupAdd, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Person") },
                            onClick = {
                                showMenu = false
                                showAddMemberDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteGroupDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (selectedTabIndex == 0 && members.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Rate: $liveRate%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (liveRate >= 80) EmeraldPresentText else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$presentCount Present • $absentCount Absent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveCurrentAttendance {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Attendance saved successfully for $selectedDate!")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_save_attendance")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSessionSavedToday) "Update Attendance" else "Save Attendance",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                FloatingActionButton(
                    onClick = { showAddMemberDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_member")
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Member")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> CheckInTabContent(
                    selectedDate = selectedDate,
                    today = today,
                    yesterday = yesterday,
                    onDateSelect = { viewModel.setSelectedDate(it) },
                    presentCount = presentCount,
                    absentCount = absentCount,
                    lateCount = lateCount,
                    excusedCount = excusedCount,
                    totalCount = totalCount,
                    liveRate = liveRate,
                    members = members,
                    attendanceDraft = attendanceDraft,
                    notesDraft = notesDraft,
                    sessionNote = sessionNote,
                    onStatusChange = { memberId, status -> viewModel.setMemberStatus(memberId, status) },
                    onOpenNoteDialog = { member -> activeMemberForNote = member },
                    onSessionNoteChange = { viewModel.setSessionNote(it) },
                    onMarkAll = { status -> viewModel.markAllAs(status) },
                    onOpenScanner = { showScannerModal = true },
                    onOpenRandomPicker = { showRandomPickerModal = true },
                    onAddMember = { showAddMemberDialog = true },
                    onEditMember = { memberToEdit = it }
                )
                1 -> MembersTabContent(
                    members = members,
                    onAddMember = { showAddMemberDialog = true },
                    onBatchImport = { showBatchImportDialog = true },
                    onViewIdCard = { selectedMemberForIdCard = it },
                    onEditMember = { memberToEdit = it },
                    onDeleteMember = { member ->
                        viewModel.deleteMember(member) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Removed ${member.name}")
                            }
                        }
                    }
                )
                2 -> HistoryTabContent(
                    group = group,
                    sessions = sessions,
                    onShareSession = { session ->
                        group?.let { g ->
                            val text = viewModel.generateShareableSummary(session, g)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Attendance Report"))
                        }
                    }
                )
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, identifier, notes, colorHex ->
                viewModel.addMemberToCurrentGroup(name, identifier, notes, colorHex) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added $name to group")
                    }
                }
                showAddMemberDialog = false
            }
        )
    }

    memberToEdit?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { memberToEdit = null },
            onConfirm = { updatedMember ->
                viewModel.updateMember(updatedMember) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Updated ${updatedMember.name}")
                    }
                }
                memberToEdit = null
            },
            onDelete = {
                viewModel.deleteMember(member) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Removed ${member.name}")
                    }
                }
                memberToEdit = null
            }
        )
    }

    activeMemberForNote?.let { member ->
        MemberNoteDialog(
            memberName = member.name,
            initialNote = notesDraft[member.id] ?: "",
            onDismiss = { activeMemberForNote = null },
            onConfirm = { note ->
                viewModel.setMemberNote(member.id, note)
                activeMemberForNote = null
            }
        )
    }

    if (showDeleteGroupDialog && group != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Group?") },
            text = { Text("Are you sure you want to delete \"${group?.name}\"? All members and recorded attendance sessions will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(group!!) {
                            showDeleteGroupDialog = false
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showScannerModal) {
        QuickScannerModal(
            members = members,
            onCheckInMember = { memberId, status, timeStr ->
                viewModel.setMemberStatus(memberId, status)
                coroutineScope.launch {
                    val memberName = members.find { it.id == memberId }?.name ?: "Member"
                    snackbarHostState.showSnackbar("Checked in $memberName as ${status.name} ($timeStr)")
                }
            },
            onDismiss = { showScannerModal = false }
        )
    }

    if (showRandomPickerModal) {
        RandomStudentPickerModal(
            members = members,
            onDismiss = { showRandomPickerModal = false }
        )
    }

    if (showBatchImportDialog) {
        BatchImportDialog(
            onDismiss = { showBatchImportDialog = false },
            onImportMembers = { batchList ->
                viewModel.batchAddMembers(batchList) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Successfully imported ${batchList.size} students")
                    }
                }
                showBatchImportDialog = false
            }
        )
    }

    selectedMemberForIdCard?.let { member ->
        DigitalIdCardDialog(
            member = member,
            group = group,
            onDismiss = { selectedMemberForIdCard = null }
        )
    }
}

@Composable
fun CheckInTabContent(
    selectedDate: String,
    today: String,
    yesterday: String,
    onDateSelect: (String) -> Unit,
    presentCount: Int,
    absentCount: Int,
    lateCount: Int,
    excusedCount: Int,
    totalCount: Int,
    liveRate: Int,
    members: List<MemberEntity>,
    attendanceDraft: Map<Long, AttendanceStatus>,
    notesDraft: Map<Long, String>,
    sessionNote: String,
    onStatusChange: (Long, AttendanceStatus) -> Unit,
    onOpenNoteDialog: (MemberEntity) -> Unit,
    onSessionNoteChange: (String) -> Unit,
    onMarkAll: (AttendanceStatus) -> Unit,
    onOpenScanner: () -> Unit = {},
    onOpenRandomPicker: () -> Unit = {},
    onAddMember: () -> Unit,
    onEditMember: (MemberEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("check_in_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Date selection chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedDate == today,
                    onClick = { onDateSelect(today) },
                    label = { Text("Today") },
                    leadingIcon = {
                        if (selectedDate == today) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("chip_date_today")
                )

                FilterChip(
                    selected = selectedDate == yesterday,
                    onClick = { onDateSelect(yesterday) },
                    label = { Text("Yesterday") },
                    leadingIcon = {
                        if (selectedDate == yesterday) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    },
                    modifier = Modifier.testTag("chip_date_yesterday")
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = selectedDate,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 2. Summary stats card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Roll Call Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$liveRate% Present",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (liveRate >= 80) EmeraldPresentText else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) liveRate / 100f else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = EmeraldPresent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatMiniBadge(count = presentCount, label = "Present", color = EmeraldPresentText, bg = EmeraldPresentBg)
                        StatMiniBadge(count = absentCount, label = "Absent", color = RoseAbsentText, bg = RoseAbsentBg)
                        StatMiniBadge(count = lateCount, label = "Late", color = AmberLateText, bg = AmberLateBg)
                        StatMiniBadge(count = excusedCount, label = "Excused", color = IndigoExcusedText, bg = IndigoExcusedBg)
                    }
                }
            }
        }

        // 3. Quick Bulk Actions & Fast Tools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onMarkAll(AttendanceStatus.PRESENT) },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("btn_mark_all_present"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("All Present", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { onMarkAll(AttendanceStatus.ABSENT) },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("btn_mark_all_absent"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("All Absent", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenScanner,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_quick_scan"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Scan", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenRandomPicker,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_random_picker"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Pick", fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        // 4. Members check-in rows
        if (members.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No members in this group yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add students or team members to begin recording daily check-ins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(onClick = onAddMember) {
                            Text("Add First Member")
                        }
                    }
                }
            }
        } else {
            items(members, key = { it.id }) { member ->
                val status = attendanceDraft[member.id] ?: AttendanceStatus.PRESENT
                val note = notesDraft[member.id] ?: ""

                MemberCheckInCard(
                    member = member,
                    status = status,
                    note = note,
                    onStatusSelected = { newStatus -> onStatusChange(member.id, newStatus) },
                    onEditNote = { onOpenNoteDialog(member) },
                    onEditMember = { onEditMember(member) }
                )
            }
        }

        // 5. Session Note field
        if (members.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sessionNote,
                    onValueChange = onSessionNoteChange,
                    label = { Text("Day Note (Optional)") },
                    placeholder = { Text("e.g. Field trip day, substitute teacher, lab experiment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_note_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun StatMiniBadge(count: Int, label: String, color: Color, bg: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}

@Composable
fun MemberCheckInCard(
    member: MemberEntity,
    status: AttendanceStatus,
    note: String,
    onStatusSelected: (AttendanceStatus) -> Unit,
    onEditNote: () -> Unit,
    onEditMember: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_checkin_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEditMember),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = member.name,
                        colorHex = member.avatarColorHex,
                        size = 40
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (member.identifier.isNotBlank()) {
                            Text(
                                text = member.identifier,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4-state buttons [P] [A] [L] [E]
                AttendanceSegmentedPicker(
                    currentStatus = status,
                    onStatusSelected = onStatusSelected
                )

                IconButton(
                    onClick = onEditNote,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Add Note",
                        tint = if (note.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "Note: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MembersTabContent(
    members: List<MemberEntity>,
    onAddMember: () -> Unit,
    onBatchImport: () -> Unit,
    onViewIdCard: (MemberEntity) -> Unit,
    onEditMember: (MemberEntity) -> Unit,
    onDeleteMember: (MemberEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var memberToDelete by remember { mutableStateOf<MemberEntity?>(null) }

    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) members
        else members.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.identifier.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("members_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or ID...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("members_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = onBatchImport,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_batch_import_in_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import")
                }

                Button(
                    onClick = onAddMember,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_add_member_in_tab")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }

        if (filteredMembers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No members match \"$searchQuery\"" else "No members in group yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onAddMember) {
                                Text("Add Person")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredMembers, key = { it.id }) { member ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditMember(member) }
                        .testTag("member_card_${member.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = member.name,
                            colorHex = member.avatarColorHex,
                            size = 42
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (member.identifier.isNotBlank()) {
                                Text(
                                    text = "ID: ${member.identifier}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (member.notes.isNotBlank()) {
                                Text(
                                    text = member.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { onViewIdCard(member) },
                            modifier = Modifier.testTag("btn_id_card_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Digital Pass for ${member.name}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }

                        IconButton(
                            onClick = { onEditMember(member) },
                            modifier = Modifier.testTag("btn_edit_member_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${member.name}",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { memberToDelete = member },
                            modifier = Modifier.testTag("btn_delete_member_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove ${member.name}",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Remove Member?") },
            text = { Text("Remove ${member.name} from this group? Past attendance records will remain preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(member)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HistoryTabContent(
    group: GroupEntity?,
    sessions: List<AttendanceSessionEntity>,
    onShareSession: (AttendanceSessionEntity) -> Unit
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Attendance History Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daily check-in logs will appear here once saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("sessions_history_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SessionAttendanceBarChart(
                    sessions = sessions,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            items(sessions, key = { it.id }) { session ->
                val rate = if (session.totalCount > 0) {
                    ((session.presentCount + session.lateCount) * 100) / session.totalCount
                } else 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = session.date,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (rate >= 80) EmeraldPresentBg else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "$rate%",
                                        color = if (rate >= 80) EmeraldPresentText else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onShareSession(session) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✅ ${session.presentCount} Present",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPresentText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "❌ ${session.absentCount} Absent",
                                style = MaterialTheme.typography.bodySmall,
                                color = RoseAbsentText,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (session.lateCount > 0) {
                                Text(
                                    text = "⏳ ${session.lateCount} Late",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberLateText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (session.excusedCount > 0) {
                                Text(
                                    text = "📝 ${session.excusedCount} Excused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IndigoExcusedText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (session.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "Note: ${session.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
