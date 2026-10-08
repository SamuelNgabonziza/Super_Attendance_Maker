package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.ActivityLogEntity
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.DashboardMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    onNavigateToGroup: (groupId: Long) -> Unit,
    onNavigateToTakeAttendance: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToMetrics: () -> Unit = {},
    onNavigateToAuditLogs: () -> Unit = {},
    onNavigateToRecentActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }

    val prettyDate = remember {
        SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date())
    }

    val todaySessionGroupIds = remember(allSessions) {
        allSessions.filter { it.date == viewModel.todayString }.map { it.groupId }.toSet()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // 1. Personalized Header with Live Theme Toggle Quick Button
            item {
                PersonalizedHeader(
                    userName = userName,
                    userRole = userRole,
                    date = prettyDate,
                    isDark = isDark,
                    themeMode = themeMode,
                    onToggleTheme = { viewModel.toggleThemeMode() },
                    onEditProfile = { showEditProfileDialog = true },
                    onOpenSettings = onNavigateToSettings
                )
            }

            // 2. Hero Visual Banner Card (Kept visually intact with smart theme blending)
            item {
                DashboardHeroBanner(
                    groupsCheckedToday = metrics.groupsCheckedInToday,
                    totalGroups = metrics.totalGroups,
                    rate = metrics.todayAttendanceRate,
                    isDark = isDark,
                    onTakeAttendance = onNavigateToTakeAttendance
                )
            }

            // Spacing
            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 3. Card 1: Take Attendance
            item {
                val pendingCount = groups.count { it.id !in todaySessionGroupIds }
                val takeAttSub = if (groups.isEmpty()) {
                    "No groups yet • Open to start"
                } else if (pendingCount == 0) {
                    "All ${groups.size} groups logged today!"
                } else {
                    "$pendingCount group${if (pendingCount == 1) "" else "s"} pending check-in"
                }

                InteractiveHubCard(
                    title = "Take Attendance",
                    summary = takeAttSub,
                    badgeText = if (groups.isEmpty()) "Start" else if (pendingCount == 0) "Completed" else "$pendingCount Pending",
                    badgeColor = if (pendingCount == 0 && groups.isNotEmpty()) {
                        if (isDark) EmeraldPresentTextDark else EmeraldPresentText
                    } else {
                        if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                    },
                    icon = Icons.Default.HowToReg,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF1E40AF), Color(0xFF1E3A8A))
                    } else {
                        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                    },
                    isDark = isDark,
                    onClick = onNavigateToTakeAttendance,
                    testTag = "hub_card_take_attendance"
                )
            }

            // 4. Card 2: Your Groups
            item {
                val groupsSub = "${groups.size} active roster${if (groups.size == 1) "" else "s"} enrolled"
                InteractiveHubCard(
                    title = "Your Groups",
                    summary = groupsSub,
                    badgeText = "${groups.size} Groups",
                    badgeColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED),
                    icon = Icons.Default.People,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF6B21A8), Color(0xFF581C87))
                    } else {
                        listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))
                    },
                    isDark = isDark,
                    onClick = onNavigateToGroups,
                    testTag = "hub_card_your_groups"
                )
            }

            // 5. Card 3: Key Attendance Metrics
            item {
                val metricsSub = "${metrics.todayAttendanceRate}% attendance rate • ${metrics.todayPresentCount} present today"
                InteractiveHubCard(
                    title = "Key Attendance Metrics",
                    summary = metricsSub,
                    badgeText = "${metrics.todayAttendanceRate}% Rate",
                    badgeColor = if (isDark) EmeraldPresentTextDark else EmeraldPresentText,
                    icon = Icons.Default.PieChart,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF065F46), Color(0xFF064E3B))
                    } else {
                        listOf(Color(0xFF059669), Color(0xFF047857))
                    },
                    isDark = isDark,
                    onClick = onNavigateToMetrics,
                    testTag = "hub_card_key_metrics"
                )
            }

            // 6. Card 4: Audit Logs
            item {
                val auditSub = "${recentActivities.size} recorded system & attendance events"
                InteractiveHubCard(
                    title = "Audit Logs",
                    summary = auditSub,
                    badgeText = "Security & Trail",
                    badgeColor = if (isDark) AmberLateTextDark else Color(0xFFB45309),
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF92400E), Color(0xFF78350F))
                    } else {
                        listOf(Color(0xFFD97706), Color(0xFFB45309))
                    },
                    isDark = isDark,
                    onClick = onNavigateToAuditLogs,
                    testTag = "hub_card_audit_logs"
                )
            }

            // 7. Card 5: Recent Activity Updates
            item {
                val lastActivityTitle = recentActivities.firstOrNull()?.title ?: "No updates yet"
                val recentSub = if (recentActivities.isEmpty()) "Live stream of recent check-ins & roster changes" else "Latest: $lastActivityTitle"
                InteractiveHubCard(
                    title = "Recent Activity Updates",
                    summary = recentSub,
                    badgeText = "Live Updates",
                    badgeColor = if (isDark) Color(0xFFD8B4FE) else Color(0xFF9333EA),
                    icon = Icons.Default.NotificationsActive,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF7E22CE), Color(0xFF6B21A8))
                    } else {
                        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
                    },
                    isDark = isDark,
                    onClick = onNavigateToRecentActivity,
                    testTag = "hub_card_recent_activity"
                )
            }

            // 8. Card 6: Settings & Management
            item {
                InteractiveHubCard(
                    title = "Settings",
                    summary = "Delete & add groups, manage members, edit roster & profile",
                    badgeText = "Roster & Controls",
                    badgeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    icon = Icons.Default.Settings,
                    cardGradient = if (isDark) {
                        listOf(Color(0xFF0369A1), Color(0xFF075985))
                    } else {
                        listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                    },
                    isDark = isDark,
                    onClick = onNavigateToSettings,
                    testTag = "hub_card_settings"
                )
            }
        }
    }

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
}

@Composable
fun InteractiveHubCard(
    title: String,
    summary: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    cardGradient: List<Color>,
    isDark: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val cardBg = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (isDark) {
        BorderStroke(1.dp, Color(0xFF2E3D59))
    } else {
        BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vibrant Icon Box with Gradient & dynamic shadow glow
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(cardGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = if (isDark) 0.22f else 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron Indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PersonalizedHeader(
    userName: String,
    userRole: String,
    date: String,
    isDark: Boolean,
    themeMode: String,
    onToggleTheme: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Hello, $userName",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(start = 4.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = userRole,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Settings Quick Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .testTag("settings_button_header")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Theme Toggle Quick Button
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Theme: $themeMode",
                        tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                UserAvatar(
                    name = userName,
                    colorHex = "#2563EB",
                    size = 46,
                    modifier = Modifier.clickable { onOpenSettings() }
                )
            }
        }
    }
}

@Composable
fun DashboardHeroBanner(
    groupsCheckedToday: Int,
    totalGroups: Int,
    rate: Int,
    isDark: Boolean,
    onTakeAttendance: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dashboard_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF172554) else MaterialTheme.colorScheme.primaryContainer
        ),
        border = if (isDark) BorderStroke(1.dp, Color(0xFF1E3A8A)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Banner illustration asset
                Image(
                    painter = painterResource(id = R.drawable.dashboard_hero_1790234716808),
                    contentDescription = "Attendance Dashboard Illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Soft gradient overlay to blend seamlessly with theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDark) {
                                    listOf(Color.Transparent, Color(0xEE090D16))
                                } else {
                                    listOf(Color.Transparent, Color(0xCC0F172A))
                                }
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (groupsCheckedToday >= totalGroups && totalGroups > 0) EmeraldPresent else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totalGroups == 0) "Get started with your first group"
                               else if (groupsCheckedToday >= totalGroups) "All groups checked-in today!"
                               else "$groupsCheckedToday of $totalGroups groups logged today",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Roll Call & Check-in",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (totalGroups == 0) "Create your class or team roster to start."
                               else "Tap below to log attendance for your members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onTakeAttendance,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("hero_take_attendance_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HowToReg,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Check-in", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
