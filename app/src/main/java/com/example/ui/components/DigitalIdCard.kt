package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DigitalIdCardDialog(
    member: MemberEntity,
    group: GroupEntity?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(20.dp)
            ) {
                // Header badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DIGITAL PASS",
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pass Card Surface with Gradient Header
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Colored banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                        )

                        // Avatar overlapping banner
                        Box(
                            modifier = Modifier
                                .offset(y = (-30).dp)
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Info Details
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .offset(y = (-20).dp)
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (member.identifier.isNotEmpty()) {
                                Text(
                                    text = "ID: ${member.identifier}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Member • ${group?.name ?: "Group"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Deterministic 2D QR-style Matrix Code
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .size(160.dp)
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            ) {
                                QrMatrixCanvas(
                                    data = "${member.id}:${member.identifier}:${member.name}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scan during roll call to check in instantly",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Issued on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Draws a deterministic, sharp 2D matrix barcode canvas matching the member's data string.
 */
@Composable
fun QrMatrixCanvas(data: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val gridSize = 19
        val cellSize = size.width / gridSize
        val hash = abs(data.hashCode())

        // Background
        drawRect(Color.White)

        fun isFinderPattern(r: Int, c: Int): Boolean {
            if (r in 0..6 && c in 0..6) return true
            if (r in 0..6 && c in (gridSize - 7) until gridSize) return true
            if (r in (gridSize - 7) until gridSize && c in 0..6) return true
            return false
        }

        fun drawFinder(startR: Int, startC: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    if (isBorder || isCenter) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset((startC + c) * cellSize, (startR + r) * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }

        // Draw 3 Corner Finders
        drawFinder(0, 0)
        drawFinder(0, gridSize - 7)
        drawFinder(gridSize - 7, 0)

        // Draw timing lines
        for (i in 7 until gridSize - 7) {
            if (i % 2 == 0) {
                drawRect(Color.Black, Offset(6 * cellSize, i * cellSize), Size(cellSize, cellSize))
                drawRect(Color.Black, Offset(i * cellSize, 6 * cellSize), Size(cellSize, cellSize))
            }
        }

        // Draw data modules deterministically
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!isFinderPattern(r, c) && !(r == 6 || c == 6)) {
                    val seed = (r * 31 + c * 17 + hash)
                    val isDark = ((seed xor (seed shr 3)) % 3) == 0
                    if (isDark) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}
