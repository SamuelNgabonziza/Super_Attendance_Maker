package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.AppTab
import com.example.ui.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AttendanceViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()

    // Handle back button: if not on GROUPS tab, switch back to GROUPS tab
    BackHandler(enabled = currentTab != AppTab.GROUPS) {
        viewModel.selectTab(AppTab.GROUPS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AttendEase",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            selectedGroup?.let {
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.GROUPS,
                    onClick = { viewModel.selectTab(AppTab.GROUPS) },
                    icon = { Icon(Icons.Default.Groups, contentDescription = "Groups") },
                    label = { Text("Groups") },
                    modifier = Modifier.testTag("nav_groups")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.SESSIONS,
                    onClick = { viewModel.selectTab(AppTab.SESSIONS) },
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = "Roll Call") },
                    label = { Text("Roll Call") },
                    modifier = Modifier.testTag("nav_sessions")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.ANALYTICS,
                    onClick = { viewModel.selectTab(AppTab.ANALYTICS) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    modifier = Modifier.testTag("nav_analytics")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.EXPORT,
                    onClick = { viewModel.selectTab(AppTab.EXPORT) },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Export") },
                    label = { Text("Export") },
                    modifier = Modifier.testTag("nav_export")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                AppTab.GROUPS -> GroupsScreen(viewModel = viewModel)
                AppTab.SESSIONS -> RollCallScreen(viewModel = viewModel)
                AppTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                AppTab.EXPORT -> ExportScreen(viewModel = viewModel)
            }
        }
    }
}
