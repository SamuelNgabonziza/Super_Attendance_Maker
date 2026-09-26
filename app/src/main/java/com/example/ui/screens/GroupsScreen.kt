package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Group
import com.example.data.model.Member
import com.example.ui.AttendanceViewModel
import com.example.ui.components.BatchImportDialog
import com.example.ui.components.DigitalIdCardDialog
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusLate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.groups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val members by viewModel.currentGroupMembers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val memberStats by viewModel.currentMemberAnalytics.collectAsState()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var groupToEdit by remember { mutableStateOf<Group?>(null) }
    var memberForPass by remember { mutableStateOf<Member?>(null) }

    val statsMap = remember(memberStats) { memberStats.associateBy { it.member.id } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Group Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Groups & Classes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = { showAddGroupDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("add_group_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Group", fontSize = 12.sp)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(groups) { group ->
                val isSelected = group.id == (selectedGroupId ?: groups.firstOrNull()?.id)
                val color = try {
                    Color(android.graphics.Color.parseColor(group.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectGroup(group.id) },
                    label = {
                        Text(
                            text = group.name,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Group Card Info
        selectedGroup?.let { group ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (group.description.isNotEmpty()) {
                                Text(
                                    text = group.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "🗓 ${group.schedule}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "🎯 Min: ${group.passingThreshold}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        IconButton(onClick = { groupToEdit = group }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Group Details"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${members.size} Enrolled Members",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showBatchImportDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { showAddMemberDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("add_member_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by name, ID or role...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("member_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Members Roster List
        if (members.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.GroupOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No members match \"$searchQuery\"" else "No members added to this group yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(members, key = { it.id }) { member ->
                    val stat = statsMap[member.id]
                    MemberRowItem(
                        member = member,
                        ratePct = stat?.let { (it.attendanceRate * 100).toInt() },
                        isAtRisk = stat?.isAtRisk ?: false,
                        streak = stat?.currentStreak ?: 0,
                        onViewPass = { memberForPass = member },
                        onEdit = { memberToEdit = member },
                        onDelete = { viewModel.deleteMember(member.id) }
                    )
                }
            }
        }
    }

    // Digital ID Pass Modal
    memberForPass?.let { mbr ->
        DigitalIdCardDialog(
            member = mbr,
            group = selectedGroup,
            onDismiss = { memberForPass = null }
        )
    }

    // Batch Import Modal
    if (showBatchImportDialog) {
        BatchImportDialog(
            onDismiss = { showBatchImportDialog = false },
            onImportMembers = { list ->
                viewModel.addBatchMembers(list)
            }
        )
    }

    // Add Group Dialog
    if (showAddGroupDialog) {
        AddEditGroupDialog(
            group = null,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, desc, sched, col, passThreshold ->
                viewModel.addGroup(name, desc, sched, col, passThreshold)
                showAddGroupDialog = false
            }
        )
    }

    // Edit Group Dialog
    groupToEdit?.let { grp ->
        AddEditGroupDialog(
            group = grp,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, desc, sched, col, passThreshold ->
                viewModel.updateGroup(grp.copy(name = name, description = desc, schedule = sched, colorHex = col, passingThreshold = passThreshold))
                groupToEdit = null
            },
            onDelete = {
                viewModel.deleteGroup(grp.id)
                groupToEdit = null
            }
        )
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AddEditMemberDialog(
            member = null,
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, id, phone, role ->
                viewModel.addMember(name, id, phone, role)
                showAddMemberDialog = false
            }
        )
    }

    // Edit Member Dialog
    memberToEdit?.let { mbr ->
        AddEditMemberDialog(
            member = mbr,
            onDismiss = { memberToEdit = null },
            onConfirm = { name, id, phone, role ->
                viewModel.updateMember(mbr.copy(fullName = name, identifier = id, phone = phone, role = role))
                memberToEdit = null
            },
            onDelete = {
                viewModel.deleteMember(mbr.id)
                memberToEdit = null
            }
        )
    }
}

@Composable
fun MemberRowItem(
    member: Member,
    ratePct: Int?,
    isAtRisk: Boolean,
    streak: Int,
    onViewPass: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.fullName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isAtRisk) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StatusAbsent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "At Risk",
                                    color = StatusAbsent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (streak >= 3) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = StatusLate.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "🔥 $streak",
                                    color = StatusLate,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (member.identifier.isNotEmpty()) {
                            Text(
                                text = member.identifier,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(member.role, fontSize = 11.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                        if (ratePct != null) {
                            Text(
                                text = "$ratePct% rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAtRisk) StatusAbsent else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Digital Pass Button
                IconButton(onClick = onViewPass) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "View Digital Pass",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Member",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditGroupDialog(
    group: Group?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, sched: String, color: String, passThreshold: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var description by remember { mutableStateOf(group?.description ?: "") }
    var schedule by remember { mutableStateOf(group?.schedule ?: "Mon, Wed, Fri") }
    var selectedColor by remember { mutableStateOf(group?.colorHex ?: "#1E3A8A") }
    var passingThreshold by remember { mutableStateOf(group?.passingThreshold ?: 75) }

    val colors = listOf("#1E3A8A", "#0D9488", "#DC2626", "#7C3AED", "#EA580C", "#059669")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "Create Group / Class" else "Edit Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Schedule (e.g. Mon, Wed, Fri)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Min Passing Attendance:")
                    Text("$passingThreshold%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = passingThreshold.toFloat(),
                    onValueChange = { passingThreshold = it.toInt() },
                    valueRange = 50f..100f,
                    steps = 9
                )

                Text(
                    text = "Color Tag",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex }
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim(), schedule.trim(), selectedColor, passingThreshold)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (group == null) "Create" else "Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun AddEditMemberDialog(
    member: Member?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, id: String, phone: String, role: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var fullName by remember { mutableStateOf(member?.fullName ?: "") }
    var identifier by remember { mutableStateOf(member?.identifier ?: "") }
    var phone by remember { mutableStateOf(member?.phone ?: "") }
    var role by remember { mutableStateOf(member?.role ?: "Student") }

    val roles = listOf("Student", "Member", "Lead", "Staff", "Guest")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "Add New Member" else "Edit Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("ID / Roll No / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Role",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(roles) { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onConfirm(fullName.trim(), identifier.trim(), phone.trim(), role)
                    }
                },
                enabled = fullName.isNotBlank()
            ) {
                Text(if (member == null) "Add" else "Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
