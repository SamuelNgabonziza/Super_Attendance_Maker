package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceStatus
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeAttendanceScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetail: (groupId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    // State: if pickedGroupId is null -> show Step 1: Group Selection Screen.
    // If not null -> show Step 2: Roll Call Attendance Taking Screen for that chosen group.
    var pickedGroupId by remember { mutableStateOf<Long?>(null) }
    var groupSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val todaySessionGroupIds = remember(allSessions) {
        allSessions.filter { it.date == viewModel.todayString }.map { it.groupId }.toSet()
    }

    if (pickedGroupId == null) {
        // STEP 1: Dedicated Group Selection Screen
        BackHandler(onBack = onNavigateBack)

        val categories = remember(groups) {
            listOf("All") + groups.map { it.category }.distinct()
        }

        val filteredGroups = remember(groups, groupSearchQuery, selectedCategory) {
            groups.filter { group ->
                val matchesSearch = group.name.contains(groupSearchQuery, ignoreCase = true) ||
                        group.description.contains(groupSearchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == "All" || group.category.equals(selectedCategory, ignoreCase = true)
                matchesSearch && matchesCategory
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Take Attendance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Select a group roster to begin roll call",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_group_select")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Home"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Groups Created Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please create a group first in 'Your Groups' or Settings to start taking attendance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("select_group_for_attendance_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search bar
                    item {
                        OutlinedTextField(
                            value = groupSearchQuery,
                            onValueChange = { groupSearchQuery = it },
                            placeholder = { Text("Search groups by name or category...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_groups_for_attendance_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Category chips
                    if (categories.size > 1) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Choose a roster below to mark attendance:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    // Group Cards
                    items(filteredGroups, key = { "select_group_${it.id}" }) { group ->
                        val isLoggedToday = group.id in todaySessionGroupIds
                        val barColor = try {
                            Color(android.graphics.Color.parseColor(group.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectGroup(group.id)
                                    pickedGroupId = group.id
                                }
                                .testTag("select_group_card_${group.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(barColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HowToReg,
                                        contentDescription = null,
                                        tint = barColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (group.description.isNotBlank()) {
                                        Text(
                                            text = group.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CategoryBadge(category = group.category)
                                        if (isLoggedToday) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldPresentBg
                                            ) {
                                                Text(
                                                    text = "✓ Logged Today",
                                                    color = EmeraldPresentText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "Pending Check-in",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Take attendance for ${group.name}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // STEP 2: Dedicated Roll Call Page for the Selected Group
        BackHandler(onBack = { pickedGroupId = null })

        RollCallForGroupView(
            groupId = pickedGroupId!!,
            viewModel = viewModel,
            onBackToGroupList = { pickedGroupId = null },
            onNavigateToGroupDetail = onNavigateToGroupDetail
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RollCallForGroupView(
    groupId: Long,
    viewModel: AttendanceViewModel,
    onBackToGroupList: () -> Unit,
    onNavigateToGroupDetail: (groupId: Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(groupId) {
        viewModel.selectGroup(groupId)
    }

    val currentGroup by viewModel.currentGroup.collectAsStateWithLifecycle()
    val members by viewModel.currentGroupMembers.collectAsStateWithLifecycle()
    val attendanceDraft by viewModel.attendanceDraft.collectAsStateWithLifecycle()
    val notesDraft by viewModel.notesDraft.collectAsStateWithLifecycle()
    val isSessionSavedToday by viewModel.isSessionSavedToday.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showScannerModal by remember { mutableStateOf(false) }
    var showRandomPickerModal by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var activeMemberForNote by remember { mutableStateOf<MemberEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<MemberEntity?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = viewModel.todayString
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = dateFormat.format(calendar.time)

    val presentCount = members.count { (attendanceDraft[it.id] ?: AttendanceStatus.PRESENT) == AttendanceStatus.PRESENT }
    val absentCount = members.count { attendanceDraft[it.id] == AttendanceStatus.ABSENT }
    val lateCount = members.count { attendanceDraft[it.id] == AttendanceStatus.LATE }
    val excusedCount = members.count { attendanceDraft[it.id] == AttendanceStatus.EXCUSED }
    val totalCount = members.size
    val liveRate = if (totalCount > 0) ((presentCount + lateCount) * 100) / totalCount else 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentGroup?.name ?: "Attendance Roll Call",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${members.size} student${if (members.size == 1) "" else "s"} on roster",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackToGroupList,
                        modifier = Modifier.testTag("btn_back_to_groups_list")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Group Selection"
                        )
                    }
                },
                actions = {
                    // Quick add member action directly from roll call!
                    IconButton(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier.testTag("btn_add_member_rollcall")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Member",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (members.isNotEmpty()) {
                        IconButton(onClick = { showScannerModal = true }) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scanner", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showRandomPickerModal = true }) {
                            Icon(imageVector = Icons.Default.Casino, contentDescription = "Pick Random", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (members.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$presentCount present • $absentCount absent",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSessionSavedToday) "✓ Saved for $selectedDate" else "Draft not saved yet",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSessionSavedToday) EmeraldPresentText else MaterialTheme.colorScheme.error
                            )
                        }

                        Button(
                            onClick = {
                                isSaving = true
                                viewModel.saveCurrentAttendance {
                                    isSaving = false
                                    Toast.makeText(context, "Attendance saved successfully!", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Saved attendance for ${currentGroup?.name}")
                                    }
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_save_attendance_page")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isSaving) "Saving..." else "Save Roll Call", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("take_attendance_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Date Picker Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedDate == today,
                        onClick = { viewModel.setSelectedDate(today) },
                        label = { Text("Today") },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedDate == yesterday,
                        onClick = { viewModel.setSelectedDate(yesterday) },
                        label = { Text("Yesterday") },
                        shape = RoundedCornerShape(10.dp)
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

            // Summary Stats Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Roll Call Progress",
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(text = "P: $presentCount", color = EmeraldPresentText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "A: $absentCount", color = RoseAbsentText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "L: $lateCount", color = AmberLateText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = "E: $excusedCount", color = IndigoExcusedText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick bulk action row + Add Person button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.markAllAs(AttendanceStatus.PRESENT) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("All Present", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { viewModel.markAllAs(AttendanceStatus.ABSENT) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("All Absent", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showAddMemberDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Add Member...", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }

            // Attendee rows
            if (members.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No members in ${currentGroup?.name ?: "this group"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add students or attendees now to take attendance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddMemberDialog = true },
                                modifier = Modifier.testTag("btn_empty_add_member")
                            ) {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Member to Roster")
                            }
                        }
                    }
                }
            } else {
                items(members, key = { it.id }) { member ->
                    val currentStatus = attendanceDraft[member.id] ?: AttendanceStatus.PRESENT
                    val note = notesDraft[member.id] ?: ""

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("take_attendance_row_${member.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    UserAvatar(
                                        name = member.name,
                                        colorHex = member.avatarColorHex,
                                        size = 38
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        if (member.identifier.isNotBlank()) {
                                            Text(
                                                text = "ID: ${member.identifier}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AttendanceSegmentedPicker(
                                        currentStatus = currentStatus,
                                        onStatusSelected = { status ->
                                            viewModel.setMemberStatus(member.id, status)
                                        }
                                    )

                                    // Quick note button
                                    IconButton(
                                        onClick = { activeMemberForNote = member },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = "Add Note",
                                            tint = if (note.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Quick delete member button
                                    IconButton(
                                        onClick = { memberToDelete = member },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete ${member.name}",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            if (note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = note,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, identifier, notes, colorHex ->
                viewModel.addMemberToCurrentGroup(name, identifier, notes, colorHex) {
                    showAddMemberDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added $name to roster")
                    }
                }
            }
        )
    }

    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Delete Member") },
            text = { Text("Are you sure you want to remove '${member.name}' from this group? Their past attendance records will remain saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(member) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted ${member.name}")
                            }
                        }
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
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
}
