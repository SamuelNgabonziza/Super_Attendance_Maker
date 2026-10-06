package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategoryBadge
import com.example.ui.components.MetricCard
import com.example.ui.components.SessionAttendanceBarChart
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyMetricsScreen(
    viewModel: AttendanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToGroup: (groupId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val presentTextColor = if (isDark) EmeraldPresentTextDark else EmeraldPresentText
    val lateTextColor = if (isDark) AmberLateTextDark else AmberLateText
    val absentTextColor = if (isDark) RoseAbsentTextDark else RoseAbsentText

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Key Attendance Metrics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Overall analytics & performance rates",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_metrics")
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
                .testTag("metrics_screen_scroll"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main Turnout Gauge Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TODAY'S OVERALL TURNOUT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { metrics.todayAttendanceRate / 100f },
                                modifier = Modifier.size(130.dp),
                                strokeWidth = 14.dp,
                                color = EmeraldPresent,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${metrics.todayAttendanceRate}%",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Present Rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${metrics.todayPresentCount}", fontWeight = FontWeight.Bold, color = presentTextColor, fontSize = 18.sp)
                                Text(text = "Present", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${metrics.todayLateCount}", fontWeight = FontWeight.Bold, color = lateTextColor, fontSize = 18.sp)
                                Text(text = "Late", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${metrics.todayAbsentCount}", fontWeight = FontWeight.Bold, color = absentTextColor, fontSize = 18.sp)
                                Text(text = "Absent", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            // 4 Core Metric Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Attendance Rate",
                        value = "${metrics.todayAttendanceRate}%",
                        subtitle = "Overall present/late",
                        icon = Icons.Default.PieChart,
                        iconTint = EmeraldPresent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Enrolled",
                        value = "${metrics.totalMembers}",
                        subtitle = "Across ${metrics.totalGroups} groups",
                        icon = Icons.Default.People,
                        iconTint = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Check-ins Today",
                        value = "${metrics.groupsCheckedInToday}/${metrics.totalGroups}",
                        subtitle = "Rosters completed",
                        icon = Icons.Default.Today,
                        iconTint = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Sessions",
                        value = "${allSessions.size}",
                        subtitle = "Historical logs",
                        icon = Icons.Default.FactCheck,
                        iconTint = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Session Attendance Trends Bar Chart
            if (allSessions.isNotEmpty()) {
                item {
                    SessionAttendanceBarChart(
                        sessions = allSessions,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Group Comparison Breakdown
            item {
                Text(
                    text = "Performance by Group",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(groups, key = { "metric_group_${it.id}" }) { group ->
                val groupSessions = allSessions.filter { it.groupId == group.id }
                val totalAttendees = groupSessions.sumOf { it.totalCount }
                val totalPresent = groupSessions.sumOf { it.presentCount + it.lateCount }
                val avgRate = if (totalAttendees > 0) (totalPresent * 100) / totalAttendees else 0

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
                            Column {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                CategoryBadge(category = group.category)
                            }

                            Text(
                                text = "$avgRate%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (avgRate >= 80) EmeraldPresentText else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { avgRate / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (avgRate >= 80) EmeraldPresent else Color(0xFFF59E0B),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${groupSessions.size} sessions recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
