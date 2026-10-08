package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.NewGroupDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetail: (groupId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var groupForAddingMember by remember { mutableStateOf<GroupEntity?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupEntity?>(null) }
    var memberToDelete by remember { mutableStateOf<Pair<GroupEntity, MemberEntity>?>(null) }

    var groupForExportCsv by remember { mutableStateOf<GroupEntity?>(null) }
    var exportGroupMembers by remember { mutableStateOf<List<MemberEntity>>(emptyList()) }
    var exportGroupSessions by remember { mutableStateOf<List<AttendanceSessionEntity>>(emptyList()) }
    var exportGroupRecords by remember { mutableStateOf<List<AttendanceRecordEntity>>(emptyList()) }

    // Expanded group state for managing members inside settings
    var expandedGroupId by remember { mutableStateOf<Long?>(null) }
    val groupMembers by viewModel.currentGroupMembers.collectAsStateWithLifecycle()

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings & Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage groups, members, delete items & appearance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_settings")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Profile & Organizer Details
            item {
                Text(
                    text = "ORGANIZER PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = userName,
                            colorHex = "#2563EB",
                            size = 50
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userRole,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit")
                        }
                    }
                }
            }

            // Section 2: App Theme & Appearance (Light / Dark / System)
            item {
                Text(
                    text = "THEME & DISPLAY MODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose App Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Currently in ${if (isDark) "Dark Theme" else "Light Theme"} mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = themeMode == "LIGHT",
                                onClick = { viewModel.setThemeMode("LIGHT") },
                                label = { Text("Light") },
                                leadingIcon = {
                                    Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = themeMode == "DARK",
                                onClick = { viewModel.setThemeMode("DARK") },
                                label = { Text("Dark") },
                                leadingIcon = {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = themeMode == "SYSTEM",
                                onClick = { viewModel.setThemeMode("SYSTEM") },
                                label = { Text("System") },
                                leadingIcon = {
                                    Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 3: Group & Member Management (Add members, delete groups, delete members)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANAGE GROUPS & ROSTERS (${groups.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Button(
                        onClick = { showNewGroupDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (groups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "No groups yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showNewGroupDialog = true }) {
                                Text("Create First Group")
                            }
                        }
                    }
                }
            } else {
                items(groups, key = { "settings_group_${it.id}" }) { group ->
                    val isExpanded = expandedGroupId == group.id
                    val barColor = try {
                        Color(android.graphics.Color.parseColor(group.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(barColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = barColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${group.category} • ${if (group.description.isNotBlank()) group.description else "No description"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                // Quick Export CSV Action
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val (_, members, sessionsAndRecords) = viewModel.getGroupExportData(group.id)
                                            exportGroupMembers = members
                                            exportGroupSessions = sessionsAndRecords.first
                                            exportGroupRecords = sessionsAndRecords.second
                                            groupForExportCsv = group
                                        }
                                    },
                                    modifier = Modifier.testTag("btn_export_group_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "Export CSV for ${group.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Quick Delete Group Action
                                IconButton(
                                    onClick = { groupToDelete = group },
                                    modifier = Modifier.testTag("btn_delete_group_${group.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Group ${group.name}",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row for group
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Add Member button
                                Button(
                                    onClick = {
                                        viewModel.selectGroup(group.id)
                                        groupForAddingMember = group
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Add Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // View Members / Expand button
                                OutlinedButton(
                                    onClick = {
                                        if (isExpanded) {
                                            expandedGroupId = null
                                        } else {
                                            viewModel.selectGroup(group.id)
                                            expandedGroupId = group.id
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isExpanded) "Hide Roster" else "View Members", fontSize = 12.sp)
                                }

                                // Open Detail
                                IconButton(
                                    onClick = { onNavigateToGroupDetail(group.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Open group details",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Expanded Members List inside this group
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Roster Members (${groupMembers.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                if (groupMembers.isEmpty()) {
                                    Text(
                                        text = "No members added yet. Tap '+ Add Member' above.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        groupMembers.forEach { member ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    UserAvatar(
                                                        name = member.name,
                                                        colorHex = member.avatarColorHex,
                                                        size = 32
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = member.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        if (member.identifier.isNotBlank()) {
                                                            Text(
                                                                text = "ID: ${member.identifier}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.outline
                                                            )
                                                        }
                                                    }

                                                    // Delete Member button
                                                    IconButton(
                                                        onClick = { memberToDelete = Pair(group, member) },
                                                        modifier = Modifier.size(30.dp)
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Data Export & Reporting
            item {
                Text(
                    text = "DATA EXPORT & REPORTING",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Export Attendance (CSV)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Generate and share CSV files for reporting and analysis",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (groups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Select group roster to export:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            groups.forEach { grp ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                val (_, members, sessionsAndRecords) = viewModel.getGroupExportData(grp.id)
                                                exportGroupMembers = members
                                                exportGroupSessions = sessionsAndRecords.first
                                                exportGroupRecords = sessionsAndRecords.second
                                                groupForExportCsv = grp
                                            }
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = grp.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = grp.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Export CSV",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.FileDownload,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Create groups and record roll call to export CSV attendance reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: Edit Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = userName,
            currentRole = userRole,
            onDismiss = { showEditProfileDialog = false },
            onConfirm = { name, role ->
                viewModel.updateProfile(name, role)
                showEditProfileDialog = false
            }
        )
    }

    // Dialog 2: New Group Dialog
    if (showNewGroupDialog) {
        NewGroupDialog(
            onDismiss = { showNewGroupDialog = false },
            onConfirm = { name, desc, category, colorHex ->
                viewModel.createGroup(name, desc, category, colorHex) { newGroupId ->
                    showNewGroupDialog = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Created group '$name'")
                    }
                }
            }
        )
    }

    // Dialog 3: Add Member to Selected Group Dialog
    groupForAddingMember?.let { g ->
        AddMemberDialog(
            onDismiss = { groupForAddingMember = null },
            onConfirm = { name, id, notes, colorHex ->
                viewModel.addMemberToCurrentGroup(name, id, notes, colorHex) {
                    groupForAddingMember = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Added '$name' to ${g.name}")
                    }
                }
            }
        )
    }

    // Dialog 4: Confirm Delete Group Dialog
    groupToDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete '${g.name}'? This will permanently remove the group roster, members, and all related check-in records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGroup(g) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Deleted group '${g.name}'")
                            }
                        }
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Group")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 5: Confirm Delete Member Dialog
    memberToDelete?.let { (g, member) ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Delete Member") },
            text = { Text("Are you sure you want to remove '${member.name}' from '${g.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMember(member) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Removed '${member.name}'")
                            }
                        }
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Member")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 6: Export CSV Dialog
    groupForExportCsv?.let { grp ->
        ExportCsvDialog(
            group = grp,
            members = exportGroupMembers,
            sessions = exportGroupSessions,
            records = exportGroupRecords,
            onDismissRequest = { groupForExportCsv = null }
        )
    }
}
