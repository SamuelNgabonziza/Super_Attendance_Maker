package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.MemberEntity
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import com.example.ui.viewmodel.AttendanceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickScannerModal(
    members: List<MemberEntity>,
    onCheckInMember: (memberId: Long, status: AttendanceStatus, timeStr: String) -> Unit,
    onDismiss: () -> Unit
) {
    var scannedQuery by remember { mutableStateOf("") }
    var lastCheckedInMember by remember { mutableStateOf<Pair<MemberEntity, String>?>(null) }
    var markAsLate by remember { mutableStateOf(false) }

    val filteredMembers = remember(scannedQuery, members) {
        if (scannedQuery.isBlank()) {
            members
        } else {
            members.filter {
                it.name.contains(scannedQuery, ignoreCase = true) ||
                it.identifier.contains(scannedQuery, ignoreCase = true) ||
                it.id.toString().contains(scannedQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quick Check-in Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Scan or type Member ID / Name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode toggle (Present vs Late)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !markAsLate,
                        onClick = { markAsLate = false },
                        label = { Text("Standard: Present", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusPresent.copy(alpha = 0.15f),
                            selectedLabelColor = StatusPresent
                        )
                    )
                    FilterChip(
                        selected = markAsLate,
                        onClick = { markAsLate = true },
                        label = { Text("Grace Period: Late", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusLate.copy(alpha = 0.15f),
                            selectedLabelColor = StatusLate
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search / Barcode Input Field
                OutlinedTextField(
                    value = scannedQuery,
                    onValueChange = { query ->
                        scannedQuery = query
                        val exactMatch = members.find {
                            it.identifier.equals(query.trim(), ignoreCase = true) ||
                            it.id.toString() == query.trim()
                        }
                        if (exactMatch != null && query.isNotBlank()) {
                            val time = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                            val status = if (markAsLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
                            onCheckInMember(exactMatch.id, status, time)
                            lastCheckedInMember = exactMatch to time
                            scannedQuery = ""
                        }
                    },
                    placeholder = { Text("Scan barcode or type ID...") },
                    leadingIcon = {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (scannedQuery.isNotEmpty()) {
                            IconButton(onClick = { scannedQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Verified banner
                AnimatedVisibility(
                    visible = lastCheckedInMember != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    lastCheckedInMember?.let { (member, time) ->
                        Surface(
                            color = StatusPresent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusPresent.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusPresent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${member.name} Checked In!",
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPresent,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${if (markAsLate) "Late" else "Present"} • Time: $time",
                                        fontSize = 11.sp,
                                        color = StatusPresent.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap any member to check in instantly:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                    val status = if (markAsLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
                                    onCheckInMember(member.id, status, time)
                                    lastCheckedInMember = member to time
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (member.identifier.isNotEmpty()) {
                                            Text(
                                                text = member.identifier,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                        val status = if (markAsLate) AttendanceStatus.LATE else AttendanceStatus.PRESENT
                                        onCheckInMember(member.id, status, time)
                                        lastCheckedInMember = member to time
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Check In", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
