package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemberAnalytics
import com.example.ui.AttendanceViewModel
import com.example.ui.components.SessionAttendanceBarChart
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent

@Composable
fun AnalyticsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val analytics by viewModel.currentGroupAnalytics.collectAsState()
    val memberStats by viewModel.currentMemberAnalytics.collectAsState()
    val sessions by viewModel.currentGroupSessions.collectAsState()

    var showOnlyAtRisk by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Attendance Analytics & Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedGroup?.name ?: "All Groups"} • Passing: ${selectedGroup?.passingThreshold ?: 75}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (analytics == null || analytics!!.totalSessions == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.InsertChartOutlined,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No analytics yet",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Take attendance across sessions to view insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            val stats = analytics!!
            val overallPct = (stats.averageAttendanceRate * 100).toInt()

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Top KPI Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Average Attendance",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = "$overallPct%",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (overallPct >= (selectedGroup?.passingThreshold ?: 75)) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = if (overallPct >= (selectedGroup?.passingThreshold ?: 75)) StatusPresent else StatusAbsent,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { stats.averageAttendanceRate },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (overallPct >= (selectedGroup?.passingThreshold ?: 75)) StatusPresent else StatusAbsent,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                QuickStat("Sessions", stats.totalSessions.toString())
                                QuickStat("Enrolled", stats.totalMembers.toString())
                                QuickStat("At-Risk", stats.atRiskCount.toString())
                            }
                        }
                    }
                }

                // Interactive Bar Chart of Recent Sessions
                item {
                    SessionAttendanceBarChart(sessions = sessions)
                }

                // Breakdown Distribution Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Status Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val total = (stats.presentCount + stats.absentCount + stats.lateCount + stats.excusedCount).coerceAtLeast(1)
                            DistributionRow("Present", stats.presentCount, total, StatusPresent)
                            Spacer(modifier = Modifier.height(6.dp))
                            DistributionRow("Late", stats.lateCount, total, StatusLate)
                            Spacer(modifier = Modifier.height(6.dp))
                            DistributionRow("Absent", stats.absentCount, total, StatusAbsent)
                            Spacer(modifier = Modifier.height(6.dp))
                            DistributionRow("Excused", stats.excusedCount, total, StatusExcused)
                        }
                    }
                }

                // Member Ranking / Leaderboard with Filter Toggle
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Member Attendance Ranking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilterChip(
                            selected = showOnlyAtRisk,
                            onClick = { showOnlyAtRisk = !showOnlyAtRisk },
                            label = { Text("At Risk Only (${stats.atRiskCount})", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }

                val displayedStats = if (showOnlyAtRisk) {
                    memberStats.filter { it.isAtRisk }
                } else {
                    memberStats
                }

                if (displayedStats.isEmpty() && showOnlyAtRisk) {
                    item {
                        Surface(
                            color = StatusPresent.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPresent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "No at-risk members! Everyone meets the ${selectedGroup?.passingThreshold ?: 75}% threshold.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StatusPresent
                                )
                            }
                        }
                    }
                } else {
                    items(displayedStats, key = { it.member.id }) { stat ->
                        MemberStatCard(stat)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStat(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun DistributionRow(label: String, count: Int, total: Int, color: Color) {
    val fraction = count.toFloat() / total
    val pct = (fraction * 100).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(64.dp)
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$count ($pct%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
fun MemberStatCard(stat: MemberAnalytics) {
    val ratePct = (stat.attendanceRate * 100).toInt()
    val rateColor = when {
        ratePct >= 85 -> StatusPresent
        ratePct >= 70 -> StatusLate
        else -> StatusAbsent
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stat.member.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stat.isAtRisk) {
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
                    if (stat.currentStreak >= 3) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = StatusLate.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "🔥 ${stat.currentStreak} Streak",
                                color = StatusLate,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${stat.presentCount} Present • ${stat.lateCount} Late • ${stat.absentCount} Absent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { stat.attendanceRate },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = rateColor,
                    trackColor = rateColor.copy(alpha = 0.2f)
                )
            }

            Surface(
                color = rateColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$ratePct%",
                    color = rateColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
