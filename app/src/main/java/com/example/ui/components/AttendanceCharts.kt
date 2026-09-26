package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceSession
import com.example.data.model.AttendanceStatus
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusExcused
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionAttendanceBarChart(
    sessions: List<AttendanceSession>,
    modifier: Modifier = Modifier
) {
    if (sessions.isEmpty()) return

    val reversedSessions = sessions.take(7).reversed()
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Session Attendance Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "Recent session turnout rates (%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val chartHeight = size.height - 30.dp.toPx()
                val barCount = reversedSessions.size
                val spacing = size.width / (barCount + 1)
                val barWidth = (spacing * 0.5f).coerceAtMost(36.dp.toPx())

                // Draw background horizontal guide lines at 50% and 100%
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.2f),
                    start = Offset(0f, chartHeight * 0.5f),
                    end = Offset(size.width, chartHeight * 0.5f),
                    strokeWidth = 1.dp.toPx()
                )

                reversedSessions.forEachIndexed { index, session ->
                    val total = session.records.size.coerceAtLeast(1)
                    val present = session.records.values.count { it.status == AttendanceStatus.PRESENT }
                    val late = session.records.values.count { it.status == AttendanceStatus.LATE }
                    val effectivePresent = present + (late * 0.75f)
                    val rate = (effectivePresent / total).coerceIn(0f, 1f)

                    val barHeight = chartHeight * rate
                    val x = spacing * (index + 1) - (barWidth / 2)
                    val y = chartHeight - barHeight

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            if (rate >= 0.8f) StatusPresent else if (rate >= 0.6f) StatusLate else StatusAbsent,
                            Color(0xFF1E3A8A)
                        ),
                        startY = y,
                        endY = chartHeight
                    )

                    // Draw rounded bar
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw date label underneath
                    val label = sdf.format(Date(session.dateMillis))
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawText(label, x + (barWidth / 2), size.height - 4.dp.toPx(), paint)
                        // Percentage text on top of bar
                        val pctText = "${(rate * 100).toInt()}%"
                        paint.textSize = 9.sp.toPx()
                        paint.color = android.graphics.Color.DKGRAY
                        drawText(pctText, x + (barWidth / 2), (y - 4.dp.toPx()).coerceAtLeast(12.dp.toPx()), paint)
                    }
                }
            }
        }
    }
}
