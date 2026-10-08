package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import com.example.ui.theme.EmeraldPresentText
import com.example.util.CsvExportHelper
import com.example.util.CsvReportFormat
import com.example.util.DateFilterRange

@Composable
fun ExportCsvDialog(
    group: GroupEntity,
    members: List<MemberEntity>,
    sessions: List<AttendanceSessionEntity>,
    records: List<AttendanceRecordEntity>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(CsvReportFormat.DETAILED) }
    var selectedDateRange by remember { mutableStateOf(DateFilterRange.ALL) }

    // Generate CSV content reactively whenever format or date filter changes
    val csvContent by remember(group, members, sessions, records, selectedFormat, selectedDateRange) {
        derivedStateOf {
            CsvExportHelper.generateCsv(
                group = group,
                members = members,
                sessions = sessions,
                records = records,
                format = selectedFormat,
                dateFilter = selectedDateRange
            )
        }
    }

    val previewLines by remember(csvContent) {
        derivedStateOf {
            csvContent.lines().take(16).joinToString("\n")
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
                .testTag("export_csv_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
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
                                text = "Export Attendance CSV",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_export_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Configuration Options
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Quick Stats Tag
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatPill(label = "Members", value = "${members.size}")
                            StatPill(label = "Sessions", value = "${sessions.size}")
                            StatPill(label = "Records", value = "${records.size}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Report Format Selector
                    Text(
                        text = "REPORT FORMAT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    CsvReportFormat.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFormat = format }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFormat = format },
                                    modifier = Modifier.testTag("radio_format_${format.name}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = format.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = format.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Date Filter Range
                    Text(
                        text = "DATE RANGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateFilterRange.values().forEach { range ->
                            val isSelected = selectedDateRange == range
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDateRange = range },
                                label = { Text(range.label) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("filter_chip_${range.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Live CSV Preview Window
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CSV PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${csvContent.toByteArray().size / 1024 + 1} KB • UTF-8",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 160.dp)
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = previewLines,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            CsvExportHelper.copyCsvToClipboard(context, csvContent, group.name)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_copy_csv_clipboard"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val success = CsvExportHelper.shareCsvFile(
                                context = context,
                                groupName = group.name,
                                csvContent = csvContent,
                                reportFormat = selectedFormat
                            )
                            if (success) {
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_share_csv_file"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share CSV File", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
