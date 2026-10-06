package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.GroupDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KeyMetricsScreen
import com.example.ui.screens.RecentActivityUpdatesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TakeAttendanceScreen
import com.example.ui.screens.YourGroupsScreen
import com.example.ui.theme.AttendEaseTheme
import com.example.ui.viewmodel.AttendanceViewModel

enum class AppDestination {
    HOME,
    TAKE_ATTENDANCE,
    YOUR_GROUPS,
    KEY_METRICS,
    AUDIT_LOGS,
    RECENT_ACTIVITY,
    SETTINGS,
    GROUP_DETAIL
}

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemInDark = isSystemInDarkTheme()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            AttendEaseTheme(darkTheme = isDark) {
                var currentDestination by remember { mutableStateOf(AppDestination.HOME) }
                var selectedGroupId by remember { mutableStateOf<Long?>(null) }
                // Track previous destination to navigate back cleanly
                var previousDestination by remember { mutableStateOf(AppDestination.HOME) }

                fun navigateTo(dest: AppDestination) {
                    previousDestination = currentDestination
                    currentDestination = dest
                }

                when (currentDestination) {
                    AppDestination.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToGroup = { id ->
                                selectedGroupId = id
                                navigateTo(AppDestination.GROUP_DETAIL)
                            },
                            onNavigateToTakeAttendance = {
                                navigateTo(AppDestination.TAKE_ATTENDANCE)
                            },
                            onNavigateToGroups = {
                                navigateTo(AppDestination.YOUR_GROUPS)
                            },
                            onNavigateToMetrics = {
                                navigateTo(AppDestination.KEY_METRICS)
                            },
                            onNavigateToAuditLogs = {
                                navigateTo(AppDestination.AUDIT_LOGS)
                            },
                            onNavigateToRecentActivity = {
                                navigateTo(AppDestination.RECENT_ACTIVITY)
                            },
                            onNavigateToSettings = {
                                navigateTo(AppDestination.SETTINGS)
                            }
                        )
                    }

                    AppDestination.TAKE_ATTENDANCE -> {
                        TakeAttendanceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onNavigateToGroupDetail = { id ->
                                selectedGroupId = id
                                navigateTo(AppDestination.GROUP_DETAIL)
                            }
                        )
                    }

                    AppDestination.YOUR_GROUPS -> {
                        YourGroupsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onNavigateToGroup = { id ->
                                selectedGroupId = id
                                navigateTo(AppDestination.GROUP_DETAIL)
                            }
                        )
                    }

                    AppDestination.KEY_METRICS -> {
                        KeyMetricsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onNavigateToGroup = { id ->
                                selectedGroupId = id
                                navigateTo(AppDestination.GROUP_DETAIL)
                            }
                        )
                    }

                    AppDestination.AUDIT_LOGS -> {
                        AuditLogsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME }
                        )
                    }

                    AppDestination.RECENT_ACTIVITY -> {
                        RecentActivityUpdatesScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME }
                        )
                    }

                    AppDestination.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { currentDestination = AppDestination.HOME },
                            onNavigateToGroupDetail = { id ->
                                selectedGroupId = id
                                navigateTo(AppDestination.GROUP_DETAIL)
                            }
                        )
                    }

                    AppDestination.GROUP_DETAIL -> {
                        BackHandler {
                            currentDestination = previousDestination
                        }
                        if (selectedGroupId != null) {
                            GroupDetailScreen(
                                groupId = selectedGroupId!!,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    currentDestination = previousDestination
                                }
                            )
                        } else {
                            currentDestination = AppDestination.HOME
                        }
                    }
                }
            }
        }
    }
}
