package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.AttendanceRecordEntity
import com.example.data.local.entity.AttendanceSessionEntity
import com.example.data.local.entity.GroupEntity
import com.example.data.local.entity.MemberEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CsvReportFormat(val title: String, val description: String) {
    DETAILED(
        title = "Detailed Log",
        description = "Every roll-call check-in with date, student, status & notes"
    ),
    MEMBER_SUMMARY(
        title = "Student Summary",
        description = "Aggregate statistics per member with totals & attendance rates"
    ),
    MATRIX_GRID(
        title = "Spreadsheet Grid (Matrix)",
        description = "Student roster as rows with session dates as columns"
    )
}

enum class DateFilterRange(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_7_DAYS("Last 7 Days")
}

object CsvExportHelper {

    fun escapeCsv(value: String): String {
        val sanitized = value.replace("\"", "\"\"")
        return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r")) {
            "\"$sanitized\""
        } else {
            sanitized
        }
    }

    fun generateCsv(
        group: GroupEntity,
        members: List<MemberEntity>,
        sessions: List<AttendanceSessionEntity>,
        records: List<AttendanceRecordEntity>,
        format: CsvReportFormat = CsvReportFormat.DETAILED,
        dateFilter: DateFilterRange = DateFilterRange.ALL
    ): String {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val exportTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Filter sessions and records by selected date range
        val filteredSessions = when (dateFilter) {
            DateFilterRange.ALL -> sessions
            DateFilterRange.THIS_MONTH -> {
                val currentMonth = todayStr.substring(0, minOf(7, todayStr.length))
                sessions.filter { it.date.startsWith(currentMonth) }
            }
            DateFilterRange.LAST_30_DAYS -> {
                val cutoff = calculateDaysAgo(30)
                sessions.filter { it.date >= cutoff }
            }
            DateFilterRange.LAST_7_DAYS -> {
                val cutoff = calculateDaysAgo(7)
                sessions.filter { it.date >= cutoff }
            }
        }.sortedBy { it.date }

        val validSessionIds = filteredSessions.map { it.id }.toSet()
        val filteredRecords = records.filter { it.sessionId in validSessionIds }

        return when (format) {
            CsvReportFormat.DETAILED -> generateDetailedCsv(
                group = group,
                sessions = filteredSessions,
                records = filteredRecords,
                exportTimestamp = exportTimestamp,
                dateFilter = dateFilter
            )
            CsvReportFormat.MEMBER_SUMMARY -> generateSummaryCsv(
                group = group,
                members = members,
                sessions = filteredSessions,
                records = filteredRecords,
                exportTimestamp = exportTimestamp,
                dateFilter = dateFilter
            )
            CsvReportFormat.MATRIX_GRID -> generateMatrixCsv(
                group = group,
                members = members,
                sessions = filteredSessions,
                records = filteredRecords,
                exportTimestamp = exportTimestamp,
                dateFilter = dateFilter
            )
        }
    }

    private fun generateDetailedCsv(
        group: GroupEntity,
        sessions: List<AttendanceSessionEntity>,
        records: List<AttendanceRecordEntity>,
        exportTimestamp: String,
        dateFilter: DateFilterRange
    ): String = buildString {
        // Metadata Header Comments
        appendLine("# AttendEase Attendance Report")
        appendLine("# Group: ${escapeCsv(group.name)} (Category: ${escapeCsv(group.category)})")
        appendLine("# Exported At: $exportTimestamp")
        appendLine("# Date Range: ${dateFilter.label}")
        appendLine("# Total Sessions: ${sessions.size}")
        appendLine("# Total Check-ins Recorded: ${records.size}")
        appendLine()

        // CSV Column Headers
        appendLine("Date,Group Name,Category,Student Name,Student Identifier,Status,Member Note,Session Note")

        // Map sessionId to session note
        val sessionNotesMap = sessions.associate { it.id to it.notes }

        // Records sorted chronologically by date, then student name
        records.sortedWith(compareBy({ it.date }, { it.memberName })).forEach { rec ->
            val sNote = sessionNotesMap[rec.sessionId].orEmpty()
            append(escapeCsv(rec.date)).append(",")
            append(escapeCsv(group.name)).append(",")
            append(escapeCsv(group.category)).append(",")
            append(escapeCsv(rec.memberName)).append(",")
            // Look up identifier if possible, or blank
            append(escapeCsv(rec.memberId.toString())).append(",")
            append(escapeCsv(rec.status)).append(",")
            append(escapeCsv(rec.note)).append(",")
            append(escapeCsv(sNote))
            appendLine()
        }
    }

    private fun generateSummaryCsv(
        group: GroupEntity,
        members: List<MemberEntity>,
        sessions: List<AttendanceSessionEntity>,
        records: List<AttendanceRecordEntity>,
        exportTimestamp: String,
        dateFilter: DateFilterRange
    ): String = buildString {
        appendLine("# AttendEase Student Attendance Summary")
        appendLine("# Group: ${escapeCsv(group.name)} (${escapeCsv(group.category)})")
        appendLine("# Exported At: $exportTimestamp")
        appendLine("# Filter: ${dateFilter.label}")
        appendLine("# Total Sessions Conducted: ${sessions.size}")
        appendLine("# Total Members: ${members.size}")
        appendLine()

        appendLine("Student Name,Identifier,Group,Category,Total Sessions,Present Count,Late Count,Absent Count,Excused Count,Attendance Rate (%)")

        val totalSessions = sessions.size

        members.sortedBy { it.name }.forEach { member ->
            val memberRecords = records.filter { it.memberId == member.id }
            val presentCount = memberRecords.count { it.status == "PRESENT" }
            val lateCount = memberRecords.count { it.status == "LATE" }
            val absentCount = memberRecords.count { it.status == "ABSENT" }
            val excusedCount = memberRecords.count { it.status == "EXCUSED" }
            val totalMarked = memberRecords.size

            val effectiveSessions = if (totalSessions > 0) totalSessions else totalMarked
            val rate = if (effectiveSessions > 0) {
                ((presentCount + lateCount) * 100) / effectiveSessions
            } else 0

            append(escapeCsv(member.name)).append(",")
            append(escapeCsv(member.identifier)).append(",")
            append(escapeCsv(group.name)).append(",")
            append(escapeCsv(group.category)).append(",")
            append(effectiveSessions).append(",")
            append(presentCount).append(",")
            append(lateCount).append(",")
            append(absentCount).append(",")
            append(excusedCount).append(",")
            append("$rate%")
            appendLine()
        }
    }

    private fun generateMatrixCsv(
        group: GroupEntity,
        members: List<MemberEntity>,
        sessions: List<AttendanceSessionEntity>,
        records: List<AttendanceRecordEntity>,
        exportTimestamp: String,
        dateFilter: DateFilterRange
    ): String = buildString {
        appendLine("# AttendEase Attendance Matrix")
        appendLine("# Group: ${escapeCsv(group.name)} (${escapeCsv(group.category)})")
        appendLine("# Exported At: $exportTimestamp")
        appendLine("# Filter: ${dateFilter.label}")
        appendLine()

        val sortedDates = sessions.map { it.date }.distinct().sorted()

        // Header Row: Student Name, Identifier, <Dates...>, Present, Late, Absent, Excused, Rate (%)
        val header = mutableListOf("Student Name", "Identifier")
        header.addAll(sortedDates)
        header.add("Total Present")
        header.add("Total Late")
        header.add("Total Absent")
        header.add("Total Excused")
        header.add("Attendance Rate (%)")
        appendLine(header.joinToString(",") { escapeCsv(it) })

        // Map (memberId, date) -> status abbreviation
        val recordMap = records.associate { (it.memberId to it.date) to it.status }

        members.sortedBy { it.name }.forEach { member ->
            val row = mutableListOf<String>()
            row.add(member.name)
            row.add(member.identifier)

            var present = 0
            var late = 0
            var absent = 0
            var excused = 0

            sortedDates.forEach { date ->
                val status = recordMap[member.id to date]
                val symbol = when (status) {
                    "PRESENT" -> { present++; "Present (P)" }
                    "LATE" -> { late++; "Late (L)" }
                    "ABSENT" -> { absent++; "Absent (A)" }
                    "EXCUSED" -> { excused++; "Excused (E)" }
                    else -> "-"
                }
                row.add(symbol)
            }

            val totalSessions = sortedDates.size
            val rate = if (totalSessions > 0) ((present + late) * 100) / totalSessions else 0

            row.add(present.toString())
            row.add(late.toString())
            row.add(absent.toString())
            row.add(excused.toString())
            row.add("$rate%")

            appendLine(row.joinToString(",") { escapeCsv(it) })
        }
    }

    fun shareCsvFile(
        context: Context,
        groupName: String,
        csvContent: String,
        reportFormat: CsvReportFormat
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val cleanGroupName = groupName.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val formatSlug = reportFormat.name.lowercase(Locale.getDefault())
            val fileName = "Attendance_${cleanGroupName}_${formatSlug}_${dateStr}.csv"
            val file = File(exportDir, fileName)

            // Write with UTF-8 BOM so Microsoft Excel automatically recognises UTF-8 encoding
            file.outputStream().use { fos ->
                fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Attendance CSV Report: $groupName")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the attendance CSV data for '$groupName' (${reportFormat.title}).\nExported via AttendEase."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Attendance CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun copyCsvToClipboard(context: Context, csvContent: String, groupName: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Attendance CSV - $groupName", csvContent)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun calculateDaysAgo(days: Int): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
}
