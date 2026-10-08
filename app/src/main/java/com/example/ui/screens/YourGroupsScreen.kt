package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.components.EmptyGroupsCard
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.GroupAttendanceCard
import com.example.ui.components.NewGroupDialog
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourGroupsScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (groupId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isTopSearchActive by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    var groupToExportCsv by remember { mutableStateOf<GroupEntity?>(null) }
    var exportGroupMembers by remember { mutableStateOf<List<MemberEntity>>(emptyList()) }
    var exportGroupSessions by remember { mutableStateOf<List<AttendanceSessionEntity>>(emptyList()) }
    var exportGroupRecords by remember { mutableStateOf<List<AttendanceRecordEntity>>(emptyList()) }

    BackHandler {
        if (isTopSearchActive || searchQuery.isNotEmpty()) {
            isTopSearchActive = false
            searchQuery = ""
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(isTopSearchActive) {
        if (isTopSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    val todaySessionGroupIds = remember(allSessions) {
        allSessions.filter { it.date == viewModel.todayString }.map { it.groupId }.toSet()
    }

    val categories = remember(groups) {
        listOf("All") + groups.map { it.category }.distinct()
    }

    val filteredGroups = remember(groups, searchQuery, selectedCategory) {
        groups.filter { group ->
            val matchesSearch = group.name.contains(searchQuery, ignoreCase = true) ||
                    group.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || group.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (isTopSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search groups by name...",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.testTag("btn_clear_top_search")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                                .testTag("top_app_bar_search_input")
                        )
                    } else {
                        Column {
                            Text(
                                text = "Your Groups",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${groups.size} active roster${if (groups.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isTopSearchActive) {
                                isTopSearchActive = false
                                searchQuery = ""
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("btn_back_groups")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isTopSearchActive) "Close Search" else "Back to Home"
                        )
                    }
                },
                actions = {
                    if (!isTopSearchActive) {
                        IconButton(
                            onClick = { isTopSearchActive = true },
                            modifier = Modifier.testTag("btn_toggle_top_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Groups"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                isTopSearchActive = false
                                searchQuery = ""
                            },
                            modifier = Modifier.testTag("btn_close_top_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Search"
                            )
                        }
                    }

                    Button(
                        onClick = { showNewGroupDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_top_bar_new_group")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "New Group", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewGroupDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_new_group_inside_groups")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Group")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "New Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("groups_screen_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Prominent persistent Search Bar at top of content
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search groups by name...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("btn_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search text"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_groups_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Category Filter Chips
            if (categories.size > 1) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Prominent "Create New Group" action card at the top
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { showNewGroupDialog = true }
                        .testTag("card_action_create_group"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create a New Group",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Add a class, lab, team, or workshop roster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Search results counter or empty notification
            if (searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "Found ${filteredGroups.size} group${if (filteredGroups.size == 1) "" else "s"} matching \"$searchQuery\"",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // Groups listing
            if (filteredGroups.isEmpty()) {
                item {
                    if (searchQuery.isNotBlank() || selectedCategory != "All") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No groups matching \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Check the spelling or try clearing the search filter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategory = "All"
                                    },
                                    modifier = Modifier.testTag("btn_clear_filters")
                                ) {
                                    Text("Clear Filters")
                                }
                            }
                        }
                    } else {
                        EmptyGroupsCard(onCreateGroup = { showNewGroupDialog = true })
                    }
                }
            } else {
                items(filteredGroups, key = { "group_item_${it.id}" }) { group ->
                    val isLoggedToday = group.id in todaySessionGroupIds
                    GroupAttendanceCard(
                        group = group,
                        isLoggedToday = isLoggedToday,
                        onClick = { onNavigateToGroup(group.id) },
                        onExportCsv = {
                            coroutineScope.launch {
                                val (_, members, sessionsAndRecords) = viewModel.getGroupExportData(group.id)
                                exportGroupMembers = members
                                exportGroupSessions = sessionsAndRecords.first
                                exportGroupRecords = sessionsAndRecords.second
                                groupToExportCsv = group
                            }
                        }
                    )
                }
            }
        }
    }

    groupToExportCsv?.let { grp ->
        ExportCsvDialog(
            group = grp,
            members = exportGroupMembers,
            sessions = exportGroupSessions,
            records = exportGroupRecords,
            onDismissRequest = { groupToExportCsv = null }
        )
    }

    if (showNewGroupDialog) {
        NewGroupDialog(
            onDismiss = { showNewGroupDialog = false },
            onConfirm = { name, desc, category, colorHex ->
                viewModel.createGroup(name, desc, category, colorHex) { newGroupId ->
                    showNewGroupDialog = false
                    onNavigateToGroup(newGroupId)
                }
            }
        )
    }
}
